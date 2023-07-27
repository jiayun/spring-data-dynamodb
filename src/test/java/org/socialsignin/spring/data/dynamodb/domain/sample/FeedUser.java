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
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondarySortKey;

import java.util.Date;
import java.util.UUID;

@DynamoDbBean
@Table(name = "feed_user")
public class FeedUser {
	@Id
	private String id = UUID.randomUUID().toString();

	private int usrNo;

	private String feedId;

	private Date feedRegDate;

	private boolean feedOpenYn;

	@DynamoDbPartitionKey
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	@DynamoDbSecondaryPartitionKey(indexNames = { "idx_global_usrNo_feedOpenYn" })
	public int getUsrNo() {
		return usrNo;
	}

	public void setUsrNo(int usrNo) {
		this.usrNo = usrNo;
	}

	public String getFeedId() {
		return feedId;
	}

	public void setFeedId(String feedId) {
		this.feedId = feedId;
	}

	public Date getFeedRegDate() {
		return feedRegDate;
	}

	public void setFeedRegDate(Date feedRegDate) {
		this.feedRegDate = feedRegDate;
	}

	@DynamoDbSecondarySortKey(indexNames = { "idx_global_usrNo_feedOpenYn" })
	public boolean isFeedOpenYn() {
		return feedOpenYn;
	}

	public void setFeedOpenYn(boolean feedOpenYn) {
		this.feedOpenYn = feedOpenYn;
	}
}
