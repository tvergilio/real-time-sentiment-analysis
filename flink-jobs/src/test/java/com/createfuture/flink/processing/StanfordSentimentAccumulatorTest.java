package com.createfuture.flink.processing;

import com.createfuture.flink.model.SlackMessage;
import org.apache.flink.api.java.tuple.Tuple2;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StanfordSentimentAccumulatorTest {

    @Test
    void testAdd() {
        var accumulator = new StanfordSentimentAccumulator();
        var message = new SlackMessage(1721903155L, "U07DET2KZ2B", "Fantastic!");

        accumulator.add(message, new Tuple2<>(List.of(3), List.of("Positive")));

        assertEquals(1, accumulator.getMessageCount());
        assertEquals(3, accumulator.getAverageScore(), 0.01);
        assertEquals("positive", accumulator.getSentiment());
        assertEquals("Fantastic!", accumulator.getMostPositiveMessage());
    }

    @Test
    void testMerge() {
        var accumulator1 = new StanfordSentimentAccumulator();
        var accumulator2 = new StanfordSentimentAccumulator();

        var message1 = new SlackMessage(1721903155L, "U07DET2KZ4P", "Fantastic!");
        var message2 = new SlackMessage(1721903155L, "U07DET2KZ4P", "Awful!");

        accumulator1.add(message1, new Tuple2<>(List.of(3), List.of("Positive")));
        accumulator2.add(message2, new Tuple2<>(List.of(1), List.of("Negative")));

        accumulator1.merge(accumulator2);

        assertEquals(2, accumulator1.getMessageCount());
        assertEquals(2.0, accumulator1.getAverageScore(), 0.01);
        assertEquals("neutral", accumulator1.getSentiment());
        assertEquals("Fantastic!", accumulator1.getMostPositiveMessage());
        assertEquals("Awful!", accumulator1.getMostNegativeMessage());
    }

    @Test
    void testClassificationNearBoundary() {
        var accumulator = new StanfordSentimentAccumulator();

        var message1 = new SlackMessage(1721903155L, "U07DET2KZ4P", "Terrible!");
        var message2 = new SlackMessage(1721903155L, "U07DET2KZ4P", "Bad!");

        accumulator.add(message1, new Tuple2<>(List.of(0), List.of("Very Negative"))); // Very low score
        accumulator.add(message2, new Tuple2<>(List.of(1), List.of("Negative"))); // Slightly higher but still negative

        assertEquals(2, accumulator.getMessageCount());
        assertEquals("negative", accumulator.getSentiment());
    }

    @Test
    void testNeutralMessagesAreNotMisclassified() {
        var accumulator = new StanfordSentimentAccumulator();

        var message1 = new SlackMessage(1721903155L, "U07DET2KZ4P", "This is okay.");
        var message2 = new SlackMessage(1721903155L, "U07DET2KZ4P", "It could be worse.");
        var message3 = new SlackMessage(1721903155L, "U07DET2KZ4P", "Awesome!");
        var message4 = new SlackMessage(1721903155L, "U07DET2KZ4P", "Terrible!");

        accumulator.add(message1, new Tuple2<>(List.of(2), List.of("Neutral")));
        accumulator.add(message2, new Tuple2<>(List.of(2), List.of("Neutral")));
        accumulator.add(message3, new Tuple2<>(List.of(4), List.of("Very Positive")));
        accumulator.add(message4, new Tuple2<>(List.of(1), List.of("Negative")));

        assertEquals(4, accumulator.getMessageCount());
        assertEquals(2.25, accumulator.getAverageScore(), 0.01);
        assertEquals("neutral", accumulator.getSentiment());
        assertEquals("Awesome!", accumulator.getMostPositiveMessage());
        assertEquals("Terrible!", accumulator.getMostNegativeMessage());
    }

    @Test
    void testIdenticalMessagesForPositiveAndNegative() {
        var accumulator = new StanfordSentimentAccumulator();

        var message = new SlackMessage(1721903155L, "U07DET2KZ4P", "This is confusing...");

        accumulator.add(message, new Tuple2<>(List.of(2), List.of("Neutral")));
        accumulator.add(new SlackMessage(1721903155L, "U07DET2KZ4P", "Great!"), new Tuple2<>(List.of(4), List.of("Very Positive")));
        accumulator.add(new SlackMessage(1721903155L, "U07DET2KZ4P", "Terrible!"), new Tuple2<>(List.of(1), List.of("Negative")));

        assertEquals(3, accumulator.getMessageCount());
        assertEquals(2.33, accumulator.getAverageScore(), 0.01);
        assertEquals("neutral", accumulator.getSentiment());
        assertEquals("Great!", accumulator.getMostPositiveMessage());
        assertEquals("Terrible!", accumulator.getMostNegativeMessage());
    }
}
