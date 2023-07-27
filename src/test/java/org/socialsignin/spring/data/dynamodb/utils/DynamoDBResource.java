/**
 * Copyright © 2018 spring-data-dynamodb (https://github.com/boostchicken/spring-data-dynamodb)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.socialsignin.spring.data.dynamodb.utils;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.Assert;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.net.URI;

/**
 * Clue {@link Configuration} class for all integration tests. It exposes the
 * {@link DynamoDbClient} pre-configured to use the launched local DynamoDB by
 * Maven's integration-test.
 */
@Configuration
public class DynamoDBResource {
    private static final String DYNAMODB_PORT_PROPERTY = "dynamodb.port";
    private static final String PORT = System.getProperty(DYNAMODB_PORT_PROPERTY);

    @Bean
    public DynamoDbClient amazonDynamoDB() {
        Assert.notNull(PORT, "System property '" + DYNAMODB_PORT_PROPERTY + " not set!");

        return DynamoDbClient.builder()
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("AWS-Key", "")))
                .endpointOverride(URI.create(String.format("http://localhost:%s", PORT)))
                .region(Region.US_EAST_1)
                .build();
    }

}
