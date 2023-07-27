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

import jakarta.persistence.Table;
import org.socialsignin.spring.data.dynamodb.core.DynamoDBOperations;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author Michael Lavelle
 * @author Sebastian Just
 */
public class DynamoDBEntityMetadataSupport<T, ID> implements DynamoDBHashKeyExtractingEntityMetadata<T> {

	private final Class<T> domainType;
	private boolean hasRangeKey;
	private String hashKeyPropertyName;
	private List<String> globalIndexHashKeyPropertyNames;
	private List<String> globalIndexRangeKeyPropertyNames;

	private String dynamoDBTableName;
	private Map<String, String[]> globalSecondaryIndexNames;
	private DynamoDbTable<T> table;

	@Override
	public String getDynamoDBTableName() {
		return dynamoDBTableName;
	}

	@Override
	public DynamoDbTable<T> getTable() {
		return table;
	}

	/**
	 * Creates a new {@link DynamoDBEntityMetadataSupport} for the given domain
	 * type.
	 *
	 * @param domainType
	 *            must not be {@literal null}.
	 */
	public DynamoDBEntityMetadataSupport(final Class<T> domainType) {
		this(domainType, null);
	}

	/**
	 * Creates a new {@link DynamoDBEntityMetadataSupport} for the given domain
	 * type and dynamoDB mapper config.
	 *
	 * @param domainType
	 *            must not be {@literal null}.
	 * @param dynamoDBOperations
	 *            dynamoDBOperations as populated from Spring Data DynamoDB Configuration
	 */
    public DynamoDBEntityMetadataSupport(final Class<T> domainType, DynamoDBOperations dynamoDBOperations) {

        Assert.notNull(domainType, "Domain type must not be null!");
        this.domainType = domainType;

        Table table = this.domainType.getAnnotation(Table.class);
        Assert.notNull(table, "Domain type must by annotated with Table!");
		this.dynamoDBTableName = table.name();

		if (dynamoDBOperations != null) {
			this.table = dynamoDBOperations.getDynamoDbTable(domainType, table.name());
		}
        this.hashKeyPropertyName = null;
        this.globalSecondaryIndexNames = new HashMap<>();
        this.globalIndexHashKeyPropertyNames = new ArrayList<>();
        this.globalIndexRangeKeyPropertyNames = new ArrayList<>();
        ReflectionUtils.doWithMethods(domainType, method -> {
            if (method.getAnnotation(DynamoDbPartitionKey.class) != null) {
                hashKeyPropertyName = getPropertyNameForAccessorMethod(method);
            }
            if (method.getAnnotation(DynamoDbSortKey.class) != null) {
                hasRangeKey = true;
            }
            DynamoDbSecondarySortKey dynamoDBRangeKeyAnnotation = method.getAnnotation(DynamoDbSecondarySortKey.class);
            DynamoDbSecondaryPartitionKey dynamoDBHashKeyAnnotation = method.getAnnotation(DynamoDbSecondaryPartitionKey.class);

            if (dynamoDBRangeKeyAnnotation != null) {
                addGlobalSecondaryIndexNames(method, dynamoDBRangeKeyAnnotation);
            }
            if (dynamoDBHashKeyAnnotation != null) {
                addGlobalSecondaryIndexNames(method, dynamoDBHashKeyAnnotation);
            }
        });
        ReflectionUtils.doWithFields(domainType, field -> {
            if (field.getAnnotation(DynamoDbPartitionKey.class) != null) {
                hashKeyPropertyName = getPropertyNameForField(field);
            }
            if (field.getAnnotation(DynamoDbSortKey.class) != null) {
                hasRangeKey = true;
            }
			DynamoDbSecondarySortKey dynamoDBRangeKeyAnnotation = field.getAnnotation(DynamoDbSecondarySortKey.class);
			DynamoDbSecondaryPartitionKey dynamoDBHashKeyAnnotation = field.getAnnotation(DynamoDbSecondaryPartitionKey.class);

            if (dynamoDBRangeKeyAnnotation != null) {
                addGlobalSecondaryIndexNames(field, dynamoDBRangeKeyAnnotation);
            }
            if (dynamoDBHashKeyAnnotation != null) {
                addGlobalSecondaryIndexNames(field, dynamoDBHashKeyAnnotation);
            }
        });
        Assert.notNull(hashKeyPropertyName, "Unable to find hash key field or getter method on " + domainType + "!");
    }

