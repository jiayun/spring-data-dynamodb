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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.socialsignin.spring.data.dynamodb.repository.DynamoDBHashAndRangeKey;
import org.springframework.data.annotation.Id;
import org.springframework.data.mapping.model.Property;
import org.springframework.data.mapping.model.SimpleTypeHolder;
import org.springframework.data.util.TypeInformation;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.util.Arrays;
import java.util.Comparator;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class DynamoDBPersistentEntityTest {

	static class DynamoDBPersistentEntity {
		private String id;

		@Id
		private DynamoDBHashAndRangeKey hashRangeKey;

		@SuppressWarnings("unused")
		private String name;

		@DynamoDbPartitionKey
		public String getId() {
			return id;
		}
	}

	@Mock
	private Comparator<DynamoDBPersistentProperty> comparator;

	private TypeInformation<DynamoDBPersistentEntity> cti = TypeInformation.of(DynamoDBPersistentEntity.class);
	private DynamoDBPersistentEntityImpl<DynamoDBPersistentEntity> underTest;

	@BeforeEach
	public void setUp() {
		underTest = new DynamoDBPersistentEntityImpl<>(cti, comparator);
	}

	@Test
	public void testSomeProperty() throws NoSuchFieldException {
		Property prop = Property.of(cti, DynamoDBPersistentEntity.class.getDeclaredField("name"));

		DynamoDBPersistentProperty property = new DynamoDBPersistentPropertyImpl(prop, underTest,
				SimpleTypeHolder.DEFAULT);
		DynamoDBPersistentProperty actual = underTest.returnPropertyIfBetterIdPropertyCandidateOrNull(property);

		assertNull(actual);
	}

	@Test
	public void testIdProperty() throws NoSuchFieldException, IntrospectionException {
		BeanInfo beanInfo = Introspector.getBeanInfo(DynamoDBPersistentEntity.class);
        PropertyDescriptor pd = Arrays.stream(beanInfo.getPropertyDescriptors())
                .filter(p -> p.getName().equals("id")).findFirst().orElse(null);

		Property prop = Property.of(cti, DynamoDBPersistentEntity.class.getDeclaredField("id"), pd);
		DynamoDBPersistentProperty property = new DynamoDBPersistentPropertyImpl(prop, underTest,
				SimpleTypeHolder.DEFAULT);
		DynamoDBPersistentProperty actual = underTest.returnPropertyIfBetterIdPropertyCandidateOrNull(property);

		assertNotNull(actual);
		assertTrue(actual.isHashKeyProperty());
	}

	@Test
	public void testCompositeIdProperty() throws NoSuchFieldException {
		Property prop = Property.of(cti, DynamoDBPersistentEntity.class.getDeclaredField("hashRangeKey"));
		DynamoDBPersistentProperty property = new DynamoDBPersistentPropertyImpl(prop, underTest,
				SimpleTypeHolder.DEFAULT);
		DynamoDBPersistentProperty actual = underTest.returnPropertyIfBetterIdPropertyCandidateOrNull(property);

		assertNotNull(actual);
		assertTrue(actual.isCompositeIdProperty());
	}
}
