package com.virginholidays.aws.dw.sqs;

import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.util.HashMap;
import java.util.Map;

/**
 * Helps clients to dispatch messages to SQS, that can be handled by a matching
 * {@link MessageHandler}.
 * 
 * @author Veysel Tosun
 */
public class MessageDispatcher {

    /**
     * Dispatches a message to SQS. {@link MessageHandler}s will handle the
     * message based on a matching value of messageType.
     * 
     * @param messageBody
     *            The body of the message.
     * @param queueUrl
     *            The SQS queue URL.
     * @param messageType
     *            The messageType.
     * @param sqsClient
     *            The SQS client.
     */
    public static void dispatch(String messageBody, String queueUrl, String messageType, SqsClient sqsClient) {
        dispatchDelayed(messageBody, queueUrl, messageType, sqsClient, 0);
    }

    public static void dispatchDelayed(String messageBody, String queueUrl,
                                       String messageType, SqsClient sqsClient, int delaySeconds) {
        sendMessage(messageBody, queueUrl, prepareMessageAttributes(messageType), sqsClient, delaySeconds);
    }

    private static Map<String, MessageAttributeValue> prepareMessageAttributes(String messageType) {
        Map<String, MessageAttributeValue> messageAttributes = new HashMap<>();

        MessageAttributeValue messageAttributeValue =
                MessageAttributeValue.builder().dataType("String").stringValue(messageType).build();

        messageAttributes.put(MessageHandler.ATTR_MESSAGE_TYPE, messageAttributeValue);
        return messageAttributes;
    }

    private static void sendMessage(String messageBody, String queueUrl,
                                    Map<String, MessageAttributeValue> messageAttributes,
                                    SqsClient sqsClient, int delaySeconds) {
        SendMessageRequest request = SendMessageRequest.builder()
                .messageBody(messageBody)
                .queueUrl(queueUrl)
                .messageAttributes(messageAttributes)
                .delaySeconds(delaySeconds)
                .build();
        sqsClient.sendMessage(request);
    }

}
