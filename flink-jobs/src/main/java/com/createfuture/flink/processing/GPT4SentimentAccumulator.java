package com.createfuture.flink.processing;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

public class GPT4SentimentAccumulator {

    private long start;
    private long end;

    private String content;

    public GPT4SentimentAccumulator(String jsonResponse) {
        this.content = extractContentFromResponse(jsonResponse);
    }

    private String extractContentFromResponse(String jsonResponse) {
        try {
            var objectMapper = new ObjectMapper();
            var rootNode = objectMapper.readTree(jsonResponse);
            return rootNode.path("choices")
                    .get(0).path("message")
                    .path("content")
                    .asText();
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse GPT-4 response", e);
        }
    }

    @Override
    public String toString() {
        return content;
    }

    public void setStart(long start) {
        this.start = start;
    }

    public void setEnd(long end) {
        this.end = end;
    }

    public String getContent() {
        return content;
    }
}
