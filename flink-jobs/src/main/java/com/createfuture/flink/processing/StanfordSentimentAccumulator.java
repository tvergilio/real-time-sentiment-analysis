package com.createfuture.flink.processing;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.createfuture.flink.model.SlackMessage;
import org.apache.flink.api.java.tuple.Tuple2;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class StanfordSentimentAccumulator {

    private final List<Integer> scores;
    private final List<String> classes;
    private long start;
    private long end;
    private double averageScore;
    private String sentiment;
    private SlackMessage mostPositiveMessage;
    private SlackMessage mostNegativeMessage;
    private int messageCount;

    public StanfordSentimentAccumulator() {
        this.scores = new ArrayList<>();
        this.classes = new ArrayList<>();
        this.messageCount = 0;
    }

    public void add(SlackMessage message, Tuple2<List<Integer>, List<String>> value) {
        scores.addAll(value.f0);
        classes.addAll(value.f1);
        messageCount++;

        int maxScore = value.f0.stream().max(Integer::compareTo).orElse(Integer.MIN_VALUE);
        int minScore = value.f0.stream().min(Integer::compareTo).orElse(Integer.MAX_VALUE);

        // Only consider messages as positive if they have a distinctly positive score
        if (maxScore > 2.5 && (mostPositiveMessage == null || maxScore > getMaxScore())) {
            mostPositiveMessage = message;
        }

        // Only consider messages as negative if they have a distinctly negative score
        if (minScore < 1.5 && (mostNegativeMessage == null || minScore < getMinScore())) {
            mostNegativeMessage = message;
        }

        updateAverageScore();
        sentiment = classifySentiment(averageScore);
    }

    public void merge(StanfordSentimentAccumulator other) {
        scores.addAll(other.scores);
        classes.addAll(other.classes);
        messageCount += other.messageCount;

        updateAverageScore();

        // Merge the most positive messages with stricter conditions
        if (other.mostPositiveMessage != null && other.getMaxScore() > 2.5) {
            if (this.mostPositiveMessage == null ||
                    other.getMaxScore() > this.getMaxScore()) {
                this.mostPositiveMessage = other.mostPositiveMessage;
            }
        }

        // Merge the most negative messages with stricter conditions
        if (other.mostNegativeMessage != null && other.getMinScore() < 1.5) {
            if (this.mostNegativeMessage == null ||
                    other.getMinScore() < this.getMinScore()) {
                this.mostNegativeMessage = other.mostNegativeMessage;
            }
        }

        sentiment = classifySentiment(this.averageScore);
    }

    private void updateAverageScore() {
        averageScore = scores.stream().mapToInt(Integer::intValue).average().orElse(0.0);
    }

    private String classifySentiment(double averageScore) {
        if (averageScore >= 3.5) {
            return "very positive";
        } else if (averageScore >= 2.5) {
            return "positive";
        } else if (averageScore >= 1.5) {
            return "neutral";
        } else if (averageScore >= 0.5) {
            return "negative";
        } else {
            return "very negative";
        }
    }

    private int getMaxScore() {
        return scores.stream().max(Integer::compareTo).orElse(Integer.MIN_VALUE);
    }

    private int getMinScore() {
        return scores.stream().min(Integer::compareTo).orElse(Integer.MAX_VALUE);
    }

    @Override
    public String toString() {
        return toJson();
    }

    public String toJson() {
        var mapper = new ObjectMapper();
        var jsonNode = mapper.createObjectNode();

        var formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss").withZone(ZoneId.of("Europe/London"));
        var startFormatted = formatter.format(Instant.ofEpochMilli(start));
        var endFormatted = formatter.format(Instant.ofEpochMilli(end));

        jsonNode.put("start", startFormatted);
        jsonNode.put("end", endFormatted);
        jsonNode.put("overallSentiment", sentiment);
        jsonNode.put("mostPositiveMessage", mostPositiveMessage != null ? mostPositiveMessage.getMessage() : "N/A");
        jsonNode.put("mostNegativeMessage", mostNegativeMessage != null ? mostNegativeMessage.getMessage() : "N/A");
        jsonNode.put("messageCount", messageCount);
        jsonNode.put("averageScore", Double.parseDouble(String.format("%.2f", averageScore)));

        try {
            return mapper.writeValueAsString(jsonNode);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return "{}";
        }
    }

    public long getStart() {
        return start;
    }

    public void setStart(long start) {
        this.start = start;
    }

    public long getEnd() {
        return end;
    }

    public void setEnd(long end) {
        this.end = end;
    }

    public int getMessageCount() {
        return messageCount;
    }

    public double getAverageScore() {
        return averageScore;
    }

    public String getSentiment() {
        return sentiment;
    }

    public String getMostPositiveMessage() {
        return mostPositiveMessage != null ? mostPositiveMessage.getMessage() : null;
    }

    public String getMostNegativeMessage() {
        return mostNegativeMessage != null ? mostNegativeMessage.getMessage() : null;
    }
}
