package com.virginholidays.aws;

import com.virginholidays.aws.dw.core.ManagedAwsClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sqs.SqsClient;

/* Test class for ManagedAwsClient
*
* @author Veysel Tosun
*/
public class ManagedAwsClientTest {

    @Test
    public void testRefuseNullSqsParameters() {
        SqsClient sqsClient = null;
        Assertions.assertThrows(IllegalArgumentException.class, () -> new ManagedAwsClient(sqsClient));
    }

    @Test
    public void testRefuseNullSnsParameters() {
        SnsClient snsClient = null;
        Assertions.assertThrows(IllegalArgumentException.class, () -> new ManagedAwsClient(snsClient));
    }


    @Test
    public void testCreateWithSQSClient() {
        try(SqsClient sqs = SqsClient.builder()
                .credentialsProvider(DefaultCredentialsProvider.create())
                .region(Region.EU_WEST_1)
                .build()) {
            Assertions.assertDoesNotThrow(() -> new ManagedAwsClient(sqs));
        }
    }

    @Test
    public void testCreateWithSNSClient() {
        try(SnsClient sns = SnsClient.builder()
                .credentialsProvider(DefaultCredentialsProvider.create())
                .region(Region.EU_WEST_1)
                .build()) {
            Assertions.assertDoesNotThrow(() -> new ManagedAwsClient(sns));
        }
    }

}
