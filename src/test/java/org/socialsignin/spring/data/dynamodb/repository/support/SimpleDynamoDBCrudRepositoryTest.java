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
package org.socialsignin.spring.data.dynamodb.repository.support;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.socialsignin.spring.data.dynamodb.core.DynamoDBOperations;
import org.socialsignin.spring.data.dynamodb.domain.sample.Playlist;
import org.socialsignin.spring.data.dynamodb.domain.sample.PlaylistId;
import org.socialsignin.spring.data.dynamodb.domain.sample.User;
import org.socialsignin.spring.data.dynamodb.exception.BatchWriteException;
import org.springframework.dao.EmptyResultDataAccessException;
import software.amazon.awssdk.enhanced.dynamodb.model.BatchWriteResult;
import software.amazon.awssdk.enhanced.dynamodb.model.PageIterable;
import software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.WriteRequest;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link SimpleDynamoDBCrudRepository}.
 *
 * @author Michael Lavelle
 * @author Sebastian Just
 */
@ExtendWith(MockitoExtension.class)
public class SimpleDynamoDBCrudRepositoryTest {

	@Mock
	private PageIterable<User> findAllResultMock;
	@Mock
	private Iterable<User> deleteAllMock;
	@Mock
	private DynamoDBOperations dynamoDBOperations;
	@Mock
	private EnableScanPermissions mockEnableScanPermissions;
	@Mock
	private DynamoDBEntityInformation<User, Long> entityWithSimpleIdInformation;
	@Mock
	private DynamoDBEntityInformation<Playlist, PlaylistId> entityWithCompositeIdInformation;

	private User testUser;
	private Playlist testPlaylist;
	private PlaylistId testPlaylistId;

	private SimpleDynamoDBCrudRepository<User, Long> repoForEntityWithOnlyHashKey;
	private SimpleDynamoDBCrudRepository<Playlist, PlaylistId> repoForEntityWithHashAndRangeKey;

	@BeforeEach
	public void setUp() {

		testUser = new User();

		testPlaylistId = new PlaylistId();
		testPlaylistId.setUserName("michael");
		testPlaylistId.setPlaylistName("playlist1");

		testPlaylist = new Playlist(testPlaylistId);

		when(entityWithSimpleIdInformation.getJavaType()).thenReturn(User.class);
		lenient().when(entityWithSimpleIdInformation.getHashKey(1l)).thenReturn(1l);

		lenient().when(mockEnableScanPermissions.isFindAllUnpaginatedScanEnabled()).thenReturn(true);
		lenient().when(mockEnableScanPermissions.isDeleteAllUnpaginatedScanEnabled()).thenReturn(true);
		lenient().when(mockEnableScanPermissions.isCountUnpaginatedScanEnabled()).thenReturn(true);

		lenient().when(entityWithCompositeIdInformation.getJavaType()).thenReturn(Playlist.class);
		lenient().when(entityWithCompositeIdInformation.getHashKey(testPlaylistId)).thenReturn("michael");
		lenient().when(entityWithCompositeIdInformation.getRangeKey(testPlaylistId)).thenReturn("playlist1");
		lenient().when(entityWithCompositeIdInformation.isRangeKeyAware()).thenReturn(true);

		repoForEntityWithOnlyHashKey = new SimpleDynamoDBCrudRepository<>(entityWithSimpleIdInformation,
				dynamoDBOperations, mockEnableScanPermissions);
		repoForEntityWithHashAndRangeKey = new SimpleDynamoDBCrudRepository<>(entityWithCompositeIdInformation,
				dynamoDBOperations, mockEnableScanPermissions);

		lenient().when(dynamoDBOperations.load(User.class, 1l, null)).thenReturn(testUser);
		lenient().when(dynamoDBOperations.load(Playlist.class, "michael", "playlist1", null)).thenReturn(testPlaylist);

	}

	@Test
	public void deleteById() {
		final long id = ThreadLocalRandom.current().nextLong();
		User testResult = new User();
		testResult.setId(Long.toString(id));

		when(entityWithSimpleIdInformation.getHashKey(id)).thenReturn(id);
		when(dynamoDBOperations.load(User.class, id, null)).thenReturn(testResult);

		repoForEntityWithOnlyHashKey.deleteById(id);

		ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
		Mockito.verify(dynamoDBOperations).delete(captor.capture(), null);
		assertEquals(Long.toString(id), captor.getValue().getId());
	}

	@Test
	public void deleteEntity() {
		repoForEntityWithOnlyHashKey.delete(testUser);

		verify(dynamoDBOperations).delete(testUser, null);
	}

