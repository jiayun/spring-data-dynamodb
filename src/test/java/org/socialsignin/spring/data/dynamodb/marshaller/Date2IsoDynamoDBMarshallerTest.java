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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

public class Date2IsoDynamoDBMarshallerTest {

	private Date2IsoDynamoDBMarshaller underTest;

	@BeforeEach
	public void setUp() {
		underTest = new Date2IsoDynamoDBMarshaller();
	}

	@Test
	public void testNullMarshall() {
		String actual = underTest.toString(null);

		assertNull(actual);
	}

	@Test
	public void testMarshall() {
		assertEquals("1970-01-01T00:00:00.000Z", underTest.toString(new Date(0)));
	}

	@Test
	public void testUnmarshallNull() {
		Date actual = underTest.fromString(null);

		assertNull(actual);
	}

	@Test
	public void testUnmarshall() {
		assertEquals(new Date(0), underTest.fromString("1970-01-01T00:00:00.000Z"));
	}

	@Test
	public void testUnmarshallGarbage() {
		assertThrows(RuntimeException.class, () -> underTest.fromString("something"));
	}
}
