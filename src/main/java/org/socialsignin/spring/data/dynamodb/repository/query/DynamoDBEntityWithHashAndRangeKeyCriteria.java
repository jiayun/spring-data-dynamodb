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
import org.socialsignin.spring.data.dynamodb.query.CountByHashAndRangeKeyQuery;
import org.socialsignin.spring.data.dynamodb.query.MultipleEntityQueryRequestQuery;
import org.socialsignin.spring.data.dynamodb.query.Query;
import org.socialsignin.spring.data.dynamodb.query.QueryRequestCountQuery;
import org.socialsignin.spring.data.dynamodb.query.SingleEntityLoadByHashAndRangeKeyQuery;
import org.socialsignin.spring.data.dynamodb.repository.support.DynamoDBIdIsHashAndRangeKeyEntityInformation;
import org.springframework.util.Assert;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.ComparisonOperator;
import software.amazon.awssdk.services.dynamodb.model.Condition;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;

import java.util.*;

/**
 * @author Michael Lavelle
 * @author Sebastian Just
 */
public class DynamoDBEntityWithHashAndRangeKeyCriteria<T, ID> extends AbstractDynamoDBQueryCriteria<T, ID> {

	private Object rangeKeyAttributeValue;
	private Object rangeKeyPropertyValue;
	private final String rangeKeyPropertyName;
	private final Set<String> indexRangeKeyPropertyNames;
	private final DynamoDBIdIsHashAndRangeKeyEntityInformation<T, ID> entityInformation;

	protected String getRangeKeyAttributeName() {
		return getAttributeName(getRangeKeyPropertyName());
	}

	protected String getRangeKeyPropertyName() {
		return rangeKeyPropertyName;
	}

	protected boolean isRangeKeyProperty(String propertyName) {
		return rangeKeyPropertyName.equals(propertyName);
	}

	public DynamoDBEntityWithHashAndRangeKeyCriteria(
			DynamoDBIdIsHashAndRangeKeyEntityInformation<T, ID> entityInformation,
			TableSchema<T> tableModel) {

		super(entityInformation, tableModel);
		this.rangeKeyPropertyName = entityInformation.getRangeKeyPropertyName();
		Set<String> indexRangeProps = entityInformation.getIndexRangeKeyPropertyNames();
		if(indexRangeProps == null) {
			indexRangeProps = new HashSet<>();
		}
		this.indexRangeKeyPropertyNames = indexRangeProps;
		this.entityInformation = entityInformation;
	}

	public Set<String> getIndexRangeKeyAttributeNames() {
		Set<String> indexRangeKeyAttributeNames = new HashSet<>();
		for (String indexRangeKeyPropertyName : indexRangeKeyPropertyNames) {
			indexRangeKeyAttributeNames.add(getAttributeName(indexRangeKeyPropertyName));
		}
		return indexRangeKeyAttributeNames;
	}

	protected Object getRangeKeyAttributeValue() {
		return rangeKeyAttributeValue;
	}

	protected Object getRangeKeyPropertyValue() {
		return rangeKeyPropertyValue;
	}

	protected boolean isRangeKeySpecified() {
		return getRangeKeyAttributeValue() != null;
	}

	protected Query<T> buildSingleEntityLoadQuery(DynamoDBOperations dynamoDBOperations) {
		return new SingleEntityLoadByHashAndRangeKeyQuery<>(dynamoDBOperations, entityInformation.getJavaType(),
				getHashKeyPropertyValue(), getRangeKeyPropertyValue(), entityInformation);
	}

	protected Query<Long> buildSingleEntityCountQuery(DynamoDBOperations dynamoDBOperations) {
		return new CountByHashAndRangeKeyQuery<>(dynamoDBOperations, entityInformation.getJavaType(),
				getHashKeyPropertyValue(), getRangeKeyPropertyValue(), entityInformation);
	}

	private void checkComparisonOperatorPermittedForCompositeHashAndRangeKey(ComparisonOperator comparisonOperator) {

		if (!ComparisonOperator.EQ.equals(comparisonOperator) && !ComparisonOperator.CONTAINS.equals(comparisonOperator)
				&& !ComparisonOperator.BEGINS_WITH.equals(comparisonOperator)) {
			throw new UnsupportedOperationException(
					"Only EQ,CONTAINS,BEGINS_WITH supported for composite id comparison");
		}

	}

