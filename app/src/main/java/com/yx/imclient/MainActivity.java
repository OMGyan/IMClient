package com.yx.imclient;

import android.annotation.TargetApi;
import android.app.AppOpsManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ApplicationInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.text.Editable;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.yx.imclient.entity.ChatInfo;
import com.yx.imclient.im.MyWebSocketClient;
import com.yx.imclient.im.MyWebSocketClientService;
import com.yx.imclient.util.StringUtils;

import org.slf4j.helpers.Util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnTextChanged;

public class MainActivity extends AppCompatActivity implements IChatMessageCallback {

    @BindView(R.id.chatmsg_recyclerview)
    RecyclerView chatmsgRecyclerview;
    @BindView(R.id.et_content)
    EditText etContent;
    @BindView(R.id.btn_send)
    Button btnSend;
    @BindView(R.id.et_login_num)
    EditText etLoginNum;
    @BindView(R.id.btn_login)
    Button btnLogin;

    private Context mContext;
    private MyWebSocketClientService.MyWebSocketClientBinder binder;
    private MyWebSocketClientService service;
    private MyWebSocketClient client;
    private List<ChatInfo> chatInfoList = new ArrayList<>();
    private String loginAccount;
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            Log.e("MainActivity", "服务与活动成功绑定");
            binder = ((MyWebSocketClientService.MyWebSocketClientBinder) iBinder);
            service = binder.getService();
            client = service.client;
            service.setiChatMessageCallback(MainActivity.this);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.e("MainActivity", "服务与活动成功断开");
        }
    };
    private MyChatInfoAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        getSupportActionBar().hide();
        mContext = MainActivity.this;
        //启动服务
        startJWebSClientService();
        //绑定服务
        bindService();
        //检测通知是否开启
        checkNotification(mContext);
        initView();
    }

    private void initView() {
        adapter = new MyChatInfoAdapter(chatInfoList);
        chatmsgRecyclerview.setLayoutManager(new LinearLayoutManager(this));
        chatmsgRecyclerview.setAdapter(adapter);
    }

    private void checkNotification(Context context) {
        if (!isNotificationEnabled(context)) {
            new AlertDialog.Builder(context).setTitle("温馨提示")
                    .setMessage("你还未开启系统通知，将影响消息的接收，要去开启吗？")
                    .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            setNotification(context);
                        }
                    }).setNegativeButton("取消", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                }
            }).show();
        }
    }

    /**
     * 如果没有开启通知，跳转至设置界面
     *
     * @param context
     */
    private void setNotification(Context context) {
        Intent localIntent = new Intent();
        //直接跳转到应用通知设置的代码：
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            localIntent.setAction("android.settings.APP_NOTIFICATION_SETTINGS");
            localIntent.putExtra("app_package", context.getPackageName());
            localIntent.putExtra("app_uid", context.getApplicationInfo().uid);
        } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT) {
            localIntent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            localIntent.addCategory(Intent.CATEGORY_DEFAULT);
            localIntent.setData(Uri.parse("package:" + context.getPackageName()));
        } else {
            //4.4以下没有从app跳转到应用通知设置页面的Action，可考虑跳转到应用详情页面,
            localIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            if (Build.VERSION.SDK_INT >= 9) {
                localIntent.setAction("android.settings.APPLICATION_DETAILS_SETTINGS");
                localIntent.setData(Uri.fromParts("package", context.getPackageName(), null));
            } else if (Build.VERSION.SDK_INT <= 8) {
                localIntent.setAction(Intent.ACTION_VIEW);
                localIntent.setClassName("com.android.settings", "com.android.setting.InstalledAppDetails");
                localIntent.putExtra("com.android.settings.ApplicationPkgName", context.getPackageName());
            }
        }
        context.startActivity(localIntent);
    }

    /**
     * 获取通知权限,监测是否开启了系统通知
     *
     * @param context
     */
    @TargetApi(Build.VERSION_CODES.KITKAT)
    private boolean isNotificationEnabled(Context context) {

        String CHECK_OP_NO_THROW = "checkOpNoThrow";
        String OP_POST_NOTIFICATION = "OP_POST_NOTIFICATION";

        AppOpsManager mAppOps = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
        ApplicationInfo appInfo = context.getApplicationInfo();
        String pkg = context.getApplicationContext().getPackageName();
        int uid = appInfo.uid;

        Class appOpsClass = null;
        try {
            appOpsClass = Class.forName(AppOpsManager.class.getName());
            Method checkOpNoThrowMethod = appOpsClass.getMethod(CHECK_OP_NO_THROW, Integer.TYPE, Integer.TYPE,
                    String.class);
            Field opPostNotificationValue = appOpsClass.getDeclaredField(OP_POST_NOTIFICATION);

            int value = (Integer) opPostNotificationValue.get(Integer.class);
            return ((Integer) checkOpNoThrowMethod.invoke(mAppOps, value, uid, pkg) == AppOpsManager.MODE_ALLOWED);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 绑定服务
     */
    private void bindService() {
        Intent bindIntent = new Intent(mContext, MyWebSocketClientService.class);
        bindService(bindIntent, serviceConnection, BIND_AUTO_CREATE);

    }

    /**
     * 启动服务（websocket客户端服务）
     */
    private void startJWebSClientService() {
        Intent innerIntent = new Intent(mContext, MyWebSocketClientService.class);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            startService(innerIntent);
        }else {
            startForegroundService(innerIntent);
        }


    }

    /**
     * websocket返回消息回调
     *
     * @param chatInfo
     */
    @Override
    public void onChatMessage(ChatInfo chatInfo) {
        runOnUiThread(()->{
            chatInfo.setItemType(ChatInfo.RECEIVE_TYPE);
            chatInfo.setTime(System.currentTimeMillis() + "");
            chatInfo.setIsMeSend(0);
            chatInfo.setIsRead(1);
            chatInfoList.add(chatInfo);
            adapter.notifyDataSetChanged();
            chatmsgRecyclerview.scrollToPosition(chatInfoList.size()-1);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        chatmsgRecyclerview.scrollToPosition(chatInfoList.size()-1);
    }

    @Override
    public void onNotifyInfo(String str) {
        runOnUiThread(()->Toast.makeText(mContext,str,Toast.LENGTH_SHORT).show());
    }

    @OnTextChanged(value = R.id.et_content)
    public void onTextChange() {
        if (etContent.getText().toString().length() > 0) {
            btnSend.setVisibility(View.VISIBLE);
        } else {
            btnSend.setVisibility(View.GONE);
        }
    }

    @OnClick({R.id.btn_send, R.id.btn_login})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.btn_send:
                String content = etContent.getText().toString();
                if (content.length() <= 0) {
                    StringUtils.showToast(mContext, "消息不能为空哟");
                    return;
                }

                if (client != null && client.isOpen()) {
                    service.sendMsg(etLoginNum.getText().toString(),loginAccount,content,800);
                    //暂时将发送的消息加入消息列表，实际以发送成功为准（也就是服务器返回你发的消息时）
                    ChatInfo chatMessage=new ChatInfo();
                    ChatInfo.DataBean dataBean = new ChatInfo.DataBean();
                    dataBean.setCons(content);
                    dataBean.setFrom_fd(loginAccount);
                    chatMessage.setData(dataBean);
                    chatMessage.setIsMeSend(1);
                    chatMessage.setIsRead(1);
                    chatMessage.setTime(System.currentTimeMillis()+"");
                    chatMessage.setItemType(ChatInfo.SEND_TYPE);
                    chatInfoList.add(chatMessage);
                    adapter.notifyDataSetChanged();
                    chatmsgRecyclerview.scrollToPosition(chatInfoList.size()-1);
                    etContent.setText("");
                } else {
                    StringUtils.showToast(mContext,"连接已断开，请稍等或重启App哟");
                }
                break;
            case R.id.btn_login:
                if(client != null && client.isOpen()){
                    if(etLoginNum.getText().toString().equals("")){
                        StringUtils.showToast(mContext,"请输入账号");
                    }else {
                        loginAccount = etLoginNum.getText().toString();
                        service.sendMsg("",loginAccount, "",801);
                    }
                }else {
                    StringUtils.showToast(mContext,"连接已断开，请稍等或重启App哟");
                }
                break;
        }
    }
}