package com.yx.imclient.im;

import android.annotation.SuppressLint;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.util.Log;
import android.widget.Toast;

import com.google.gson.Gson;
import com.yx.imclient.IChatMessageCallback;
import com.yx.imclient.MainActivity;
import com.yx.imclient.R;
import com.yx.imclient.entity.ChatInfo;
import com.yx.imclient.entity.SendMessage;
import com.yx.imclient.util.ThreadManager;
import com.yx.imclient.util.gson.YXGson;

import org.java_websocket.handshake.ServerHandshake;


import java.lang.ref.WeakReference;
import java.net.URI;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import static androidx.core.app.NotificationCompat.VISIBILITY_PUBLIC;


public class MyWebSocketClientService extends Service {

    private MyWebSocketClientBinder mBinder = new MyWebSocketClientBinder();

    public MyWebSocketClient client;

    private final static int GRAY_SERVICE_ID = 1001;

    private final MyHandler myHandler = new MyHandler(Looper.getMainLooper(),this);
    private String CHANNEL_ONE_ID = "CHANNEL_ONE_ID";
    private String CHANNEL_ONE_NAME= "CHANNEL_ONE_NAME";
    //静态内部类加软引用的方式来防止handler内存泄漏
    private static class MyHandler extends Handler{

        private final WeakReference<MyWebSocketClientService> weakReference;

        public MyHandler(@NonNull Looper looper,MyWebSocketClientService service) {
            super(looper);
            this.weakReference = new WeakReference<>(service);
        }


    }

    private static final long HEART_BEAT_RATE = 10 * 1000;

    private WeakReference<IChatMessageCallback> iChatMessageCallbackWeakReference;

    public void setiChatMessageCallback(IChatMessageCallback iChatMessageCallback) {

        iChatMessageCallbackWeakReference = new WeakReference<>(iChatMessageCallback);

    }

    private Runnable heartBeatRunnable = new Runnable() {
        @Override
        public void run() {

            Log.v("MyWebSocketClientService", "心跳包检测websocket连接状态");

            if (client != null) {
                if (client.isClosed()) {
                    reconnectWebSocket();
                }
            } else {
                //如果client已为空，重新初始化连接
                client = null;
                initWebSocket();
            }
            //每隔一定的时间，对长连接进行一次心跳检测
            myHandler.postDelayed(this,HEART_BEAT_RATE);

        }
    };

    //灰色保活
    public static class GrayInnerService extends Service {

        @Override
        public int onStartCommand(Intent intent, int flags, int startId) {
            startForeground(GRAY_SERVICE_ID,new Notification());
            stopForeground(true);
            stopSelf();
            return super.onStartCommand(intent, flags, startId);
        }

        @Override
        public IBinder onBind(Intent intent) {
            return null;
        }
    }

