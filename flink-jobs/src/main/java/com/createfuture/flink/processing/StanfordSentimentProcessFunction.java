package com.createfuture.flink.processing;

import com.createfuture.flink.model.SlackMessage;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.windowing.ProcessAllWindowFunction;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;

import java.util.List;
import java.util.Properties;

public class StanfordSentimentProcessFunction extends ProcessAllWindowFunction<SlackMessage, StanfordSentimentAccumulator, TimeWindow> {

    private StanfordCoreNLP pipeline;

    @Override
    public void open(Configuration configuration) {
        Properties properties = new Properties();
        properties.setProperty("annotators", "tokenize, ssplit, parse, sentiment");
        pipeline = new StanfordCoreNLP(properties);
    }

    @Override
    public void process(Context context, Iterable<SlackMessage> elements, Collector<StanfordSentimentAccumulator> out) {

        if (!elements.iterator().hasNext()) {
            return; // Skip processing if no messages are in the window
        }

        var accumulator = new StanfordSentimentAccumulator();

        for (SlackMessage message : elements) {
            // Perform sentiment analysis on the message
            Tuple2<List<Integer>, List<String>> sentiment = getSentiment(message.getMessage());
            accumulator.add(message, sentiment);
        }

        // Set the window start and end times
        accumulator.setStart(context.window().getStart());
        accumulator.setEnd(context.window().getEnd());

        // Emit the accumulated sentiment result
        out.collect(accumulator);
    }

    private Tuple2<List<Integer>, List<String>> getSentiment(String message) {
        var sentimentFunction = new StanfordSentimentAnalysisFunction();
        sentimentFunction.open(new Configuration());
        return sentimentFunction.getSentiment(message);
    }
}
