package com.example.hookyarn;

public class MessageModel {

    private String senderId;
    private String message;

    public MessageModel(){}

    public MessageModel(String senderId, String message) {
        this.senderId = senderId;
        this.message = message;
    }

    public String getSenderId() {
        return senderId;
    }

    public String getMessage() {
        return message;
    }
}