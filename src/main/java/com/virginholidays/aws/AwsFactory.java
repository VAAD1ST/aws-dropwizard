package com.virginholidays.aws;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.virginholidays.aws.dw.core.ManagedAwsClient;
import io.dropwizard.core.setup.Environment;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sqs.SqsClient;

import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

/**
 * Builds a managed {@link SqsClient} instance.
 * 
 * @author Veysel Tosun
 */
@Getter
public class AwsFactory {

    private static final Logger LOG = LoggerFactory.getLogger(AwsFactory.class);

    @JsonProperty
    private String awsAccessKeyId;

    @JsonProperty
    private String awsSecretKey;

    @JsonProperty
    private String awsRegion;

    private static final Region DEFAULT_REGION = Region.EU_WEST_1;

    /**
     * Builds an {@link SqsClient} instance that is managed by the server's
     * lifecycle. Reference: http://docs.aws.amazon.com/AWSSdkDocsJava/latest/DeveloperGuide/credentials.html
     *
     * @param env
     *            The environment where the {@link SqsClient} will be
     *            registered.
     * @return A managed instance.
     */
    public SqsClient buildSQSClient(Environment env) {
        LOG.info("Initialize Amazon SQS entry point");

        // Initialize SQS Client based on credentials
        SqsClient sqs;
        if (isEmpty(awsAccessKeyId) || isEmpty(awsSecretKey)) {
            sqs = SqsClient.builder()
                    .credentialsProvider(DefaultCredentialsProvider.create())
                    .region(isNotEmpty(awsRegion) ? Region.of(awsRegion) : DEFAULT_REGION)
                    .build();
        } else {
            AwsBasicCredentials awsCreds = AwsBasicCredentials.create(awsAccessKeyId, awsSecretKey);
            sqs = SqsClient.builder()
                    .credentialsProvider(StaticCredentialsProvider.create(awsCreds))
                    .region(isNotEmpty(awsRegion) ? Region.of(awsRegion) : DEFAULT_REGION)
                    .build();
        }

        // Manage the SQS client with Dropwizard's lifecycle
        env.lifecycle().manage(new ManagedAwsClient(sqs));

        return sqs;
    }

    /**
     * Builds an {@link SnsClient} instance that is managed by the server's
     * lifecycle. Reference: http://docs.aws.amazon.com/AWSSdkDocsJava/latest/DeveloperGuide/credentials.html
     *
     * @param env
     *            The environment where the {@link SnsClient} will be
     *            registered.
     * @return A managed instance.
     */
    public SnsClient buildSNSClient(Environment env) {
        LOG.info("Initialize AMAZON SNS entry point");

        // Initialize SQS Client based on credentials
        SnsClient sns;
        if (isEmpty(awsAccessKeyId) || isEmpty(awsSecretKey)) {
            sns = SnsClient.builder()
                    .credentialsProvider(DefaultCredentialsProvider.create())
                    .region(isNotEmpty(awsRegion) ? Region.of(awsRegion) : DEFAULT_REGION)
                    .build();
        } else {
            AwsBasicCredentials awsCreds = AwsBasicCredentials.create(awsAccessKeyId, awsSecretKey);
            sns = SnsClient.builder()
                    .credentialsProvider(StaticCredentialsProvider.create(awsCreds))
                    .region(isNotEmpty(awsRegion) ? Region.of(awsRegion) : DEFAULT_REGION)
                    .build();
        }

        env.lifecycle().manage(new ManagedAwsClient(sns));

        return sns;
    }
}
