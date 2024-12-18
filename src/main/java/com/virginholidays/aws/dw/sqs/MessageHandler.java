package com.virginholidays.aws.dw.sqs;

import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;

/**
 * Handles messages that were received by the {@link SqsListenerImpl}.
 * 
 * @author Veysel Tosun
 */
public abstract class MessageHandler {

    /**
     * Message attribute name that identifies the message type.
     */
    public static final String ATTR_MESSAGE_TYPE = "MessageType";

    private final String messageType;

    /**
     * Implementors are strongly encouraged to call this constructor with a
     * fixed value from their constructor(s).
     * 
     * @param messageType
     *            Identifies the type of messages that this handler will handle.
     */
    protected MessageHandler(String messageType) {
        if (messageType == null) {
            throw new IllegalArgumentException("Message type cannot be null!");
        }

        this.messageType = messageType;
    }

    /**
     * Determines whether the supplied messages can be handled by this handler.
     * 
     * @param message
     *            The message to test.
     * @return True when the supplied message can be handled, false otherwise.
     */
    public boolean canHandle(Message message) {
        if (message == null) {
            return false;
        }

        MessageAttributeValue attrValue = message.messageAttributes().get(ATTR_MESSAGE_TYPE);
        if (attrValue == null) {
            return false;
        }

        return attrValue.stringValue().equalsIgnoreCase(messageType);
    }

    /**
     * Implementations of this method must be able to handle messages of the
     * type that was supplied to the constructor.
     * 
     * @param message
     *            The message to be handled.
     */
    public abstract void handle(Message message);
}
