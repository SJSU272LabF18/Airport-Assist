package com.example.caroline.airportnav.Bean;

public class Message {
    private String message;
    private String sender;
    public Message(String msg,String send){
        message=msg;
        sender=send;
    }

    public String getMessage() {
        return message;
    }

    public String getSender() {
        return sender;
    }
}
