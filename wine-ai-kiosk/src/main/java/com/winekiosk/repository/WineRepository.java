package com.winekiosk.repository;

import com.winekiosk.model.Wine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface WineRepository extends JpaRepository<Wine, Long> {

    @Query("SELECT w FROM Wine w WHERE " +
           "(:wineType IS NULL OR LOWER(w.wineType) = LOWER(:wineType)) AND " +
           "(:country IS NULL OR LOWER(w.country) = LOWER(:country)) AND " +
           "(:region IS NULL OR LOWER(w.region) = LOWER(:region)) AND " +
           "(:grapeVariety IS NULL OR LOWER(w.grapeVariety) = LOWER(:grapeVariety)) AND " +
           "(:sweetness IS NULL OR LOWER(w.sweetness) = LOWER(:sweetness)) AND " +
           "(:body IS NULL OR LOWER(w.body) = LOWER(:body)) AND " +
           "(:minPrice IS NULL OR w.price >= :minPrice) AND " +
           "(:maxPrice IS NULL OR w.price <= :maxPrice) AND " +
           "(:minRating IS NULL OR w.rating >= :minRating) AND " +
           "(:searchText IS NULL OR " +
           "  LOWER(w.name) LIKE LOWER(CONCAT('%', :searchText, '%')) OR " +
           "  LOWER(w.winery) LIKE LOWER(CONCAT('%', :searchText, '%')) OR " +
           "  LOWER(w.flavorNotes) LIKE LOWER(CONCAT('%', :searchText, '%')) OR " +
           "  LOWER(w.description) LIKE LOWER(CONCAT('%', :searchText, '%')))")
    List<Wine> findByFilters(
            @Param("wineType") String wineType,
            @Param("country") String country,
            @Param("region") String region,
            @Param("grapeVariety") String grapeVariety,
            @Param("sweetness") String sweetness,
            @Param("body") String body,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("minRating") BigDecimal minRating,
            @Param("searchText") String searchText
    );

    @Query("SELECT DISTINCT w.country FROM Wine w WHERE w.country IS NOT NULL ORDER BY w.country")
    List<String> findDistinctCountries();

    @Query("SELECT DISTINCT w.region FROM Wine w WHERE w.region IS NOT NULL ORDER BY w.region")
    List<String> findDistinctRegions();

    @Query("SELECT DISTINCT w.grapeVariety FROM Wine w WHERE w.grapeVariety IS NOT NULL ORDER BY w.grapeVariety")
    List<String> findDistinctGrapeVarieties();

    @Query("SELECT DISTINCT w.wineType FROM Wine w WHERE w.wineType IS NOT NULL ORDER BY w.wineType")
    List<String> findDistinctWineTypes();

    @Query("SELECT DISTINCT w.sweetness FROM Wine w WHERE w.sweetness IS NOT NULL ORDER BY w.sweetness")
    List<String> findDistinctSweetness();
}
