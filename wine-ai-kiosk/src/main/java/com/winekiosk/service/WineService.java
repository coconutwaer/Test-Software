package com.winekiosk.service;

import com.winekiosk.dto.WineFilterRequest;
import com.winekiosk.model.Wine;
import com.winekiosk.repository.WineRepository;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class WineService {

    private final WineRepository wineRepository;

    public WineService(WineRepository wineRepository) {
        this.wineRepository = wineRepository;
    }

    public List<Wine> getAllWines() {
        return wineRepository.findAll();
    }

    public Optional<Wine> getWineById(Long id) {
        return wineRepository.findById(id);
    }

    public List<Wine> filterWines(WineFilterRequest request) {
        return wineRepository.findByFilters(
                nullIfBlank(request.getWineType()),
                nullIfBlank(request.getCountry()),
                nullIfBlank(request.getRegion()),
                nullIfBlank(request.getGrapeVariety()),
                nullIfBlank(request.getSweetness()),
                nullIfBlank(request.getBody()),
                request.getMinPrice(),
                request.getMaxPrice(),
                request.getMinRating(),
                nullIfBlank(request.getSearchText())
        );
    }

    public Wine saveWine(Wine wine) {
        return wineRepository.save(wine);
    }

    public void deleteWine(Long id) {
        wineRepository.deleteById(id);
    }

    public Map<String, List<String>> getFilterOptions() {
        Map<String, List<String>> options = new HashMap<>();
        options.put("countries", wineRepository.findDistinctCountries());
        options.put("regions", wineRepository.findDistinctRegions());
        options.put("grapeVarieties", wineRepository.findDistinctGrapeVarieties());
        options.put("wineTypes", wineRepository.findDistinctWineTypes());
        options.put("sweetness", wineRepository.findDistinctSweetness());
        return options;
    }

    private String nullIfBlank(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }
}