	@SuppressWarnings("unchecked")
	@Override
	public DynamoDBQueryCriteria<T, ID> withSingleValueCriteria(
			String propertyName, ComparisonOperator comparisonOperator, Object value, Class<?> propertyType) {

		if (entityInformation.isCompositeHashAndRangeKeyProperty(propertyName)) {
			checkComparisonOperatorPermittedForCompositeHashAndRangeKey(comparisonOperator);
			Object hashKey = entityInformation.getHashKey((ID) value);
			Object rangeKey = entityInformation.getRangeKey((ID) value);
			if (hashKey != null) {
				withSingleValueCriteria(getHashKeyPropertyName(), comparisonOperator, hashKey, hashKey.getClass());
			}
			if (rangeKey != null) {
				withSingleValueCriteria(getRangeKeyPropertyName(), comparisonOperator, rangeKey, rangeKey.getClass());
			}
			return this;
		} else {
			return super.withSingleValueCriteria(propertyName, comparisonOperator, value, propertyType);
		}
	}

	protected List<Condition> getRangeKeyConditions() {
		List<Condition> rangeKeyConditions = null;
		if (isApplicableForGlobalSecondaryIndex() && entityInformation.getGlobalSecondaryIndexNamesByPropertyName().containsKey(getRangeKeyPropertyName())) {
			rangeKeyConditions = getRangeKeyAttributeValue() == null
					? null
					: Collections.singletonList(createSingleValueCondition(getRangeKeyPropertyName(), ComparisonOperator.EQ,
					getRangeKeyAttributeValue(), getRangeKeyAttributeValue().getClass(), true));

		}
		return rangeKeyConditions;
	}

	protected Query<T> buildFinderQuery(DynamoDBOperations dynamoDBOperations) {
		if (isApplicableForQuery()) {
			if (isApplicableForGlobalSecondaryIndex()) {
				String tableName = entityInformation.getDynamoDBTableName();
				QueryEnhancedRequest queryRequest = buildQueryEnhancedRequest(
						getHashKeyAttributeName(), getRangeKeyAttributeName(), this.getRangeKeyPropertyName(),
						getHashKeyConditions(), getRangeKeyConditions());
				return new MultipleEntityQueryRequestQuery<>(dynamoDBOperations, entityInformation.getJavaType(),
						queryRequest, entityInformation);
			} else {
				throw new UnsupportedOperationException("Query without a global secondary index is not supported");
			}
		} else {
			throw new UnsupportedOperationException("Scan is not supported");
		}
	}

	protected Query<Long> buildFinderCountQuery(DynamoDBOperations dynamoDBOperations, boolean pageQuery) {
		if (isApplicableForQuery()) {
			if (isApplicableForGlobalSecondaryIndex()) {
				String tableName = entityInformation.getDynamoDBTableName();
				QueryRequest.Builder queryRequest = buildQueryRequest(tableName, getGlobalSecondaryIndexName(),
						getHashKeyAttributeName(), getRangeKeyAttributeName(), this.getRangeKeyPropertyName(),
						getHashKeyConditions(), getRangeKeyConditions());
				return new QueryRequestCountQuery(dynamoDBOperations, queryRequest);

			} else {
				throw new UnsupportedOperationException("Count is not supported for query without a global secondary index");
			}
		} else {
			throw new UnsupportedOperationException("Count is not supported for scan");
		}
	}

	@Override
	public boolean isApplicableForLoad() {
		return attributeConditions.size() == 0 && isHashAndRangeKeySpecified();
	}

	protected boolean isHashAndRangeKeySpecified() {
		return isHashKeySpecified() && isRangeKeySpecified();
	}

	protected boolean isOnlyASingleAttributeConditionAndItIsOnEitherRangeOrIndexRangeKey() {
		boolean isOnlyASingleAttributeConditionAndItIsOnEitherRangeOrIndexRangeKey = false;
		if (!isRangeKeySpecified() && attributeConditions.size() == 1) {
			Map.Entry<String, List<Condition>> conditionsEntry = attributeConditions.entrySet().iterator().next();
			if (conditionsEntry.getKey().equals(getRangeKeyAttributeName())
					|| getIndexRangeKeyAttributeNames().contains(conditionsEntry.getKey())) {
				if (conditionsEntry.getValue().size() == 1) {
					isOnlyASingleAttributeConditionAndItIsOnEitherRangeOrIndexRangeKey = true;
				}
			}
		}
		return isOnlyASingleAttributeConditionAndItIsOnEitherRangeOrIndexRangeKey;

	}

