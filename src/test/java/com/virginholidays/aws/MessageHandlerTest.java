package com.virginholidays.aws;

import java.util.Map;

import com.google.common.collect.Maps;
import com.virginholidays.aws.dw.sqs.MessageHandler;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;

import static org.junit.jupiter.api.Assertions.*;

public class MessageHandlerTest {

    private static final String TEST_TYPE = "TestType";

    @Test
    public void testConstructor() {
        assertThrows(IllegalArgumentException.class, () -> new MessageHandler(null) {
            @Override
            public void handle(Message message) {
            }
        }, "Constructor should not have accepted null");
    }

    @Test
    public void canHandleUnknownValue() {
        // Prepare message
        Map<String, MessageAttributeValue> messageAttributes = Maps.newHashMap();

        MessageAttributeValue messageAttributeValue =
                MessageAttributeValue.builder().dataType("String").stringValue("unknown-value").build();
        messageAttributes.put(MessageHandler.ATTR_MESSAGE_TYPE, messageAttributeValue);

        Message message = Message.builder().messageAttributes(messageAttributes).build();

        // Prepare handler
        MessageHandler fixture = new MessageHandler(TEST_TYPE) {
            @Override
            public void handle(Message message) {
            }
        };

        // Verify
        assertFalse(fixture.canHandle(message));
    }

    @Test
    public void canHandle() {
        // Prepare message
        Map<String, MessageAttributeValue> messageAttributes = Maps.newHashMap();

        MessageAttributeValue messageAttributeValue =
                MessageAttributeValue.builder().dataType("String").stringValue(TEST_TYPE).build();
        messageAttributes.put(MessageHandler.ATTR_MESSAGE_TYPE, messageAttributeValue);

        Message message = Message.builder().messageAttributes(messageAttributes).build();

        // Prepare handler
        MessageHandler fixture = new MessageHandler(TEST_TYPE) {
            @Override
            public void handle(Message message) {
            }
        };

        // Verify
        assertTrue(fixture.canHandle(message));
        assertFalse(fixture.canHandle(Message.builder().build()));
        assertFalse(fixture.canHandle(null));
    }
}
