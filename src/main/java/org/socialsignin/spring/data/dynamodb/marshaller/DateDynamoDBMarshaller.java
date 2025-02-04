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

import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;

@SuppressWarnings("deprecation")
public abstract class DateDynamoDBMarshaller implements StringConverter<Date>, AttributeConverter<Date> {

	public abstract DateFormat getDateFormat();

	@Override
	public AttributeValue transformFrom(Date input) {
		return AttributeValue.builder().s(toString(input)).build();
	}

	@Override
	public Date transformTo(AttributeValue input) {
		return fromString(input.s());
	}

	@Override
	public AttributeValueType attributeValueType() {
		return AttributeValueType.S;
	}

	@Override
	public Date fromString(String string) {
		if (!StringUtils.hasText(string)) {
			return null;
		} else {
			try {
				return getDateFormat().parse(string);
			} catch (ParseException e) {
				throw new RuntimeException(e);
			}
		}
	}

	@Override
	public EnhancedType<Date> type() {
		return EnhancedType.of(Date.class);
	}

	@Override
	public String toString(Date object) {
		if (object == null) {
			return null;
		} else {
			return getDateFormat().format(object);
		}
	}

}
