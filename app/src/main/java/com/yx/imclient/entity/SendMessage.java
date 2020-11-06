package com.yx.imclient.entity;

/**
 * Author by YX, Date on 2020/10/24.
 */
public class SendMessage {
    private String fd;
    private String cons;
    private int code;
    private String from_fd;

    public String getFrom_fd() {
        return from_fd;
    }

    public void setFrom_fd(String from_fd) {
        this.from_fd = from_fd;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getFd() {
        return fd;
    }

    public void setFd(String fd) {
        this.fd = fd;
    }

    public String getCons() {
        return cons;
    }

    public void setCons(String cons) {
        this.cons = cons;
    }
}
