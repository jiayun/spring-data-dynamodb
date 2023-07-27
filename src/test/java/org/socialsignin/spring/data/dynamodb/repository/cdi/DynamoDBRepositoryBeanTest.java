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
package org.socialsignin.spring.data.dynamodb.repository.cdi;

import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.socialsignin.spring.data.dynamodb.core.DynamoDBOperations;
import org.socialsignin.spring.data.dynamodb.domain.sample.User;
import org.springframework.data.repository.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
public class DynamoDBRepositoryBeanTest {
	interface SampleRepository extends Repository<User, String> {
	}

	@Mock
	private CreationalContext<DynamoDbClient> creationalContext;
	@Mock
	private CreationalContext<SampleRepository> repoCreationalContext;
	@Mock
	private BeanManager beanManager;
	@Mock
	private Bean<DynamoDbClient> amazonDynamoDBBean;
	@Mock
	private DynamoDbClient amazonDynamoDB;
	@Mock
	private Bean<DynamoDBOperations> dynamoDBOperationsBean;

	@Mock
	private Bean<DynamoDbEnhancedClient> dynamoDBMapperBean;
	private Set<Annotation> qualifiers = Collections.emptySet();
	private Class<SampleRepository> repositoryType = SampleRepository.class;

	@BeforeEach
	public void setUp() {
		lenient().when(beanManager.createCreationalContext(amazonDynamoDBBean)).thenReturn(creationalContext);
		lenient().when(beanManager.getReference(amazonDynamoDBBean, DynamoDbClient.class, creationalContext))
				.thenReturn(amazonDynamoDB);
	}

	@Test
	public void testNullOperationsOk() {
		DynamoDBRepositoryBean<SampleRepository> underTest = new DynamoDBRepositoryBean<>(beanManager,
				amazonDynamoDBBean, null, dynamoDBMapperBean, qualifiers, repositoryType);

		assertNotNull(underTest);
	}

	@Test
	public void testNullOperationFail() {
		assertThrows(IllegalArgumentException.class, () -> {
			new DynamoDBRepositoryBean<>(beanManager, null, null, null, qualifiers,
					repositoryType);
		}, "amazonDynamoDBBean must not be null!");
	}

	@Test
	public void testSetOperationOk1() {
		DynamoDBRepositoryBean<SampleRepository> underTest = new DynamoDBRepositoryBean<>(beanManager, null,
				dynamoDBOperationsBean, dynamoDBMapperBean, qualifiers, repositoryType);

		assertNotNull(underTest);
	}

	@Test
	public void testSetOperationFail1() {
		assertThrows(IllegalArgumentException.class, () -> {
			new DynamoDBRepositoryBean<>(beanManager, null, dynamoDBOperationsBean, dynamoDBMapperBean,
					qualifiers, repositoryType);
		}, "Cannot specify both dynamoDBMapperConfigBean bean and dynamoDBOperationsBean in repository configuration");
	}

	@Test
	public void testSetOperationFail2() {
		assertThrows(IllegalArgumentException.class, () -> {
			new DynamoDBRepositoryBean<>(beanManager, amazonDynamoDBBean, dynamoDBOperationsBean,
					dynamoDBMapperBean, qualifiers, repositoryType);
		}, "Cannot specify both amazonDynamoDB bean and dynamoDBOperationsBean in repository configuration");
	}

	@Test
	public void testCreateRepository() {
		DynamoDBRepositoryBean<SampleRepository> underTest = new DynamoDBRepositoryBean<>(beanManager,
				amazonDynamoDBBean, null, dynamoDBMapperBean, qualifiers, repositoryType);

		SampleRepository actual = underTest.create(repoCreationalContext, SampleRepository.class);
		assertNotNull(actual);
	}
}
