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
package org.socialsignin.spring.data.dynamodb.mapping;

import jakarta.persistence.Table;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.socialsignin.spring.data.dynamodb.repository.DynamoDBHashAndRangeKey;
import org.springframework.data.annotation.Id;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbIgnore;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Unit tests for {@link DynamoDBMappingContext}.
 * 
 * @author Michael Lavelle
 * @author Sebastian Just
 */
@ExtendWith(MockitoExtension.class)
public class DynamoDBMappingContextTest {
	@DynamoDbBean
	@Table(name = "a")
	static class DynamoDBMappingContextTestFieldEntity {

		private String hashKey;

		private String rangeKey;

		@SuppressWarnings("unused")
		private String someProperty;

		@DynamoDbPartitionKey
		public String getHashKey() {
			return hashKey;
		}

		@DynamoDbSortKey
		public String getRangeKey() {
			return rangeKey;
		}
	}

	@DynamoDbBean
	@Table(name = "b")
	static class DynamoDBMappingContextTestMethodEntity {

		@DynamoDbPartitionKey
		public String getHashKey() {
			return null;
		}

		@DynamoDbSortKey
		public String getRangeKey() {
			return null;
		}

		public String getSomeProperty() {
			return null;
		}
	}

	@DynamoDbBean
	@Table(name = "c")
	static class DynamoDBMappingContextTestIdEntity {
		@Id
		private DynamoDBHashAndRangeKey hashRangeKey;

		@DynamoDbIgnore
		public String getSomething() {
			return null;
		}
	}

	private DynamoDBMappingContext underTest;

	@BeforeEach
	public void setUp() {
		underTest = new DynamoDBMappingContext();
	}

	@Test
	public void detectPropertyAnnotation() {

		DynamoDBPersistentEntityImpl<?> entity = underTest
				.getPersistentEntity(DynamoDBMappingContextTestFieldEntity.class);

		assertNotNull(entity);
		assertThat(entity.getIdProperty(), is(notNullValue()));
	}

	@Test
	@Disabled
	public void detectMethodsAnnotation() {
		DynamoDBPersistentEntityImpl<?> entity = underTest
				.getPersistentEntity(DynamoDBMappingContextTestMethodEntity.class);

		assertNotNull(entity);
		assertThat(entity.getIdProperty(), is(notNullValue()));

	}

	@Test
	public void detectMethodsId() {
		DynamoDBPersistentEntityImpl<?> entity = underTest
				.getPersistentEntity(DynamoDBMappingContextTestIdEntity.class);

		assertNotNull(entity);
		assertThat(entity.getIdProperty(), is(notNullValue()));

	}
}
