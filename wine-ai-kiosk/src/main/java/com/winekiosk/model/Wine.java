package com.winekiosk.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "wines", indexes = {
        @Index(name = "idx_wine_type", columnList = "wine_type"),
        @Index(name = "idx_country", columnList = "country"),
        @Index(name = "idx_region", columnList = "region"),
        @Index(name = "idx_grape_variety", columnList = "grape_variety"),
        @Index(name = "idx_sweetness", columnList = "sweetness"),
        @Index(name = "idx_price", columnList = "price"),
        @Index(name = "idx_rating", columnList = "rating")
})
public class Wine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "winery", length = 255)
    private String winery;

    @Column(name = "country", length = 100)
    private String country;

    @Column(name = "region", length = 100)
    private String region;

    @Column(name = "grape_variety", length = 100)
    private String grapeVariety;

    @Column(name = "wine_type", length = 50)
    private String wineType;

    @Column(name = "sweetness", length = 50)
    private String sweetness;

    @Column(name = "body", length = 50)
    private String body;

    @Column(name = "acidity", length = 50)
    private String acidity;

    @Column(name = "tannin", length = 50)
    private String tannin;

    @Column(name = "flavor_notes", length = 500)
    private String flavorNotes;

    @Column(name = "food_pairing", length = 500)
    private String foodPairing;

    @Column(name = "price", nullable = false, precision = 8, scale = 2)
    private BigDecimal price;

    @Column(name = "rating", precision = 3, scale = 1)
    private BigDecimal rating;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "description", length = 1000)
    private String description;

    public Wine() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getWinery() { return winery; }
    public void setWinery(String winery) { this.winery = winery; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }

    public String getGrapeVariety() { return grapeVariety; }
    public void setGrapeVariety(String grapeVariety) { this.grapeVariety = grapeVariety; }

    public String getWineType() { return wineType; }
    public void setWineType(String wineType) { this.wineType = wineType; }

    public String getSweetness() { return sweetness; }
    public void setSweetness(String sweetness) { this.sweetness = sweetness; }

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }

    public String getAcidity() { return acidity; }
    public void setAcidity(String acidity) { this.acidity = acidity; }

    public String getTannin() { return tannin; }
    public void setTannin(String tannin) { this.tannin = tannin; }

    public String getFlavorNotes() { return flavorNotes; }
    public void setFlavorNotes(String flavorNotes) { this.flavorNotes = flavorNotes; }

    public String getFoodPairing() { return foodPairing; }
    public void setFoodPairing(String foodPairing) { this.foodPairing = foodPairing; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public BigDecimal getRating() { return rating; }
    public void setRating(BigDecimal rating) { this.rating = rating; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    /**
     * Returns a concise summary of this wine for use in AI prompts.
     */
    public String toSummary() {
        return String.format(
                "[%d] %s by %s | %s | %s, %s | Grape: %s | Sweetness: %s | Body: %s | " +
                "Acidity: %s | Tannin: %s | Flavors: %s | Food: %s | Price: €%.2f | Rating: %.1f",
                id, name, winery, wineType, region, country, grapeVariety,
                sweetness, body, acidity, tannin, flavorNotes, foodPairing,
                price, rating != null ? rating : BigDecimal.ZERO
        );
    }
}
