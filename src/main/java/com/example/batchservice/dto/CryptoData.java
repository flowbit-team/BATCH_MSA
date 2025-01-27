package com.example.batchservice.dto;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class CryptoData {
    private final List<CryptoInfo> cryptos = new ArrayList<>();

    public void addCrypto(String name, String actualPrice, String actualDate, String predictedPrice, String predictedDate, String imageUrl) {
        cryptos.add(new CryptoInfo(name, actualPrice, actualDate, predictedPrice, predictedDate, imageUrl));
    }

    public List<CryptoInfo> getCryptos() {
        return cryptos;
    }

    @Getter
    public static class CryptoInfo {
        private final String name;
        private final String actualPrice;
        private final String actualDate;
        private final String predictedPrice;
        private final String predictedDate;
        private final String imageUrl;

        public CryptoInfo(String name, String actualPrice, String actualDate, String predictedPrice, String predictedDate, String imageUrl) {
            this.name = name;
            this.actualPrice = actualPrice;
            this.actualDate = actualDate;
            this.predictedPrice = predictedPrice;
            this.predictedDate = predictedDate;
            this.imageUrl = imageUrl;
        }

        public String getName() {
            return name;
        }

        public String getActualPrice() {
            return actualPrice;
        }

        public String getActualDate() {
            return actualDate;
        }

        public String getPredictedPrice() {
            return predictedPrice;
        }

        public String getPredictedDate() {
            return predictedDate;
        }

        public String getImageUrl() {
            return imageUrl;
        }
    }
}
