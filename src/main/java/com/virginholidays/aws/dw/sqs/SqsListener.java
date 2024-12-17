package com.virginholidays.aws.dw.sqs;

import io.dropwizard.lifecycle.Managed;

/**
 * Managed {@link software.amazon.awssdk.services.sqs.SqsClient} queue listener.
 * 
 * @author Veysel Tosun
 */
public interface SqsListener extends Managed {

    /**
     * Health check of the associated SQS queue.
     * 
     * @return True when the SQS queue for this instance is healthy, false
     *         otherwise.
     */
    boolean isHealthy();

    /**
     * @return The URL of the associated SQS queue.
     */
    String getQueueUrl();
}
