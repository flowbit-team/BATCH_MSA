package com.example.batchservice.processor;

import com.example.batchservice.dto.CryptoData;
import com.example.batchservice.dto.NewsData;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jsoup.Jsoup;
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

    public String generateDiscordMessage() {
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

            // 예측 가격과 실제 가격 마크다운 테이블 추가
            StringBuilder discordMessage = new StringBuilder();
            discordMessage.append("**플로우빗 예측가격과 최신뉴스 업데이트** \n\n");

            // 마크다운 테이블 헤더
            discordMessage.append("| 암호화폐 | 실제 가격 (원) | 예측 가격 (원) | 가격 변동 |\n");
            discordMessage.append("|----------|----------------|----------------|-----------|\n");
            discordMessage.append(formatCryptoDataForMarkdown(priceRoot));

            // 최신 뉴스 추가
            discordMessage.append("\n**최신 뉴스 업데이트**:\n");
            for (NewsData news : newsDataList) {
                discordMessage.append("[").append(news.getTitle()).append("](").append(news.getLink()).append(")\n");
            }

            return discordMessage.toString();

        } catch (Exception e) {
            e.printStackTrace();
            return "데이터를 가져오는 데 오류가 발생했습니다.";
        }
    }

    private String formatCryptoDataForMarkdown(JsonNode rootNode) {
        StringBuilder formattedData = new StringBuilder();
        rootNode.fields().forEachRemaining(entry -> {
            String crypto = entry.getKey();
            JsonNode data = entry.getValue();
            JsonNode actualData = data.get("actual_data");
            JsonNode predictedData = data.get("predicted_data");

            if (actualData != null && predictedData != null) {
                Double actualPrice = actualData.get("close_price").asDouble();
                Double predictedPrice = predictedData.get("predicted_krw").asDouble();
                String priceChange = getPriceChange(actualPrice, predictedPrice);  // 가격 변동 (상승/하락 여부)
                String changeRate = calculateChangeRate(actualPrice, predictedPrice);


                // 마크다운 테이블 행 추가
                formattedData.append("| ").append(crypto).append(" | ")
                        .append(formatPrice(actualPrice)).append(" | ")
                        .append(formatPrice(predictedPrice)).append(" | ")
                        .append(priceChange).append(" |\n");
            }
        });
        return formattedData.toString();
    }

    /**
     * 가격 차이 계산하여 상승/하락 여부를 반환하는 메소드
     */
    private String getPriceChange(Double actualPrice, Double predictedPrice) {
        if (actualPrice == null || predictedPrice == null) {
            return "";
        }

        if (predictedPrice > actualPrice) {
            return "(상승)";
        } else if (predictedPrice < actualPrice) {
            return "(하락)";
        } else {
            return "(변동 없음)";
        }
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

            Double actualPriceVal = actualData != null ? actualData.get("close_price").asDouble() : null;
            Double predictedPriceVal = predictedData != null ? predictedData.get("predicted_krw").asDouble() : null;

            String actualPrice = formatPrice(actualPriceVal);
            String predictedPrice = formatPrice(predictedPriceVal);
            String changeRate = calculateChangeRate(actualPriceVal, predictedPriceVal);
            String iconName = changeRate.startsWith("+") ? "up_badge.svg" : "down_badge.svg";
            String iconUrl = "https://likelionvideo.s3.ap-northeast-2.amazonaws.com/" + iconName; // s3

            cryptoData.addCrypto(
                    crypto,
                    actualPrice,
                    actualData != null ? actualData.get("timestamp").asText() : null,
                    predictedPrice,
                    predictedData != null ? predictedData.get("timestamp").asText() : null,
                    getImagePath(crypto),
                    changeRate,
                    iconUrl
            );

        });
        return cryptoData;
    }

    private String calculateChangeRate(Double actual, Double predicted) {
        if (actual == null || predicted == null) return "0.00%";
        double change = (predicted - actual) / actual * 100;
        return String.format("%+.2f%%", change);
    }


    private List<NewsData> extractNewsData(JsonNode rootNode, String tag) {
        List<NewsData> newsDataList = new ArrayList<>();
        rootNode.get("data").get("content").forEach(item -> {
            String title = Jsoup.parse(item.get("title").asText()).text();
            String description = Jsoup.parse(item.get("description").asText()).text();
            String link = item.get("link").asText();
            String rawImageUrl = item.get("img").asText();

            // 이미지 URL 정제 로직
            String imageUrl = refineImageUrl(rawImageUrl);

            newsDataList.add(new NewsData(
                    title,
                    link,
                    description,
                    imageUrl,
                    tag
            ));
        });
        return newsDataList;
    }

    private String refineImageUrl(String rawUrl) {
        if (rawUrl == null || rawUrl.isEmpty()) return "";

        // "http://..." 안에 "//www." 또는 "/www." 중복이 있는 경우 처리
        int wwwIndex = rawUrl.indexOf("//www.");
        if (wwwIndex == -1) {
            wwwIndex = rawUrl.indexOf("/www.");
        }

        if (wwwIndex != -1) {
            return "https://" + rawUrl.substring(wwwIndex + 2);  // www. 포함해서 뒤쪽만 사용
        }

        return rawUrl; // 문제가 없는 경우 그대로 반환
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

