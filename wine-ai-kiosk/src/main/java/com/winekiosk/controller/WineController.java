package com.winekiosk.controller;

import com.winekiosk.dto.WineFilterRequest;
import com.winekiosk.model.Wine;
import com.winekiosk.service.WineService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/wines")
public class WineController {

    private final WineService wineService;

    public WineController(WineService wineService) {
        this.wineService = wineService;
    }

    @GetMapping
    public ResponseEntity<List<Wine>> getAllWines() {
        return ResponseEntity.ok(wineService.getAllWines());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Wine> getWineById(@PathVariable Long id) {
        return wineService.getWineById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/filter")
    public ResponseEntity<List<Wine>> filterWines(@RequestBody WineFilterRequest request) {
        return ResponseEntity.ok(wineService.filterWines(request));
    }

    @GetMapping("/filters")
    public ResponseEntity<Map<String, List<String>>> getFilterOptions() {
        return ResponseEntity.ok(wineService.getFilterOptions());
    }
}
