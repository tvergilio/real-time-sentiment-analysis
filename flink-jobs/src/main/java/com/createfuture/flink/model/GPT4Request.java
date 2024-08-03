package com.createfuture.flink.model;

public class GPT4Request {
    private final String prompt;

    public GPT4Request(String prompt) {
        this.prompt = prompt;
    }

    public String getPrompt() {
        return prompt;
    }
}
