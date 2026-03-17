package com.winekiosk.dto;

import com.winekiosk.model.Wine;
import java.util.List;

public class ChatResponse {

    private String reply;
    private List<Wine> recommendedWines;

    public ChatResponse() {}

    public ChatResponse(String reply, List<Wine> recommendedWines) {
        this.reply = reply;
        this.recommendedWines = recommendedWines;
    }

    public String getReply() { return reply; }
    public void setReply(String reply) { this.reply = reply; }

    public List<Wine> getRecommendedWines() { return recommendedWines; }
    public void setRecommendedWines(List<Wine> recommendedWines) { this.recommendedWines = recommendedWines; }
}
