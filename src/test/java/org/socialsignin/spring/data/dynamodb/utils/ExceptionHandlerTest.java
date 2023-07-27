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
package org.socialsignin.spring.data.dynamodb.utils;

import org.junit.jupiter.api.Test;
import org.socialsignin.spring.data.dynamodb.exception.BatchWriteException;

import static org.junit.jupiter.api.Assertions.*;

public class ExceptionHandlerTest {

	private ExceptionHandler underTest = new ExceptionHandler() {
	};

	@Test
	public void testEmpty() {
		assertDoesNotThrow(() -> underTest.repackageToException(BatchWriteException.class));
	}

	@Test
	public void testSimple() {
		BatchWriteException actual = underTest.repackageToException(BatchWriteException.class);

		assertEquals("Processing of entities failed!",
				actual.getMessage());
	}
}
