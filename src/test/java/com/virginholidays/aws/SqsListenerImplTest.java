package com.virginholidays.aws;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Set;

import com.virginholidays.aws.dw.sqs.MessageHandler;
import com.virginholidays.aws.dw.sqs.SqsListenerImpl;
import org.glassfish.jersey.internal.guava.Sets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;

/**
 * Tests {@link SqsListenerImpl} lifecycle scenario's.
 * 
 * @author Veysel Tosun
 */
@ExtendWith(MockitoExtension.class)
public class SqsListenerImplTest {

    private static final int WAIT = 500;

    private static final Logger LOG = LoggerFactory.getLogger(SqsListenerImplTest.class);

    private static final String TEST_QUEUE_URL = "test-queue-url";

    @Mock
    private SqsClient sqsClient;

    @Mock
    private MessageHandler handler;

    private SqsListenerImpl fixture;

    private final List<Message> messageList = List.of(Message.builder().build(), Message.builder().build());

    @BeforeEach
    public void setUp() {
        Set<MessageHandler> handlers = Sets.newHashSet();
        handlers.add(handler);
        fixture = new SqsListenerImpl(sqsClient, TEST_QUEUE_URL, handlers);
    }

    @Test
    public void testLifecycleHealthy() throws Exception {
        LOG.debug("testLifecycleHealthy()...");
        fixture.start();
        assertTrue(fixture.isHealthy());
        fixture.stop();
    }

    @Test
    public void testLifecycleUnhealthy() throws Exception {
        LOG.debug("testLifecycleUnhealthy()...");

        // Mocking the SQS client to throw an exception
        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenThrow(SqsException.builder()
                        .message("Simulated SQS exception")
                        .awsErrorDetails(AwsErrorDetails.builder()
                                .errorMessage("Simulated error for " + TEST_QUEUE_URL)
                                .build())
                        .build());

        fixture.start();
        Thread.sleep(WAIT);
        assertFalse(fixture.isHealthy());
        fixture.stop();
    }

    @Test
    public void testLifecycleHealthyWithMessages() throws Exception {
        LOG.debug("testLifecycleHealthyWithMessages()...");

        ReceiveMessageResponse result = ReceiveMessageResponse.builder().messages(messageList).build();

        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class))).thenReturn(result);

        fixture.start();
        Thread.sleep(WAIT);
        assertTrue(fixture.isHealthy());
        fixture.stop();
    }

    @Test
    public void testDoesNotDeleteMessagesIfHandlerThrowsException() throws Exception {
        LOG.debug("testDoesNotDeleteMessagesIfHandlerThrowsException()...");

        ReceiveMessageResponse result = ReceiveMessageResponse.builder().messages(messageList).build();

        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class))).thenReturn(result);

        when(handler.canHandle(any(Message.class))).thenReturn(true);

        doThrow(new RuntimeException("should prevent message being deleted"))
                .when(handler).handle(any(Message.class));

        fixture.start();
        Thread.sleep(WAIT);
        fixture.stop();

        verify(sqsClient, never()).deleteMessage(any(DeleteMessageRequest.class));
    }

    @Test
    public void testDoesNotDeleteMessagesIfHandlerCannotProcess() throws Exception {
        LOG.debug("testDoesNotDeleteMessagesIfHandlerCannotProcess()...");

        ReceiveMessageResponse result = ReceiveMessageResponse.builder().messages(messageList).build();

        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class))).thenReturn(result);

        fixture.start();
        Thread.sleep(WAIT);
        fixture.stop();

        verify(sqsClient, never()).deleteMessage(any(DeleteMessageRequest.class));
    }

    @Test
    public void testDeletesMessagesIfHandlerProcessesOK() throws Exception {
        LOG.debug("testDeletesMessagesIfHandlerProcessesOK()...");

        ReceiveMessageResponse result = ReceiveMessageResponse.builder().messages(messageList).build();

        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class))).thenReturn(result);

        when(handler.canHandle(any(Message.class))).thenReturn(true);

        fixture.start();
        Thread.sleep(WAIT);
        fixture.stop();

        verify(sqsClient, atLeastOnce()).deleteMessage(any(DeleteMessageRequest.class));
    }
}