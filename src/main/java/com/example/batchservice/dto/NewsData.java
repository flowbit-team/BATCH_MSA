package com.example.batchservice.dto;

public class NewsData {
    private final String title;
    private final String link;
    private final String description;
    private final String img;
    private final String pubDate;

    public NewsData(String title, String link, String description, String img, String pubDate) {
        this.title = title;
        this.link = link;
        this.description = description;
        this.img = img;
        this.pubDate = pubDate;
    }

    public String getTitle() {
        return title;
    }

    public String getLink() {
        return link;
    }

    public String getDescription() {
        return description;
    }

    public String getImg() {
        return img;
    }

    public String getPubDate() {
        return pubDate;
    }
}
