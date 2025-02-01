package com.example.batchservice.processor;

import com.example.batchservice.dto.CryptoData;
import com.example.batchservice.dto.NewsData;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jsoup.Jsoup;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

@Component
public class EmailProcessor {

    private static final String PRICE_API_URL = "https://api.flowbit.co.kr/bitcoin-service/predicted-value-list";
    private static final String NEWS_API_URL = "https://api.flowbit.co.kr/board-service/api/v1/news?sort=createdAt,desc&tag=";
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#,##0");

    private final TemplateEngine templateEngine;
    private String cachedTemplate = null; //  캐시된 템플릿

    public EmailProcessor(TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    /**
     *  매일 오전 6시에 템플릿 캐시 초기화
     */
    public void resetTemplateCache() {
        cachedTemplate = null;
        System.out.println("[EmailProcessor] 🔄 캐시된 이메일 템플릿이 초기화되었습니다. (7시)");
    }

    /**
     * 이메일 템플릿을 한 번만 생성하여 모든 구독자에게 사용
     */
    public String generateEmailTemplate() {
        if (cachedTemplate != null) {
            return cachedTemplate; //  이미 생성된 템플릿이 있으면 재사용
        }

        try {
            RestTemplate restTemplate = new RestTemplate();
            ObjectMapper objectMapper = new ObjectMapper();

            // 암호화폐 가격 API 호출
            String priceResponse = restTemplate.getForObject(PRICE_API_URL, String.class);
            JsonNode priceRoot = objectMapper.readTree(priceResponse);

            // 뉴스 데이터 API 호출
            List<NewsData> newsDataList = new ArrayList<>();
            for (String tag : List.of("비트코인", "이더리움", "리플")) {
                String newsResponse = restTemplate.getForObject(NEWS_API_URL + tag + "&page=0&size=3", String.class);
                JsonNode newsRoot = objectMapper.readTree(newsResponse);
                newsDataList.addAll(extractNewsData(newsRoot, tag));
            }

            // 데이터를 템플릿에 전달
            Context context = new Context();
            context.setVariable("cryptoData", extractCryptoData(priceRoot));
            context.setVariable("newsData", newsDataList);

            // 템플릿 생성 후 캐싱
            cachedTemplate = templateEngine.process("cryptoEmailTemplate", context);
            System.out.println("[EmailProcessor] ✅ 새 이메일 템플릿이 생성되었습니다.");
            return cachedTemplate;
        } catch (Exception e) {
            e.printStackTrace();
            return "<html><body><p>이메일 템플릿 생성 오류</p></body></html>";
        }
    }

    private CryptoData extractCryptoData(JsonNode rootNode) {
        CryptoData cryptoData = new CryptoData();
        rootNode.fields().forEachRemaining(entry -> {
            String crypto = entry.getKey();
            JsonNode data = entry.getValue();
            JsonNode actualData = data.get("actual_data");
            JsonNode predictedData = data.get("predicted_data");

            cryptoData.addCrypto(
                    crypto,
                    formatPrice(actualData != null ? actualData.get("close_price").asDouble() : null),
                    actualData != null ? actualData.get("timestamp").asText() : null,
                    formatPrice(predictedData != null ? predictedData.get("predicted_krw").asDouble() : null),
                    predictedData != null ? predictedData.get("timestamp").asText() : null,
                    getImagePath(crypto)
            );
        });
        return cryptoData;
    }

    private List<NewsData> extractNewsData(JsonNode rootNode, String tag) {
        List<NewsData> newsDataList = new ArrayList<>();
        rootNode.get("data").get("content").forEach(item -> {
            String title = Jsoup.parse(item.get("title").asText()).text();
            String description = Jsoup.parse(item.get("description").asText()).text();

            newsDataList.add(new NewsData(
                    title,
                    item.get("link").asText(),
                    description,
                    item.get("img").asText(),
                    tag
            ));
        });
        return newsDataList;
    }

    private String formatPrice(Double price) {
        return (price == null) ? "-" : DECIMAL_FORMAT.format(price);
    }

    private String getImagePath(String crypto) {
        switch (crypto.toUpperCase()) {
            case "BTC":
                return "https://likelionvideo.s3.ap-northeast-2.amazonaws.com/bitcoin.png";
            case "ETH":
                return "https://likelionvideo.s3.ap-northeast-2.amazonaws.com/ethrium.webp";
            case "XRP":
                return "https://likelionvideo.s3.ap-northeast-2.amazonaws.com/ripple.png";
            default:
                return "https://likelionvideo.s3.ap-northeast-2.amazonaws.com/flowbit.png";
        }
    }
}

