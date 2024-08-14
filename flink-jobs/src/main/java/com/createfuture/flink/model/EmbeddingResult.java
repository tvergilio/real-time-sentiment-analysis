package com.createfuture.flink.model;

import java.util.Arrays;

public class EmbeddingResult {

    private String userId;
    private int[] embedding;

    public EmbeddingResult(String userId, int[] embedding) {
        this.userId = userId;
        this.embedding = embedding;
    }

    public String getUserId() {
        return userId;
    }

    public int[] getEmbedding() {
        return embedding;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setEmbedding(int[] embedding) {
        this.embedding = embedding;
    }

    @Override
    public String toString() {
        return "EmbeddingResult{" +
                "userId='" + userId + '\'' +
                ", embedding=" + Arrays.toString(embedding) +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EmbeddingResult that = (EmbeddingResult) o;
        return userId.equals(that.userId) && Arrays.equals(embedding, that.embedding);
    }

    @Override
    public int hashCode() {
        int result = userId.hashCode();
        result = 31 * result + Arrays.hashCode(embedding);
        return result;
    }
}
