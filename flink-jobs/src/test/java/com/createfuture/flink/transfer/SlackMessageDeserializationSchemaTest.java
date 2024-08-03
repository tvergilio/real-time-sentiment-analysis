package com.createfuture.flink.transfer;

import com.createfuture.flink.model.SlackMessage;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SlackMessageDeserializationSchemaTest {

    @Test
    void testDeserialize() throws Exception {
        String input = "Timestamp: 1721903155.837829, User: U07DET2KZ2B, Message: Fantastic!";
        SlackMessageDeserializationSchema schema = new SlackMessageDeserializationSchema();
        SlackMessage message = schema.deserialize(input.getBytes());

        assertEquals(1721903155L, message.getTimestamp());
        assertEquals("U07DET2KZ2B", message.getUser());
        assertEquals("Fantastic!", message.getMessage());
    }

    @Test
    void testIsEndOfStream() {
        SlackMessageDeserializationSchema schema = new SlackMessageDeserializationSchema();
        assertFalse(schema.isEndOfStream(new SlackMessage()));
    }

    @Test
    void testGetProducedType() {
        SlackMessageDeserializationSchema schema = new SlackMessageDeserializationSchema();
        assertEquals(TypeInformation.of(SlackMessage.class), schema.getProducedType());
    }
}

