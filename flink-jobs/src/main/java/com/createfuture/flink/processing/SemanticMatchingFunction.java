package com.createfuture.flink.processing;

import com.createfuture.flink.model.EmbeddingResult;
import com.createfuture.flink.model.SemanticMatchResult;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.streaming.api.functions.co.BroadcastProcessFunction;
import org.apache.flink.util.Collector;

import java.util.Arrays;
import java.util.Map;

public class SemanticMatchingFunction extends BroadcastProcessFunction<EmbeddingResult, Map<String, int[]>, SemanticMatchResult> {

    private MapStateDescriptor<String, int[]> themeEmbeddingsDescriptor;

    public SemanticMatchingFunction() {
        themeEmbeddingsDescriptor = new MapStateDescriptor<>("ThemeEmbeddings", String.class, int[].class);
    }

    @Override
    public void processElement(EmbeddingResult embeddingResult, ReadOnlyContext ctx, Collector<SemanticMatchResult> out) throws Exception {
        var themeEmbeddings = ctx.getBroadcastState(themeEmbeddingsDescriptor);
        var embedding = embeddingResult.getEmbedding();
        String bestMatchTheme = null;
        var highestSimilarity = Double.NEGATIVE_INFINITY;

        for (var entry : themeEmbeddings.immutableEntries()) {
            var similarity = cosineSimilarity(embedding, entry.getValue());
            if (similarity > highestSimilarity) {
                highestSimilarity = similarity;
                bestMatchTheme = entry.getKey();
            }
        }

        out.collect(new SemanticMatchResult(embeddingResult.getUserId(), bestMatchTheme, highestSimilarity));
    }

    @Override
    public void processBroadcastElement(Map<String, int[]> themeEmbeddings, Context ctx, Collector<SemanticMatchResult> out) throws Exception {
        var broadcastState = ctx.getBroadcastState(themeEmbeddingsDescriptor);
        for (var entry : themeEmbeddings.entrySet()) {
            broadcastState.put(entry.getKey(), entry.getValue());
        }
    }

    private double cosineSimilarity(int[] vectorA, int[] vectorB) {
        int maxLength = Math.max(vectorA.length, vectorB.length);

        // Pad or trim vectors to ensure they have the same length
        vectorA = padVector(vectorA, maxLength);
        vectorB = padVector(vectorB, maxLength);

        var dotProduct = 0.0;
        var normA = 0.0;
        var normB = 0.0;
        for (var i = 0; i < vectorA.length; i++) {
            dotProduct += vectorA[i] * vectorB[i];
            normA += Math.pow(vectorA[i], 2);
            normB += Math.pow(vectorB[i], 2);
        }
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    private int[] padVector(int[] vector, int targetLength) {
        if (vector.length >= targetLength) {
            return Arrays.copyOf(vector, targetLength);
        }
        int[] paddedVector = new int[targetLength];
        System.arraycopy(vector, 0, paddedVector, 0, vector.length);
        return paddedVector;
    }
}