	@Test
	public void deleteIterable() {
		repoForEntityWithOnlyHashKey.deleteAll(deleteAllMock);

		verify(dynamoDBOperations).batchDelete(deleteAllMock, null);
	}

	@Test
	public void deleteAll() {
		when(dynamoDBOperations.scan(eq(User.class), any(ScanEnhancedRequest.class), null)).thenReturn(findAllResultMock);

		repoForEntityWithOnlyHashKey.deleteAll();
		verify(dynamoDBOperations).batchDelete(findAllResultMock, null);
	}

	@Test
	public void testFindAll() {
		when(dynamoDBOperations.scan(eq(User.class), any(ScanEnhancedRequest.class), null)).thenReturn(findAllResultMock);

		Iterable<User> actual = repoForEntityWithOnlyHashKey.findAll();

		assertSame(actual, findAllResultMock);
	}

	/**
	 * /**
	 *
	 * @see <a href="https://jira.spring.io/browse/DATAJPA-177">DATAJPA-177</a>
	 */
	@Test
	public void throwsExceptionIfEntityOnlyHashKeyToDeleteDoesNotExist() {
		assertThrows(EmptyResultDataAccessException.class, () -> {
			repoForEntityWithOnlyHashKey.deleteById(4711L);
		});
	}

	@Test
	public void testEntityDelete() {
		final long id = ThreadLocalRandom.current().nextLong();
		User entity = new User();
		entity.setId(Long.toString(id));

		repoForEntityWithOnlyHashKey.delete(entity);

		ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
		Mockito.verify(dynamoDBOperations).delete(captor.capture(), null);
		assertEquals(Long.toString(id), captor.getValue().getId());
	}

	@Test
	public void existsEntityWithOnlyHashKey() {
		when(dynamoDBOperations.load(User.class, 1l, null)).thenReturn(null);

		boolean actual = repoForEntityWithOnlyHashKey.existsById(1l);

		assertFalse(actual);
	}

	@Test
	public void testCount() {
		repoForEntityWithOnlyHashKey.count();

		verify(dynamoDBOperations).count(eq(User.class), any(QueryRequest.Builder.class));
	}

	@Test
	public void findOneEntityWithOnlyHashKey() {
		Optional<User> user = repoForEntityWithOnlyHashKey.findById(1l);
		Mockito.verify(dynamoDBOperations).load(User.class, 1l, null);
		assertEquals(testUser, user.get());
	}

	@Test
	public void findOneEntityWithHashAndRangeKey() {
		Optional<Playlist> playlist = repoForEntityWithHashAndRangeKey.findById(testPlaylistId);
		assertEquals(testPlaylist, playlist.get());
	}

	@Test
	public void testSave() {
		final long id = ThreadLocalRandom.current().nextLong();
		User entity = new User();
		entity.setId(Long.toString(id));

		repoForEntityWithOnlyHashKey.save(entity);

		verify(dynamoDBOperations).save(entity, null);
	}

	@Test
	public void testBatchSave() {

		List<User> entities = new ArrayList<>();
		entities.add(new User());
		entities.add(new User());
		when(dynamoDBOperations.batchSave(anyIterable(), null)).thenReturn(BatchWriteResult.builder().build());

		repoForEntityWithOnlyHashKey.saveAll(entities);

		verify(dynamoDBOperations).batchSave(entities, null);
	}

	@Test
	public void testBatchSaveFailure() {
		Map<String, List<WriteRequest>> map = new HashMap<>();
		map.put("user", Collections.singletonList(WriteRequest.builder().build()));

		List<User> entities = new ArrayList<>();
		entities.add(new User());
		entities.add(new User());
		when(dynamoDBOperations.batchSave(anyIterable(), null))
				.thenReturn(BatchWriteResult.builder().unprocessedRequests(map).build());

		assertThrows(BatchWriteException.class, () -> {
			repoForEntityWithOnlyHashKey.saveAll(entities);
		}, "Processing of entities failed!");
	}

	/**
	 * @see <a href="https://jira.spring.io/browse/DATAJPA-177">DATAJPA-177</a>
	 */
	@Test
	public void throwsExceptionIfEntityWithHashAndRangeKeyToDeleteDoesNotExist() {

		PlaylistId playlistId = new PlaylistId();
		playlistId.setUserName("someUser");
		playlistId.setPlaylistName("somePlaylistName");

		assertThrows(EmptyResultDataAccessException.class, () -> {
			repoForEntityWithHashAndRangeKey.deleteById(playlistId);
		});
	}
}
