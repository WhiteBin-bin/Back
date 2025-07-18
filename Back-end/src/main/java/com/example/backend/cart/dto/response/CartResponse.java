package com.example.backend.cart.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

public class CartResponse {

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CartDetailResponse {
        private UUID cartId;
        private String region;
        private List<TourInfo> tours;
        private int totalCount;
        private long totalPrice;

        public static CartDetailResponse empty() {
            return new CartDetailResponse(null, "", List.of(), 0, 0L);
        }
    }

    @Getter
    @Builder
    public static class TourInfo {
        private UUID tourId;
        private java.math.BigDecimal longitude;
        private java.math.BigDecimal latitude;
        private String address;
        private String address2;
        private String zipcode;
        private String areaCode;
        private String cat1;
        private String cat2;
        private String cat3;
        private String createdTime;
        private String firstImage;
        private String firstImage2;
        private String cpyrhtDivCd;
        private String mapX;
        private String mapY;
        private String mlevel;
        private String modifiedTime;
        private String sigunguCode;
        private String tel;
        private String overview;
        private String lDongRegnCd;
        private String lDongSignguCd;
        private String lclsSystm1;
        private String lclsSystm2;
        private String lclsSystm3;
        private String contentId;
        private String contentTypeId;
        private String title;
        private String image;
        private String tema;
        private String category;
        private Long price;
        private String thema;
    }

    @Getter
    @AllArgsConstructor
    public static class AddTourResponse {
        private UUID tourId;
        private String message;
    }
}
