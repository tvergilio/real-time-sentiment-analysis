package com.createfuture.flink.processing;

import com.createfuture.flink.model.EmbeddingResult;
import com.createfuture.flink.model.SlackMessage;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.windowing.RichWindowFunction;
import org.apache.flink.util.Collector;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static com.createfuture.flink.utils.EnvironmentUtils.getOrDefault;

public class EmbeddingGenerationFunction extends RichWindowFunction<SlackMessage, EmbeddingResult, String, TimeWindow> {

    private final String model;
    private final String modelEndpoint;
    private transient OkHttpClient client;
    private transient ObjectMapper objectMapper;

    public EmbeddingGenerationFunction() {
        this.model = getOrDefault("LLAMA_MODEL", "tinyllama");
        this.modelEndpoint = getOrDefault("LLAMA_ENDPOINT", "http://ollama:11434/api/generate");
        if (this.model == null || this.modelEndpoint == null) {
            throw new IllegalStateException("Model name and endpoint must be set in the environment variables");
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
    public void apply(String key, TimeWindow window, Iterable<SlackMessage> input, Collector<EmbeddingResult> out) throws Exception {
        for (SlackMessage message : input) {
            try {
                // Prepare the JSON payload for the API request using Jackson
                var requestPayload = Map.of(
                        "model", model,
                        "prompt", message.getMessage(),
                        "stream", false,
                        "options", Map.of(
                                "num_predict", 0,
                                "temperature", 0.0,
                                "num_ctx", 1024,
                                "num_thread", 4,
                                "num_gpu", 1,
                                "f16_kv", true,
                                "use_mmap", true
                        )
                );

                // Convert the request payload to JSON string
                var jsonRequest = objectMapper.writeValueAsString(requestPayload);
                var body = RequestBody.create(jsonRequest, MediaType.get("application/json; charset=utf-8"));

                // Build the HTTP request
                var request = new Request.Builder()
                        .url(modelEndpoint)
                        .post(body)
                        .build();

                // Execute the request and process the response
                try (var response = client.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        throw new IOException("Unexpected code " + response);
                    }

                    var result = response.body().string();
                    var embeddings = parseEmbeddingFromResponse(result);

                    // Collect the embedding result
                    out.collect(new EmbeddingResult(message.getUser(), embeddings));
                }
            } catch (IOException e) {
                // Handle exceptions appropriately
                System.out.println("Failed to call embedding API: " + e.getMessage());
                throw e; // Rethrow the exception if necessary
            }
        }
    }

    private int[] parseEmbeddingFromResponse(String response) throws IOException {
        // Use Jackson to parse the response
        var responseObject = objectMapper.readValue(response, Map.class);

        // Safely cast the 'context' field to a List of Integer
        var contextObject = responseObject.get("context");
        if (contextObject == null || !(contextObject instanceof List)) {
            throw new IOException("Invalid response: 'context' field is missing or not a list");
        }

        var contextArray = objectMapper.convertValue(contextObject, new TypeReference<List<Integer>>() {});

        // Convert the list of integers to a double array representing the embedding
        var embedding = new int[contextArray.size()];
        for (int i = 0; i < contextArray.size(); i++) {
            embedding[i] = contextArray.get(i);
        }

        return embedding;
    }
}
