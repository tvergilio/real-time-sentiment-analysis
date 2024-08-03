package com.createfuture.flink.transfer;

import com.createfuture.flink.model.SlackMessage;
import org.apache.flink.api.common.serialization.DeserializationSchema;
import org.apache.flink.api.common.typeinfo.TypeInformation;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class SlackMessageDeserializationSchema implements DeserializationSchema<SlackMessage> {

    @Override
    public SlackMessage deserialize(byte[] message) throws IOException {
        var msg = new String(message, StandardCharsets.UTF_8);
        // Example input: "Timestamp: 1721903155.837829, User: U07DET2KZ4P, Message: Fantastic!"
        var parts = msg.split(", ", 3);  // Limit the split to 3 parts to avoid splitting the message content

        var timestamp = (long) Double.parseDouble(parts[0].split(": ")[1]);
        var user = parts[1].split(": ")[1];
        var messageText = parts[2].split(": ", 2)[1];  // Limit the split to 2 parts to keep the message content intact

        return new SlackMessage(timestamp, user, messageText);
    }

    @Override
    public boolean isEndOfStream(SlackMessage nextElement) {
        return false;
    }

    @Override
    public TypeInformation<SlackMessage> getProducedType() {
        return TypeInformation.of(SlackMessage.class);
    }
}
