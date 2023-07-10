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
package org.socialsignin.spring.data.dynamodb.repository.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class Entity2DDLTest {

	@Test
	public void testFromExistingValue() {
		Entity2DDL actual = Entity2DDL.fromValue(Entity2DDL.NONE.getConfigurationValue());

		assertEquals(Entity2DDL.NONE, actual);
	}

	@Test
	public void testFromNotExistingValue() {
		assertThrows(IllegalArgumentException.class, () -> Entity2DDL.fromValue("doesnt exist"));
	}

}
