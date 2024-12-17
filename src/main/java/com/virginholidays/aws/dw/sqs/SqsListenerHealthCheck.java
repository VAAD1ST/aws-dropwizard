package com.virginholidays.aws.dw.sqs;

import com.codahale.metrics.health.HealthCheck;

/**
 * Implements a health check for the {@link SqsListener}.
 * 
 * @author Veysel Tosun
 */
public class SqsListenerHealthCheck extends HealthCheck {

    private final SqsListener sqsListener;

    public SqsListenerHealthCheck(SqsListener sqsListener) {
        this.sqsListener = sqsListener;
    }

    @Override
    protected Result check() {
        if (sqsListener.isHealthy()) {
            return Result.healthy();
        } else {
            return Result.unhealthy("There is a problem with the SQS listener for queue: " + sqsListener.getQueueUrl());
        }
    }
}
