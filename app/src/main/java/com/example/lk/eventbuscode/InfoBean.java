package com.example.lk.eventbuscode;

/**
 * Created by lk on 2018/10/8.
 */

public class InfoBean {
    private String message;
    private int  num;

    public InfoBean(String message, int num) {
        this.message = message;
        this.num = num;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getNum() {
        return num;
    }

    public void setNum(int num) {
        this.num = num;
    }
}
