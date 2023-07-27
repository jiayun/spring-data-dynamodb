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
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.time.LocalDateTime;
import java.util.UUID;

@DynamoDbBean
@Table(name = "gz_feed")
public class Feed {
	private String idx = UUID.randomUUID().toString();
	private int userIdx;
	private String message;
	private int paymentType;
	private LocalDateTime regDate;

	@DynamoDbPartitionKey
	public String getIdx() {
		return idx;
	}

	public int getUserIdx() {
		return userIdx;
	}

	@DynamoDbSecondaryPartitionKey(indexNames  = {"aaa"})
	public String getMessage() {
		return message;
	}

	// @DynamoDBIndexRangeKey(attributeName = "PaymentType",
	// globalSecondaryIndexName = "aaa")
	public int getPaymentType() {
		return paymentType;
	}

	@DynamoDbConvertedBy(LocalDateTimeConverter.class)
	@DynamoDbSecondarySortKey(indexNames  = {"aaa"})
	public LocalDateTime getRegDate() {
		return regDate;
	}

	public void setIdx(String idx) {
		this.idx = idx;
	}

	public void setUserIdx(int userIdx) {
		this.userIdx = userIdx;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public void setPaymentType(int paymentType) {
		this.paymentType = paymentType;
	}

	public void setRegDate(LocalDateTime regDate) {
		this.regDate = regDate;
	}

	static public class LocalDateTimeConverter implements AttributeConverter<LocalDateTime> {

		@Override
		public AttributeValue transformFrom(LocalDateTime input) {
			return AttributeValue.builder().s(input.toString()).build();
		}

		@Override
		public LocalDateTime transformTo(AttributeValue input) {
			return LocalDateTime.parse(input.s());
		}

		@Override
		public EnhancedType<LocalDateTime> type() {
			return EnhancedType.of(LocalDateTime.class);
		}

		@Override
		public AttributeValueType attributeValueType() {
			return AttributeValueType.S;
		}

	}
}
