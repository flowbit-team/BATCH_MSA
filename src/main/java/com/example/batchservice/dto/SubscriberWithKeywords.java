package com.example.batchservice.dto;

import lombok.Data;

import java.util.List;

@Data
public class SubscriberWithKeywords {
    private String email;
    private List<String> keywords;
}
