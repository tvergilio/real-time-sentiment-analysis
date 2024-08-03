package com.createfuture.flink.processing;

import com.createfuture.flink.model.SlackMessage;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.windowing.ProcessAllWindowFunction;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class StanfordSentimentProcessFunctionTest {

    private StanfordSentimentProcessFunction processFunction;
    private ProcessAllWindowFunction.Context mockContext;
    private Collector<StanfordSentimentAccumulator> mockCollector;

    @BeforeEach
    void setUp() {
        // Arrange
        processFunction = new StanfordSentimentProcessFunction();
        processFunction.open(new Configuration());
        mockContext = mock(ProcessAllWindowFunction.Context.class);
        mockCollector = mock(Collector.class);
    }

    @Test
    void testProcessEmptyMessages() throws Exception {
        // Arrange
        when(mockContext.window()).thenReturn(new TimeWindow(0, 1000));

        // Act
        processFunction.process(mockContext, Collections.emptyList(), mockCollector);

        // Assert
        verify(mockCollector, never()).collect(any(StanfordSentimentAccumulator.class));
    }

    @Test
    void testProcessSingleMessage() throws Exception {
        // Arrange
        var message = new SlackMessage(123L, "U123", "Fantastic!");
        when(mockContext.window()).thenReturn(new TimeWindow(0, 1000));

        // Act
        processFunction.process(mockContext, List.of(message), mockCollector);

        // Assert
        var captor = ArgumentCaptor.forClass(StanfordSentimentAccumulator.class);
        verify(mockCollector, times(1)).collect(captor.capture());

        var accumulator = captor.getValue();
        assertEquals(1, accumulator.getMessageCount());
        assertEquals("Fantastic!", accumulator.getMostPositiveMessage());
    }

    @Test
    void testProcessMultipleMessages() throws Exception {
        // Arrange
        var message1 = new SlackMessage(123L, "U123", "This is a positive message");
        var message2 = new SlackMessage(124L, "U124", "This is a negative message");
        when(mockContext.window()).thenReturn(new TimeWindow(0, 1000));

        // Act
        processFunction.process(mockContext, List.of(message1, message2), mockCollector);

        // Assert
        var captor = ArgumentCaptor.forClass(StanfordSentimentAccumulator.class);
        verify(mockCollector, times(1)).collect(captor.capture());

        var accumulator = captor.getValue();
        assertEquals(2, accumulator.getMessageCount());
    }
}
