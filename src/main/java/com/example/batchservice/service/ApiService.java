package com.example.batchservice.service;

import com.example.batchservice.config.ApiConfig;
import com.example.batchservice.dto.NewsData;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Service
public class ApiService {
    private static final Logger logger = LoggerFactory.getLogger(ApiService.class);
    private static final int MAX_RETRIES = 3;
    private static final int INITIAL_RETRY_DELAY = 1000;
    private static final int MAX_RETRY_DELAY = 5000;
    private static final ExecutorService executorService = Executors.newFixedThreadPool(5);
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final ApiConfig apiConfig;

    public ApiService(RestTemplate restTemplate, ObjectMapper objectMapper, ApiConfig apiConfig) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.apiConfig = apiConfig;
    }

    @Retryable(
        value = {RestClientException.class},
        maxAttempts = MAX_RETRIES,
        backoff = @Backoff(delay = INITIAL_RETRY_DELAY, multiplier = 2, maxDelay = MAX_RETRY_DELAY)
    )
    public JsonNode fetchCryptoPriceData() {
        try {
            String response = restTemplate.getForObject(apiConfig.getPriceApiUrl(), String.class);
            return objectMapper.readTree(response);
        } catch (Exception e) {
            logger.error("Failed to fetch crypto price data after {} retries", MAX_RETRIES, e);
            throw new RuntimeException("Failed to fetch crypto price data", e);
        }
    }

    public List<NewsData> fetchNewsData(List<String> tags) {
        List<CompletableFuture<List<NewsData>>> futures = tags.stream()
            .map(tag -> CompletableFuture.supplyAsync(() -> fetchNewsForTag(tag), executorService))
            .collect(Collectors.toList());

        return futures.stream()
            .map(CompletableFuture::join)
            .flatMap(List::stream)
            .collect(Collectors.toList());
    }

    @Retryable(
        value = {RestClientException.class},
        maxAttempts = MAX_RETRIES,
        backoff = @Backoff(delay = INITIAL_RETRY_DELAY, multiplier = 2, maxDelay = MAX_RETRY_DELAY)
    )
    private List<NewsData> fetchNewsForTag(String tag) {
        try {
            String url = String.format("%s?tag=%s&page=0&size=3", apiConfig.getNewsApiUrl(), tag);
            String response = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(response);
            return extractNewsData(root, tag);
        } catch (Exception e) {
            logger.error("Failed to fetch news data for tag: {} after {} retries", tag, MAX_RETRIES, e);
            return new ArrayList<>();
        }
    }

    private List<NewsData> extractNewsData(JsonNode rootNode, String tag) {
        List<NewsData> newsDataList = new ArrayList<>();
        if (rootNode != null && rootNode.has("data") && rootNode.get("data").has("content")) {
            rootNode.get("data").get("content").forEach(item -> {
                try {
                    newsDataList.add(createNewsData(item, tag));
                } catch (Exception e) {
                    logger.error("Error processing news item for tag: {}", tag, e);
                }
            });
        }
        return newsDataList;
    }

    private NewsData createNewsData(JsonNode item, String tag) {
        return new NewsData(
            Jsoup.parse(item.get("title").asText()).text(),
            item.get("link").asText(),
            Jsoup.parse(item.get("description").asText()).text(),
            refineImageUrl(item.get("img").asText()),
            tag
        );
    }

    private String refineImageUrl(String rawUrl) {
        if (rawUrl == null || rawUrl.isEmpty()) return "";
        return rawUrl.replaceAll("(?:https?:)?(?:/+www\\.|/www\\.)", "https://www.");
    }
} 