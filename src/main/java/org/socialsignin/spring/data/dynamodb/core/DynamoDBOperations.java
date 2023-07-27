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

import org.socialsignin.spring.data.dynamodb.repository.support.DynamoDBEntityInformation;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.BatchWriteResult;
import software.amazon.awssdk.enhanced.dynamodb.model.PageIterable;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;

import java.util.List;
import java.util.Map;

/**
 * Interface to DynmaoDB - as seen from the Spring-Data world
 */
public interface DynamoDBOperations {

	<T> int count(Class<T> clazz, QueryRequest.Builder mutableQueryRequest);

	<T, ID> int count(Class<T> clazz, ScanRequest.Builder mutableScanRequest, DynamoDBEntityInformation<T, ID> entityInformation);

	<T, ID> PageIterable<T> query(Class<T> clazz, QueryEnhancedRequest queryRequest, DynamoDBEntityInformation<T, ID> entityInformation);

	<T, ID> PageIterable<T> scan(Class<T> clazz, ScanEnhancedRequest scanRequest, DynamoDBEntityInformation<T, ID> entityInformation);

	<T, ID> T load(Class<T> domainClass, Object hashKey, Object rangeKey, DynamoDBEntityInformation<T, ID> entityInformation);
	<T, ID> T load(Class<T> domainClass, Object hashKey, DynamoDBEntityInformation<T, ID> entityInformation);
	<T, ID> List<T> batchLoad(Map<Class<?>, List<Key>> itemsToGet, DynamoDBEntityInformation<T, ID> entityInformation);

	<T, ID> T save(T entity, DynamoDBEntityInformation<T, ID> entityInformation);
	<T, S, ID> BatchWriteResult batchSave(Iterable<S> entities, DynamoDBEntityInformation<T, ID> entityInformation);

	<T, ID> T delete(T entity, DynamoDBEntityInformation<T, ID> entityInformation);
	<T, S, ID> BatchWriteResult batchDelete(Iterable<S> entities, DynamoDBEntityInformation<T, ID> entityInformation);
	<T> BatchWriteResult batchDelete(List<T> entities, DynamoDbTable<T> dynamoDbTable);

	<T> DynamoDbTable<T> getDynamoDbTable(Class<T> domainClass, String tableName);

	/**
	 * Provides access to the DynamoDB mapper table model of the underlying domain
	 * type.
	 *
	 * @param <T>
	 *            The type of the domain type itself
	 * @param domainClass
	 *            A domain type
	 * @return Corresponding DynamoDB table model
	 */
	<T> TableSchema<T> getTableModel(Class<T> domainClass);

}
