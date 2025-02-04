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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class SortHandlerTest {

	private SortHandler underTest = new SortHandler() {
	};

	@Test
	public void testThrowUnsupportedSortException() {
		assertThrows(UnsupportedOperationException.class, () -> underTest.throwUnsupportedSortOperationException());
	}

	@Test
	public void testEnsureNoSortUnsorted() {
		underTest.ensureNoSort(Sort.unsorted());
	}

	@Test
	public void testEnsureNoSortSorted() {
		assertThrows(UnsupportedOperationException.class, () -> underTest.ensureNoSort(Sort.by("property")));
	}

	@Test
	public void testEnsureNoSortUnpaged() {
		underTest.ensureNoSort(Pageable.unpaged());
	}

	@Test
	public void TestEnsureNoSortPagedUnsorted() {
		underTest.ensureNoSort(PageRequest.of(0, 1, Sort.unsorted()));
	}

	@Test
	public void TestEnsureNoSortPagedSorted() {
		assertThrows(UnsupportedOperationException.class, () ->
				underTest.ensureNoSort(PageRequest.of(0, 1, Sort.by("property"))));
	}
}
