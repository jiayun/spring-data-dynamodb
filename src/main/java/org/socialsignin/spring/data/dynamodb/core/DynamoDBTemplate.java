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

import org.socialsignin.spring.data.dynamodb.mapping.event.AfterDeleteEvent;
import org.socialsignin.spring.data.dynamodb.mapping.event.AfterLoadEvent;
import org.socialsignin.spring.data.dynamodb.mapping.event.AfterSaveEvent;
import org.socialsignin.spring.data.dynamodb.mapping.event.BeforeDeleteEvent;
import org.socialsignin.spring.data.dynamodb.mapping.event.BeforeSaveEvent;
import org.socialsignin.spring.data.dynamodb.mapping.event.DynamoDBMappingEvent;
import org.socialsignin.spring.data.dynamodb.repository.support.DynamoDBEntityInformation;
import org.socialsignin.spring.data.dynamodb.utils.AttributeValueUtil;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.*;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class DynamoDBTemplate implements DynamoDBOperations, ApplicationContextAware {
	private final DynamoDbEnhancedClient dynamoDBMapper;
	private final DynamoDbClient amazonDynamoDB;
	private ApplicationEventPublisher eventPublisher;

	@Autowired
	public DynamoDBTemplate(DynamoDbClient amazonDynamoDB, DynamoDbEnhancedClient dynamoDBMapper) {
		Assert.notNull(amazonDynamoDB, "amazonDynamoDB must not be null!");
		Assert.notNull(dynamoDBMapper, "dynamoDBMapper must not be null!");

		this.amazonDynamoDB = amazonDynamoDB;
		this.dynamoDBMapper = dynamoDBMapper;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.eventPublisher = applicationContext;
	}

	private Key getKey(Object hashKey, Object rangeKey) {
		return Key.builder().partitionValue(AttributeValueUtil.toAttributeValue(hashKey))
				.sortValue(AttributeValueUtil.toAttributeValue(rangeKey))
				.build();
	}

	private Key getKey(Object hashKey) {
		return Key.builder().partitionValue(AttributeValueUtil.toAttributeValue(hashKey))
				.build();
	}

	@Override
	public <T, ID> T load(Class<T> domainClass, Object hashKey, Object rangeKey, DynamoDBEntityInformation<T, ID> entityInformation) {
		T item = entityInformation.getTable().getItem(getKey(hashKey, rangeKey));
		maybeEmitEvent(item, AfterLoadEvent::new);

		return item;
	}

	@Override
	public <T, ID> T load(Class<T> domainClass, Object hashKey, DynamoDBEntityInformation<T, ID> entityInformation) {
		T item = entityInformation.getTable().getItem(getKey(hashKey));
		maybeEmitEvent(item, AfterLoadEvent::new);

		return item;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T, ID> List<T> batchLoad(Map<Class<?>, List<Key>> itemsToGet, DynamoDBEntityInformation<T, ID> entityInformation) {

		// TODO: only one class currently supported

		DynamoDbTable<T> table = entityInformation.getTable();
		Class<T> type = entityInformation.getJavaType();

		ReadBatch.Builder builder = ReadBatch.builder(type);
		itemsToGet.forEach((k, v) -> builder.addGetItem(v));

		BatchGetResultPageIterable resultPages = dynamoDBMapper.batchGetItem(b -> b.readBatches(builder.build()));
		return resultPages.resultsForTable(table).stream().toList();
	}

	@Override
	public <T, ID> T save(T entity, DynamoDBEntityInformation<T, ID> entityInformation) {
		maybeEmitEvent(entity, BeforeSaveEvent::new);
		entityInformation.getTable().putItem(entity);
		maybeEmitEvent(entity, AfterSaveEvent::new);
		return entity;
	}

	@Override
	public <T, S, ID> BatchWriteResult batchSave(Iterable<S> entities, DynamoDBEntityInformation<T, ID> entityInformation) {
		entities.forEach(it -> maybeEmitEvent(it, BeforeSaveEvent::new));

		WriteBatch.Builder builder = WriteBatch.builder(entityInformation.getJavaType());
		entities.forEach(builder::addPutItem);
		BatchWriteResult result = dynamoDBMapper.batchWriteItem(b -> b.writeBatches(builder.build()));

		entities.forEach(it -> maybeEmitEvent(it, AfterSaveEvent::new));
		return result;
	}

	@Override
	public <T, ID> T delete(T entity, DynamoDBEntityInformation<T, ID> entityInformation) {
		maybeEmitEvent(entity, BeforeDeleteEvent::new);
		entityInformation.getTable().deleteItem(entity);
		maybeEmitEvent(entity, AfterDeleteEvent::new);
		return entity;
	}

	@Override
	public <T, S, ID> BatchWriteResult batchDelete(Iterable<S> entities, DynamoDBEntityInformation<T, ID> entityInformation) {
		entities.forEach(it -> maybeEmitEvent(it, BeforeDeleteEvent::new));

		WriteBatch.Builder builder = WriteBatch.builder(entityInformation.getJavaType());
		entities.forEach(builder::addDeleteItem);
		BatchWriteResult result = dynamoDBMapper.batchWriteItem(b -> b.writeBatches(builder.build()));

		entities.forEach(it -> maybeEmitEvent(it, AfterDeleteEvent::new));
		return result;
	}

	@Override
	public <T> BatchWriteResult batchDelete(List<T> entities, DynamoDbTable<T> table) {
		Class<T> rawClass = table.tableSchema().itemType().rawClass();

		entities.forEach(it -> maybeEmitEvent(it, BeforeDeleteEvent::new));

		WriteBatch.Builder builder = WriteBatch.builder(rawClass);
		entities.forEach(builder::addDeleteItem);
		BatchWriteResult result = dynamoDBMapper.batchWriteItem(b -> b.writeBatches(builder.build()));

		entities.forEach(it -> maybeEmitEvent(it, AfterDeleteEvent::new));
		return result;
	}

	@Override
	public <T, ID> PageIterable<T> query(Class<T> clazz, QueryEnhancedRequest queryRequest, DynamoDBEntityInformation<T, ID> entityInformation) {
		DynamoDbTable<T> table = entityInformation.getTable();
		return table.query(queryRequest);
	}

	@Override
	public <T, ID> PageIterable<T> scan(Class<T> clazz, ScanEnhancedRequest scanRequest, DynamoDBEntityInformation<T, ID> entityInformation) {
		DynamoDbTable<T> table = entityInformation.getTable();
		return table.scan(scanRequest);
	}

	@Override
	public <T> int count(Class<T> clazz, QueryRequest.Builder mutableQueryRequest) {
		mutableQueryRequest.select(Select.COUNT);

		// Count queries can also be truncated for large datasets
		int count = 0;
		QueryResponse response = null;
		do {
			response = amazonDynamoDB.query(mutableQueryRequest.build());
			count += response.count();
			mutableQueryRequest.exclusiveStartKey(response.lastEvaluatedKey());
		} while (response.lastEvaluatedKey() != null);

		return count;
	}

	@Override
	public <T, ID> int count(Class<T> clazz, ScanRequest.Builder mutableScanRequest, DynamoDBEntityInformation<T, ID> entityInformation) {
		mutableScanRequest.select(Select.COUNT);

		int count = 0;
		ScanResponse response;
		do {
			response = amazonDynamoDB.scan(mutableScanRequest.build());
			count += response.count();
			mutableScanRequest.exclusiveStartKey(response.lastEvaluatedKey());
		} while (response.lastEvaluatedKey() != null);

		return count;
	}

	@Override
	public <T> DynamoDbTable<T> getDynamoDbTable(Class<T> domainClass, String tableName) {
		return dynamoDBMapper.table(tableName, TableSchema.fromBean(domainClass));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public <T> TableSchema<T> getTableModel(Class<T> domainClass) {
		return TableSchema.fromBean(domainClass);
	}

	protected <T> void maybeEmitEvent(@Nullable T source, Function<T, DynamoDBMappingEvent<T>> factory) {
		if (eventPublisher != null) {
			if (source != null) {
				DynamoDBMappingEvent<T> event = factory.apply(source);

				eventPublisher.publishEvent(event);
			}
		}

	}
}
