package com.example.batchservice.processor;

import com.example.batchservice.dto.CryptoData;
import com.example.batchservice.dto.NewsData;
import com.fasterxml.jackson.core.JsonProcessingException;
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
    private static final String NEWS_DATA_PLACE_HOLDER = "##NEWS_DATA_PLACE##";
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#,##0");

    private final TemplateEngine templateEngine;
    private String cachedTemplate = null; //  캐시된 템플릿

    public EmailProcessor(TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    /**
     * 매일 오전 6시에 템플릿 캐시 초기화
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
    public String generateEmailTemplate(List<String> keywords) {
        try {
            if (cachedTemplate == null) {
                cachedTemplate = buildCachedTemplate();
            }
            String personalizedCryptoEmailTemplate = cachedTemplate.replace(NEWS_DATA_PLACE_HOLDER, buildNewsDataTemplate(keywords));
            System.out.println("[EmailProcessor] ✅ 새 이메일 템플릿이 생성되었습니다.");
            return personalizedCryptoEmailTemplate;
        } catch (Exception e) {
            e.printStackTrace();
            return "<html><body><p>이메일 템플릿 생성 오류</p></body></html>";
        }
    }

    private String buildCachedTemplate() throws JsonProcessingException {
        RestTemplate restTemplate = new RestTemplate();
        ObjectMapper objectMapper = new ObjectMapper();
        Context context = new Context();

        // 암호화폐 가격 API 호출
        String priceResponse = restTemplate.getForObject(PRICE_API_URL, String.class);
        JsonNode priceRoot = objectMapper.readTree(priceResponse);

        // 데이터를 템플릿에 전달
        context.setVariable("cryptoData", extractCryptoData(priceRoot));
        context.setVariable("newsDataPlaceHolder", NEWS_DATA_PLACE_HOLDER);

        // 템플릿 생성 후 캐싱
        return templateEngine.process("cryptoEmailTemplate", context);
    }

    private String buildNewsDataTemplate(List<String> keywords) throws JsonProcessingException {
        RestTemplate restTemplate = new RestTemplate();
        ObjectMapper objectMapper = new ObjectMapper();
        Context context = new Context();

        // 뉴스 데이터 API 호출
        List<NewsData> newsDataList = new ArrayList<>();
        for (String tag : keywords) {
            String newsResponse = restTemplate.getForObject(NEWS_API_URL + tag + "&page=0&size=3", String.class);
            JsonNode newsRoot = objectMapper.readTree(newsResponse);
            newsDataList.addAll(extractNewsData(newsRoot, tag));
        }
        context.setVariable("newsData", newsDataList);
        return templateEngine.process("newsDataTemplate", context);
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

