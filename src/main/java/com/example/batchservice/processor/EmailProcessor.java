package com.example.batchservice.processor;

<<<<<<< Updated upstream
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Component
public class EmailProcessor implements ItemProcessor<String, String> {

    @Override
    public String process(String email) {
        return "Hello " + email + ", this is your daily update!";
=======
import com.example.batchservice.constants.AppConstants;
import com.example.batchservice.dto.CryptoData;
import com.example.batchservice.dto.NewsData;
import com.example.batchservice.service.ApiService;
import com.example.batchservice.service.TemplateCache;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.text.DecimalFormat;
import java.util.List;

@Component
public class EmailProcessor {
    private static final Logger logger = LoggerFactory.getLogger(EmailProcessor.class);
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat(AppConstants.Format.PRICE_FORMAT);

    private final TemplateEngine templateEngine;
    private final ApiService apiService;
    private final TemplateCache templateCache;

    public EmailProcessor(TemplateEngine templateEngine, ApiService apiService, TemplateCache templateCache) {
        this.templateEngine = templateEngine;
        this.apiService = apiService;
        this.templateCache = templateCache;
    }

    public String generateDiscordMessage() {
        try {
            JsonNode priceRoot = apiService.fetchCryptoPriceData();
            List<NewsData> newsDataList = apiService.fetchNewsData(List.of("비트코인", "이더리움", "리플"));
            return buildDiscordMessage(priceRoot, newsDataList);
        } catch (Exception e) {
            logger.error("Error generating Discord message", e);
            return "데이터를 가져오는 데 오류가 발생했습니다.";
        }
    }

    private String buildDiscordMessage(JsonNode priceRoot, List<NewsData> newsDataList) {
        StringBuilder discordMessage = new StringBuilder()
            .append("**플로우빗 예측가격과 최신뉴스 업데이트** \n\n")
            .append("| 암호화폐 | 실제 가격 (원) | 예측 가격 (원) | 가격 변동 |\n")
            .append("|----------|----------------|----------------|-----------|\n")
            .append(formatCryptoDataForMarkdown(priceRoot))
            .append("\n**최신 뉴스 업데이트**:\n");

        newsDataList.forEach(news -> 
            discordMessage.append("[").append(news.getTitle()).append("](")
                .append(news.getLink()).append(")\n")
        );

        return discordMessage.toString();
    }

    public String generateEmailTemplate(List<String> keywords) {
        try {
            if (!templateCache.hasBaseTemplate()) {
                templateCache.setBaseTemplate(buildBaseTemplate());
            }

            String baseTemplate = templateCache.getBaseTemplate();
            if (baseTemplate == null) {
                logger.error("Failed to retrieve base template from cache");
                return generateErrorTemplate();
            }

            return baseTemplate
                .replace(AppConstants.Template.KEYWORDS_PLACEHOLDER, buildKeywordsTemplate(keywords))
                .replace(AppConstants.Template.NEWS_DATA_PLACEHOLDER, buildNewsDataTemplate(keywords));
        } catch (Exception e) {
            logger.error("Error generating email template", e);
            return generateErrorTemplate();
        }
    }

    private String generateErrorTemplate() {
        return "<html><body><p>이메일 템플릿 생성 오류</p></body></html>";
    }

    private String buildBaseTemplate() {
        Context context = new Context();
        JsonNode priceRoot = apiService.fetchCryptoPriceData();
        
        context.setVariable("cryptoData", extractCryptoData(priceRoot));
        context.setVariable("newsDataPlaceHolder", AppConstants.Template.NEWS_DATA_PLACEHOLDER);
        context.setVariable("keywordsPlaceHolder", AppConstants.Template.KEYWORDS_PLACEHOLDER);

        return templateEngine.process("cryptoEmailTemplate", context);
    }

    private String buildNewsDataTemplate(List<String> keywords) {
        Context context = new Context();
        List<NewsData> newsDataList = apiService.fetchNewsData(keywords);
        context.setVariable("newsData", newsDataList);
        return templateEngine.process("newsDataTemplate", context);
    }

    private String buildKeywordsTemplate(List<String> keywords) {
        Context context = new Context();
        context.setVariable("keywords", keywords);
        return templateEngine.process("keywordsTemplate", context);
    }

    private String formatCryptoDataForMarkdown(JsonNode rootNode) {
        StringBuilder formattedData = new StringBuilder();
        rootNode.fields().forEachRemaining(entry -> {
            String crypto = entry.getKey();
            JsonNode data = entry.getValue();
            JsonNode actualData = data.get("actual_data");
            JsonNode predictedData = data.get("predicted_data");

            if (actualData != null && predictedData != null) {
                processCryptoDataForMarkdown(formattedData, crypto, actualData, predictedData);
            }
        });
        return formattedData.toString();
    }

    private void processCryptoDataForMarkdown(StringBuilder formattedData, String crypto, 
            JsonNode actualData, JsonNode predictedData) {
        Double actualPrice = actualData.get("close_price").asDouble();
        Double predictedPrice = predictedData.get("predicted_krw").asDouble();
        String priceChange = getPriceChange(actualPrice, predictedPrice);

        formattedData.append("| ").append(crypto).append(" | ")
            .append(formatPrice(actualPrice)).append(" | ")
            .append(formatPrice(predictedPrice)).append(" | ")
            .append(priceChange).append(" |\n");
    }

    private CryptoData extractCryptoData(JsonNode rootNode) {
        CryptoData cryptoData = new CryptoData();
        rootNode.fields().forEachRemaining(entry -> 
            processCryptoDataEntry(cryptoData, entry.getKey(), entry.getValue())
        );
        return cryptoData;
    }

    private void processCryptoDataEntry(CryptoData cryptoData, String crypto, JsonNode data) {
        JsonNode actualData = data.get("actual_data");
        JsonNode predictedData = data.get("predicted_data");

        if (actualData != null && predictedData != null) {
            Double actualPriceVal = actualData.get("close_price").asDouble();
            Double predictedPriceVal = predictedData.get("predicted_krw").asDouble();

            cryptoData.addCrypto(
                crypto,
                formatPrice(actualPriceVal),
                actualData.get("timestamp").asText(),
                formatPrice(predictedPriceVal),
                predictedData.get("timestamp").asText(),
                getImagePath(crypto),
                calculateChangeRate(actualPriceVal, predictedPriceVal),
                getPriceBadgeUrl(actualPriceVal, predictedPriceVal)
            );
        }
    }

    private String getPriceChange(Double actualPrice, Double predictedPrice) {
        if (actualPrice == null || predictedPrice == null) {
            return "";
        }
        return predictedPrice > actualPrice ? "(상승)" : 
               predictedPrice < actualPrice ? "(하락)" : "(변동 없음)";
    }

    private String calculateChangeRate(Double actual, Double predicted) {
        if (actual == null || predicted == null) return "0.00%";
        double change = (predicted - actual) / actual * 100;
        return String.format(AppConstants.Format.CHANGE_RATE_FORMAT, change);
    }

    private String formatPrice(Double price) {
        return price != null ? DECIMAL_FORMAT.format(price) : "N/A";
    }

    private String getImagePath(String crypto) {
        switch (crypto.toUpperCase()) {
            case AppConstants.CryptoSymbols.BTC:
                return AppConstants.S3.BITCOIN_IMAGE;
            case AppConstants.CryptoSymbols.ETH:
                return AppConstants.S3.ETHEREUM_IMAGE;
            case AppConstants.CryptoSymbols.XRP:
                return AppConstants.S3.RIPPLE_IMAGE;
            default:
                return AppConstants.S3.FLOWBIT_IMAGE;
        }
>>>>>>> Stashed changes
    }

    private String getPriceBadgeUrl(Double actualPrice, Double predictedPrice) {
        return predictedPrice > actualPrice ? AppConstants.S3.UP_BADGE : AppConstants.S3.DOWN_BADGE;
    }
}
