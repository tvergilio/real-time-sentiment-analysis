package com.createfuture.flink.job;

import com.createfuture.flink.processing.GPT4SentimentAccumulator;
import com.createfuture.flink.processing.GPT4SentimentProcessFunction;
import com.createfuture.flink.processing.StanfordSentimentAccumulator;
import com.createfuture.flink.processing.StanfordSentimentProcessFunction;
import com.createfuture.flink.transfer.SlackMessageDeserializationSchema;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.configuration.RestartStrategyOptions;
import org.apache.flink.connector.base.DeliveryGuarantee;
import org.apache.flink.connector.kafka.sink.KafkaRecordSerializationSchema;
import org.apache.flink.connector.kafka.sink.KafkaSink;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.assigners.SlidingProcessingTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer;

import java.time.Duration;
import java.util.Properties;

/**
 * A Flink job that reads Slack messages from a Kafka topic, performs sentiment analysis using the Stanford NLP model and
 * GPT-4, and writes the results to different Kafka topics.
 */
public class SentimentAnalysisJob {

    public static void main(String[] args) throws Exception {
        Configuration config = new Configuration();
        config.set(RestartStrategyOptions.RESTART_STRATEGY, "fixed-delay");
        config.set(RestartStrategyOptions.RESTART_STRATEGY_FIXED_DELAY_ATTEMPTS, 3); // number of restart attempts
        config.set(RestartStrategyOptions.RESTART_STRATEGY_FIXED_DELAY_DELAY, Duration.ofSeconds(10)); // delay
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment(config);
        var properties = new Properties();
        properties.setProperty("bootstrap.servers", "kafka:9092");
        properties.setProperty("group.id", "flink-group");
        run(env, properties);
    }

    public static void run(StreamExecutionEnvironment env, Properties kafkaProperties) throws Exception {
        // Disable operator chaining for better visualisation
        env.disableOperatorChaining();

        // Consumer for reading Slack messages
        var slackMessagesConsumer = new FlinkKafkaConsumer<>("slack_messages", new SlackMessageDeserializationSchema(), kafkaProperties);
        var slackMessagesStream = env.addSource(slackMessagesConsumer)
                .name("Kafka Source: Slack Messages")
                .uid("kafka-source-slack-messages");

        // Apply windowing function
        var windowedStream = slackMessagesStream
                .windowAll(SlidingProcessingTimeWindows.of(Time.minutes(2), Time.minutes(1)));

        // Stanford Sentiment Analysis
        var stanfordSentimentResultsStream = windowedStream
                .process(new StanfordSentimentProcessFunction())
                .name("Stanford Sentiment Analysis")
                .uid("stanford-sentiment-analysis");

        // GPT-4 Sentiment Analysis
        var gpt4SentimentResultsStream = windowedStream
                .process(new GPT4SentimentProcessFunction())
                .name("GPT-4 Sentiment Analysis")
                .uid("gpt4-sentiment-analysis");

        // Convert the Stamford results to JSON format
        var stanfordJsonResultsStream = stanfordSentimentResultsStream
                .map(StanfordSentimentAccumulator::toString)
                .name("Convert Stanford to JSON")
                .uid("map-convert-stanford-to-json");

        // Convert the GPT-4 results (extract content)
        var gpt4OutputResultsStream = gpt4SentimentResultsStream
                .map(GPT4SentimentAccumulator::getContent)
                .name("Extract GPT-4 Content")
                .uid("map-extract-gpt4-content");

        // Sink the results to different Kafka topics
        stanfordJsonResultsStream.sinkTo(createKafkaSink(kafkaProperties.getProperty("bootstrap.servers"), "stanford_results"))
                .name("Sink Stanford Results to Kafka")
                .uid("kafka-sink-stanford-sentiment-results");

        gpt4OutputResultsStream.sinkTo(createKafkaSink(kafkaProperties.getProperty("bootstrap.servers"), "gpt4_results"))
                .name("Sink GPT-4 Results to Kafka")
                .uid("kafka-sink-gpt4-sentiment-results");

        env.execute("Sentiment Comparison Job");
    }

    private static KafkaSink<String> createKafkaSink(String bootstrapServers, String topic) {
        return KafkaSink.<String>builder()
                .setBootstrapServers(bootstrapServers)
                .setRecordSerializer(KafkaRecordSerializationSchema.builder()
                        .setTopic(topic)
                        .setValueSerializationSchema(new SimpleStringSchema())
                        .build())
                .setDeliveryGuarantee(DeliveryGuarantee.AT_LEAST_ONCE)
                .build();
    }
}
