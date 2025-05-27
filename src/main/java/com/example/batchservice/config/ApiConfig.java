package com.example.batchservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApiConfig {
    @Value("${api.flowbit.base-url:https://api.flowbit.co.kr}")
    private String baseUrl;

    @Value("${api.flowbit.bitcoin-service.predicted-value-list:/bitcoin-service/predicted-value-list}")
    private String predictedValueListPath;

    @Value("${api.flowbit.board-service.news:/board-service/api/v1/news}")
    private String newsPath;

    public String getPriceApiUrl() {
        return baseUrl + predictedValueListPath;
    }

    public String getNewsApiUrl() {
        return baseUrl + newsPath;
    }
}