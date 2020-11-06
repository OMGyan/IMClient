package com.yx.imclient.im;

import android.util.Log;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;

/**
 * Author by YX, Date on 2020/10/27.
 */
public class MyWebSocketClient extends WebSocketClient {

    private String Tag = "MyWebSocketClient";

    //初始化WebSocketClient时传入WebSocket Uri地址
    public MyWebSocketClient(URI serverUri) {
        super(serverUri);
    }

    //websocket连接开启时调用
    @Override
    public void onOpen(ServerHandshake handshakedata) {
        Log.v(Tag,"onOpen()");
    }
    //websocket接收到消息时调用
    @Override
    public void onMessage(String message) {
        Log.v(Tag,"onMessage()");
    }
    //websocket连接断开时调用
    @Override
    public void onClose(int code, String reason, boolean remote) {
        Log.v(Tag,"onClose()");
    }
    //websocket连接出错时调用
    @Override
    public void onError(Exception ex) {
        Log.v(Tag,"onError() "+ex.toString());
    }

}
