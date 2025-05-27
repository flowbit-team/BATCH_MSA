package com.example.batchservice.constants;

public final class AppConstants {
    private AppConstants() {
        // Prevent instantiation
    }

    public static final class Template {
        public static final String NEWS_DATA_PLACEHOLDER = "##NEWS_DATA_PLACE##";
        public static final String KEYWORDS_PLACEHOLDER = "##KEYWORDS_PLACE##";
        public static final String BASE_TEMPLATE_KEY = "base_template";
    }

    public static final class S3 {
        private static final String BASE_URL = "https://likelionvideo.s3.ap-northeast-2.amazonaws.com/";
        
        public static final String BITCOIN_IMAGE = BASE_URL + "bitcoin.png";
        public static final String ETHEREUM_IMAGE = BASE_URL + "ethrium.png";
        public static final String RIPPLE_IMAGE = BASE_URL + "ripple.png";
        public static final String FLOWBIT_IMAGE = BASE_URL + "flowbit.png";
        public static final String UP_BADGE = BASE_URL + "up_badge.png";
        public static final String DOWN_BADGE = BASE_URL + "down_badge.png";
    }

    public static final class Format {
        public static final String PRICE_FORMAT = "#,##0";
        public static final String CHANGE_RATE_FORMAT = "%+.2f%%";
    }

    public static final class CryptoSymbols {
        public static final String BTC = "BTC";
        public static final String ETH = "ETH";
        public static final String XRP = "XRP";
    }
} 