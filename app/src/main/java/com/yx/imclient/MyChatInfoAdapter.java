package com.yx.imclient;

import android.graphics.Color;


import com.chad.library.adapter.base.BaseMultiItemQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.yx.imclient.entity.ChatInfo;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Author by YX, Date on 2020/10/29.
 */
public class MyChatInfoAdapter extends BaseMultiItemQuickAdapter<ChatInfo, BaseViewHolder> {

    public MyChatInfoAdapter(List<ChatInfo> data) {
        super(data);
        addItemType(ChatInfo.RECEIVE_TYPE,R.layout.item_chat_receive_text);
        addItemType(ChatInfo.SEND_TYPE,R.layout.item_chat_send_text);
    }

    @Override
    protected void convert(BaseViewHolder helper, ChatInfo item) {
        helper.setText(R.id.tv_sendtime,formatTime(item.getTime()));
        helper.setVisible(R.id.tv_content,true);
        helper.setText(R.id.tv_content,item.getData().getCons());
        switch (helper.getItemViewType()){
            case ChatInfo.RECEIVE_TYPE:
                helper.setVisible(R.id.tv_display_name,true);
                helper.setText(R.id.tv_display_name,item.getData().getFrom_fd());
                break;
            case ChatInfo.SEND_TYPE:
                helper.setText(R.id.tv_isRead,"已读");
                helper.setTextColor(R.id.tv_isRead,Color.GRAY);
                break;
        }
    }
    /**
     * 将毫秒数转为日期格式
     *
     * @param timeMillis
     * @return
     */
    private String formatTime(String timeMillis) {
        long timeMillisl=Long.parseLong(timeMillis);
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = new Date(timeMillisl);
        return simpleDateFormat.format(date);
    }

}
