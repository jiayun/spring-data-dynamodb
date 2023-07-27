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
package org.socialsignin.spring.data.dynamodb.domain.sample;

import jakarta.persistence.Table;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondarySortKey;

import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

/**
 * Test for <a href=
 * "https://github.com/spring-data-dynamodb/spring-data-dynamodb/issues/52">Issue
 * 52</a>.
 */
@DynamoDbBean
@Table(name = "installations")
public class Installation implements Serializable {
	private static final long serialVersionUID = 1L;

	public String id = UUID.randomUUID().toString();

	private String systemId;

	private Date updatedAt;

	public Installation() {

	}

	public Installation(final String systemId, final Date updatedAt) {
		this.systemId = systemId;
		this.updatedAt = updatedAt;
	}

	@DynamoDbPartitionKey
	public String getId() {
		return id;
	}

	public void setId(final String id) {
		this.id = id;
	}

	@DynamoDbSecondaryPartitionKey(indexNames = { "idx-global-systemid" })
	public String getSystemId() {
		return systemId;
	}

	public void setSystemId(final String systemId) {
		this.systemId = systemId;
	}

	@DynamoDbSecondarySortKey(indexNames = { "idx-global-systemid" })
	public Date getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(final Date updatedAt) {
		this.updatedAt = updatedAt;
	}

	@Override
	public String toString() {
		return "Installation [id='" + id + "', systemId='" + systemId + "', updatedAt='" + updatedAt + "']";
	}
}
