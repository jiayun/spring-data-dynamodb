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
package org.socialsignin.spring.data.dynamodb.core;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.socialsignin.spring.data.dynamodb.domain.sample.Playlist;
import org.socialsignin.spring.data.dynamodb.domain.sample.PlaylistId;
import org.socialsignin.spring.data.dynamodb.domain.sample.User;
import org.socialsignin.spring.data.dynamodb.repository.support.DynamoDBEntityInformation;
import org.springframework.context.ApplicationContext;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.model.BatchWriteItemEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DynamoDBTemplateTest {
	@Mock
	private DynamoDbEnhancedClient dynamoDBMapper;
	@Mock
	private DynamoDbClient dynamoDB;
	@Mock
	private ApplicationContext applicationContext;

	private DynamoDBTemplate dynamoDBTemplate;

	@Mock
	private DynamoDBEntityInformation<User, String> userEntityInformation;

	@Mock
	private DynamoDBEntityInformation<Playlist, PlaylistId> playlistEntityInformation;

	@BeforeEach
	public void setUp() {
		this.dynamoDBTemplate = new DynamoDBTemplate(dynamoDB, dynamoDBMapper);
		this.dynamoDBTemplate.setApplicationContext(applicationContext);
	}

	@Test
	public void testConstructorAllNull() {
		try {
			dynamoDBTemplate = new DynamoDBTemplate(null, null);
			fail("AmazonDynamoDB must not be null!");
		} catch (IllegalArgumentException iae) {
			// ignored
		}

		try {
			dynamoDBTemplate = new DynamoDBTemplate(dynamoDB, null);
			fail("DynamoDBMapper must not be null!");
		} catch (IllegalArgumentException iae) {
			// ignored
		}
		try {
			dynamoDBTemplate = new DynamoDBTemplate(dynamoDB, dynamoDBMapper);
			fail("DynamoDBMapperConfig must not be null!");
		} catch (IllegalArgumentException iae) {
			// ignored
		}
		assertTrue(true);
	}

	// TODO remove and replace with postprocessor test
	@Test
	public void testConstructorOptionalPreconfiguredDynamoDBMapper() {
		// Introduced constructor via #91 should not fail its assert
		assertDoesNotThrow(() -> new DynamoDBTemplate(dynamoDB, dynamoDBMapper));
	}

	@Test
	public void testDelete() {
		DynamoDbTable<User> table = dynamoDBTemplate.getDynamoDbTable(User.class, "user");
		when(userEntityInformation.getTable()).thenReturn(table);

		User user = new User();
		dynamoDBTemplate.delete(user, userEntityInformation);

		verify(table).deleteItem(user);
	}

	@Test
	public void testBatchDelete_CallsCorrectDynamoDBMapperMethod() {
		DynamoDbTable<User> table = dynamoDBTemplate.getDynamoDbTable(User.class, "user");
		when(userEntityInformation.getTable()).thenReturn(table);

		List<User> users = new ArrayList<>();
		dynamoDBTemplate.batchDelete(users, userEntityInformation);
		verify(dynamoDBMapper).batchWriteItem(any(BatchWriteItemEnhancedRequest.class));
	}

	@Test
	public void testSave() {
		DynamoDbTable<User> table = dynamoDBTemplate.getDynamoDbTable(User.class, "user");
		when(userEntityInformation.getTable()).thenReturn(table);

		User user = new User();
		dynamoDBTemplate.save(user, userEntityInformation);

		verify(table).putItem(user);
	}

	@Test
	public void testBatchSave_CallsCorrectDynamoDBMapperMethod() {
		DynamoDbTable<User> table = dynamoDBTemplate.getDynamoDbTable(User.class, "user");
		when(userEntityInformation.getTable()).thenReturn(table);

		List<User> users = new ArrayList<>();
		dynamoDBTemplate.batchSave(users, userEntityInformation);

		verify(dynamoDBMapper).batchWriteItem(any(BatchWriteItemEnhancedRequest.class));
	}

	@Test
	public void testCountQuery() {
		QueryRequest.Builder builder = QueryRequest.builder();
		builder.tableName("user");
		QueryRequest query = builder.build();

		dynamoDBTemplate.count(User.class, builder);

		verify(dynamoDB).query(query);
	}

	@Test
	public void testCountScan() {
		ScanRequest.Builder builder = ScanRequest.builder();
		builder.tableName("user");
		ScanRequest scan = builder.build();

		int actual = dynamoDBTemplate.count(User.class, builder, null);

		assertEquals(0, actual);
		verify(dynamoDB).scan(scan);
	}

	@Test
	public void testLoadByHashKey_WhenDynamoDBMapperReturnsNull() {
		DynamoDbTable<User> table = dynamoDBTemplate.getDynamoDbTable(User.class, "user");
		when(userEntityInformation.getTable()).thenReturn(table);

		User user = dynamoDBTemplate.load(User.class, "someHashKey", userEntityInformation);
		Assertions.assertNull(user);
	}

	@Test
	public void testLoadByHashKeyAndRangeKey_WhenDynamoDBMapperReturnsNull() {
		DynamoDbTable<Playlist> table = dynamoDBTemplate.getDynamoDbTable(Playlist.class, "playlist");
		when(playlistEntityInformation.getTable()).thenReturn(table);

		Playlist playlist = dynamoDBTemplate.load(Playlist.class, "someHashKey", "someRangeKey", playlistEntityInformation);
		Assertions.assertNull(playlist);
	}

}
