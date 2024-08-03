package com.createfuture.flink.processing;

import com.createfuture.flink.model.SlackMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.windowing.ProcessAllWindowFunction;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class GPT4SentimentProcessFunction extends ProcessAllWindowFunction<SlackMessage, GPT4SentimentAccumulator, TimeWindow> {

    private final String apiKey;
    private final String model;
    private transient OkHttpClient client;
    private transient ObjectMapper objectMapper;

    public GPT4SentimentProcessFunction() {
        this.apiKey = System.getenv("OPENAI_API_KEY");
        this.model = System.getenv("OPENAI_MODEL");
        if (this.apiKey == null) {
            throw new IllegalStateException("OpenAI API key must be set in the environment variables");
        } else if (this.model == null) {
            throw new IllegalStateException("OpenAI model must be set in the environment variables");
        }
    }

    @Override
    public void open(Configuration parameters) {
        if (client == null) {
            client = new OkHttpClient();
        }
        if (objectMapper == null) {
            objectMapper = new ObjectMapper();
        }
    }

    @Override
    public void process(Context context, Iterable<SlackMessage> elements, Collector<GPT4SentimentAccumulator> out) throws Exception {

        if (!elements.iterator().hasNext()) {
            return; // Skip processing if no messages are in the window
        }

        List<SlackMessage> messages = new ArrayList<>();
        for (SlackMessage message : elements) {
            messages.add(message);
        }

        long windowStart = context.window().getStart();
        long windowEnd = context.window().getEnd();

        try {
            var accumulator = analyzeSentiment(messages, windowStart, windowEnd);
            accumulator.setStart(windowStart);
            accumulator.setEnd(windowEnd);
            out.collect(accumulator);
        } catch (IOException e) {
            // Log the error and continue
            System.out.println("Failed to call GPT-4 API: " + e.getMessage());
        }
    }

    private GPT4SentimentAccumulator analyzeSentiment(List<SlackMessage> messages, long windowStart, long windowEnd) throws IOException {
        var prompt = createPrompt(messages, windowStart, windowEnd);
        var response = callGPT4API(prompt);

        return new GPT4SentimentAccumulator(response);
    }

    private String createPrompt(List<SlackMessage> messages, long windowStart, long windowEnd) {
        var formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss").withZone(ZoneId.of("Europe/London"));
        var startFormatted = formatter.format(Instant.ofEpochMilli(windowStart));
        var endFormatted = formatter.format(Instant.ofEpochMilli(windowEnd));

        return String.format(
                "Please analyse the following set of Slack messages and provide a summary of the overall sentiment. " +
                        "Indicate whether the sentiment is very positive, positive, neutral, negative, or very negative. " +
                        "Provide a summary paragraph that describes the overall sentiment, including the general tone.\n\n" +
                        "Window Start: %s\n" +
                        "Window End: %s\n" +
                        "Messages Processed: %d\n\nMessages:\n%s\n\n" +
                        "Provide your answer in JSON format, as per the following structure:\n\n" +
                        "{\n" +
                        "  \"start\": \"[Start Time]\",\n" +
                        "  \"end\": \"[End Time]\",\n" +
                        "  \"overallSentiment\": \"[Overall Sentiment]\",\n" +
                        "  \"mostPositiveMessage\": \"[Most Positive Message]\",\n" +
                        "  \"mostNegativeMessage\": \"[Most Negative Message]\",\n" +
                        "  \"messageCount\": [Message Count],\n" +
                        "  \"descriptiveParagraph\": \"[Descriptive Paragraph]\"\n" +
                        "}\n\n" +
                        "Ensure that your answer is in JSON format, the JSON object is properly formatted and includes " +
                        "all the required fields. Do not include ```json or ``` around your response.\n\n",
                startFormatted, endFormatted, messages.size(),
                messages.stream().map(SlackMessage::getMessage).collect(Collectors.joining("\n- ", "- ", "")),
                startFormatted, endFormatted
        );
    }

    private String callGPT4API(String prompt) throws IOException {
        int retryCount = 0;
        int maxRetries = 3;
        long waitTime = 1000; // Initial wait time of 1 second

        while (retryCount < maxRetries) {
            try {
                var jsonRequest = objectMapper.writeValueAsString(Map.of(
                        "model", model,
                        "messages", List.of(
                                Map.of("role", "system", "content", "You are a helpful assistant."),
                                Map.of("role", "user", "content", prompt)
                        )
                ));

                var body = RequestBody.create(jsonRequest, MediaType.get("application/json; charset=utf-8"));

                var request = new Request.Builder()
                        .url("https://api.openai.com/v1/chat/completions")
                        .header("Authorization", "Bearer " + apiKey)
                        .post(body)
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        throw new IOException("Unexpected code " + response);
                    }
                    return response.body().string();
                }
            } catch (IOException e) {
                retryCount++;
                if (retryCount >= maxRetries) {
                    throw e; // Rethrow exception if max retries reached
                }
                try {
                    Thread.sleep(waitTime);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Retry interrupted", ie);
                }
                waitTime *= 2; // Exponential backoff
            }
        }
        throw new IOException("Failed to call GPT-4 API after " + maxRetries + " attempts");
    }
}
