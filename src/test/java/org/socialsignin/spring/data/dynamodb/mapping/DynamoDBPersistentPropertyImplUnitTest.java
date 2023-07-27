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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbIgnore;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Unit tests for {@link DynamoDBPersistentPropertyImpl}.
 * 
 * @author Michael Lavelle
 * @author Sebastian Just
 */
@ExtendWith(MockitoExtension.class)
public class DynamoDBPersistentPropertyImplUnitTest {

	DynamoDBMappingContext context;
	DynamoDBPersistentEntity<?> entity;

	@BeforeEach
	public void setUp() {

		context = new DynamoDBMappingContext();
		entity = context.getPersistentEntity(Sample.class);
	}

	/**
	 * @see DATAJPA-284
	 */
	@Test
	public void considersOtherPropertiesAsNotTransient() {

		DynamoDBPersistentProperty property = entity.getPersistentProperty("otherProp");
		assertThat(property, is(notNullValue()));
	}

	/**
	 * @see DATAJPA-376
	 */
	@Test
	public void considersDynamoDBIgnoredPropertiesAsTransient() {
		assertThat(entity.getPersistentProperty("ignoredProp"), is(nullValue()));
	}

	@DynamoDbBean
	@Table(name = "sample")
	static class Sample {

		private String ignoredProp = "ignored";
		private String otherProp = "other";

		public String getOtherProp() {
			return otherProp;
		}

		public void setOtherProp(String otherProp) {
			this.otherProp = otherProp;
		}

		@DynamoDbIgnore
		public String getIgnoredProp() {
			return ignoredProp;
		}

		public void setIgnoredProp(String ignoredProp) {
			this.ignoredProp = ignoredProp;
		}
	}

}
