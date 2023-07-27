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
import org.springframework.data.annotation.Id;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;

@DynamoDbBean
@Table(name = "customerhistory")
public class CustomerHistory {
	@Id
	private CustomerHistoryId id;

	private String tag;

	@DynamoDbSecondaryPartitionKey(indexNames = {"idx_global_tag"})
	public String getTag() {
		return tag;
	}

	public void setTag(String tag) {
		this.tag = tag;
	}

	@DynamoDbPartitionKey
	@DynamoDbAttribute("customerId")
	public String getId() {
		return id != null ? id.getCustomerId() : null;
	}

	public void setId(String customerId) {
		if (this.id == null) {
			this.id = new CustomerHistoryId();
		}
		this.id.setCustomerId(customerId);
	}

	@DynamoDbSortKey
	@DynamoDbAttribute("createDt")
	public String getCreateDt() {
		return id != null ? id.getCreateDt() : null;
	}

	public void setCreateDt(String createDt) {
		if (this.id == null) {
			this.id = new CustomerHistoryId();
		}

		this.id.setCreateDt(createDt);
	}
}