	@Override
	protected boolean hasIndexHashKeyEqualCondition() {

		boolean hasCondition = super.hasIndexHashKeyEqualCondition();
		if (!hasCondition) {
			if (rangeKeyAttributeValue != null
					&& entityInformation.isGlobalIndexHashKeyProperty(rangeKeyPropertyName)) {
				hasCondition = true;
			}
		}
		return hasCondition;
	}

	@Override
	protected boolean hasIndexRangeKeyCondition() {
		boolean hasCondition = super.hasIndexRangeKeyCondition();
		if (!hasCondition) {
			if (rangeKeyAttributeValue != null
					&& entityInformation.isGlobalIndexRangeKeyProperty(rangeKeyPropertyName)) {
				hasCondition = true;
			}
		}
		return hasCondition;
	}

	protected boolean isApplicableForGlobalSecondaryIndex() {
		boolean global = super.isApplicableForGlobalSecondaryIndex();
		if (global && getRangeKeyAttributeValue() != null && !entityInformation
				.getGlobalSecondaryIndexNamesByPropertyName().containsKey(getRangeKeyPropertyName())) {
			return false;
		}

		return global;

	}

	protected String getGlobalSecondaryIndexName() {
		// Get the target global secondary index name using the property
		// conditions
		String globalSecondaryIndexName = super.getGlobalSecondaryIndexName();

		// Hash and Range Entities store range key equals conditions as
		// rangeKeyAttributeValue attribute instead of as property condition
		// Check this attribute and if specified in the query conditions and
		// it's the only global secondary index range candidate,
		// then set the index range key to be that associated with the range key
		if (globalSecondaryIndexName == null) {
			if (this.hashKeyAttributeValue == null && getRangeKeyAttributeValue() != null) {
				String[] rangeKeyIndexNames = entityInformation.getGlobalSecondaryIndexNamesByPropertyName()
						.get(this.getRangeKeyPropertyName());
				globalSecondaryIndexName = rangeKeyIndexNames != null && rangeKeyIndexNames.length > 0
						? rangeKeyIndexNames[0]
						: null;
			}
		}
		return globalSecondaryIndexName;
	}

	public boolean isApplicableForQuery() {

		return isOnlyHashKeySpecified()
				|| (isHashKeySpecified() && isOnlyASingleAttributeConditionAndItIsOnEitherRangeOrIndexRangeKey()
						&& comparisonOperatorsPermittedForQuery())
				|| isApplicableForGlobalSecondaryIndex();

	}

	public DynamoDBQueryCriteria<T, ID> withRangeKeyEquals(Object value) {
		Assert.notNull(value, "Creating conditions on null range keys not supported: please specify a value for '"
				+ getRangeKeyPropertyName() + "'");

		rangeKeyAttributeValue = getPropertyAttributeValue(getRangeKeyPropertyName(), value);
		rangeKeyPropertyValue = value;
		return this;
	}

	@SuppressWarnings("unchecked")
	@Override
	public DynamoDBQueryCriteria<T, ID> withPropertyEquals(String propertyName, Object value, Class<?> propertyType) {
		if (isHashKeyProperty(propertyName)) {
			return withHashKeyEquals(value);
		} else if (isRangeKeyProperty(propertyName)) {
			return withRangeKeyEquals(value);
		} else if (entityInformation.isCompositeHashAndRangeKeyProperty(propertyName)) {
			Assert.notNull(value,
					"Creating conditions on null composite id properties not supported: please specify a value for '"
							+ propertyName + "'");
			Object hashKey = entityInformation.getHashKey((ID) value);
			Object rangeKey = entityInformation.getRangeKey((ID) value);
			if (hashKey != null) {
				withHashKeyEquals(hashKey);
			}
			if (rangeKey != null) {
				withRangeKeyEquals(rangeKey);
			}
			return this;
		} else {
			Condition condition = createSingleValueCondition(propertyName, ComparisonOperator.EQ, value, propertyType,
					false);
			return withCondition(propertyName, condition);
		}

	}

	@Override
	protected boolean isOnlyHashKeySpecified() {
		return isHashKeySpecified() && attributeConditions.size() == 0 && !isRangeKeySpecified();
	}

}
