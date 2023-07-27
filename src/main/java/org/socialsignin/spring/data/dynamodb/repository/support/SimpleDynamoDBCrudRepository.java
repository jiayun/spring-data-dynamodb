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
package org.socialsignin.spring.data.dynamodb.repository.support;

import org.socialsignin.spring.data.dynamodb.core.DynamoDBOperations;
import org.socialsignin.spring.data.dynamodb.exception.BatchWriteException;
import org.socialsignin.spring.data.dynamodb.repository.DynamoDBCrudRepository;
import org.socialsignin.spring.data.dynamodb.utils.AttributeValueUtil;
import org.socialsignin.spring.data.dynamodb.utils.ExceptionHandler;
import org.socialsignin.spring.data.dynamodb.utils.SortHandler;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.BatchWriteResult;
import software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Default implementation of the
 * {@link org.springframework.data.repository.CrudRepository} interface.
 * 
 * @param <T>
 *            the type of the entity to handle
 * @param <ID>
 *            the type of the entity's identifier
 */
public class SimpleDynamoDBCrudRepository<T, ID>
		implements
			DynamoDBCrudRepository<T, ID>,
			SortHandler,
			ExceptionHandler {

	protected DynamoDBEntityInformation<T, ID> entityInformation;

	protected Class<T> domainType;

	protected EnableScanPermissions enableScanPermissions;

	protected DynamoDBOperations dynamoDBOperations;

	public SimpleDynamoDBCrudRepository(DynamoDBEntityInformation<T, ID> entityInformation,
			DynamoDBOperations dynamoDBOperations, EnableScanPermissions enableScanPermissions) {
		Assert.notNull(entityInformation, "entityInformation must not be null");
		Assert.notNull(dynamoDBOperations, "dynamoDBOperations must not be null");

		this.entityInformation = entityInformation;
		this.dynamoDBOperations = dynamoDBOperations;
		this.domainType = entityInformation.getJavaType();
		this.enableScanPermissions = enableScanPermissions;
	}

	@Override
	public Optional<T> findById(ID id) {

		Assert.notNull(id, "The given id must not be null!");

		T result;
		if (entityInformation.isRangeKeyAware()) {
			result = dynamoDBOperations.load(domainType, entityInformation.getHashKey(id),
					entityInformation.getRangeKey(id), entityInformation);
		} else {
			result = dynamoDBOperations.load(domainType, entityInformation.getHashKey(id), entityInformation);
		}

		return Optional.ofNullable(result);
	}

	@Override
	public List<T> findAllById(Iterable<ID> ids) {

		Assert.notNull(ids, "The given ids must not be null!");

		// Works only with non-parallel streams!
		AtomicInteger idx = new AtomicInteger();
		List<Key> keyPairs = StreamSupport.stream(ids.spliterator(), false).map(id -> {

			Assert.notNull(id, "The given id at position " + idx.getAndIncrement() + " must not be null!");

			if (entityInformation.isRangeKeyAware()) {
				return Key.builder().partitionValue(AttributeValueUtil.toAttributeValue(entityInformation.getHashKey(id)))
						.sortValue(AttributeValueUtil.toAttributeValue(entityInformation.getRangeKey(id))).build();
			} else {
				return Key.builder().partitionValue(AttributeValueUtil.toAttributeValue(id)).build();
			}
		}).collect(Collectors.toList());

		Map<Class<?>, List<Key>> keyPairsMap = Collections.<Class<?>, List<Key>>singletonMap(domainType,
				keyPairs);
		return dynamoDBOperations.batchLoad(keyPairsMap, entityInformation);
	}

	@Override
	public <S extends T> S save(S entity) {
		dynamoDBOperations.save(entity, entityInformation);
		return entity;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @throws BatchWriteException
	 *             in case of an error during saving
	 */
	@Override
	public <S extends T> Iterable<S> saveAll(Iterable<S> entities)
			throws BatchWriteException, IllegalArgumentException {

		Assert.notNull(entities, "The given Iterable of entities not be null!");
		BatchWriteResult failedBatches = dynamoDBOperations.batchSave(entities, entityInformation);

		if (failedBatches.unprocessedPutItemsForTable(entityInformation.getTable()).isEmpty()) {
			// Happy path
			return entities;
		} else {
			// Error handling:
			throw repackageToException(BatchWriteException.class);
		}
	}

	@Override
	public boolean existsById(ID id) {

		Assert.notNull(id, "The given id must not be null!");
		return findById(id).isPresent();
	}

	void assertScanEnabled(boolean scanEnabled, String methodName) {
		Assert.isTrue(scanEnabled, "Scanning for unpaginated " + methodName + "() queries is not enabled.  "
				+ "To enable, re-implement the " + methodName
				+ "() method in your repository interface and annotate with @EnableScan, or "
				+ "enable scanning for all repository methods by annotating your repository interface with @EnableScan");
	}

	@Override
	public Iterable<T> findAll() {
		assertScanEnabled(enableScanPermissions.isFindAllUnpaginatedScanEnabled(), "findAll");
		ScanEnhancedRequest scanExpression = ScanEnhancedRequest.builder().build();
		return dynamoDBOperations.scan(domainType, scanExpression, entityInformation).items();
	}

	@Override
	public long count() {
		assertScanEnabled(enableScanPermissions.isCountUnpaginatedScanEnabled(), "count");
		final ScanRequest.Builder scanExpression = ScanRequest.builder();
		return dynamoDBOperations.count(domainType, scanExpression, entityInformation);
	}

	@Override
	public void deleteById(ID id) {

		Assert.notNull(id, "The given id must not be null!");

		Optional<T> entity = findById(id);

		if (entity.isPresent()) {
			dynamoDBOperations.delete(entity.get(), entityInformation);

		} else {
			throw new EmptyResultDataAccessException(String.format("No %s entity with id %s exists!", domainType, id),
					1);
		}
	}

	@Override
	public void delete(T entity) {
		Assert.notNull(entity, "The entity must not be null!");
		dynamoDBOperations.delete(entity, entityInformation);
	}

	@Override
	public void deleteAllById(Iterable<? extends ID> ids) {
		// TODO
	}

	@Override
	public void deleteAll(Iterable<? extends T> entities) {

		Assert.notNull(entities, "The given Iterable of entities not be null!");
		dynamoDBOperations.batchDelete(entities, entityInformation);
	}

	@Override
	public void deleteAll() {

		assertScanEnabled(enableScanPermissions.isDeleteAllUnpaginatedScanEnabled(), "deleteAll");
		dynamoDBOperations.batchDelete(findAll(), entityInformation);
	}

	@NonNull
	public DynamoDBEntityInformation<T, ID> getEntityInformation() {
		return this.entityInformation;
	}
}
