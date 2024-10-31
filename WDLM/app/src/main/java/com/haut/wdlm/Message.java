package com.haut.wdlm;

import android.graphics.Bitmap;

public class Message {
    private String content;
    private Bitmap image;
    private boolean isSent;

    public Message(String content, Bitmap image, boolean isSent) {
        this.content = content;
        this.image = image;
        this.isSent = isSent;
    }

    public String getContent() {
        return content;
    }

    public Bitmap getImage() {
        return image;
    }

    public boolean isSent() {
        return isSent;
    }
}