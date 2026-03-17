package com.winekiosk.dto;

import java.math.BigDecimal;

public class WineFilterRequest {

    private String wineType;
    private String country;
    private String region;
    private String grapeVariety;
    private String sweetness;
    private String body;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private BigDecimal minRating;
    private String searchText;

    public WineFilterRequest() {}

    public String getWineType() { return wineType; }
    public void setWineType(String wineType) { this.wineType = wineType; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }

    public String getGrapeVariety() { return grapeVariety; }
    public void setGrapeVariety(String grapeVariety) { this.grapeVariety = grapeVariety; }

    public String getSweetness() { return sweetness; }
    public void setSweetness(String sweetness) { this.sweetness = sweetness; }

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }

    public BigDecimal getMinPrice() { return minPrice; }
    public void setMinPrice(BigDecimal minPrice) { this.minPrice = minPrice; }

    public BigDecimal getMaxPrice() { return maxPrice; }
    public void setMaxPrice(BigDecimal maxPrice) { this.maxPrice = maxPrice; }

    public BigDecimal getMinRating() { return minRating; }
    public void setMinRating(BigDecimal minRating) { this.minRating = minRating; }

    public String getSearchText() { return searchText; }
    public void setSearchText(String searchText) { this.searchText = searchText; }
}
