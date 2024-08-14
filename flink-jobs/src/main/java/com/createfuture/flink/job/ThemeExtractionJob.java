package com.createfuture.flink.job;

import com.createfuture.flink.model.SlackMessage;
import com.createfuture.flink.processing.EmbeddingGenerationFunction;
import com.createfuture.flink.processing.SemanticMatchingFunction;
import com.createfuture.flink.processing.ClusteringFunction;
import com.createfuture.flink.transfer.SlackMessageDeserializationSchema;

import static com.createfuture.flink.processing.ThemeEmbeddingsGenerator.generateDynamicThemeEmbeddings;
import static com.createfuture.flink.utils.EnvironmentUtils.*;

import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.configuration.RestartStrategyOptions;
import org.apache.flink.connector.base.DeliveryGuarantee;
import org.apache.flink.connector.kafka.sink.KafkaRecordSerializationSchema;
import org.apache.flink.connector.kafka.sink.KafkaSink;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.assigners.TumblingProcessingTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer;

import java.time.Duration;
import java.util.Properties;

public class ThemeExtractionJob {

    public static void main(String[] args) throws Exception {
        var config = new Configuration();
        config.set(RestartStrategyOptions.RESTART_STRATEGY, getOrDefault("RESTART_STRATEGY", "fixed-delay"));
        config.set(RestartStrategyOptions.RESTART_STRATEGY_FIXED_DELAY_ATTEMPTS, getOrDefault("RESTART_STRATEGY_FIXED_DELAY_ATTEMPTS", 3));
        config.set(RestartStrategyOptions.RESTART_STRATEGY_FIXED_DELAY_DELAY, Duration.ofSeconds(getOrDefault("RESTART_STRATEGY_FIXED_DELAY_DELAY", 1000L)));

        var env = StreamExecutionEnvironment.getExecutionEnvironment(config);
        var properties = new Properties();
        properties.setProperty("bootstrap.servers", getOrDefault("KAFKA_BOOTSTRAP_SERVERS", "kafka:9092"));
        properties.setProperty("group.id", getOrDefault("KAFKA_FLINK_GROUP_ID", "flink-group"));

        run(env, properties);
    }

    public static void run(StreamExecutionEnvironment env, Properties kafkaProperties) throws Exception {
        // Retrieve Kafka topics from environment variables
        var slackMessagesTopic = getOrDefault("SLACK_MESSAGES_TOPIC", "slack_messages");
        var llamaResultsTopic = getOrDefault("LLAMA_RESULTS_TOPIC", "llama_results");

        // Disable operator chaining for better visualization
        env.disableOperatorChaining();

        // Consumer for reading Slack messages
        var slackMessagesConsumer = new FlinkKafkaConsumer<>(slackMessagesTopic, new SlackMessageDeserializationSchema(), kafkaProperties);
        var slackMessagesStream = env.addSource(slackMessagesConsumer)
                .name("Kafka Source: Slack Messages")
                .uid("kafka-source-slack-messages");

        // Apply windowing function
        var windowedStream = slackMessagesStream
                .keyBy(SlackMessage::getUser)
                .window(TumblingProcessingTimeWindows.of(Time.minutes(1)));

        // Embedding Generation
        var embeddingsStream = windowedStream
                .apply(new EmbeddingGenerationFunction())
                .name("Generate Embeddings")
                .uid("generate-embeddings");

        // Define a MapStateDescriptor for the broadcast state
        var themeEmbeddingsDescriptor = new MapStateDescriptor<>(
                "ThemeEmbeddings", String.class, int[].class);
        var themeEmbeddingsStream = generateDynamicThemeEmbeddings(env);
        var themeEmbeddingsBroadcastStream = themeEmbeddingsStream
                .broadcast(themeEmbeddingsDescriptor);

        // Semantic Matching
        var semanticMatchedStream = embeddingsStream
                .connect(themeEmbeddingsBroadcastStream)
                .process(new SemanticMatchingFunction())
                .name("Semantic Matching")
                .uid("semantic-matching");

        // Clustering Function
        var clusteredThemesStream = semanticMatchedStream
                .map(new ClusteringFunction())
                .name("Clustering Function")
                .uid("clustering-function");

        // Sink the results to a Kafka topic
        clusteredThemesStream.sinkTo(createKafkaSink(kafkaProperties.getProperty("bootstrap.servers"), llamaResultsTopic))
                .name("Sink Theme Extraction Results to Kafka")
                .uid("kafka-sink-theme-extraction-results");

        env.execute("Theme Extraction Job");
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