    //重新连接websocket
    private void reconnectWebSocket() {
        myHandler.removeCallbacks(heartBeatRunnable);
        ThreadManager.getThreadPollProxy().execute(()->{
            try {
                Log.v("MyWebSocketClientService", "开启重连");
                client.reconnectBlocking();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        closeConnect();
        //移除所有回调和消息，防止内存泄漏
        myHandler.removeCallbacksAndMessages(null);
        if(iChatMessageCallbackWeakReference!=null){
            iChatMessageCallbackWeakReference.clear();
            iChatMessageCallbackWeakReference = null;
        }
    }

    /**
     * 发送消息
     *
     * @param msg
     */
    public void sendMsg(String df,String from_fd,String msg,int code) {
        if (null != client && client.isOpen()) {
            SendMessage message = new SendMessage();
            message.setFd(df);
            message.setFrom_fd(from_fd);
            message.setCons(msg);
            message.setCode(code);
            String s = new Gson().toJson(message);
            client.send(s);
        }
    }

    /**
     * 断开连接
     */
    private void closeConnect() {
        try {
            if (null != client) {
                client.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            client = null;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        return mBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        //初始化websocket
        initWebSocket();
        //每隔10秒进行一次WebSocket连接心跳检测
        myHandler.postDelayed(heartBeatRunnable,HEART_BEAT_RATE);
        //对服务进行保活处理、
        keepAliveService();
        return START_STICKY;
    }

    private void keepAliveService() {
        //设置service为前台服务，提高优先级
        if (Build.VERSION.SDK_INT < 18) {
            //Android4.3以下 ，隐藏Notification上的图标
            startForeground(GRAY_SERVICE_ID,new Notification());
        } else if (Build.VERSION.SDK_INT > 18 && Build.VERSION.SDK_INT < 25) {
            //Android4.3 - Android7.0，隐藏Notification上的图标
            Intent innerIntent = new Intent(this,GrayInnerService.class);
            startService(innerIntent);
            startForeground(GRAY_SERVICE_ID,new Notification());
        } else {
            //Android7.0以上app启动后通知栏会出现一条"正在运行"的通知
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                startForeground(GRAY_SERVICE_ID,new Notification());
            }else {
                NotificationChannel notificationChannel= new NotificationChannel(CHANNEL_ONE_ID,
                        CHANNEL_ONE_NAME, NotificationManager.IMPORTANCE_HIGH);
                NotificationManager notificationManager = getSystemService(NotificationManager.class);
                notificationManager.createNotificationChannel(notificationChannel);
                Notification notification= new NotificationCompat.Builder(this,CHANNEL_ONE_ID)
                        .build();
                startForeground(GRAY_SERVICE_ID,notification);
            }
        }
        acquireWakeLock();
    }

    PowerManager.WakeLock wakeLock;//锁屏唤醒
    //获取电源锁，保持该服务在屏幕熄灭时仍然获取CPU时，保持运行
    @SuppressLint("InvalidWakeLockTag")
    private void acquireWakeLock() {
        if (null == wakeLock) {
            PowerManager pm = (PowerManager) this.getSystemService(Context.POWER_SERVICE);
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE, "PostLocationService");
            if (null != wakeLock) {
                wakeLock.acquire();
            }
        }
    }

    /**
     * 检查锁屏状态，如果锁屏先点亮屏幕
     *
     * @param content
     */
    private void checkLockAndShowNotification(String name,String content) {
        //管理锁屏的一个服务
        KeyguardManager km = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        if (km.inKeyguardRestrictedInputMode()) {//锁屏
            //获取电源管理器对象
            PowerManager pm = (PowerManager) this.getSystemService(Context.POWER_SERVICE);
            if (!pm.isScreenOn()) {
                @SuppressLint("InvalidWakeLockTag") PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.ACQUIRE_CAUSES_WAKEUP |
                        PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "bright");
                wl.acquire();  //点亮屏幕
                wl.release();  //任务结束后释放
            }
            sendNotification(name,content);
        } else {
            sendNotification(name,content);
        }
    }


    /**
     * 发送通知
     *
     * @param content
     */
    private void sendNotification(String name,String content) {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.setClass(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationManager notifyManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        Notification notification = null;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            notification = new NotificationCompat.Builder(this)
                    .setAutoCancel(true)
                    // 设置该通知优先级
                    .setPriority(Notification.PRIORITY_MAX)
                    .setSmallIcon(R.drawable.icon)
                    .setContentTitle(name)
                    .setContentText(content)
                    .setVisibility(VISIBILITY_PUBLIC)
                    .setWhen(System.currentTimeMillis())
                    // 向通知添加声音、闪灯和振动效果
                    .setDefaults(Notification.DEFAULT_VIBRATE | Notification.DEFAULT_ALL | Notification.DEFAULT_SOUND)
                    .setContentIntent(pendingIntent)
                    .build();
        }else {
            NotificationChannel notificationChannel= new NotificationChannel(CHANNEL_ONE_ID,CHANNEL_ONE_NAME, NotificationManager.IMPORTANCE_MAX);
            notificationChannel.enableLights(true);
            notificationChannel.enableVibration(true);
            notificationChannel.setLightColor(Color.RED);
            notificationChannel.setShowBadge(true);

            notificationChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            notifyManager.createNotificationChannel(notificationChannel);
            notification = new Notification.Builder(this,CHANNEL_ONE_ID)
                    .setAutoCancel(true)
                    .setSmallIcon(R.drawable.icon)
                    .setContentTitle(name)
                    .setContentText(content)
                    .setWhen(System.currentTimeMillis())
                    .setContentIntent(pendingIntent)
                    .build();
        }
        notifyManager.notify(1,notification);//id要保证唯一
    }

    private void initWebSocket() {
        URI uri = URI.create("ws://8.129.0.252:9501");
        client = new MyWebSocketClient(uri){
            @Override
            public void onOpen(ServerHandshake handshakedata) {
                super.onOpen(handshakedata);
            }

            @Override
            public void onMessage(String message) {
                super.onMessage(message);
                ChatInfo info = YXGson.newGson().fromJson(message, ChatInfo.class);
                if(info.getCode()==20001){
                    if(iChatMessageCallbackWeakReference!=null){
                        iChatMessageCallbackWeakReference.get().onChatMessage(info);
                    }
                    checkLockAndShowNotification(info.getData().getFrom_fd(),info.getData().getCons());
                } else {
                    if(iChatMessageCallbackWeakReference!=null){
                        iChatMessageCallbackWeakReference.get().onNotifyInfo(info.getMessage());
                    }
                }
            }

            @Override
            public void onError(Exception ex) {
                super.onError(ex);
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                super.onClose(code, reason, remote);
            }
        };

        ThreadManager.getThreadPollProxy().execute(()->{
            //connectBlocking多出一个等待操作，会先连接再发送，否则未连接发送会报错
            try {
                client.connectBlocking();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
    }

    public class MyWebSocketClientBinder extends Binder{
        public MyWebSocketClientService getService(){
            return MyWebSocketClientService.this;
        }
    }

}
