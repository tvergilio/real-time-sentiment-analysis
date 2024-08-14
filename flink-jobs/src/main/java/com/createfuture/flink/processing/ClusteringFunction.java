package com.createfuture.flink.processing;

import com.createfuture.flink.model.SemanticMatchResult;
import org.apache.flink.api.common.functions.MapFunction;

public class ClusteringFunction implements MapFunction<SemanticMatchResult, String> {

    // Assume simple thresholds for clustering based on similarity score
    private static final double HIGH_SIMILARITY_THRESHOLD = 0.8;
    private static final double MEDIUM_SIMILARITY_THRESHOLD = 0.5;

    @Override
    public String map(SemanticMatchResult matchResult) throws Exception {
        var similarityScore = matchResult.getSimilarityScore();
        String clusterLabel;

        if (similarityScore >= HIGH_SIMILARITY_THRESHOLD) {
            clusterLabel = "High Similarity Cluster";
        } else if (similarityScore >= MEDIUM_SIMILARITY_THRESHOLD) {
            clusterLabel = "Medium Similarity Cluster";
        } else {
            clusterLabel = "Low Similarity Cluster";
        }

        return String.format("User: %s, Theme: %s, Cluster: %s, Similarity: %.2f",
                matchResult.getUserId(), matchResult.getMatchedTheme(), clusterLabel, similarityScore);
    }
}
