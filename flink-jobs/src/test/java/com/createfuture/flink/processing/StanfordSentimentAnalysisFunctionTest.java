package com.createfuture.flink.processing;

import com.createfuture.flink.model.SlackMessage;
import org.apache.flink.configuration.Configuration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StanfordSentimentAnalysisFunctionTest {

    private StanfordSentimentAnalysisFunction function;

    @BeforeEach
    void setUp() {
        function = new StanfordSentimentAnalysisFunction();
        function.open(new Configuration());
    }

    @Test
    void testMapVeryPositive() throws Exception {
        var message = new SlackMessage(1721903155L, "U07DET2KZ2B", "Fantastic!");
        var result = function.map(message);

        assertNotNull(result);
        assertEquals(message, result.f0);
        assertEquals(1, result.f1.f0.size());
        assertEquals("Very positive", result.f1.f1.get(0));
    }

    @Test
    void testMapPositive() throws Exception {
        var message = new SlackMessage(1721903155L, "U07DET2KZ2B", "Good job!");
        var result = function.map(message);

        assertNotNull(result);
        assertEquals(message, result.f0);
        assertEquals(1, result.f1.f0.size());
        assertEquals("Positive", result.f1.f1.get(0));
    }

    @Test
    void testMapNeutral() throws Exception {
        var message = new SlackMessage(1721903155L, "U07DET2KZ2B", "It's so-so.");
        var result = function.map(message);

        assertNotNull(result);
        assertEquals(message, result.f0);
        assertEquals(1, result.f1.f0.size());
        assertEquals("Neutral", result.f1.f1.get(0));
    }

    @Test
    void testMapNegative() throws Exception {
        var message = new SlackMessage(1721903155L, "U07DET2KZ2B", "This is bad.");
        var result = function.map(message);

        assertNotNull(result);
        assertEquals(message, result.f0);
        assertEquals(1, result.f1.f0.size());
        assertEquals("Negative", result.f1.f1.get(0));
    }

    @Test
    void testMapVeryNegative() throws Exception {
        var message = new SlackMessage(1721903155L, "U07DET2KZ2B", "Terrible!");
        var result = function.map(message);

        assertNotNull(result);
        assertEquals(message, result.f0);
        assertEquals(1, result.f1.f0.size());
        assertEquals("Very negative", result.f1.f1.get(0));
    }

    @Test
    void testMapEmptyMessage() throws Exception {
        var message = new SlackMessage(1721903155L, "U07DET2KZ2B", "");
        var result = function.map(message);

        assertNotNull(result);
        assertEquals(message, result.f0);
        assertTrue(result.f1.f0.isEmpty());
        assertTrue(result.f1.f1.isEmpty());
    }

    @Test
    void testMapNullMessage() throws Exception {
        var message = new SlackMessage(1721903155L, "U07DET2KZ2B", null);
        var result = function.map(message);

        assertNotNull(result);
        assertEquals(message, result.f0);
        assertTrue(result.f1.f0.isEmpty());
        assertTrue(result.f1.f1.isEmpty());
    }

    @Test
    void testGetSentimentWithEmptyMessage() {
        // Arrange
        var config = new Configuration();
        function.open(config);

        // Act
        var result = function.getSentiment("");

        // Assert
        assertEquals(0, result.f0.size()); // No sentiment score
        assertEquals(0, result.f1.size()); // No sentiment class
    }

    @Test
    void testGetSentimentWithNullMessage() {
        // Arrange
        var config = new Configuration();
        function.open(config);

        // Act
        var result = function.getSentiment(null);

        // Assert
        assertEquals(0, result.f0.size()); // No sentiment score
        assertEquals(0, result.f1.size()); // No sentiment class
    }

    @Test
    void testGetSentimentWithoutInitializingPipeline() {
        // Arrange
        function = new StanfordSentimentAnalysisFunction();

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> function.getSentiment("This is a test message"));
    }
}
