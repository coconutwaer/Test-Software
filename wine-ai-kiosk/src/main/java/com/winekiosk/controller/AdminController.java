package com.winekiosk.controller;

import com.winekiosk.model.Wine;
import com.winekiosk.service.WineService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/wines")
public class AdminController {

    private final WineService wineService;

    public AdminController(WineService wineService) {
        this.wineService = wineService;
    }

    @GetMapping
    public ResponseEntity<List<Wine>> getAllWines() {
        return ResponseEntity.ok(wineService.getAllWines());
    }

    @PostMapping
    public ResponseEntity<Wine> createWine(@RequestBody Wine wine) {
        wine.setId(null);
        Wine saved = wineService.saveWine(wine);
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Wine> updateWine(@PathVariable Long id, @RequestBody Wine wine) {
        if (wineService.getWineById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        wine.setId(id);
        Wine updated = wineService.saveWine(wine);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteWine(@PathVariable Long id) {
        if (wineService.getWineById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        wineService.deleteWine(id);
        return ResponseEntity.noContent().build();
    }
}
