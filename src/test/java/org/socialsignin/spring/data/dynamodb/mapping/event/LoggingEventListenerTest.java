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
package org.socialsignin.spring.data.dynamodb.mapping.event;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.socialsignin.spring.data.dynamodb.domain.sample.User;
import software.amazon.awssdk.enhanced.dynamodb.model.PageIterable;
import uk.org.lidalia.slf4jext.Level;
import uk.org.lidalia.slf4jtest.TestLogger;
import uk.org.lidalia.slf4jtest.TestLoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.lenient;
import static uk.org.lidalia.slf4jtest.LoggingEvent.trace;

@ExtendWith(MockitoExtension.class)
public class LoggingEventListenerTest {

	private final TestLogger logger = TestLoggerFactory.getTestLogger(LoggingEventListener.class);
	private final User sampleEntity = new User();
	@Mock
	private PageIterable<User> sampleQueryList;
	@Mock
	private PageIterable<User> sampleScanList;

	private LoggingEventListener underTest;

	@BeforeEach
	public void setUp() {
		underTest = new LoggingEventListener();

		logger.setEnabledLevels(Level.TRACE);

		List<User> queryList = new ArrayList<>();
		queryList.add(sampleEntity);
		lenient().when(sampleQueryList.items().stream()).thenReturn(queryList.stream());
		lenient().when(sampleScanList.items().stream()).thenReturn(queryList.stream());
	}

	@AfterEach
	public void clearLoggers() {
		TestLoggerFactory.clear();
	}

	@Test
	public void testAfterDelete() {
		underTest.onApplicationEvent(new AfterDeleteEvent<>(sampleEntity));

		assertThat(logger.getLoggingEvents(), is(asList(trace("onAfterDelete: {}", sampleEntity))));
	}

	@Test
	public void testAfterLoad() {
		underTest.onApplicationEvent(new AfterLoadEvent<>(sampleEntity));

		assertThat(logger.getLoggingEvents(), is(asList(trace("onAfterLoad: {}", sampleEntity))));
	}

	@Test
	public void testAfterQuery() {
		underTest.onApplicationEvent(new AfterQueryEvent<>(sampleQueryList));

		assertThat(logger.getLoggingEvents(), is(asList(trace("onAfterQuery: {}", sampleEntity))));
	}

	@Test
	public void testAfterSave() {
		underTest.onApplicationEvent(new AfterSaveEvent<>(sampleEntity));

		assertThat(logger.getLoggingEvents(), is(asList(trace("onAfterSave: {}", sampleEntity))));
	}

	@Test
	public void testAfterScan() {
		underTest.onApplicationEvent(new AfterScanEvent<>(sampleScanList));

		assertThat(logger.getLoggingEvents(), is(asList(trace("onAfterScan: {}", sampleEntity))));
	}

	@Test
	public void testBeforeDelete() {
		underTest.onApplicationEvent(new BeforeDeleteEvent<>(sampleEntity));

		assertThat(logger.getLoggingEvents(), is(asList(trace("onBeforeDelete: {}", sampleEntity))));
	}

	@Test
	public void testBeforeSave() {
		underTest.onApplicationEvent(new BeforeSaveEvent<>(sampleEntity));

		assertThat(logger.getLoggingEvents(), is(asList(trace("onBeforeSave: {}", sampleEntity))));
	}

}
