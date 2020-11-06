package com.yx.imclient;

import com.yx.imclient.entity.ChatInfo;

/**
 * Author by YX, Date on 2020/10/28.
 */
public interface IChatMessageCallback {

    void onChatMessage(ChatInfo chatInfo);

    void onNotifyInfo(String str);

}