	public DynamoDBEntityInformation<T, ID> getEntityInformation() {

		if (hasRangeKey) {
			DynamoDBHashAndRangeKeyExtractingEntityMetadataImpl<T, ID> metadata = new DynamoDBHashAndRangeKeyExtractingEntityMetadataImpl<T, ID>(
					domainType);
			return new DynamoDBIdIsHashAndRangeKeyEntityInformationImpl<>(domainType, metadata);
		} else {
			return new DynamoDBIdIsHashKeyEntityInformationImpl<>(domainType, this);
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.springframework.data.repository.core.EntityMetadata#getJavaType()
	 */
	@Override
	public Class<T> getJavaType() {
		return domainType;
	}

	@Override
	public boolean isHashKeyProperty(String propertyName) {
		return hashKeyPropertyName.equals(propertyName);
	}

	protected boolean isFieldAnnotatedWith(final String propertyName, final Class<? extends Annotation> annotation) {

		Field field = findField(propertyName);
		return field != null && field.getAnnotation(annotation) != null;
	}

	private String toGetMethodName(String propertyName) {
		String methodName = propertyName.substring(0, 1).toUpperCase();
		if (propertyName.length() > 1) {
			methodName = methodName + propertyName.substring(1);
		}
		return "get" + methodName;
	}

	protected String toSetterMethodNameFromAccessorMethod(Method method) {
		String accessorMethodName = method.getName();
		if (accessorMethodName.startsWith("get")) {
			return "set" + accessorMethodName.substring(3);
		} else if (accessorMethodName.startsWith("is")) {
			return "is" + accessorMethodName.substring(2);
		}
		return null;
	}

	private String toIsMethodName(String propertyName) {
		String methodName = propertyName.substring(0, 1).toUpperCase();
		if (propertyName.length() > 1) {
			methodName = methodName + propertyName.substring(1);
		}
		return "is" + methodName;
	}

	private Method findMethod(String propertyName) {
		Method method = ReflectionUtils.findMethod(domainType, toGetMethodName(propertyName));
		if (method == null) {
			method = ReflectionUtils.findMethod(domainType, toIsMethodName(propertyName));
		}
		return method;

	}

	private Field findField(String propertyName) {
		return ReflectionUtils.findField(domainType, propertyName);
	}

	public String getOverriddenAttributeName(Method method) {

		if (method != null) {
			if (method.getAnnotation(DynamoDbAttribute.class) != null
					&& StringUtils.hasText(method.getAnnotation(DynamoDbAttribute.class).value())) {
				return method.getAnnotation(DynamoDbAttribute.class).value();
			}
		}
		return null;

	}

	@Override
	public Optional<String> getOverriddenAttributeName(final String propertyName) {

		Method method = findMethod(propertyName);
		if (method != null) {
			if (method.getAnnotation(DynamoDbAttribute.class) != null
					&& StringUtils.hasText(method.getAnnotation(DynamoDbAttribute.class).value())) {
				return Optional.of(method.getAnnotation(DynamoDbAttribute.class).value());
			}
		}

		Field field = findField(propertyName);
		if (field != null) {
			if (field.getAnnotation(DynamoDbAttribute.class) != null
					&& StringUtils.hasText(field.getAnnotation(DynamoDbAttribute.class).value())) {
				return Optional.of(field.getAnnotation(DynamoDbAttribute.class).value());
			}
		}

		return Optional.empty();
	}

	@Override
	public AttributeConverter<?> getTypeConverterForProperty(final String propertyName) {
		DynamoDbConvertedBy annotation = null;

		Method method = findMethod(propertyName);
		if (method != null) {
			annotation = method.getAnnotation(DynamoDbConvertedBy.class);
		}

		if (annotation == null) {
			Field field = findField(propertyName);
			if (field != null) {
				annotation = field.getAnnotation(DynamoDbConvertedBy.class);
			}
		}

		if (annotation != null) {
			try {
				return annotation.value().getDeclaredConstructor().newInstance();
			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
					| InvocationTargetException | NoSuchMethodException | SecurityException e) {
				throw new RuntimeException(e);
			}
		}

		return null;
	}

	protected String getPropertyNameForAccessorMethod(Method method) {
		String methodName = method.getName();
		String propertyName = null;
		if (methodName.startsWith("get")) {
			propertyName = methodName.substring(3);
		} else if (methodName.startsWith("is")) {
			propertyName = methodName.substring(2);
		}
		Assert.notNull(propertyName, "Hash or range key annotated accessor methods must start with 'get' or 'is'");

		String firstLetter = propertyName.substring(0, 1);
		String remainder = propertyName.substring(1);
		return firstLetter.toLowerCase() + remainder;
	}

	protected String getPropertyNameForField(Field field) {
		return field.getName();
	}

	@Override
	public String getHashKeyPropertyName() {
		return hashKeyPropertyName;
	}

	private void addGlobalSecondaryIndexNames(Method method, DynamoDbSecondarySortKey dynamoDBIndexRangeKey) {

		if (dynamoDBIndexRangeKey.indexNames() != null
				&& dynamoDBIndexRangeKey.indexNames().length > 0) {
			String propertyName = getPropertyNameForAccessorMethod(method);

			globalSecondaryIndexNames.put(propertyName,
					method.getAnnotation(DynamoDbSecondarySortKey.class).indexNames());
			globalIndexRangeKeyPropertyNames.add(propertyName);

		}
	}

	private void addGlobalSecondaryIndexNames(Field field, DynamoDbSecondarySortKey dynamoDBIndexRangeKey) {

		if (dynamoDBIndexRangeKey.indexNames() != null
				&& dynamoDBIndexRangeKey.indexNames().length > 0) {
			String propertyName = getPropertyNameForField(field);

			globalSecondaryIndexNames.put(propertyName,
					field.getAnnotation(DynamoDbSecondarySortKey.class).indexNames());
			globalIndexRangeKeyPropertyNames.add(propertyName);

		}
	}

	private void addGlobalSecondaryIndexNames(Method method, DynamoDbSecondaryPartitionKey dynamoDBIndexHashKey) {

		if (dynamoDBIndexHashKey.indexNames() != null
				&& dynamoDBIndexHashKey.indexNames().length > 0) {
			String propertyName = getPropertyNameForAccessorMethod(method);

			globalSecondaryIndexNames.put(propertyName,
					method.getAnnotation(DynamoDbSecondaryPartitionKey.class).indexNames());
			globalIndexHashKeyPropertyNames.add(propertyName);

		}
	}

	private void addGlobalSecondaryIndexNames(Field field, DynamoDbSecondaryPartitionKey dynamoDBIndexHashKey) {

		if (dynamoDBIndexHashKey.indexNames() != null
				&& dynamoDBIndexHashKey.indexNames().length > 0) {
			String propertyName = getPropertyNameForField(field);

			globalSecondaryIndexNames.put(propertyName,
					field.getAnnotation(DynamoDbSecondaryPartitionKey.class).indexNames());
			globalIndexHashKeyPropertyNames.add(propertyName);

		}
	}

	@Override
	public Map<String, String[]> getGlobalSecondaryIndexNamesByPropertyName() {
		return globalSecondaryIndexNames;
	}

	@Override
	public boolean isGlobalIndexHashKeyProperty(String propertyName) {
		return globalIndexHashKeyPropertyNames.contains(propertyName);
	}

	@Override
	public boolean isGlobalIndexRangeKeyProperty(String propertyName) {
		return globalIndexRangeKeyPropertyNames.contains(propertyName);
	}

}
