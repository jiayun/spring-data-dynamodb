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
package org.socialsignin.spring.data.dynamodb.query;

import org.socialsignin.spring.data.dynamodb.core.DynamoDBOperations;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;

public class QueryRequestCountQuery extends AbstractSingleEntityQuery<Long> {

	private final DynamoDBOperations dynamoDBOperations;
	private final QueryRequest.Builder queryRequest;

	public QueryRequestCountQuery(DynamoDBOperations dynamoDBOperations, QueryRequest.Builder queryRequest) {
		super(null, Long.class);
		this.queryRequest = queryRequest;
		this.dynamoDBOperations = dynamoDBOperations;
	}

	@Override
	public Long getSingleResult() {
		return Long.valueOf(dynamoDBOperations.count(clazz, queryRequest));
	}

}
