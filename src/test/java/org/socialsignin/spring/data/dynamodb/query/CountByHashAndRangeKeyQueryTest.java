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
package org.socialsignin.spring.data.dynamodb.query;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.socialsignin.spring.data.dynamodb.core.DynamoDBOperations;
import org.socialsignin.spring.data.dynamodb.domain.sample.User;

import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CountByHashAndRangeKeyQueryTest {
	private static final Class<User> DOMAIN_CLASS = User.class;
	@Mock
	private DynamoDBOperations dynamoDBOperations;
	@Mock
	private User sampleEntity;
	private Object hashKey;
	private Object rangeKey;
	private CountByHashAndRangeKeyQuery<User> underTest;

	@BeforeEach
	public void setUp() {
		hashKey = ThreadLocalRandom.current().nextLong();
		rangeKey = ThreadLocalRandom.current().nextLong();
		underTest = new CountByHashAndRangeKeyQuery<User>(dynamoDBOperations, DOMAIN_CLASS, hashKey, rangeKey, null);
	}

	@Test
	public void testGetSingleResultExists() {
		when(dynamoDBOperations.load(DOMAIN_CLASS, hashKey, rangeKey, null)).thenReturn(sampleEntity);
		Long actual = underTest.getSingleResult();

		assertEquals(Long.valueOf(1), actual);
	}

	@Test
	public void testGetSingleResultDoesntExist() {
		when(dynamoDBOperations.load(DOMAIN_CLASS, hashKey, rangeKey, null)).thenReturn(null);
		Long actual = underTest.getSingleResult();

		assertEquals(Long.valueOf(0), actual);
	}
}
