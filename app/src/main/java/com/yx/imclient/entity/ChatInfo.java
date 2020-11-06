package com.yx.imclient.entity;

import com.chad.library.adapter.base.entity.MultiItemEntity;

/**
 * Author by YX, Date on 2020/10/27.
 */
public class ChatInfo implements MultiItemEntity {

    /**
     * status : true
     * message : 接收到信息！
     * data : {"cons":"er","from_fd":"446"}
     * code : 20001
     */
    private String time;
    private int isMeSend;//0是对方发送 1是自己发送
    private int isRead;//是否已读（0未读 1已读）
    private boolean status;
    private String message;
    private DataBean data;
    private int code;
    public static final int RECEIVE_TYPE = 1;
    public static final int SEND_TYPE = 2;
    private int itemType;

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public int getIsMeSend() {
        return isMeSend;
    }

    public void setIsMeSend(int isMeSend) {
        this.isMeSend = isMeSend;
    }

    public int getIsRead() {
        return isRead;
    }

    public void setIsRead(int isRead) {
        this.isRead = isRead;
    }

    public void setItemType(int itemType) {
        this.itemType = itemType;
    }

    public boolean isStatus() {
        return status;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public DataBean getData() {
        return data;
    }

    public void setData(DataBean data) {
        this.data = data;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    @Override
    public int getItemType() {
        return itemType;
    }

    public static class DataBean {
        /**
         * cons : er
         * from_fd : 446
         */

        private String cons;
        private String from_fd;

        public String getCons() {
            return cons;
        }

        public void setCons(String cons) {
            this.cons = cons;
        }

        public String getFrom_fd() {
            return from_fd;
        }

        public void setFrom_fd(String from_fd) {
            this.from_fd = from_fd;
        }
    }
}
