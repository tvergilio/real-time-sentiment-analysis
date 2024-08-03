package com.createfuture.flink.processing;

import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Paths;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class GPT4SentimentAccumulatorTest {

    @Test
    void testDescriptiveParagraphExtraction() throws Exception {
        // Arrange
        var jsonResponse = new String(Files.readAllBytes(Paths.get("src/test/resources/gpt4_response.json")));

        // Act
        var accumulator = new GPT4SentimentAccumulator(jsonResponse);
        var actualContent = accumulator.getContent();

        // Assert
        var expectedContent = "{\"start\":\"31/07/2024 15:18:30\",\"end\":\"31/07/2024 15:19:30\",\"overallSentiment\":\"positive\",\"mostPositiveMessage\":\"That was amazing!\",\"mostNegativeMessage\":\"Average, I think. I have seen better.\",\"messageCount\":5,\"descriptiveParagraph\":\"The overall sentiment of the messages processed during the time window is positive. The tone of the messages indicates that the users found their experiences to be generally favorable, with phrases highlighting that things were 'amazing' and 'fantastic,' suggesting a strong sense of satisfaction and enthusiasm. While there are some neutral comments, the overall tone is upbeat and positive.\"}";
        assertEquals(expectedContent, actualContent);
    }
}
