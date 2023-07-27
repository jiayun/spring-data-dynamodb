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
package org.socialsignin.spring.data.dynamodb.repository.query;

import org.socialsignin.spring.data.dynamodb.core.DynamoDBOperations;
import org.socialsignin.spring.data.dynamodb.query.CountByHashKeyQuery;
import org.socialsignin.spring.data.dynamodb.query.MultipleEntityQueryRequestQuery;
import org.socialsignin.spring.data.dynamodb.query.Query;
import org.socialsignin.spring.data.dynamodb.query.QueryRequestCountQuery;
import org.socialsignin.spring.data.dynamodb.query.SingleEntityLoadByHashKeyQuery;
import org.socialsignin.spring.data.dynamodb.repository.support.DynamoDBEntityInformation;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.ComparisonOperator;
import software.amazon.awssdk.services.dynamodb.model.Condition;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.Select;

import java.util.List;

/**
 * @author Michael Lavelle
 * @author Sebastian Just
 */
public class DynamoDBEntityWithHashKeyOnlyCriteria<T, ID> extends AbstractDynamoDBQueryCriteria<T, ID> {

	private final DynamoDBEntityInformation<T, ID> entityInformation;

	public DynamoDBEntityWithHashKeyOnlyCriteria(DynamoDBEntityInformation<T, ID> entityInformation,
			TableSchema<T> tableModel) {
		super(entityInformation, tableModel);
		this.entityInformation = entityInformation;
	}

	protected Query<T> buildSingleEntityLoadQuery(DynamoDBOperations dynamoDBOperations) {
		return new SingleEntityLoadByHashKeyQuery<>(dynamoDBOperations, clazz, getHashKeyPropertyValue(), entityInformation);
	}

	protected Query<Long> buildSingleEntityCountQuery(DynamoDBOperations dynamoDBOperations) {
		return new CountByHashKeyQuery<>(dynamoDBOperations, clazz, getHashKeyPropertyValue(), entityInformation);
	}

	protected Query<T> buildFinderQuery(DynamoDBOperations dynamoDBOperations) {
		if (isApplicableForGlobalSecondaryIndex()) {

			List<Condition> hashKeyConditions = getHashKeyConditions();
			QueryEnhancedRequest queryRequest = buildQueryEnhancedRequest(
				getHashKeyAttributeName(), null, null, hashKeyConditions, null);
			return new MultipleEntityQueryRequestQuery<>(dynamoDBOperations, entityInformation.getJavaType(),
					queryRequest, entityInformation);
		} else {
			throw new UnsupportedOperationException(
					"Query by example is not supported for entities with no range key and no global secondary index");
		}
	}

	protected Query<Long> buildFinderCountQuery(DynamoDBOperations dynamoDBOperations, boolean pageQuery) {
		if (isApplicableForGlobalSecondaryIndex()) {

			List<Condition> hashKeyConditions = getHashKeyConditions();
			QueryRequest.Builder queryRequest = buildQueryRequest(
					entityInformation.getDynamoDBTableName(),
					getGlobalSecondaryIndexName(), getHashKeyAttributeName(), null, null, hashKeyConditions, null);
			queryRequest.select(Select.COUNT);
			return new QueryRequestCountQuery(dynamoDBOperations, queryRequest);

		} else {
			throw new UnsupportedOperationException(
					"Query by example is not supported for entities with no range key and no global secondary index");
		}
	}

	@Override
	protected boolean isOnlyHashKeySpecified() {
		return attributeConditions.size() == 0 && isHashKeySpecified();
	}

	@Override
	public boolean isApplicableForLoad() {
		return isOnlyHashKeySpecified();
	}

	@Override
	public DynamoDBQueryCriteria<T, ID> withPropertyEquals(String propertyName, Object value, Class<?> propertyType) {
		if (isHashKeyProperty(propertyName)) {
			return withHashKeyEquals(value);
		} else {
			Condition condition = createSingleValueCondition(propertyName, ComparisonOperator.EQ, value, propertyType,
					false);
			return withCondition(propertyName, condition);
		}
	}

}
