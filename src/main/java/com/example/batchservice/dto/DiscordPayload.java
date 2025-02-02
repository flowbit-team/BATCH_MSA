package com.example.batchservice.dto;

public class DiscordPayload {
    private String content;

    public DiscordPayload(String content) {
        this.content = content;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
