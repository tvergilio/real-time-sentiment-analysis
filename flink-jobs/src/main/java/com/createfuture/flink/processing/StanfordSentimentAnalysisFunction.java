package com.createfuture.flink.processing;

import com.createfuture.flink.model.SlackMessage;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.sentiment.SentimentCoreAnnotations.SentimentAnnotatedTree;
import edu.stanford.nlp.sentiment.SentimentCoreAnnotations.SentimentClass;
import org.apache.flink.api.common.functions.RichMapFunction;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.configuration.Configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static edu.stanford.nlp.neural.rnn.RNNCoreAnnotations.getPredictedClass;

public class StanfordSentimentAnalysisFunction extends RichMapFunction<SlackMessage, Tuple2<SlackMessage, Tuple2<List<Integer>, List<String>>>> {

    private StanfordCoreNLP pipeline;

    /**
     * These properties are used to configure the StanfordCoreNLP pipeline.
     * The annotators property specifies the NLP tasks to be performed on the text.
     * - tokenize: tokenises the text into words
     * - ssplit: splits the text into sentences
     * - parse: understands the grammatical structure of each sentence
     * - sentiment: performs sentiment analysis on each sentence
     */
    @Override
    public void open(Configuration configuration) {
        var properties = new Properties();
        properties.setProperty("annotators", "tokenize, ssplit, parse, sentiment");
        pipeline = new StanfordCoreNLP(properties);
    }

    @Override
    public Tuple2<SlackMessage, Tuple2<List<Integer>, List<String>>> map(SlackMessage slackMessage) {
        return new Tuple2<>(slackMessage, getSentiment(slackMessage.getMessage()));
    }

    public Tuple2<List<Integer>, List<String>> getSentiment(String message) {
        List<Integer> scores = new ArrayList<>();
        List<String> classes = new ArrayList<>();

        if (pipeline == null) {
            throw new IllegalStateException("StanfordCoreNLP pipeline is not initialised.");
        }

        if (message != null && !message.isEmpty()) {
            var annotation = pipeline.process(message);

            // The Stanford sentiment analysis algorithm processes text at sentence level.
            // Therefore, we iterate over each sentence in the message to get the sentiment score and class.
            annotation.get(SentencesAnnotation.class).forEach(sentence -> {
                // sentiment score
                var tree = sentence.get(SentimentAnnotatedTree.class);
                scores.add(getPredictedClass(tree));

                // sentiment class
                classes.add(sentence.get(SentimentClass.class));
            });
        }

        return new Tuple2<>(scores, classes);
    }
}
