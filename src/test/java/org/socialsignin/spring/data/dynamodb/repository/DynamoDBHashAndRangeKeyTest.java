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
package org.socialsignin.spring.data.dynamodb.repository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@ExtendWith(MockitoExtension.class)
public class DynamoDBHashAndRangeKeyTest {

	@Mock
	private Object hash;
	@Mock
	private Object range;

	@Test
	public void testConstructor() {
		DynamoDBHashAndRangeKey underTest = new DynamoDBHashAndRangeKey(hash, range);

		assertEquals(hash, underTest.getHashKey());
		assertEquals(range, underTest.getRangeKey());
	}

	@Test
	public void testDefaultConstructor() {
		DynamoDBHashAndRangeKey underTest = new DynamoDBHashAndRangeKey();

		assertNull(underTest.getHashKey());
		assertNull(underTest.getRangeKey());
	}

	@Test
	public void testGetterSetter() {
		DynamoDBHashAndRangeKey underTest = new DynamoDBHashAndRangeKey();

		underTest.setHashKey(hash);
		underTest.setRangeKey(range);

		assertEquals(hash, underTest.getHashKey());
		assertEquals(range, underTest.getRangeKey());
	}
}
