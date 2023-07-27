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
package org.socialsignin.spring.data.dynamodb.marshaller;

import org.springframework.util.StringUtils;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.StringConverter;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.time.Instant;

@SuppressWarnings("deprecation")
public class Instant2EpochDynamoDBMarshaller
		implements
		StringConverter<Instant>,
		AttributeConverter<Instant> {

	@Override
	public AttributeValue transformFrom(Instant input) {
		return AttributeValue.builder().s(toString(input)).build();
	}

	@Override
	public Instant transformTo(AttributeValue input) {
		return fromString(input.s());
	}

	@Override
	public AttributeValueType attributeValueType() {
		return AttributeValueType.S;
	}

	@Override
	public Instant fromString(String string) {
		if (!StringUtils.hasText(string)) {
			return null;
		} else {
			return Instant.ofEpochMilli(Long.parseLong(string));
		}
	}

	@Override
	public EnhancedType<Instant> type() {
		return EnhancedType.of(Instant.class);
	}

	@Override
	public String toString(Instant object) {
		if (object == null) {
			return null;
		} else {
			return Long.toString(object.toEpochMilli());
		}
	}
}
