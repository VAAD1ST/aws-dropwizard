package com.virginholidays.aws.dw.core;

import io.dropwizard.lifecycle.Managed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sqs.SqsClient;

/**
 * Manages an aws client with the Dropwizard lifecycle.
 */
public class ManagedAwsClient implements Managed {

    private static final Logger LOG = LoggerFactory.getLogger(ManagedAwsClient.class);

    private SqsClient sqsClient;
    private SnsClient snsClient;

    public ManagedAwsClient(SqsClient sqsClient) {
        if (sqsClient == null) {
            throw new IllegalArgumentException("aws sqs client cannot be null");
        }

        this.sqsClient = sqsClient;
    }

    public ManagedAwsClient(SnsClient snsClient) {
        if (snsClient == null) {
            throw new IllegalArgumentException("aws sns client cannot be null");
        }
        this.snsClient = snsClient;
    }

    @Override
    public void start() {
        // Do nothing...
    }

    @Override
    public void stop() {
        if(sqsClient != null) {
            LOG.info("Shutting down aws sqs client, {}", sqsClient.getClass());
            sqsClient.close();
        } else if (snsClient != null) {
            LOG.info("Shutting down aws sns client, {}", snsClient.getClass());
            snsClient.close();
        }
    }

}
