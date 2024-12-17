package com.virginholidays.aws.dw.sqs;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import com.virginholidays.aws.AwsFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

/**
 * Listens to a queue and dispatches received messages to the supplied
 * {@link MessageHandler} implementation.
 * 
 * @see AwsFactory
 * @see SqsListenerHealthCheck
 * @author Veysel Tosun
 */
public class SqsListenerImpl implements SqsListener {

    private static final int SLEEP_ON_ERROR = 5000;

    private static final Logger LOG = LoggerFactory.getLogger(SqsListenerImpl.class);

    /**
     * SQS message receiver flag that indicates all message attributes should be returned
     */
    private static final String ATTR_ALL = "All";

    private final AtomicBoolean healthy = new AtomicBoolean(true);
    private final SqsClient sqsClient;
    private final String sqsListenQueueUrl;
    private final Set<MessageHandler> handlers;
    private final String interruptedMsg;

    private Thread pollingThread;

    /**
     * @param sqsClient
     *            Managed {@link SqsClient} instance that this listener will use
     *            to connect to its queue.
     * @param sqsListenQueueUrl
     *            URL of the queue where this instance will listen to (
     *            {@link Named} sqsListenQueueUrl).
     * @param handlers
     *            All handlers will be called for every message that this
     *            instance receives.
     */
    @Inject
    public SqsListenerImpl(SqsClient sqsClient, @Named("sqsListenQueueUrl") String sqsListenQueueUrl,
                           Set<MessageHandler> handlers) {
        this.sqsClient = sqsClient;
        this.sqsListenQueueUrl = sqsListenQueueUrl;
        this.handlers = handlers;

        interruptedMsg = "Stop listening to queue: " + sqsListenQueueUrl;
    }

    @Override
    public void start() {
        pollingThread = new Thread(this::pollMessages);
        pollingThread.start();
    }

    private void pollMessages() {
        LOG.info("Start listening to queue: {}", sqsListenQueueUrl);

        ReceiveMessageRequest receiveMessageRequest = createReceiveMessageRequest();

        while (!Thread.currentThread().isInterrupted()) {
            try {
                List<Message> messages = sqsClient.receiveMessage(receiveMessageRequest).messages();
                processMessages(messages);
                handleRecovery();
            } catch (Exception e) {
                handleQueueError(e);
            }
        }

        LOG.info(interruptedMsg);
    }

    private ReceiveMessageRequest createReceiveMessageRequest() {
        return ReceiveMessageRequest.builder()
                .messageAttributeNames(ATTR_ALL)
                .queueUrl(sqsListenQueueUrl)
                .build();
    }

    private void processMessages(List<Message> messages) {
        LOG.debug("Received {} messages", messages.size());

        for (int i = 0; i < messages.size(); i++) {
            Message msg = messages.get(i);
            LOG.debug("Processing message {} of {}", i + 1, messages.size());
            processMessageWithHandlers(msg, i, messages);
        }
    }

    private void processMessageWithHandlers(Message msg, int index, List<Message> messages) {
        try {
            for (MessageHandler handler : handlers) {
                if (tryHandleMessage(handler, msg)) {
                    deleteMessage(msg, index, messages);
                    break; // Stop after first successful handler
                }
            }
        } catch (Exception e) {
            logProcessingError(msg, e);
        }
    }

    private boolean tryHandleMessage(MessageHandler handler, Message msg) {
        LOG.debug("Calling message handler: {}", handler);
        if (handler.canHandle(msg)) {
            LOG.debug("Message accepted.");
            handler.handle(msg);
            return true;
        }
        LOG.debug("Message refused.");
        return false;
    }

    private void handleRecovery() {
        if (healthy.compareAndSet(false, true)) {
            LOG.info("Queue '{}' recovered from error condition", sqsListenQueueUrl);
        }
    }

    private void deleteMessage(Message msg, int i, List<Message> messages) {
        String messageReceiptHandle = msg.receiptHandle();
        DeleteMessageRequest deleteMessageRequest = DeleteMessageRequest.builder()
                .queueUrl(sqsListenQueueUrl)
                .receiptHandle(messageReceiptHandle)
                .build();
        sqsClient.deleteMessage(deleteMessageRequest);
        LOG.debug("Message {} of {} is processed and deleted from queue '{}'",
                i + 1, messages.size(), sqsListenQueueUrl);
    }

    private void logProcessingError(Message msg, Exception e) {
        StringBuilder builder = new StringBuilder()
                .append("An error occurred while processing the following message:")
                .append("\n\tMessageId:     ").append(msg.messageId())
                .append("\n\tReceiptHandle: ").append(msg.receiptHandle())
                .append("\n\tMD5OfBody:     ").append(msg.md5OfBody())
                .append("\n\tBody:          ").append(msg.body());

        msg.messageAttributes().forEach((key, value) ->
                builder.append("\n\tAttribute:")
                        .append("\n\t\tName:  ").append(key)
                        .append("\n\t\tValue: ").append(value.toString())
        );

        LOG.error(builder.toString(), e);
    }

    private void handleQueueError(Exception e) {
        boolean firstAttempt = healthy.compareAndSet(true, false);
        String errorMsg = "An error occurred while listening to '%s', waiting '%s' ms before retrying...";
        if (!firstAttempt) {
            errorMsg = "Retry failed while listening to '%s', waiting '%s' ms before retrying...";
        }
        LOG.error(String.format(errorMsg, sqsListenQueueUrl, SLEEP_ON_ERROR), e);
        try {
            Thread.sleep(SLEEP_ON_ERROR);
        } catch (InterruptedException ie) {
            LOG.info(interruptedMsg);
        }
    }

    @Override
    public void stop() {
        pollingThread.interrupt();
    }

    @Override
    public boolean isHealthy() {
        return healthy.get();
    }

    @Override
    public String getQueueUrl() {
        return sqsListenQueueUrl;
    }

}
