package com.createfuture.flink.model;

public class SemanticMatchResult {

    private String userId;
    private String matchedTheme;
    private double similarityScore;

    public SemanticMatchResult(String userId, String matchedTheme, double similarityScore) {
        this.userId = userId;
        this.matchedTheme = matchedTheme;
        this.similarityScore = similarityScore;
    }

    public String getUserId() {
        return userId;
    }

    public String getMatchedTheme() {
        return matchedTheme;
    }

    public double getSimilarityScore() {
        return similarityScore;
    }

    @Override
    public String toString() {
        return "SemanticMatchResult{" +
                "userId='" + userId + '\'' +
                ", matchedTheme='" + matchedTheme + '\'' +
                ", similarityScore=" + similarityScore +
                '}';
    }
}
