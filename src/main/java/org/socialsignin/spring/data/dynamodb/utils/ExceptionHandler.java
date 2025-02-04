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

import org.springframework.dao.DataAccessException;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public interface ExceptionHandler {

	default <T extends DataAccessException> T repackageToException(Class<T> targetType) {

		try {
			Constructor<T> ctor = targetType.getConstructor(String.class, Throwable.class);
			T e = ctor.newInstance("Processing of entities failed!", null);
			return e;
		} catch (NoSuchMethodException | InstantiationException | IllegalAccessException
				| InvocationTargetException e) {
			assert false; // we should never end up here
			throw new RuntimeException("Could not repackage to " + targetType, e);
		}
	}
}
