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
package org.socialsignin.spring.data.dynamodb.repository.query;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.socialsignin.spring.data.dynamodb.core.DynamoDBOperations;
import org.socialsignin.spring.data.dynamodb.domain.sample.DynamoDBYearMarshaller;
import org.socialsignin.spring.data.dynamodb.domain.sample.Playlist;
import org.socialsignin.spring.data.dynamodb.domain.sample.PlaylistId;
import org.socialsignin.spring.data.dynamodb.domain.sample.User;
import org.socialsignin.spring.data.dynamodb.repository.QueryConstants;
import org.socialsignin.spring.data.dynamodb.repository.support.DynamoDBEntityInformation;
import org.socialsignin.spring.data.dynamodb.repository.support.DynamoDBIdIsHashAndRangeKeyEntityInformation;
import org.springframework.data.repository.query.Parameter;
import org.springframework.data.repository.query.Parameters;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.util.ClassUtils;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.model.PageIterable;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.ComparisonOperator;
import software.amazon.awssdk.services.dynamodb.model.Condition;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
public class PartTreeDynamoDBQueryUnitTest {

	private RepositoryQuery partTreeDynamoDBQuery;

	@Mock
	private DynamoDBOperations mockDynamoDBOperations;
	@Mock
	private DynamoDBQueryMethod<User, String> mockDynamoDBUserQueryMethod;
	@Mock
	private DynamoDBEntityInformation<User, String> mockUserEntityMetadata;
	@Mock
	private DynamoDBQueryMethod<Playlist, PlaylistId> mockDynamoDBPlaylistQueryMethod;
	@Mock
	private DynamoDBIdIsHashAndRangeKeyEntityInformation<Playlist, PlaylistId> mockPlaylistEntityMetadata;

	@SuppressWarnings("rawtypes")
	@Mock
	private Parameters mockParameters;

	@Mock
	private User mockUser;
	@Mock
	private Playlist mockPlaylist;

	@Mock
	private PageIterable<User> mockUserScanResults;
	@Mock
	private PageIterable<Playlist> mockPlaylistScanResults;
	@Mock
	private PageIterable<Playlist> mockPlaylistQueryResults;
	@Mock
	private PageIterable<User> mockUserQueryResults;

	// Mock out specific DynamoDBOperations behavior expected by this method
	ArgumentCaptor<QueryEnhancedRequest> playlistQueryCaptor;
	ArgumentCaptor<QueryRequest> queryResultCaptor = ArgumentCaptor.forClass(QueryRequest.class);
	ArgumentCaptor<QueryEnhancedRequest> queryEnhancedRequestCaptor = ArgumentCaptor.forClass(QueryEnhancedRequest.class);
	ArgumentCaptor<Class<Playlist>> playlistClassCaptor;
	ArgumentCaptor<Class<User>> userClassCaptor;
	ArgumentCaptor<ScanRequest> scanCaptor;
	ArgumentCaptor<ScanEnhancedRequest> scanEnhancedCaptor;

	ArgumentCaptor<QueryEnhancedRequest.Builder> playlistQueryCaptorBuilder;
	ArgumentCaptor<ScanRequest.Builder> scanCaptorBuilder;
	ArgumentCaptor<ScanEnhancedRequest.Builder> scanEnhancedCaptorBuilder;

	@BeforeEach
	@SuppressWarnings("unchecked")
	public void setUp() {
		lenient().when(mockPlaylistEntityMetadata.isCompositeHashAndRangeKeyProperty("playlistId")).thenReturn(true);
		lenient().when(mockPlaylistEntityMetadata.getHashKeyPropertyName()).thenReturn("userName");
		// Mockito.when(mockPlaylistEntityMetadata.isHashKeyProperty("userName")).thenReturn(true);
		lenient().when(mockPlaylistEntityMetadata.getJavaType()).thenReturn(Playlist.class);
		lenient().when(mockPlaylistEntityMetadata.getRangeKeyPropertyName()).thenReturn("playlistName");
		lenient().when(mockPlaylistEntityMetadata.getIndexRangeKeyPropertyNames()).thenReturn(new HashSet<String>());
		lenient().when(mockDynamoDBUserQueryMethod.getEntityInformation()).thenReturn(mockUserEntityMetadata);
		lenient().when(mockDynamoDBUserQueryMethod.getParameters()).thenReturn(mockParameters);
		lenient().when(mockDynamoDBUserQueryMethod.getConsistentReadMode()).thenReturn(QueryConstants.ConsistentReadMode.DEFAULT);
		lenient().when(mockDynamoDBPlaylistQueryMethod.getEntityInformation()).thenReturn(mockPlaylistEntityMetadata);
		lenient().when(mockDynamoDBPlaylistQueryMethod.getParameters()).thenReturn(mockParameters);
		lenient().when(mockUserEntityMetadata.getHashKeyPropertyName()).thenReturn("id");
		// Mockito.when(mockUserEntityMetadata.isHashKeyProperty("id")).thenReturn(true);
		lenient().when(mockUserEntityMetadata.getJavaType()).thenReturn(User.class);
		lenient().when(mockDynamoDBUserQueryMethod.isScanEnabled()).thenReturn(true);
		lenient().when(mockDynamoDBPlaylistQueryMethod.isScanEnabled()).thenReturn(true);

		// Mock out specific DynamoDBOperations behavior expected by this method
		playlistQueryCaptor = ArgumentCaptor.forClass(QueryEnhancedRequest.class);
		playlistClassCaptor = ArgumentCaptor.forClass(Class.class);
		userClassCaptor = ArgumentCaptor.forClass(Class.class);
		scanCaptor = ArgumentCaptor.forClass(ScanRequest.class);
		scanEnhancedCaptor = ArgumentCaptor.forClass(ScanEnhancedRequest.class);
		playlistQueryCaptorBuilder = ArgumentCaptor.forClass(QueryEnhancedRequest.Builder.class);
		scanCaptorBuilder = ArgumentCaptor.forClass(ScanRequest.Builder.class);
		scanEnhancedCaptorBuilder = ArgumentCaptor.forClass(ScanEnhancedRequest.Builder.class);
	}

	private <T, ID extends Serializable> void setupCommonMocksForThisRepositoryMethod(
			DynamoDBEntityInformation<T, ID> mockEntityMetadata, DynamoDBQueryMethod<T, ID> mockDynamoDBQueryMethod,
			Class<T> clazz, String repositoryMethodName, int numberOfParameters, String hashKeyProperty,
			String rangeKeyProperty) {

		if (rangeKeyProperty != null) {
			Mockito.when(mockEntityMetadata.isRangeKeyAware()).thenReturn(true);
		}

		lenient().when(mockDynamoDBQueryMethod.getEntityType()).thenReturn(clazz);
		lenient().when(mockDynamoDBQueryMethod.getName()).thenReturn(repositoryMethodName);
		lenient().when(mockDynamoDBQueryMethod.getParameters()).thenReturn(mockParameters);
		lenient().when(mockDynamoDBQueryMethod.getConsistentReadMode()).thenReturn(QueryConstants.ConsistentReadMode.DEFAULT);
		lenient().when(mockParameters.getBindableParameters()).thenReturn(mockParameters);
		lenient().when(mockParameters.getNumberOfParameters()).thenReturn(numberOfParameters);
		// Mockito.when(mockDynamoDBQueryMethod.getReturnedObjectType()).thenReturn(clazz);
		if (hashKeyProperty != null) {
			// Mockito.when(mockEntityMetadata.isHashKeyProperty(hashKeyProperty)).thenReturn(true);
		}

		for (int i = 0; i < numberOfParameters; i++) {
			Parameter mockParameter = Mockito.mock(Parameter.class);
			lenient().when(mockParameter.getIndex()).thenReturn(i);
			lenient().when(mockParameters.getBindableParameter(i)).thenReturn(mockParameter);
		}
		partTreeDynamoDBQuery = new PartTreeDynamoDBQuery<>(mockDynamoDBOperations, mockDynamoDBQueryMethod);
	}

	@Test
	public void testGetQueryMethod() {
		setupCommonMocksForThisRepositoryMethod(mockUserEntityMetadata, mockDynamoDBUserQueryMethod, User.class,
				"findById", 1, "id", null);
		assertEquals(mockDynamoDBUserQueryMethod, partTreeDynamoDBQuery.getQueryMethod());
	}

	@Test
	public void testExecute_WhenFinderMethodIsFindingSingleEntity_WithSingleStringParameter_WhenFindingByHashKey() {
		setupCommonMocksForThisRepositoryMethod(mockUserEntityMetadata, mockDynamoDBUserQueryMethod, User.class,
				"findById", 1, "id", null);

		// Mock out specific DynamoDBOperations behavior expected by this method
		Mockito.when(mockDynamoDBOperations.load(User.class, "someId", null)).thenReturn(mockUser);

		// Execute the query
		Object[] parameters = new Object[]{"someId"};
		Object o = partTreeDynamoDBQuery.execute(parameters);

		// Assert that we obtain the expected single result
		assertEquals(o, mockUser);

		// Verify that the expected DynamoDBOperations method was called
		Mockito.verify(mockDynamoDBOperations).load(User.class, "someId", null);
	}

	@Test
	public void testExecute_WhenFinderMethodIsFindingSingleEntityWithCompositeId_WithSingleStringParameter_WhenFindingByHashAndRangeKey() {
		setupCommonMocksForThisRepositoryMethod(mockPlaylistEntityMetadata, mockDynamoDBPlaylistQueryMethod,
				Playlist.class, "findByUserNameAndPlaylistName", 2, "userName", "playlistName");

		// Mock out specific DynamoDBOperations behavior expected by this method
		Mockito.when(mockDynamoDBOperations.load(Playlist.class, "someUserName", "somePlaylistName", null))
				.thenReturn(mockPlaylist);

		// Execute the query
		Object[] parameters = new Object[]{"someUserName", "somePlaylistName"};
		Object o = partTreeDynamoDBQuery.execute(parameters);

		// Assert that we obtain the expected single result
		assertEquals(o, mockPlaylist);

		// Verify that the expected DynamoDBOperations method was called
		Mockito.verify(mockDynamoDBOperations).load(Playlist.class, "someUserName", "somePlaylistName", null);
	}

	@Test
	public void testExecute_WhenFinderMethodIsFindingSingleEntityWithCompositeId_WhenFindingByCompositeId() {
		PlaylistId playlistId = new PlaylistId();
		playlistId.setUserName("someUserName");
		playlistId.setPlaylistName("somePlaylistName");

		setupCommonMocksForThisRepositoryMethod(mockPlaylistEntityMetadata, mockDynamoDBPlaylistQueryMethod,
				Playlist.class, "findByPlaylistId", 1, "userName", "playlistName");
		Mockito.when(mockPlaylistEntityMetadata.getHashKey(playlistId)).thenReturn("someUserName");
		Mockito.when(mockPlaylistEntityMetadata.getRangeKey(playlistId)).thenReturn("somePlaylistName");

		// Mock out specific DynamoDBOperations behavior expected by this method
		Mockito.when(mockDynamoDBOperations.load(Playlist.class, "someUserName", "somePlaylistName", null))
				.thenReturn(mockPlaylist);

		// Execute the query

		Object[] parameters = new Object[]{playlistId};
		Object o = partTreeDynamoDBQuery.execute(parameters);

		// Assert that we obtain the expected single result
		assertEquals(o, mockPlaylist);

		// Verify that the expected DynamoDBOperations method was called
		Mockito.verify(mockDynamoDBOperations).load(Playlist.class, "someUserName", "somePlaylistName", null);
	}

	@Test
	public void testExecute_WhenFinderMethodIsFindingEntityWithCompositeIdList_WhenFindingByNotCompositeId() {
		PlaylistId playlistId = new PlaylistId();
		playlistId.setUserName("someUserName");
		playlistId.setPlaylistName("somePlaylistName");

		setupCommonMocksForThisRepositoryMethod(mockPlaylistEntityMetadata, mockDynamoDBPlaylistQueryMethod,
				Playlist.class, "findByPlaylistIdNot", 1, "userName", "playlistName");

		Mockito.when(mockDynamoDBPlaylistQueryMethod.isCollectionQuery()).thenReturn(true);

		// Mockito.when(mockPlaylistEntityMetadata.getHashKey(playlistId)).thenReturn("someUserName");
		// Mockito.when(mockPlaylistEntityMetadata.getRangeKey(playlistId)).thenReturn("somePlaylistName");

		// Mock out specific DynamoDBOperations behavior expected by this method
		// ArgumentCaptor<DynamoDBScanExpression> scanCaptor =
		// ArgumentCaptor.forClass(DynamoDBScanExpression.class);
		// ArgumentCaptor<Class> classCaptor = ArgumentCaptor.forClass(Class.class);
		// Mockito.when(mockPlaylistScanResults.get(0)).thenReturn(mockPlaylist);
		// Mockito.when(mockPlaylistScanResults.size()).thenReturn(1);
		// Mockito.when(mockDynamoDBOperations.scan(classCaptor.capture(),
		// scanCaptor.capture())).thenReturn(
		// mockPlaylistScanResults);

		// Execute the query

		Object[] parameters = new Object[]{playlistId};
		assertThrows(UnsupportedOperationException.class, () -> {
			partTreeDynamoDBQuery.execute(parameters);
		});
	}

	@Test
	public void testExecute_WhenFinderMethodIsFindingEntityWithCompositeIdList_WhenFindingByRangeKeyOnly() {

		setupCommonMocksForThisRepositoryMethod(mockPlaylistEntityMetadata, mockDynamoDBPlaylistQueryMethod,
				Playlist.class, "findByPlaylistName", 1, "userName", "playlistName");

		Mockito.when(mockDynamoDBPlaylistQueryMethod.isCollectionQuery()).thenReturn(true);

		Mockito.when(mockPlaylistScanResults.items().stream().toList().get(0)).thenReturn(mockPlaylist);
		Mockito.when(mockPlaylistScanResults.items().stream().toList().size()).thenReturn(1);
		Mockito.when(mockDynamoDBOperations.scan(playlistClassCaptor.capture(), scanEnhancedCaptor.capture(), null))
				.thenReturn(mockPlaylistScanResults);

		// Execute the query

		Object[] parameters = new Object[]{"somePlaylistName"};
		Object o = partTreeDynamoDBQuery.execute(parameters);

		// Assert that we obtain the expected results
		assertEquals(mockPlaylistScanResults, o);
		assertEquals(1, mockPlaylistScanResults.items().stream().toList().size());
		assertEquals(mockPlaylist, mockPlaylistScanResults.items().stream().toList().get(0));

		// Assert that we scanned DynamoDB for the correct class
		assertEquals(playlistClassCaptor.getValue(), Playlist.class);

		// Assert that we only one filter condition for the one property
		Expression expression = scanEnhancedCaptor.getValue().filterExpression();
		// TODO

//		Map<String, Condition> filterConditions = scanCaptor.getValue().filterExpression().getScanFilter();
//		assertEquals(1, filterConditions.size());
//		Condition filterCondition = filterConditions.get("playlistName");
//
//		assertNotNull(filterCondition);
//
//		assertEquals(ComparisonOperator.EQ, filterCondition.comparisonOperator());
//
//		// Assert we only have one attribute value for the filter condition
//		assertEquals(1, filterCondition.attributeValueList().size());
//
//		// Assert that there the attribute value type for this attribute value
//		// is String,
//		// and its value is the parameter expected
//		assertEquals("somePlaylistName", filterCondition.attributeValueList().get(0).s());

		// Verify that the expected DynamoDBOperations method was called
		Mockito.verify(mockDynamoDBOperations).scan(playlistClassCaptor.getValue(), scanEnhancedCaptor.getValue(), null);
	}

	@Test
	public void testExecute_WhenFinderMethodIsFindingEntityWithCompositeIdList_WhenFindingByCompositeIdWithRangeKeyOnly() {
		PlaylistId playlistId = new PlaylistId();
		playlistId.setPlaylistName("somePlaylistName");

		setupCommonMocksForThisRepositoryMethod(mockPlaylistEntityMetadata, mockDynamoDBPlaylistQueryMethod,
				Playlist.class, "findByPlaylistId", 1, "userName", "playlistName");

		Mockito.when(mockDynamoDBPlaylistQueryMethod.isCollectionQuery()).thenReturn(true);
		Mockito.when(mockPlaylistEntityMetadata.getHashKey(playlistId)).thenReturn(null);
		Mockito.when(mockPlaylistEntityMetadata.getRangeKey(playlistId)).thenReturn("somePlaylistName");

		Mockito.when(mockPlaylistScanResults.items().stream().toList().get(0)).thenReturn(mockPlaylist);
		Mockito.when(mockPlaylistScanResults.items().stream().toList().size()).thenReturn(1);
		Mockito.when(mockDynamoDBOperations.scan(playlistClassCaptor.capture(), scanEnhancedCaptor.capture(), null))
				.thenReturn(mockPlaylistScanResults);

		// Execute the query

		Object[] parameters = new Object[]{playlistId};
		Object o = partTreeDynamoDBQuery.execute(parameters);

		// Assert that we obtain the expected results
		assertEquals(mockPlaylistScanResults, o);
		assertEquals(1, mockPlaylistScanResults.items().stream().toList().size());
		assertEquals(mockPlaylist, mockPlaylistScanResults.items().stream().toList().get(0));

		// Assert that we scanned DynamoDB for the correct class
		assertEquals(playlistClassCaptor.getValue(), Playlist.class);

		// Assert that we only one filter condition for the one property
		//TODO
//		Map<String, Condition> filterConditions = scanCaptor.getValue().getScanFilter();
//		assertEquals(1, filterConditions.size());
//		Condition filterCondition = filterConditions.get("playlistName");
//
//		assertNotNull(filterCondition);
//
//		assertEquals(ComparisonOperator.EQ, filterCondition.comparisonOperator());
//
//		// Assert we only have one attribute value for the filter condition
//		assertEquals(1, filterCondition.attributeValueList().size());
//
//		// Assert that there the attribute value type for this attribute value
//		// is String,
//		// and its value is the parameter expected
//		assertEquals("somePlaylistName", filterCondition.attributeValueList().get(0).s());
//
//		// Assert that all other attribute value types other than String type
//		// are null
//		assertNull(filterCondition.attributeValueList().get(0).ss());
//		assertNull(filterCondition.attributeValueList().get(0).n());
//		assertNull(filterCondition.attributeValueList().get(0).ns());
//		assertNull(filterCondition.attributeValueList().get(0).b());
//		assertNull(filterCondition.attributeValueList().get(0).bs());

		// Verify that the expected DynamoDBOperations method was called
		Mockito.verify(mockDynamoDBOperations).scan(playlistClassCaptor.getValue(), scanEnhancedCaptor.getValue(), null);
	}

	@Test
	public void testExecute_WhenFinderMethodIsFindingEntityWithCompositeIdList_WithSingleStringParameter_WhenFindingByHashKeyOnly() {
		setupCommonMocksForThisRepositoryMethod(mockPlaylistEntityMetadata, mockDynamoDBPlaylistQueryMethod,
				Playlist.class, "findByUserName", 1, "userName", "playlistName");
		Mockito.when(mockDynamoDBPlaylistQueryMethod.isCollectionQuery()).thenReturn(true);
		Playlist prototypeHashKey = new Playlist();
		prototypeHashKey.setUserName("someUserName");
		Mockito.when(mockPlaylistEntityMetadata.getHashKeyPropotypeEntityForHashKey("someUserName"))
				.thenReturn(prototypeHashKey);

		Mockito.when(mockPlaylistQueryResults.items().stream().toList().get(0)).thenReturn(mockPlaylist);
		Mockito.when(mockPlaylistQueryResults.items().stream().toList().size()).thenReturn(1);
		Mockito.when(mockDynamoDBOperations.query(playlistClassCaptor.capture(), playlistQueryCaptor.capture(), null))
				.thenReturn(mockPlaylistQueryResults);

		// Execute the query
		Object[] parameters = new Object[]{"someUserName"};
		Object o = partTreeDynamoDBQuery.execute(parameters);

		// Assert that we obtain the expected results
		assertEquals(mockPlaylistQueryResults, o);
		assertEquals(1, mockPlaylistQueryResults.items().stream().toList().size());
		assertEquals(mockPlaylist, mockPlaylistQueryResults.items().stream().toList().get(0));

		// Assert that we scanned DynamoDB for the correct class
		assertEquals(playlistClassCaptor.getValue(), Playlist.class);

		// Assert that we have only one filter condition, for the name of the
		// property
		// TODO
//		Object hashKeyPrototypeObject = playlistQueryCaptor.getValue().getHashKeyValues();
//		assertTrue(hashKeyPrototypeObject instanceof Playlist);
//		Playlist hashKeyPropertyPlaylist = (Playlist) hashKeyPrototypeObject;
//		assertEquals("someUserName", hashKeyPropertyPlaylist.getUserName());

		// Verify that the expected DynamoDBOperations method was called
		Mockito.verify(mockDynamoDBOperations).query(playlistClassCaptor.getValue(), playlistQueryCaptor.getValue(), null);
	}

	@Test
	public void testExecute_WhenFinderMethodIsCountingEntityWithCompositeIdList_WhenFindingByRangeKeyOnly_ScanCountEnabled() {

		setupCommonMocksForThisRepositoryMethod(mockPlaylistEntityMetadata, mockDynamoDBPlaylistQueryMethod,
				Playlist.class, "countByPlaylistName", 1, "userName", "playlistName");

		Mockito.when(mockDynamoDBPlaylistQueryMethod.isCollectionQuery()).thenReturn(false);
		Mockito.when(mockDynamoDBPlaylistQueryMethod.isScanCountEnabled()).thenReturn(true);

		Mockito.when(mockDynamoDBOperations.count(playlistClassCaptor.capture(), scanCaptorBuilder.capture(), null)).thenReturn(100);

		// Execute the query

		Object[] parameters = new Object[]{"somePlaylistName"};
		Object o = partTreeDynamoDBQuery.execute(parameters);

		// Assert that we obtain the expected results
		assertEquals(100l, o);

		// Assert that we scanned DynamoDB for the correct class
		assertEquals(playlistClassCaptor.getValue(), Playlist.class);

		// Assert that we only one filter condition for the one property
		// TODO
//		Map<String, Condition> filterConditions = scanCaptor.getValue().getScanFilter();
//		assertEquals(1, filterConditions.size());
//		Condition filterCondition = filterConditions.get("playlistName");
//
//		assertNotNull(filterCondition);
//
//		assertEquals(ComparisonOperator.EQ, filterCondition.comparisonOperator());
//
//		// Assert we only have one attribute value for the filter condition
//		assertEquals(1, filterCondition.attributeValueList().size());
//
//		// Assert that there the attribute value type for this attribute value
//		// is String,
//		// and its value is the parameter expected
//		assertEquals("somePlaylistName", filterCondition.attributeValueList().get(0).s());
//
//		// Assert that all other attribute value types other than String type
//		// are null
//		assertNull(filterCondition.attributeValueList().get(0).ss());
//		assertNull(filterCondition.attributeValueList().get(0).n());
//		assertNull(filterCondition.attributeValueList().get(0).ns());
//		assertNull(filterCondition.attributeValueList().get(0).b());
//		assertNull(filterCondition.attributeValueList().get(0).bs());

		// Verify that the expected DynamoDBOperations method was called
		Mockito.verify(mockDynamoDBOperations).count(playlistClassCaptor.getValue(), scanCaptorBuilder.getValue(), null);
	}

	@Test
	public void testExecute_WhenFinderMethodIsCountingEntityWithCompositeIdList_WhenFindingByRangeKeyOnly_ScanCountDisabled() {

		setupCommonMocksForThisRepositoryMethod(mockPlaylistEntityMetadata, mockDynamoDBPlaylistQueryMethod,
				Playlist.class, "countByPlaylistName", 1, "userName", "playlistName");

		Mockito.when(mockDynamoDBPlaylistQueryMethod.isCollectionQuery()).thenReturn(false);
		Mockito.when(mockDynamoDBPlaylistQueryMethod.isScanCountEnabled()).thenReturn(false);

		// Mock out specific DynamoDBOperations behavior expected by this method
		// ArgumentCaptor<DynamoDBScanExpression> scanCaptor =
		// ArgumentCaptor.forClass(DynamoDBScanExpression.class);
		// ArgumentCaptor<Class<Playlist>> classCaptor =
		// ArgumentCaptor.forClass(Class.class);
		// Mockito.when(mockDynamoDBOperations.count(classCaptor.capture(),
		// scanCaptor.capture())).thenReturn(
		// 100);

		// Execute the query

		Object[] parameters = new Object[]{"somePlaylistName"};
		assertThrows(IllegalArgumentException.class, () -> {
			partTreeDynamoDBQuery.execute(parameters);
		});
	}

	@Test
	public void testExecute_WhenFinderMethodIsFindingEntityWithCompositeIdList_WithSingleStringParameter_WhenFindingByHashKeyAndNotRangeKey() {
		setupCommonMocksForThisRepositoryMethod(mockPlaylistEntityMetadata, mockDynamoDBPlaylistQueryMethod,
				Playlist.class, "findByUserNameAndPlaylistNameNot", 2, "userName", "playlistName");
		Mockito.when(mockDynamoDBPlaylistQueryMethod.isCollectionQuery()).thenReturn(true);
		Playlist prototypeHashKey = new Playlist();
		prototypeHashKey.setUserName("someUserName");

		// Mockito.when(mockPlaylistEntityMetadata.getHashKeyPropotypeEntityForHashKey("someUserName")).thenReturn(
		// prototypeHashKey);

		Mockito.when(mockPlaylistScanResults.items().stream().toList().get(0)).thenReturn(mockPlaylist);
		Mockito.when(mockPlaylistScanResults.items().stream().toList().size()).thenReturn(1);
		Mockito.when(mockDynamoDBOperations.scan(playlistClassCaptor.capture(), scanEnhancedCaptor.capture(), null))
				.thenReturn(mockPlaylistScanResults);

		// Execute the query
		Object[] parameters = new Object[]{"someUserName", "somePlaylistName"};
		Object o = partTreeDynamoDBQuery.execute(parameters);

		// Assert that we obtain the expected results
		assertEquals(mockPlaylistScanResults, o);
		assertEquals(1, mockPlaylistScanResults.items().stream().toList().size());
		assertEquals(mockPlaylist, mockPlaylistScanResults.items().stream().toList().get(0));

		// Assert that we scanned DynamoDB for the correct class
		assertEquals(playlistClassCaptor.getValue(), Playlist.class);

		// Assert that we have the correct filter conditions
		// TODO
//		Map<String, Condition> filterConditions = scanCaptor.getValue().getScanFilter();
//		assertEquals(2, filterConditions.size());
//		Condition filterCondition1 = filterConditions.get("userName");
//		Condition filterCondition2 = filterConditions.get("playlistName");
//
//		assertEquals(ComparisonOperator.EQ, filterCondition1.comparisonOperator());
//		assertEquals(ComparisonOperator.NE, filterCondition2.comparisonOperator());
//
//		// Assert we only have one attribute value for this filter condition
//		assertEquals(1, filterCondition1.attributeValueList().size());
//		assertEquals(1, filterCondition2.attributeValueList().size());
//
//		// Assert that there the attribute value type for this attribute value
//		// is String,
//		// and its value is the parameter expected
//		assertEquals("someUserName", filterCondition1.attributeValueList().get(0).s());
//		assertEquals("somePlaylistName", filterCondition2.attributeValueList().get(0).s());
//
//		// Assert that all other attribute value types other than String type
//		// are null
//		assertNull(filterCondition1.attributeValueList().get(0).ss());
//		assertNull(filterCondition1.attributeValueList().get(0).n());
//		assertNull(filterCondition1.attributeValueList().get(0).ns());
//		assertNull(filterCondition1.attributeValueList().get(0).b());
//		assertNull(filterCondition1.attributeValueList().get(0).bs());
//
//		assertNull(filterCondition2.attributeValueList().get(0).ss());
//		assertNull(filterCondition2.attributeValueList().get(0).n());
//		assertNull(filterCondition2.attributeValueList().get(0).ns());
//		assertNull(filterCondition2.attributeValueList().get(0).n());
//		assertNull(filterCondition2.attributeValueList().get(0).ns());

		// Verify that the expected DynamoDBOperations method was called
		Mockito.verify(mockDynamoDBOperations).scan(playlistClassCaptor.getValue(), scanEnhancedCaptor.getValue(), null); // Assert
		// that
		// we
		// obtain
		// the
		// expected
		// results

	}

	@Test
	public void testExecute_WhenFinderMethodIsFindingEntityWithCompositeIdList_WhenFindingByCompositeIdWithHashKeyOnly() {
		PlaylistId playlistId = new PlaylistId();
		playlistId.setUserName("someUserName");

		setupCommonMocksForThisRepositoryMethod(mockPlaylistEntityMetadata, mockDynamoDBPlaylistQueryMethod,
				Playlist.class, "findByPlaylistId", 1, "userName", "playlistName");
		Mockito.when(mockDynamoDBPlaylistQueryMethod.isCollectionQuery()).thenReturn(true);
		Playlist prototypeHashKey = new Playlist();
		prototypeHashKey.setUserName("someUserName");
		Mockito.when(mockPlaylistEntityMetadata.getHashKeyPropotypeEntityForHashKey("someUserName"))
				.thenReturn(prototypeHashKey);
		Mockito.when(mockPlaylistEntityMetadata.getHashKey(playlistId)).thenReturn("someUserName");
		Mockito.when(mockPlaylistEntityMetadata.getRangeKey(playlistId)).thenReturn(null);

		// Mock out specific DynamoDBOperations behavior expected by this method
		Mockito.when(mockPlaylistQueryResults.items().stream().toList().get(0)).thenReturn(mockPlaylist);
		Mockito.when(mockPlaylistQueryResults.items().stream().toList().size()).thenReturn(1);
		Mockito.when(mockDynamoDBOperations.query(playlistClassCaptor.capture(), playlistQueryCaptor.capture(), null))
				.thenReturn(mockPlaylistQueryResults);

		// Execute the query
		Object[] parameters = new Object[]{playlistId};
		Object o = partTreeDynamoDBQuery.execute(parameters);

		// Assert that we obtain the expected results
		assertEquals(mockPlaylistQueryResults, o);
		assertEquals(1, mockPlaylistQueryResults.items().stream().toList().size());
		assertEquals(mockPlaylist, mockPlaylistQueryResults.items().stream().toList().get(0));

		// Assert that we scanned DynamoDB for the correct class
		assertEquals(playlistClassCaptor.getValue(), Playlist.class);

		// Assert that we have only one filter condition, for the name of the
		// property
		// TODO
//		Object hashKeyPrototypeObject = playlistQueryCaptor.getValue().getHashKeyValues();
//		assertTrue(hashKeyPrototypeObject instanceof Playlist);
//		Playlist hashKeyPropertyPlaylist = (Playlist) hashKeyPrototypeObject;
//		assertEquals("someUserName", hashKeyPropertyPlaylist.getUserName());
//
//		assertEquals(0, playlistQueryCaptor.getValue().getRangeKeyConditions().size());

		// Verify that the expected DynamoDBOperations method was called
		Mockito.verify(mockDynamoDBOperations).query(playlistClassCaptor.getValue(), playlistQueryCaptor.getValue(), null);

	}

	@Test
	public void testExecute_WhenFinderMethodIsFindingEntityWithCompositeIdList_WhenFindingByCompositeId_HashKey() {
		PlaylistId playlistId = new PlaylistId();
		playlistId.setUserName("someUserName");

		setupCommonMocksForThisRepositoryMethod(mockPlaylistEntityMetadata, mockDynamoDBPlaylistQueryMethod,
				Playlist.class, "findByPlaylistIdUserName", 1, "userName", "playlistName");
		Mockito.when(mockDynamoDBPlaylistQueryMethod.isCollectionQuery()).thenReturn(true);
		Playlist prototypeHashKey = new Playlist();
		prototypeHashKey.setUserName("someUserName");
		Mockito.when(mockPlaylistEntityMetadata.getHashKeyPropotypeEntityForHashKey("someUserName"))
				.thenReturn(prototypeHashKey);
		// Mockito.when(mockPlaylistEntityMetadata.getHashKey(playlistId)).thenReturn("someUserName");
		// Mockito.when(mockPlaylistEntityMetadata.getRangeKey(playlistId)).thenReturn(null);

		// Mock out specific DynamoDBOperations behavior expected by this method
		Mockito.when(mockPlaylistQueryResults.items().stream().toList().get(0)).thenReturn(mockPlaylist);
		Mockito.when(mockPlaylistQueryResults.items().stream().toList().size()).thenReturn(1);
		Mockito.when(mockDynamoDBOperations.query(playlistClassCaptor.capture(), playlistQueryCaptor.capture(), null))
				.thenReturn(mockPlaylistQueryResults);

		// Execute the query
		Object[] parameters = new Object[]{"someUserName"};
		Object o = partTreeDynamoDBQuery.execute(parameters);

		// Assert that we obtain the expected results
		assertEquals(mockPlaylistQueryResults, o);
		assertEquals(1, mockPlaylistQueryResults.items().stream().toList().size());
		assertEquals(mockPlaylist, mockPlaylistQueryResults.items().stream().toList().get(0));

		// Assert that we scanned DynamoDB for the correct class
		assertEquals(playlistClassCaptor.getValue(), Playlist.class);

		// Assert that we have only one filter condition, for the name of the
		// property
		// TODO
//		Object hashKeyPrototypeObject = playlistQueryCaptor.getValue().getHashKeyValues();
//		assertTrue(hashKeyPrototypeObject instanceof Playlist);
//		Playlist hashKeyPropertyPlaylist = (Playlist) hashKeyPrototypeObject;
//		assertEquals("someUserName", hashKeyPropertyPlaylist.getUserName());
//
//		assertEquals(0, playlistQueryCaptor.getValue().getRangeKeyConditions().size());

		// Verify that the expected DynamoDBOperations method was called
		Mockito.verify(mockDynamoDBOperations).query(playlistClassCaptor.getValue(), playlistQueryCaptor.getValue(), null);

	}

	@Test
	public void testExecute_WhenFinderMethodIsFindingEntityWithCompositeIdList_WhenFindingByCompositeId_HashKeyAndIndexRangeKey() {
		PlaylistId playlistId = new PlaylistId();
		playlistId.setUserName("someUserName");

		setupCommonMocksForThisRepositoryMethod(mockPlaylistEntityMetadata, mockDynamoDBPlaylistQueryMethod,
				Playlist.class, "findByPlaylistIdUserNameAndDisplayName", 2, "userName", "playlistName");
		Mockito.when(mockDynamoDBPlaylistQueryMethod.isCollectionQuery()).thenReturn(true);
		Playlist prototypeHashKey = new Playlist();
		prototypeHashKey.setUserName("someUserName");
		Mockito.when(mockPlaylistEntityMetadata.getHashKeyPropotypeEntityForHashKey("someUserName"))
				.thenReturn(prototypeHashKey);
		// Mockito.when(mockPlaylistEntityMetadata.getHashKey(playlistId)).thenReturn("someUserName");
		// Mockito.when(mockPlaylistEntityMetadata.getRangeKey(playlistId)).thenReturn(null);
		Set<String> indexRangeKeyPropertyNames = new HashSet<String>();
		indexRangeKeyPropertyNames.add("displayName");
		Mockito.when(mockPlaylistEntityMetadata.getIndexRangeKeyPropertyNames()).thenReturn(indexRangeKeyPropertyNames);

		// Mock out specific DynamoDBOperations behavior expected by this method
		Mockito.when(mockPlaylistQueryResults.items().stream().toList().get(0)).thenReturn(mockPlaylist);
		Mockito.when(mockPlaylistQueryResults.items().stream().toList().size()).thenReturn(1);
		Mockito.when(mockDynamoDBOperations.query(playlistClassCaptor.capture(), playlistQueryCaptor.capture(), null))
				.thenReturn(mockPlaylistQueryResults);

		// Execute the query
		Object[] parameters = new Object[]{"someUserName", "someDisplayName"};
		Object o = partTreeDynamoDBQuery.execute(parameters);

		// Assert that we obtain the expected results
		assertEquals(mockPlaylistQueryResults, o);
		assertEquals(1, mockPlaylistQueryResults.items().stream().toList().size());
		assertEquals(mockPlaylist, mockPlaylistQueryResults.items().stream().toList().get(0));

		// Assert that we scanned DynamoDB for the correct class
		assertEquals(playlistClassCaptor.getValue(), Playlist.class);

		// Assert that we have two filter condition, for the name of the
		// property
		// TODO
//		Object hashKeyPrototypeObject = playlistQueryCaptor.getValue().getHashKeyValues();
//		assertTrue(hashKeyPrototypeObject instanceof Playlist);
//		Playlist hashKeyPropertyPlaylist = (Playlist) hashKeyPrototypeObject;
//		assertEquals("someUserName", hashKeyPropertyPlaylist.getUserName());
//
//		assertEquals(1, playlistQueryCaptor.getValue().getRangeKeyConditions().size());
//
//		Condition condition = (Condition) playlistQueryCaptor.getValue().getRangeKeyConditions().get("displayName");
//		assertEquals(ComparisonOperator.EQ, condition.comparisonOperator());
//		assertEquals(1, condition.attributeValueList().size());
//		assertEquals("someDisplayName", condition.attributeValueList().get(0).s());

		// Verify that the expected DynamoDBOperations method was called
		Mockito.verify(mockDynamoDBOperations).query(playlistClassCaptor.getValue(), playlistQueryCaptor.getValue(), null);

	}

	@Test
	public void testExecute_WhenFinderMethodIsFindingSingleEntityWithCompositeId_WhenFindingByCompositeId_HashKeyAndCompositeId_RangeKey() {
		PlaylistId playlistId = new PlaylistId();
		playlistId.setUserName("someUserName");
		playlistId.setPlaylistName("somePlaylistName");

		setupCommonMocksForThisRepositoryMethod(mockPlaylistEntityMetadata, mockDynamoDBPlaylistQueryMethod,
				Playlist.class, "findByPlaylistIdUserNameAndPlaylistIdPlaylistName", 2, "userName", "playlistName");
		// Mockito.when(mockPlaylistEntityMetadata.getHashKey(playlistId)).thenReturn("someUserName");
		// Mockito.when(mockPlaylistEntityMetadata.getRangeKey(playlistId)).thenReturn("somePlaylistName");

		// Mock out specific DynamoDBOperations behavior expected by this method
		Mockito.when(mockDynamoDBOperations.load(Playlist.class, "someUserName", "somePlaylistName", null))
				.thenReturn(mockPlaylist);

		// Execute the query

		Object[] parameters = new Object[]{"someUserName", "somePlaylistName"};
		Object o = partTreeDynamoDBQuery.execute(parameters);

		// Assert that we obtain the expected single result
		assertEquals(o, mockPlaylist);

		// Verify that the expected DynamoDBOperations method was called
		Mockito.verify(mockDynamoDBOperations).load(Playlist.class, "someUserName", "somePlaylistName", null);
	}

	@Test
	public void testExecute_WhenFinderMethodIsFindingEntityWithCompositeIdList_WhenFindingByCompositeIdWithHashKeyOnly_WhenSortingByRangeKey() {
		PlaylistId playlistId = new PlaylistId();
		playlistId.setUserName("someUserName");

		setupCommonMocksForThisRepositoryMethod(mockPlaylistEntityMetadata, mockDynamoDBPlaylistQueryMethod,
				Playlist.class, "findByPlaylistIdOrderByPlaylistNameDesc", 1, "userName", "playlistName");
		Mockito.when(mockDynamoDBPlaylistQueryMethod.isCollectionQuery()).thenReturn(true);
		Playlist prototypeHashKey = new Playlist();
		prototypeHashKey.setUserName("someUserName");
		Mockito.when(mockPlaylistEntityMetadata.getHashKeyPropotypeEntityForHashKey("someUserName"))
				.thenReturn(prototypeHashKey);
		Mockito.when(mockPlaylistEntityMetadata.getHashKey(playlistId)).thenReturn("someUserName");
		Mockito.when(mockPlaylistEntityMetadata.getRangeKey(playlistId)).thenReturn(null);

		Mockito.when(mockPlaylistQueryResults.items().stream().toList().get(0)).thenReturn(mockPlaylist);
		Mockito.when(mockPlaylistQueryResults.items().stream().toList().size()).thenReturn(1);
		Mockito.when(mockDynamoDBOperations.query(playlistClassCaptor.capture(), playlistQueryCaptor.capture(), null))
				.thenReturn(mockPlaylistQueryResults);

		// Execute the query
		Object[] parameters = new Object[]{playlistId};
		Object o = partTreeDynamoDBQuery.execute(parameters);

		// Assert that we obtain the expected results
		assertEquals(mockPlaylistQueryResults, o);
		assertEquals(1, mockPlaylistQueryResults.items().stream().toList().size());
		assertEquals(mockPlaylist, mockPlaylistQueryResults.items().stream().toList().get(0));

		// Assert that we scanned DynamoDB for the correct class
		assertEquals(playlistClassCaptor.getValue(), Playlist.class);

		// Assert that we have only one filter condition, for the name of the
		// property
		// TODO
//		Object hashKeyPrototypeObject = playlistQueryCaptor.getValue().getHashKeyValues();
//		assertTrue(hashKeyPrototypeObject instanceof Playlist);
//		Playlist hashKeyPropertyPlaylist = (Playlist) hashKeyPrototypeObject;
//		assertEquals("someUserName", hashKeyPropertyPlaylist.getUserName());
//
//		assertEquals(0, playlistQueryCaptor.getValue().getRangeKeyConditions().size());

		// Verify that the expected DynamoDBOperations method was called
		Mockito.verify(mockDynamoDBOperations).query(playlistClassCaptor.getValue(), playlistQueryCaptor.getValue(), null);

	}

	// Can't sort by indexrangekey when querying by hash key only
	@Test
	public void testExecute_WhenFinderMethodIsFindingEntityWithCompositeIdList_WhenFindingByCompositeIdWithHashKeyOnly_WhenSortingByIndexRangeKey() {
		PlaylistId playlistId = new PlaylistId();
		playlistId.setUserName("someUserName");

		setupCommonMocksForThisRepositoryMethod(mockPlaylistEntityMetadata, mockDynamoDBPlaylistQueryMethod,
				Playlist.class, "findByPlaylistIdOrderByDisplayNameDesc", 1, "userName", "playlistName");
		Mockito.when(mockDynamoDBPlaylistQueryMethod.isCollectionQuery()).thenReturn(true);
		Playlist prototypeHashKey = new Playlist();
		prototypeHashKey.setUserName("someUserName");
		Mockito.when(mockPlaylistEntityMetadata.getHashKeyPropotypeEntityForHashKey("someUserName"))
				.thenReturn(prototypeHashKey);
		Mockito.when(mockPlaylistEntityMetadata.getHashKey(playlistId)).thenReturn("someUserName");
		Mockito.when(mockPlaylistEntityMetadata.getRangeKey(playlistId)).thenReturn(null);
		Set<String> indexRangeKeyPropertyNames = new HashSet<String>();
		indexRangeKeyPropertyNames.add("displayName");
		Mockito.when(mockPlaylistEntityMetadata.getIndexRangeKeyPropertyNames()).thenReturn(indexRangeKeyPropertyNames);

		// Mock out specific DynamoDBOperations behavior expected by this method
		// ArgumentCaptor<DynamoDBQueryExpression> queryCaptor =
		// ArgumentCaptor.forClass(DynamoDBQueryExpression.class);
		// ArgumentCaptor<Class> classCaptor = ArgumentCaptor.forClass(Class.class);
		// Mockito.when(mockPlaylistQueryResults.get(0)).thenReturn(mockPlaylist);
		// Mockito.when(mockPlaylistQueryResults.size()).thenReturn(1);
		// Mockito.when(mockDynamoDBOperations.query(classCaptor.capture(),
		// queryCaptor.capture())).thenReturn(
		// mockPlaylistQueryResults);

		// Execute the query
		Object[] parameters = new Object[]{playlistId};
		assertThrows(UnsupportedOperationException.class, () -> {
			partTreeDynamoDBQuery.execute(parameters);
		});
	}

	@Test
	public void testExecute_WhenFinderMethodIsFindingEntityWithCompositeIdList_WhenFindingByHashKeyAndIndexRangeKey() {

		setupCommonMocksForThisRepositoryMethod(mockPlaylistEntityMetadata, mockDynamoDBPlaylistQueryMethod,
				Playlist.class, "findByUserNameAndDisplayName", 2, "userName", "playlistName");
		Set<String> indexRangeKeyPropertyNames = new HashSet<String>();
		indexRangeKeyPropertyNames.add("displayName");
		Mockito.when(mockPlaylistEntityMetadata.getIndexRangeKeyPropertyNames()).thenReturn(indexRangeKeyPropertyNames);
		Mockito.when(mockDynamoDBPlaylistQueryMethod.isCollectionQuery()).thenReturn(true);
		Playlist prototypeHashKey = new Playlist();
		prototypeHashKey.setUserName("someUserName");
		Mockito.when(mockPlaylistEntityMetadata.getHashKeyPropotypeEntityForHashKey("someUserName"))
				.thenReturn(prototypeHashKey);

		Mockito.when(mockPlaylistQueryResults.items().stream().toList().get(0)).thenReturn(mockPlaylist);
		Mockito.when(mockPlaylistQueryResults.items().stream().toList().size()).thenReturn(1);
		Mockito.when(mockDynamoDBOperations.query(playlistClassCaptor.capture(), playlistQueryCaptor.capture(), null))
				.thenReturn(mockPlaylistQueryResults);

		// Execute the query
		Object[] parameters = new Object[]{"someUserName", "someDisplayName"};
		Object o = partTreeDynamoDBQuery.execute(parameters);

		// Assert that we obtain the expected results
		assertEquals(mockPlaylistQueryResults, o);
		assertEquals(1, mockPlaylistQueryResults.items().stream().toList().size());
		assertEquals(mockPlaylist, mockPlaylistQueryResults.items().stream().toList().get(0));

		// Assert that we scanned DynamoDB for the correct class
		assertEquals(playlistClassCaptor.getValue(), Playlist.class);

		// Assert that we have only one filter condition for the hash key,and for the
		// index range key
		// TODO
//		Playlist hashKeyPropertyPlaylist = playlistQueryCaptor.getValue().getHashKeyValues();
//		assertEquals("someUserName", hashKeyPropertyPlaylist.getUserName());
//
//		assertEquals(1, playlistQueryCaptor.getValue().getRangeKeyConditions().size());
//		Condition condition = (Condition) playlistQueryCaptor.getValue().getRangeKeyConditions().get("displayName");
//		assertEquals(ComparisonOperator.EQ, condition.comparisonOperator());
//		assertEquals(1, condition.attributeValueList().size());
//		assertEquals("someDisplayName", condition.attributeValueList().get(0).s());

		// Verify that the expected DynamoDBOperations method was called
		Mockito.verify(mockDynamoDBOperations).query(playlistClassCaptor.getValue(), playlistQueryCaptor.getValue(), null);

	}

	@Test
	public void testExecute_WhenFinderMethodIsFindingEntityWithCompositeIdList_WhenFindingByHashKeyAndIndexRangeKey_WithValidOrderSpecified() {

		setupCommonMocksForThisRepositoryMethod(mockPlaylistEntityMetadata, mockDynamoDBPlaylistQueryMethod,
				Playlist.class, "findByUserNameAndDisplayNameOrderByDisplayNameDesc", 2, "userName", "playlistName");
		Set<String> indexRangeKeyPropertyNames = new HashSet<String>();
		indexRangeKeyPropertyNames.add("displayName");
		Mockito.when(mockPlaylistEntityMetadata.getIndexRangeKeyPropertyNames()).thenReturn(indexRangeKeyPropertyNames);
		Mockito.when(mockDynamoDBPlaylistQueryMethod.isCollectionQuery()).thenReturn(true);
		Playlist prototypeHashKey = new Playlist();
		prototypeHashKey.setUserName("someUserName");
		Mockito.when(mockPlaylistEntityMetadata.getHashKeyPropotypeEntityForHashKey("someUserName"))
				.thenReturn(prototypeHashKey);

		Mockito.when(mockPlaylistQueryResults.items().stream().toList().get(0)).thenReturn(mockPlaylist);
		Mockito.when(mockPlaylistQueryResults.items().stream().toList().size()).thenReturn(1);
		Mockito.when(mockDynamoDBOperations.query(playlistClassCaptor.capture(), playlistQueryCaptor.capture(), null))
				.thenReturn(mockPlaylistQueryResults);

		// Execute the query
		Object[] parameters = new Object[]{"someUserName", "someDisplayName"};
		Object o = partTreeDynamoDBQuery.execute(parameters);

		// Assert that we obtain the expected results
		assertEquals(mockPlaylistQueryResults, o);
		assertEquals(1, mockPlaylistQueryResults.items().stream().toList().size());
		assertEquals(mockPlaylist, mockPlaylistQueryResults.items().stream().toList().get(0));

		// Assert that we scanned DynamoDB for the correct class
		assertEquals(playlistClassCaptor.getValue(), Playlist.class);

		// Assert that we have only one filter condition for the hash key,and for the
		// index range key
		// TODO
//		Object hashKeyPrototypeObject = playlistQueryCaptor.getValue().getHashKeyValues();
//		assertTrue(hashKeyPrototypeObject instanceof Playlist);
//		Playlist hashKeyPropertyPlaylist = (Playlist) hashKeyPrototypeObject;
//		assertEquals("someUserName", hashKeyPropertyPlaylist.getUserName());
//
//		assertEquals(1, playlistQueryCaptor.getValue().getRangeKeyConditions().size());
//		Condition condition = (Condition) playlistQueryCaptor.getValue().getRangeKeyConditions().get("displayName");
//		assertEquals(ComparisonOperator.EQ, condition.comparisonOperator());
//		assertEquals(1, condition.attributeValueList().size());
//		assertEquals("someDisplayName", condition.attributeValueList().get(0).s());

		assertFalse(playlistQueryCaptor.getValue().scanIndexForward());

		// Verify that the expected DynamoDBOperations method was called
		Mockito.verify(mockDynamoDBOperations).query(playlistClassCaptor.getValue(), playlistQueryCaptor.getValue(), null);

	}

	@Test
	public void testExecute_WhenFinderMethodIsFindingEntityWithCompositeIdList_WhenFindingByHashKeyAndIndexRangeKey_WithInvalidOrderSpecified() {

		setupCommonMocksForThisRepositoryMethod(mockPlaylistEntityMetadata, mockDynamoDBPlaylistQueryMethod,
				Playlist.class, "findByUserNameAndDisplayNameOrderByPlaylistNameDesc", 2, "userName", "playlistName");
		Set<String> indexRangeKeyPropertyNames = new HashSet<String>();
		indexRangeKeyPropertyNames.add("displayName");
		Mockito.when(mockPlaylistEntityMetadata.getIndexRangeKeyPropertyNames()).thenReturn(indexRangeKeyPropertyNames);
		Mockito.when(mockDynamoDBPlaylistQueryMethod.isCollectionQuery()).thenReturn(true);
		Playlist prototypeHashKey = new Playlist();
		prototypeHashKey.setUserName("someUserName");
		Mockito.when(mockPlaylistEntityMetadata.getHashKeyPropotypeEntityForHashKey("someUserName"))
				.thenReturn(prototypeHashKey);

		// Mock out specific DynamoDBOperations behavior expected by this method
		// ArgumentCaptor<DynamoDBQueryExpression<Playlist>> queryCaptor =
		// ArgumentCaptor.forClass(DynamoDBQueryExpression.class);
		// ArgumentCaptor<Class<Playlist>> classCaptor =
		// ArgumentCaptor.forClass(Class.class);
		// Mockito.when(mockPlaylistQueryResults.get(0)).thenReturn(mockPlaylist);
		// Mockito.when(mockPlaylistQueryResults.size()).thenReturn(1);
		// Mockito.when(mockDynamoDBOperations.query(classCaptor.capture(),
		// queryCaptor.capture())).thenReturn(
		// mockPlaylistQueryResults);

		// Execute the query
		Object[] parameters = new Object[]{"someUserName", "someDisplayName"};
		assertThrows(UnsupportedOperationException.class, () -> {
			partTreeDynamoDBQuery.execute(parameters);
		});
	}

	@Test
	public void testExecute_WhenFinderMethodIsFindingEntityWithCompositeIdList_WhenFindingByHashKeyAndIndexRangeKey_OrderByIndexRangeKey() {

		setupCommonMocksForThisRepositoryMethod(mockPlaylistEntityMetadata, mockDynamoDBPlaylistQueryMethod,
				Playlist.class, "findByUserNameAndDisplayNameOrderByDisplayNameDesc", 2, "userName", "playlistName");
		Set<String> indexRangeKeyPropertyNames = new HashSet<String>();
		indexRangeKeyPropertyNames.add("displayName");
		Mockito.when(mockPlaylistEntityMetadata.getIndexRangeKeyPropertyNames()).thenReturn(indexRangeKeyPropertyNames);
		Mockito.when(mockDynamoDBPlaylistQueryMethod.isCollectionQuery()).thenReturn(true);
		Playlist prototypeHashKey = new Playlist();
		prototypeHashKey.setUserName("someUserName");
		Mockito.when(mockPlaylistEntityMetadata.getHashKeyPropotypeEntityForHashKey("someUserName"))
				.thenReturn(prototypeHashKey);

		Mockito.when(mockPlaylistQueryResults.items().stream().toList().get(0)).thenReturn(mockPlaylist);
		Mockito.when(mockPlaylistQueryResults.items().stream().toList().size()).thenReturn(1);
		Mockito.when(mockDynamoDBOperations.query(playlistClassCaptor.capture(), playlistQueryCaptor.capture(), null))
				.thenReturn(mockPlaylistQueryResults);

		// Execute the query
		Object[] parameters = new Object[]{"someUserName", "someDisplayName"};
		Object o = partTreeDynamoDBQuery.execute(parameters);

		// Assert that we obtain the expected results
		assertEquals(mockPlaylistQueryResults, o);
		assertEquals(1, mockPlaylistQueryResults.items().stream().toList().size());
		assertEquals(mockPlaylist, mockPlaylistQueryResults.items().stream().toList().get(0));

		// Assert that we scanned DynamoDB for the correct class
		assertEquals(playlistClassCaptor.getValue(), Playlist.class);

		// Assert that we have only one filter condition for the hash key,and for the
		// index range key
		// TODO
//		Playlist hashKeyPropertyPlaylist = playlistQueryCaptor.getValue().getHashKeyValues();
//		assertEquals("someUserName", hashKeyPropertyPlaylist.getUserName());
//
//		assertEquals(1, playlistQueryCaptor.getValue().getRangeKeyConditions().size());
//		Condition condition = (Condition) playlistQueryCaptor.getValue().getRangeKeyConditions().get("displayName");
//		assertEquals(ComparisonOperator.EQ, condition.comparisonOperator());
//		assertEquals(1, condition.attributeValueList().size());
//		assertEquals("someDisplayName", condition.attributeValueList().get(0).s());

		// Verify that the expected DynamoDBOperations method was called
		Mockito.verify(mockDynamoDBOperations).query(playlistClassCaptor.getValue(), playlistQueryCaptor.getValue(), null);

	}

	// Sorting by range key when querying by indexrangekey not supported
	@Test
	public void testExecute_WhenFinderMethodIsFindingEntityWithCompositeIdList_WhenFindingByHashKeyAndIndexRangeKey_OrderByRangeKey() {

		setupCommonMocksForThisRepositoryMethod(mockPlaylistEntityMetadata, mockDynamoDBPlaylistQueryMethod,
				Playlist.class, "findByUserNameAndDisplayNameOrderByPlaylistNameDesc", 2, "userName", "playlistName");
		Set<String> indexRangeKeyPropertyNames = new HashSet<String>();
		indexRangeKeyPropertyNames.add("displayName");
		Mockito.when(mockPlaylistEntityMetadata.getIndexRangeKeyPropertyNames()).thenReturn(indexRangeKeyPropertyNames);
		Mockito.when(mockDynamoDBPlaylistQueryMethod.isCollectionQuery()).thenReturn(true);
		Playlist prototypeHashKey = new Playlist();
		prototypeHashKey.setUserName("someUserName");
		Mockito.when(mockPlaylistEntityMetadata.getHashKeyPropotypeEntityForHashKey("someUserName"))
				.thenReturn(prototypeHashKey);

		// Mock out specific DynamoDBOperations behavior expected by this method
		// ArgumentCaptor<DynamoDBQueryExpression<Playlist>> queryCaptor =
		// ArgumentCaptor.forClass(DynamoDBQueryExpression.class);
		// ArgumentCaptor<Class<Playlist>> classCaptor =
		// ArgumentCaptor.forClass(Class.class);
		// Mockito.when(mockPlaylistQueryResults.get(0)).thenReturn(mockPlaylist);
		// Mockito.when(mockPlaylistQueryResults.size()).thenReturn(1);
		// Mockito.when(mockDynamoDBOperations.query(classCaptor.capture(),
		// queryCaptor.capture())).thenReturn(
		// mockPlaylistQueryResults);

		// Execute the query
		Object[] parameters = new Object[]{"someUserName", "someDisplayName"};
		assertThrows(UnsupportedOperationException.class, () -> {
			partTreeDynamoDBQuery.execute(parameters);
		});
	}

	@Test
	public void testExecute_WhenFinderMethodIsFindingEntityWithCompositeIdList_WhenFindingByHashKeyAndIndexRangeKeyWithOveriddenName() {

		setupCommonMocksForThisRepositoryMethod(mockPlaylistEntityMetadata, mockDynamoDBPlaylistQueryMethod,
				Playlist.class, "findByUserNameAndDisplayName", 2, "userName", "playlistName");
		Set<String> indexRangeKeyPropertyNames = new HashSet<String>();
		indexRangeKeyPropertyNames.add("displayName");
		Mockito.when(mockPlaylistEntityMetadata.getIndexRangeKeyPropertyNames()).thenReturn(indexRangeKeyPropertyNames);
		Mockito.when(mockPlaylistEntityMetadata.getOverriddenAttributeName("displayName"))
				.thenReturn(Optional.of("DisplayName"));

		Mockito.when(mockDynamoDBPlaylistQueryMethod.isCollectionQuery()).thenReturn(true);
		Playlist prototypeHashKey = new Playlist();
		prototypeHashKey.setUserName("someUserName");
		Mockito.when(mockPlaylistEntityMetadata.getHashKeyPropotypeEntityForHashKey("someUserName"))
				.thenReturn(prototypeHashKey);

		Mockito.when(mockPlaylistQueryResults.items().stream().toList().get(0)).thenReturn(mockPlaylist);
		Mockito.when(mockPlaylistQueryResults.items().stream().toList().size()).thenReturn(1);
		Mockito.when(mockDynamoDBOperations.query(playlistClassCaptor.capture(), playlistQueryCaptor.capture(), null))
				.thenReturn(mockPlaylistQueryResults);

		// Execute the query
		Object[] parameters = new Object[]{"someUserName", "someDisplayName"};
		Object o = partTreeDynamoDBQuery.execute(parameters);

		// Assert that we obtain the expected results
		assertEquals(mockPlaylistQueryResults, o);
		assertEquals(1, mockPlaylistQueryResults.items().stream().toList().size());
		assertEquals(mockPlaylist, mockPlaylistQueryResults.items().stream().toList().get(0));

		// Assert that we scanned DynamoDB for the correct class
		assertEquals(playlistClassCaptor.getValue(), Playlist.class);

		// Assert that we have only one filter condition for the hash key,and for the
		// index range key
		// TODO
//		Playlist hashKeyPropertyPlaylist = playlistQueryCaptor.getValue().getHashKeyValues();
//		assertEquals("someUserName", hashKeyPropertyPlaylist.getUserName());
//
//		assertEquals(1, playlistQueryCaptor.getValue().getRangeKeyConditions().size());
//		Condition condition = (Condition) playlistQueryCaptor.getValue().getRangeKeyConditions().get("DisplayName");
//		assertEquals(ComparisonOperator.EQ, condition.comparisonOperator());
//		assertEquals(1, condition.attributeValueList().size());
//		assertEquals("someDisplayName", condition.attributeValueList().get(0).s());

		// Verify that the expected DynamoDBOperations method was called
		Mockito.verify(mockDynamoDBOperations).query(playlistClassCaptor.getValue(), playlistQueryCaptor.getValue(), null);

	}

	@Test
	public void testExecute_WhenFinderMethodIsFindingEntityWithCompositeIdList_WhenFindingByCompositeIdWithHashKeyOnlyAndByAnotherPropertyWithOverriddenAttributeName() {
		PlaylistId playlistId = new PlaylistId();
		playlistId.setUserName("someUserName");

		setupCommonMocksForThisRepositoryMethod(mockPlaylistEntityMetadata, mockDynamoDBPlaylistQueryMethod,
				Playlist.class, "findByPlaylistIdAndDisplayName", 2, "userName", "playlistName");
		Mockito.when(mockDynamoDBPlaylistQueryMethod.isCollectionQuery()).thenReturn(true);
		Playlist prototypeHashKey = new Playlist();
		prototypeHashKey.setUserName("someUserName");
		// Mockito.when(mockPlaylistEntityMetadata.getHashKeyPropotypeEntityForHashKey("someUserName")).thenReturn(
		// prototypeHashKey);
		Mockito.when(mockPlaylistEntityMetadata.getHashKey(playlistId)).thenReturn("someUserName");
		Mockito.when(mockPlaylistEntityMetadata.getRangeKey(playlistId)).thenReturn(null);
		Mockito.when(mockPlaylistEntityMetadata.getOverriddenAttributeName("displayName"))
				.thenReturn(Optional.of("DisplayName"));

		// Mockito.when(mockUserScanResults.get(0)).thenReturn(mockUser);
		// Mockito.when(mockUserScanResults.size()).thenReturn(1);
		Mockito.when(mockDynamoDBOperations.scan(userClassCaptor.capture(), scanEnhancedCaptor.capture(), null))
				.thenReturn(mockUserScanResults);

		// Execute the query
		Object[] parameters = new Object[]{playlistId, "someDisplayName"};
		partTreeDynamoDBQuery.execute(parameters);

		// Assert that we scanned DynamoDB for the correct class
		assertEquals(userClassCaptor.getValue(), Playlist.class);

		// Assert that we have only three filter conditions
		// TODO
//		Map<String, Condition> filterConditions = scanCaptor.getValue().getScanFilter();
//		assertEquals(2, filterConditions.size());
//		Condition filterCondition1 = filterConditions.get("userName");
//		Condition filterCondition2 = filterConditions.get("DisplayName");
//
//		assertEquals(ComparisonOperator.EQ, filterCondition1.comparisonOperator());
//		assertEquals(ComparisonOperator.EQ, filterCondition2.comparisonOperator());
//
//		// Assert we only have one attribute value for this filter condition
//		assertEquals(1, filterCondition1.attributeValueList().size());
//		assertEquals(1, filterCondition2.attributeValueList().size());
//
//		// Assert that there the attribute value type for this attribute value
//		// is String,
//		// and its value is the parameter expected
//		assertEquals("someUserName", filterCondition1.attributeValueList().get(0).s());
//		assertEquals("someDisplayName", filterCondition2.attributeValueList().get(0).s());
//
//		// Assert that all other attribute value types other than String type
//		// are null
//		assertNull(filterCondition1.attributeValueList().get(0).ss());
//		assertNull(filterCondition1.attributeValueList().get(0).n());
//		assertNull(filterCondition1.attributeValueList().get(0).ns());
//		assertNull(filterCondition1.attributeValueList().get(0).b());
//		assertNull(filterCondition1.attributeValueList().get(0).bs());
//
//		assertNull(filterCondition2.attributeValueList().get(0).ss());
//		assertNull(filterCondition2.attributeValueList().get(0).n());
//		assertNull(filterCondition2.attributeValueList().get(0).ns());
//		assertNull(filterCondition2.attributeValueList().get(0).b());
//		assertNull(filterCondition2.attributeValueList().get(0).bs());

		// Verify that the expected DynamoDBOperations method was called
		Mockito.verify(mockDynamoDBOperations).scan(userClassCaptor.getValue(), scanEnhancedCaptor.getValue(), null); // Assert
		// that we obtain the expected results
	}

	@Test
	public void testExecute_WhenFinderMethodIsFindingEntityWithCompositeIdList_WhenFindingByCompositeIdWithRangeKeyOnlyAndByAnotherPropertyWithOverriddenAttributeName() {
		PlaylistId playlistId = new PlaylistId();
		playlistId.setPlaylistName("somePlaylistName");

		setupCommonMocksForThisRepositoryMethod(mockPlaylistEntityMetadata, mockDynamoDBPlaylistQueryMethod,
				Playlist.class, "findByPlaylistIdAndDisplayName", 2, "userName", "playlistName");
		Mockito.when(mockDynamoDBPlaylistQueryMethod.isCollectionQuery()).thenReturn(true);
		Playlist prototypeHashKey = new Playlist();
		prototypeHashKey.setUserName("someUserName");
		Mockito.when(mockPlaylistEntityMetadata.getHashKey(playlistId)).thenReturn(null);
		Mockito.when(mockPlaylistEntityMetadata.getRangeKey(playlistId)).thenReturn("somePlaylistName");
		Mockito.when(mockPlaylistEntityMetadata.getOverriddenAttributeName("displayName"))
				.thenReturn(Optional.of("DisplayName"));

		// Mockito.when(mockUserScanResults.get(0)).thenReturn(mockUser);
		// Mockito.when(mockUserScanResults.size()).thenReturn(1);
		Mockito.when(mockDynamoDBOperations.scan(userClassCaptor.capture(), scanEnhancedCaptor.capture(), null))
				.thenReturn(mockUserScanResults);

		// Execute the query
		Object[] parameters = new Object[]{playlistId, "someDisplayName"};
		partTreeDynamoDBQuery.execute(parameters);

		// Assert that we scanned DynamoDB for the correct class
		assertEquals(userClassCaptor.getValue(), Playlist.class);

		// Assert that we have only three filter conditions
		// TODO
//		Map<String, Condition> filterConditions = scanCaptor.getValue().getScanFilter();
//		assertEquals(2, filterConditions.size());
//		Condition filterCondition1 = filterConditions.get("playlistName");
//		Condition filterCondition2 = filterConditions.get("DisplayName");
//
//		assertEquals(ComparisonOperator.EQ, filterCondition1.comparisonOperator());
//		assertEquals(ComparisonOperator.EQ, filterCondition2.comparisonOperator());
//
//		// Assert we only have one attribute value for this filter condition
//		assertEquals(1, filterCondition1.attributeValueList().size());
//		assertEquals(1, filterCondition2.attributeValueList().size());
//
//		// Assert that there the attribute value type for this attribute value
//		// is String,
//		// and its value is the parameter expected
//		assertEquals("somePlaylistName", filterCondition1.attributeValueList().get(0).s());
//		assertEquals("someDisplayName", filterCondition2.attributeValueList().get(0).s());
//
//		// Assert that all other attribute value types other than String type
//		// are null
//		assertNull(filterCondition1.attributeValueList().get(0).ss());
//		assertNull(filterCondition1.attributeValueList().get(0).n());
//		assertNull(filterCondition1.attributeValueList().get(0).ns());
//		assertNull(filterCondition1.attributeValueList().get(0).b());
//		assertNull(filterCondition1.attributeValueList().get(0).bs());
//
//		assertNull(filterCondition2.attributeValueList().get(0).ss());
//		assertNull(filterCondition2.attributeValueList().get(0).n());
//		assertNull(filterCondition2.attributeValueList().get(0).ns());
//		assertNull(filterCondition2.attributeValueList().get(0).b());
//		assertNull(filterCondition2.attributeValueList().get(0).bs());

		// Verify that the expected DynamoDBOperations method was called
		Mockito.verify(mockDynamoDBOperations).scan(userClassCaptor.getValue(), scanEnhancedCaptor.getValue(), null); // Assert
		// that we obtain the expected results
	}

	@Test
	public void testExecute_WhenFinderMethodIsFindingEntityWithCompositeIdList_WhenFindingByNotHashKeyAndNotRangeKey() {

		setupCommonMocksForThisRepositoryMethod(mockPlaylistEntityMetadata, mockDynamoDBPlaylistQueryMethod,
				Playlist.class, "findByUserNameNotAndPlaylistNameNot", 2, "userName", "playlistName");
		Mockito.when(mockDynamoDBPlaylistQueryMethod.isCollectionQuery()).thenReturn(true);

		// Mockito.when(mockUserScanResults.get(0)).thenReturn(mockUser);
		// Mockito.when(mockUserScanResults.size()).thenReturn(1);
		Mockito.when(mockDynamoDBOperations.scan(userClassCaptor.capture(), scanEnhancedCaptor.capture(), null))
				.thenReturn(mockUserScanResults);

		// Execute the query
		Object[] parameters = new Object[]{"someUserName", "somePlaylistName"};
		partTreeDynamoDBQuery.execute(parameters);

		// Assert that we scanned DynamoDB for the correct class
		assertEquals(userClassCaptor.getValue(), Playlist.class);

		// Assert that we have only three filter conditions
		// TODO
//		Map<String, Condition> filterConditions = scanCaptor.getValue().getScanFilter();
//		assertEquals(2, filterConditions.size());
//		Condition filterCondition1 = filterConditions.get("userName");
//		Condition filterCondition2 = filterConditions.get("playlistName");
//
//		assertEquals(ComparisonOperator.NE, filterCondition1.comparisonOperator());
//		assertEquals(ComparisonOperator.NE, filterCondition2.comparisonOperator());
//
//		// Assert we only have one attribute value for this filter condition
//		assertEquals(1, filterCondition1.attributeValueList().size());
//		assertEquals(1, filterCondition2.attributeValueList().size());
//
//		// Assert that there the attribute value type for this attribute value
//		// is String,
//		// and its value is the parameter expected
//		assertEquals("someUserName", filterCondition1.attributeValueList().get(0).s());
//		assertEquals("somePlaylistName", filterCondition2.attributeValueList().get(0).s());
//
//		// Assert that all other attribute value types other than String type
//		// are null
//		assertNull(filterCondition1.attributeValueList().get(0).ss());
//		assertNull(filterCondition1.attributeValueList().get(0).n());
//		assertNull(filterCondition1.attributeValueList().get(0).ns());
//		assertNull(filterCondition1.attributeValueList().get(0).b());
//		assertNull(filterCondition1.attributeValueList().get(0).bs());
//
//		assertNull(filterCondition2.attributeValueList().get(0).ss());
//		assertNull(filterCondition2.attributeValueList().get(0).n());
//		assertNull(filterCondition2.attributeValueList().get(0).ns());
//		assertNull(filterCondition2.attributeValueList().get(0).b());
//		assertNull(filterCondition2.attributeValueList().get(0).bs());

		// Verify that the expected DynamoDBOperations method was called
		Mockito.verify(mockDynamoDBOperations).scan(userClassCaptor.getValue(), scanEnhancedCaptor.getValue(), null); // Assert
		// that we obtain the expected results
	}

	@Test
	public void testExecute_WhenFinderMethodIsFindingEntityWithCompositeIdList_WhenFindingByCompositeIdAndByAnotherPropertyWithOverriddenAttributeName() {
		PlaylistId playlistId = new PlaylistId();
		playlistId.setUserName("someUserName");
		playlistId.setPlaylistName("somePlaylistName");

		setupCommonMocksForThisRepositoryMethod(mockPlaylistEntityMetadata, mockDynamoDBPlaylistQueryMethod,
				Playlist.class, "findByPlaylistIdAndDisplayName", 2, "userName", "playlistName");
		Mockito.when(mockDynamoDBPlaylistQueryMethod.isCollectionQuery()).thenReturn(true);
		Playlist prototypeHashKey = new Playlist();
		prototypeHashKey.setUserName("someUserName");

		// Mockito.when(mockPlaylistEntityMetadata.getHashKeyPropotypeEntityForHashKey("someUserName")).thenReturn(
		// prototypeHashKey);
		Mockito.when(mockPlaylistEntityMetadata.getHashKey(playlistId)).thenReturn("someUserName");
		Mockito.when(mockPlaylistEntityMetadata.getRangeKey(playlistId)).thenReturn("somePlaylistName");
		Mockito.when(mockPlaylistEntityMetadata.getOverriddenAttributeName("displayName"))
				.thenReturn(Optional.of("DisplayName"));

		// Mockito.when(mockUserScanResults.get(0)).thenReturn(mockUser);
		// Mockito.when(mockUserScanResults.size()).thenReturn(1);
		Mockito.when(mockDynamoDBOperations.scan(userClassCaptor.capture(), scanEnhancedCaptor.capture(), null))
				.thenReturn(mockUserScanResults);

		// Execute the query
		Object[] parameters = new Object[]{playlistId, "someDisplayName"};
		partTreeDynamoDBQuery.execute(parameters);

		// Assert that we scanned DynamoDB for the correct class
		assertEquals(userClassCaptor.getValue(), Playlist.class);

		// Assert that we have only three filter conditions
		// TODO
//		Map<String, Condition> filterConditions = scanCaptor.getValue().getScanFilter();
//		assertEquals(3, filterConditions.size());
//		Condition filterCondition1 = filterConditions.get("userName");
//		Condition filterCondition2 = filterConditions.get("playlistName");
//		Condition filterCondition3 = filterConditions.get("DisplayName");
//
//		assertEquals(ComparisonOperator.EQ, filterCondition1.comparisonOperator());
//		assertEquals(ComparisonOperator.EQ, filterCondition2.comparisonOperator());
//		assertEquals(ComparisonOperator.EQ, filterCondition3.comparisonOperator());
//
//		// Assert we only have one attribute value for this filter condition
//		assertEquals(1, filterCondition1.attributeValueList().size());
//		assertEquals(1, filterCondition2.attributeValueList().size());
//		assertEquals(1, filterCondition3.attributeValueList().size());
//
//		// Assert that there the attribute value type for this attribute value
//		// is String,
//		// and its value is the parameter expected
//		assertEquals("someUserName", filterCondition1.attributeValueList().get(0).s());
//		assertEquals("somePlaylistName", filterCondition2.attributeValueList().get(0).s());
//		assertEquals("someDisplayName", filterCondition3.attributeValueList().get(0).s());
//
//		// Assert that all other attribute value types other than String type
//		// are null
//		assertNull(filterCondition1.attributeValueList().get(0).ss());
//		assertNull(filterCondition1.attributeValueList().get(0).n());
//		assertNull(filterCondition1.attributeValueList().get(0).ns());
//		assertNull(filterCondition1.attributeValueList().get(0).b());
//		assertNull(filterCondition1.attributeValueList().get(0).bs());
//
//		assertNull(filterCondition2.attributeValueList().get(0).ss());
//		assertNull(filterCondition2.attributeValueList().get(0).n());
//		assertNull(filterCondition2.attributeValueList().get(0).ns());
//		assertNull(filterCondition2.attributeValueList().get(0).b());
//		assertNull(filterCondition2.attributeValueList().get(0).bs());
//
//		assertNull(filterCondition3.attributeValueList().get(0).ss());
//		assertNull(filterCondition3.attributeValueList().get(0).n());
//		assertNull(filterCondition3.attributeValueList().get(0).ns());
//		assertNull(filterCondition3.attributeValueList().get(0).b());
//		assertNull(filterCondition3.attributeValueList().get(0).bs());

		// Verify that the expected DynamoDBOperations method was called
		Mockito.verify(mockDynamoDBOperations).scan(userClassCaptor.getValue(), scanEnhancedCaptor.getValue(), null); // Assert
		// that we obtain the expected results
	}

	@Test
	public void testExecute_WhenFinderMethodIsFindingSingleEntity_WithSingleStringParameter_WhenNotFindingByHashKey() {
		setupCommonMocksForThisRepositoryMethod(mockUserEntityMetadata, mockDynamoDBUserQueryMethod, User.class,
				"findByName", 1, "id", null);

		Mockito.when(mockUserScanResults.items().stream().toList().get(0)).thenReturn(mockUser);
		Mockito.when(mockUserScanResults.items().stream().toList().size()).thenReturn(1);
		Mockito.when(mockDynamoDBOperations.scan(userClassCaptor.capture(), scanEnhancedCaptor.capture(), null))
				.thenReturn(mockUserScanResults);

		// Execute the query
		Object[] parameters = new Object[]{"someName"};
		Object o = partTreeDynamoDBQuery.execute(parameters);

		// Assert that we obtain the expected single result
		assertEquals(o, mockUser);

		// Assert that we scanned DynamoDB for the correct class
		assertEquals(userClassCaptor.getValue(), User.class);

		// Assert that we have only one filter condition, for the name of the
		// property
		// TODO
//		Map<String, Condition> filterConditions = scanCaptor.getValue().getScanFilter();
//		assertEquals(1, filterConditions.size());
//		Condition filterCondition = filterConditions.get("name");
//		assertNotNull(filterCondition);
//
//		assertEquals(ComparisonOperator.EQ, filterCondition.comparisonOperator());
//
//		// Assert we only have one attribute value for this filter condition
//		assertEquals(1, filterCondition.attributeValueList().size());
//
//		// Assert that there the attribute value type for this attribute value
//		// is String,
//		// and its value is the parameter expected
//		assertEquals("someName", filterCondition.attributeValueList().get(0).s());
//
//		// Assert that all other attribute value types other than String type
//		// are null
//		assertNull(filterCondition.attributeValueList().get(0).ss());
//		assertNull(filterCondition.attributeValueList().get(0).n());
//		assertNull(filterCondition.attributeValueList().get(0).ns());
//		assertNull(filterCondition.attributeValueList().get(0).b());
//		assertNull(filterCondition.attributeValueList().get(0).bs());

		// Verify that the expected DynamoDBOperations method was called
		Mockito.verify(mockDynamoDBOperations).scan(userClassCaptor.getValue(), scanEnhancedCaptor.getValue(), null);
	}

	@Test
	public void testExecute_WhenFinderMethodIsFindingSingleEntity_WithSingleStringParameter_WhenNotFindingByHashKey_WhenDynamoAttributeNameOverridden() {
		setupCommonMocksForThisRepositoryMethod(mockUserEntityMetadata, mockDynamoDBUserQueryMethod, User.class,
				"findByName", 1, "id", null);
		Mockito.when(mockUserEntityMetadata.getOverriddenAttributeName("name")).thenReturn(Optional.of("Name"));

		Mockito.when(mockUserScanResults.items().stream().toList().get(0)).thenReturn(mockUser);
		Mockito.when(mockUserScanResults.items().stream().toList().size()).thenReturn(1);
		Mockito.when(mockDynamoDBOperations.scan(userClassCaptor.capture(), scanEnhancedCaptor.capture(), null))
				.thenReturn(mockUserScanResults);

		// Execute the query
		Object[] parameters = new Object[]{"someName"};
		Object o = partTreeDynamoDBQuery.execute(parameters);

		// Assert that we obtain the expected single result
		assertEquals(o, mockUser);

		// Assert that we scanned DynamoDB for the correct class
		assertEquals(userClassCaptor.getValue(), User.class);

		// Assert that we have only one filter condition, for the name of the
		// property
		// TODO
//		Map<String, Condition> filterConditions = scanCaptor.getValue().getScanFilter();
//		assertEquals(1, filterConditions.size());
//		Condition filterCondition = filterConditions.get("Name");
//		assertNotNull(filterCondition);
//
//		assertEquals(ComparisonOperator.EQ, filterCondition.comparisonOperator());
//
//		// Assert we only have one attribute value for this filter condition
//		assertEquals(1, filterCondition.attributeValueList().size());
//
//		// Assert that there the attribute value type for this attribute value
//		// is String,
//		// and its value is the parameter expected
//		assertEquals("someName", filterCondition.attributeValueList().get(0).s());
//
//		// Assert that all other attribute value types other than String type
//		// are null
//		assertNull(filterCondition.attributeValueList().get(0).ss());
//		assertNull(filterCondition.attributeValueList().get(0).n());
//		assertNull(filterCondition.attributeValueList().get(0).ns());
//		assertNull(filterCondition.attributeValueList().get(0).b());
//		assertNull(filterCondition.attributeValueList().get(0).bs());

		// Verify that the expected DynamoDBOperations method was called
		Mockito.verify(mockDynamoDBOperations).scan(userClassCaptor.getValue(), scanEnhancedCaptor.getValue(), null);
	}

	@Test
	public void testExecute_WhenFinderMethodIsFindingSingleEntity_WithMultipleStringParameters_WhenFindingByHashKeyAndANonHashOrRangeProperty() {
		setupCommonMocksForThisRepositoryMethod(mockUserEntityMetadata, mockDynamoDBUserQueryMethod, User.class,
				"findByIdAndName", 2, "id", null);

		Mockito.when(mockUserScanResults.items().stream().toList().get(0)).thenReturn(mockUser);
		Mockito.when(mockUserScanResults.items().stream().toList().size()).thenReturn(1);
		Mockito.when(mockDynamoDBOperations.scan(userClassCaptor.capture(), scanEnhancedCaptor.capture(), null))
				.thenReturn(mockUserScanResults);

		// Execute the query
		Object[] parameters = new Object[]{"someId", "someName"};
		Object o = partTreeDynamoDBQuery.execute(parameters);

		// Assert that we obtain the expected single result
		assertEquals(o, mockUser);

		// Assert that we scanned DynamoDB for the correct class
		assertEquals(userClassCaptor.getValue(), User.class);

		// Assert that we have two filter conditions, for the id and name
		// TODO
//		Map<String, Condition> filterConditions = scanCaptor.getValue().getScanFilter();
//		assertEquals(2, filterConditions.size());
//		Condition nameFilterCondition = filterConditions.get("name");
//		assertNotNull(nameFilterCondition);
//		Condition idFilterCondition = filterConditions.get("id");
//		assertNotNull(idFilterCondition);
//
//		assertEquals(ComparisonOperator.EQ, nameFilterCondition.comparisonOperator());
//		assertEquals(ComparisonOperator.EQ, idFilterCondition.comparisonOperator());
//
//		// Assert we only have one attribute value for each filter condition
//		assertEquals(1, nameFilterCondition.attributeValueList().size());
//		assertEquals(1, idFilterCondition.attributeValueList().size());
//
//		// Assert that there the attribute value type for this attribute value
//		// is String,
//		// and its value is the parameter expected
//		assertEquals("someName", nameFilterCondition.attributeValueList().get(0).s());
//		assertEquals("someId", idFilterCondition.attributeValueList().get(0).s());
//
//		// Assert that all other attribute value types other than String type
//		// are null
//		assertNull(nameFilterCondition.attributeValueList().get(0).ss());
//		assertNull(nameFilterCondition.attributeValueList().get(0).n());
//		assertNull(nameFilterCondition.attributeValueList().get(0).ns());
//		assertNull(nameFilterCondition.attributeValueList().get(0).b());
//		assertNull(nameFilterCondition.attributeValueList().get(0).bs());
//		assertNull(idFilterCondition.attributeValueList().get(0).ss());
//		assertNull(idFilterCondition.attributeValueList().get(0).n());
//		assertNull(idFilterCondition.attributeValueList().get(0).ns());
//		assertNull(idFilterCondition.attributeValueList().get(0).b());
//		assertNull(idFilterCondition.attributeValueList().get(0).bs());

		// Verify that the expected DynamoDBOperations method was called
		Mockito.verify(mockDynamoDBOperations).scan(userClassCaptor.getValue(), scanEnhancedCaptor.getValue(), null);
	}

	@Test
	public void testExecute_WhenFinderMethodIsFindingSingleEntity_WithMultipleStringParameters_WhenFindingByHashKeyAndACollectionProperty() {
		setupCommonMocksForThisRepositoryMethod(mockUserEntityMetadata, mockDynamoDBUserQueryMethod, User.class,
				"findByTestSet", 1, "id", null);

		Set<String> testSet = new HashSet<String>();
		testSet.add("testData");

		Mockito.when(mockUserScanResults.items().stream().toList().get(0)).thenReturn(mockUser);
		Mockito.when(mockUserScanResults.items().stream().toList().size()).thenReturn(1);
		Mockito.when(mockDynamoDBOperations.scan(userClassCaptor.capture(), scanEnhancedCaptor.capture(), null))
				.thenReturn(mockUserScanResults);

		// Execute the query
		Object[] parameters = new Object[]{testSet};
		Object o = partTreeDynamoDBQuery.execute(parameters);

		// Assert that we obtain the expected single result
		assertEquals(o, mockUser);

		// Assert that we scanned DynamoDB for the correct class
		assertEquals(userClassCaptor.getValue(), User.class);

		// Assert that we have one filter condition
		// TODO
//		Map<String, Condition> filterConditions = scanCaptor.getValue().getScanFilter();
//		assertEquals(1, filterConditions.size());
//		Condition testSetFilterCondition = filterConditions.get("testSet");
//		assertNotNull(testSetFilterCondition);
//
//		assertEquals(ComparisonOperator.EQ, testSetFilterCondition.comparisonOperator());
//
//		// Assert we only have one attribute value for each filter condition
//		assertEquals(1, testSetFilterCondition.attributeValueList().size());
//
//		// Assert that there the attribute value type for this attribute value
//		// is String,
//		// and its value is the parameter expected
//		assertNotNull(testSetFilterCondition.attributeValueList().get(0).ss());
//
//		assertTrue(ClassUtils.isAssignable(Iterable.class,
//				testSetFilterCondition.attributeValueList().get(0).ss().getClass()));
//
//		List<String> returnObjects = testSetFilterCondition.attributeValueList().get(0).ss();
//		assertEquals(1, returnObjects.size());
//		assertEquals("testData", returnObjects.get(0));
//
//		// Assert that all other attribute value types other than String type
//		// are null
//		assertNull(testSetFilterCondition.attributeValueList().get(0).s());
//		assertNull(testSetFilterCondition.attributeValueList().get(0).n());
//		assertNull(testSetFilterCondition.attributeValueList().get(0).ns());
//		assertNull(testSetFilterCondition.attributeValueList().get(0).b());
//		assertNull(testSetFilterCondition.attributeValueList().get(0).bs());

		// Verify that the expected DynamoDBOperations method was called
		Mockito.verify(mockDynamoDBOperations).scan(userClassCaptor.getValue(), scanEnhancedCaptor.getValue(), null);
	}

	@Test
	public void testExecute_WhenFinderMethodIsFindingSingleEntity_WithMultipleStringParameters_WhenFindingByHashKeyAndANonHashOrRangeProperty_WhenDynamoDBAttributeNamesOveridden() {
		setupCommonMocksForThisRepositoryMethod(mockUserEntityMetadata, mockDynamoDBUserQueryMethod, User.class,
				"findByIdAndName", 2, "id", null);

		Mockito.when(mockUserEntityMetadata.getOverriddenAttributeName("name")).thenReturn(Optional.of("Name"));
		Mockito.when(mockUserEntityMetadata.getOverriddenAttributeName("id")).thenReturn(Optional.of("Id"));

		Mockito.when(mockUserScanResults.items().stream().toList().get(0)).thenReturn(mockUser);
		Mockito.when(mockUserScanResults.items().stream().toList().size()).thenReturn(1);
		Mockito.when(mockDynamoDBOperations.scan(userClassCaptor.capture(), scanEnhancedCaptor.capture(), null))
				.thenReturn(mockUserScanResults);

		// Execute the query
		Object[] parameters = new Object[]{"someId", "someName"};
		Object o = partTreeDynamoDBQuery.execute(parameters);

		// Assert that we obtain the expected single result
		assertEquals(o, mockUser);

		// Assert that we scanned DynamoDB for the correct class
		assertEquals(userClassCaptor.getValue(), User.class);

		// Assert that we have two filter conditions, for the id and name
		// TODO
//		Map<String, Condition> filterConditions = scanCaptor.getValue().getScanFilter();
//		assertEquals(2, filterConditions.size());
//		Condition nameFilterCondition = filterConditions.get("Name");
//		assertNotNull(nameFilterCondition);
//		Condition idFilterCondition = filterConditions.get("Id");
//		assertNotNull(idFilterCondition);
//
//		assertEquals(ComparisonOperator.EQ, nameFilterCondition.comparisonOperator());
//		assertEquals(ComparisonOperator.EQ, idFilterCondition.comparisonOperator());
//
//		// Assert we only have one attribute value for each filter condition
//		assertEquals(1, nameFilterCondition.attributeValueList().size());
//		assertEquals(1, idFilterCondition.attributeValueList().size());
//
//		// Assert that there the attribute value type for this attribute value
//		// is String,
//		// and its value is the parameter expected
//		assertEquals("someName", nameFilterCondition.attributeValueList().get(0).s());
//		assertEquals("someId", idFilterCondition.attributeValueList().get(0).s());
//
//		// Assert that all other attribute value types other than String type
//		// are null
//		assertNull(nameFilterCondition.attributeValueList().get(0).ss());
//		assertNull(nameFilterCondition.attributeValueList().get(0).n());
//		assertNull(nameFilterCondition.attributeValueList().get(0).ns());
//		assertNull(nameFilterCondition.attributeValueList().get(0).b());
//		assertNull(nameFilterCondition.attributeValueList().get(0).bs());
//		assertNull(idFilterCondition.attributeValueList().get(0).ss());
//		assertNull(idFilterCondition.attributeValueList().get(0).n());
//		assertNull(idFilterCondition.attributeValueList().get(0).ns());
//		assertNull(idFilterCondition.attributeValueList().get(0).b());
//		assertNull(idFilterCondition.attributeValueList().get(0).bs());

		// Verify that the expected DynamoDBOperations method was called
		Mockito.verify(mockDynamoDBOperations).scan(userClassCaptor.getValue(), scanEnhancedCaptor.getValue(), null);
	}

	@Test
	public void testExecute_WhenFinderMethodIsFindingEntityList_WithSingleStringParameter_WhenNotFindingByHashKey() {
		setupCommonMocksForThisRepositoryMethod(mockUserEntityMetadata, mockDynamoDBUserQueryMethod, User.class,
				"findByName", 1, "id", null);
		Mockito.when(mockDynamoDBUserQueryMethod.isCollectionQuery()).thenReturn(true);

		Mockito.when(mockUserScanResults.items().stream().toList().get(0)).thenReturn(mockUser);
		Mockito.when(mockUserScanResults.items().stream().toList().size()).thenReturn(1);
		Mockito.when(mockDynamoDBOperations.scan(userClassCaptor.capture(), scanEnhancedCaptor.capture(), null))
				.thenReturn(mockUserScanResults);

		// Execute the query
		Object[] parameters = new Object[]{"someName"};
		Object o = partTreeDynamoDBQuery.execute(parameters);

		// Assert that we obtain the expected list of results
		assertEquals(o, mockUserScanResults);

		// Assert that the list of results contains the correct elements
		assertEquals(1, mockUserScanResults.items().stream().toList().size());
		assertEquals(mockUser, mockUserScanResults.items().stream().toList().get(0));

		// Assert that we scanned DynamoDB for the correct class
		assertEquals(userClassCaptor.getValue(), User.class);

		// Assert that we have only one filter condition, for the name of the
		// property
		// TODO
//		Map<String, Condition> filterConditions = scanCaptor.getValue().getScanFilter();
//		assertEquals(1, filterConditions.size());
//		Condition filterCondition = filterConditions.get("name");
//		assertNotNull(filterCondition);
//
//		assertEquals(ComparisonOperator.EQ, filterCondition.comparisonOperator());
//
//		// Assert we only have one attribute value for this filter condition
//		assertEquals(1, filterCondition.attributeValueList().size());
//
//		// Assert that there the attribute value type for this attribute value
//		// is String,
//		// and its value is the parameter expected
//		assertEquals("someName", filterCondition.attributeValueList().get(0).s());
//
//		// Assert that all other attribute value types other than String type
//		// are null
//		assertNull(filterCondition.attributeValueList().get(0).ss());
//		assertNull(filterCondition.attributeValueList().get(0).n());
//		assertNull(filterCondition.attributeValueList().get(0).ns());
//		assertNull(filterCondition.attributeValueList().get(0).b());
//		assertNull(filterCondition.attributeValueList().get(0).bs());

		// Verify that the expected DynamoDBOperations method was called
		Mockito.verify(mockDynamoDBOperations).scan(userClassCaptor.getValue(), scanEnhancedCaptor.getValue(), null);
	}

	@Test
	// Not yet supported
	public void testExecute_WhenFinderMethodIsFindingEntityList_WithSingleStringParameterIgnoringCase_WhenNotFindingByHashKey() {
		setupCommonMocksForThisRepositoryMethod(mockUserEntityMetadata, mockDynamoDBUserQueryMethod, User.class,
				"findByNameIgnoringCase", 1, "id", null);
		Mockito.when(mockDynamoDBUserQueryMethod.isCollectionQuery()).thenReturn(true);

		// Mock out specific DynamoDBOperations behavior expected by this method
		// ArgumentCaptor<DynamoDBScanExpression> scanCaptor =
		// ArgumentCaptor.forClass(DynamoDBScanExpression.class);
		// ArgumentCaptor<Class<User>> classCaptor =
		// ArgumentCaptor.forClass(Class.class);
		// Mockito.when(mockUserScanResults.get(0)).thenReturn(mockUser);
		// Mockito.when(mockUserScanResults.size()).thenReturn(1);
		// Mockito.when(mockDynamoDBOperations.scan(classCaptor.capture(),
		// scanCaptor.capture())).thenReturn(
		// mockUserScanResults);

		// Execute the query
		Object[] parameters = new Object[]{"someName"};
		assertThrows(UnsupportedOperationException.class, () -> {
			partTreeDynamoDBQuery.execute(parameters);
		});
	}

	@Test
	// Not yet supported
	public void testExecute_WhenFinderMethodIsFindingEntityList_WithSingleStringParameter_WithSort_WhenNotFindingByHashKey() {
		setupCommonMocksForThisRepositoryMethod(mockUserEntityMetadata, mockDynamoDBUserQueryMethod, User.class,
				"findByNameOrderByNameAsc", 1, "id", null);
		Mockito.when(mockDynamoDBUserQueryMethod.isCollectionQuery()).thenReturn(true);

		// Mock out specific DynamoDBOperations behavior expected by this method
		// ArgumentCaptor<DynamoDBScanExpression> scanCaptor =
		// ArgumentCaptor.forClass(DynamoDBScanExpression.class);
		// ArgumentCaptor<Class<User>> classCaptor =
		// ArgumentCaptor.forClass(Class.class);
		// Mockito.when(mockUserScanResults.get(0)).thenReturn(mockUser);
		// Mockito.when(mockUserScanResults.size()).thenReturn(1);
		// Mockito.when(mockDynamoDBOperations.scan(classCaptor.capture(),
		// scanCaptor.capture())).thenReturn(
		// mockUserScanResults);

		// Execute the query
		Object[] parameters = new Object[]{"someName"};
		assertThrows(UnsupportedOperationException.class, () -> {
			partTreeDynamoDBQuery.execute(parameters);
		});
	}

	@Test
	public void testExecute_WhenFinderMethodIsFindingEntityList_WithSingleStringArrayParameter_WithIn_WhenNotFindingByHashKey() {
		setupCommonMocksForThisRepositoryMethod(mockUserEntityMetadata, mockDynamoDBUserQueryMethod, User.class,
				"findByNameIn", 1, "id", null);
		Mockito.when(mockDynamoDBUserQueryMethod.isCollectionQuery()).thenReturn(true);

		String[] names = new String[]{"someName", "someOtherName"};

		Mockito.when(mockUserScanResults.items().stream().toList().get(0)).thenReturn(mockUser);
		Mockito.when(mockUserScanResults.items().stream().toList().size()).thenReturn(1);
		Mockito.when(mockDynamoDBOperations.scan(userClassCaptor.capture(), scanEnhancedCaptor.capture(), null))
				.thenReturn(mockUserScanResults);

		// Execute the query
		Object[] parameters = new Object[]{names};
		Object o = partTreeDynamoDBQuery.execute(parameters);

		// Assert that we obtain the expected list of results
		assertEquals(o, mockUserScanResults);

		// Assert that the list of results contains the correct elements
		assertEquals(1, mockUserScanResults.items().stream().toList().size());
		assertEquals(mockUser, mockUserScanResults.items().stream().toList().get(0));

		// Assert that we scanned DynamoDB for the correct class
		assertEquals(userClassCaptor.getValue(), User.class);

		// Assert that we have only one filter condition, for the name of the
		// property
		// TODO
//		Map<String, Condition> filterConditions = scanCaptor.getValue().getScanFilter();
//		assertEquals(1, filterConditions.size());
//		Condition filterCondition = filterConditions.get("name");
//		assertNotNull(filterCondition);
//
//		assertEquals(ComparisonOperator.IN, filterCondition.comparisonOperator());
//
//		// Assert we only have an attribute value for each element of the IN array
//		assertEquals(2, filterCondition.attributeValueList().size());
//
//		// Assert that there the attribute value type for this attribute value
//		// is String,
//		// and its value is the parameter expected
//		assertEquals(names[0], filterCondition.attributeValueList().get(0).s());
//		assertEquals(names[1], filterCondition.attributeValueList().get(1).s());
//
//		// Assert that all other attribute value types other than String type
//		// are null
//		assertNull(filterCondition.attributeValueList().get(0).ss());
//		assertNull(filterCondition.attributeValueList().get(0).n());
//		assertNull(filterCondition.attributeValueList().get(0).ns());
//		assertNull(filterCondition.attributeValueList().get(0).b());
//		assertNull(filterCondition.attributeValueList().get(0).bs());

		// Verify that the expected DynamoDBOperations method was called
		Mockito.verify(mockDynamoDBOperations).scan(userClassCaptor.getValue(), scanEnhancedCaptor.getValue(), null);
	}

	@Test
	public void testExecute_WhenFinderMethodIsFindingEntityList_WithSingleListParameter_WithIn_WhenNotFindingByHashKey() {
		setupCommonMocksForThisRepositoryMethod(mockUserEntityMetadata, mockDynamoDBUserQueryMethod, User.class,
				"findByNameIn", 1, "id", null);
		Mockito.when(mockDynamoDBUserQueryMethod.isCollectionQuery()).thenReturn(true);

		List<String> names = Arrays.asList(new String[]{"someName", "someOtherName"});

		Mockito.when(mockUserScanResults.items().stream().toList().get(0)).thenReturn(mockUser);
		Mockito.when(mockUserScanResults.items().stream().toList().size()).thenReturn(1);
		Mockito.when(mockDynamoDBOperations.scan(userClassCaptor.capture(), scanEnhancedCaptor.capture(), null))
				.thenReturn(mockUserScanResults);

		// Execute the query
		Object[] parameters = new Object[]{names};
		Object o = partTreeDynamoDBQuery.execute(parameters);

		// Assert that we obtain the expected list of results
		assertEquals(o, mockUserScanResults);

		// Assert that the list of results contains the correct elements
		assertEquals(1, mockUserScanResults.items().stream().toList().size());
		assertEquals(mockUser, mockUserScanResults.items().stream().toList().get(0));

		// Assert that we scanned DynamoDB for the correct class
		assertEquals(userClassCaptor.getValue(), User.class);

		// Assert that we have only one filter condition, for the name of the
		// property
		// TODO
//		Map<String, Condition> filterConditions = scanCaptor.getValue().getScanFilter();
//		assertEquals(1, filterConditions.size());
//		Condition filterCondition = filterConditions.get("name");
//		assertNotNull(filterCondition);
//
//		assertEquals(ComparisonOperator.IN, filterCondition.comparisonOperator());
//
//		// Assert we only have an attribute value for each element of the IN array
//		assertEquals(2, filterCondition.attributeValueList().size());
//
//		// Assert that there the attribute value type for this attribute value
//		// is String,
//		// and its value is the parameter expected
//		assertEquals(names.get(0), filterCondition.attributeValueList().get(0).s());
//		assertEquals(names.get(1), filterCondition.attributeValueList().get(1).s());
//
//		// Assert that all other attribute value types other than String type
//		// are null
//		assertNull(filterCondition.attributeValueList().get(0).ss());
//		assertNull(filterCondition.attributeValueList().get(0).n());
//		assertNull(filterCondition.attributeValueList().get(0).ns());
//		assertNull(filterCondition.attributeValueList().get(0).b());
//		assertNull(filterCondition.attributeValueList().get(0).bs());

		// Verify that the expected DynamoDBOperations method was called
		Mockito.verify(mockDynamoDBOperations).scan(userClassCaptor.getValue(), scanEnhancedCaptor.getValue(), null);
	}

	@Test
	public void testExecute_WhenFinderMethodIsFindingEntityList_WithSingleDateParameter_WhenNotFindingByHashKey()
			throws ParseException {
		String joinDateString = "2013-09-12T14:04:03.123Z";
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
		Date joinDate = dateFormat.parse(joinDateString);

		setupCommonMocksForThisRepositoryMethod(mockUserEntityMetadata, mockDynamoDBUserQueryMethod, User.class,
				"findByJoinDate", 1, "id", null);
		Mockito.when(mockDynamoDBUserQueryMethod.isCollectionQuery()).thenReturn(true);

		Mockito.when(mockUserScanResults.items().stream().toList().get(0)).thenReturn(mockUser);
		Mockito.when(mockUserScanResults.items().stream().toList().size()).thenReturn(1);
		Mockito.when(mockDynamoDBOperations.scan(userClassCaptor.capture(), scanEnhancedCaptor.capture(), null))
				.thenReturn(mockUserScanResults);

		// Execute the query
		Object[] parameters = new Object[]{joinDate};
		Object o = partTreeDynamoDBQuery.execute(parameters);

		// Assert that we obtain the expected list of results
		assertEquals(o, mockUserScanResults);

		// Assert that the list of results contains the correct elements
		assertEquals(1, mockUserScanResults.items().stream().toList().size());
		assertEquals(mockUser, mockUserScanResults.items().stream().toList().get(0));

		// Assert that we scanned DynamoDB for the correct class
		assertEquals(userClassCaptor.getValue(), User.class);

		// Assert that we have only one filter condition, for the name of the
		// property
		// TODO
//		Map<String, Condition> filterConditions = scanCaptor.getValue().getScanFilter();
//		assertEquals(1, filterConditions.size());
//		Condition filterCondition = filterConditions.get("joinDate");
//		assertNotNull(filterCondition);
//
//		assertEquals(ComparisonOperator.EQ, filterCondition.comparisonOperator());
//
//		// Assert we only have one attribute value for this filter condition
//		assertEquals(1, filterCondition.attributeValueList().size());
//
//		// Assert that there the attribute value type for this attribute value
//		// is String,
//		// and its value is the parameter expected
//		assertEquals(joinDateString, filterCondition.attributeValueList().get(0).s());
//
//		// Assert that all other attribute value types other than String type
//		// are null
//		assertNull(filterCondition.attributeValueList().get(0).ss());
//		assertNull(filterCondition.attributeValueList().get(0).n());
//		assertNull(filterCondition.attributeValueList().get(0).ns());
//		assertNull(filterCondition.attributeValueList().get(0).b());
//		assertNull(filterCondition.attributeValueList().get(0).bs());

		// Verify that the expected DynamoDBOperations method was called
		Mockito.verify(mockDynamoDBOperations).scan(userClassCaptor.getValue(), scanEnhancedCaptor.getValue(), null);
	}

	@Test
	public void testExecute_WhenFinderMethodIsFindingEntityList_WithSingleDateParameter_WithCustomMarshaller_WhenNotFindingByHashKey()
			throws ParseException {
		String joinYearString = "2013";
		DateFormat dateFormat = new SimpleDateFormat("yyyy");
		Date joinYear = dateFormat.parse(joinYearString);

		setupCommonMocksForThisRepositoryMethod(mockUserEntityMetadata, mockDynamoDBUserQueryMethod, User.class,
				"findByJoinYear", 1, "id", null);
		Mockito.when(mockDynamoDBUserQueryMethod.isCollectionQuery()).thenReturn(true);
		DynamoDBYearMarshaller marshaller = new DynamoDBYearMarshaller();

		Mockito.when(mockUserScanResults.items().stream().toList().get(0)).thenReturn(mockUser);
		Mockito.when(mockUserScanResults.items().stream().toList().size()).thenReturn(1);
		Mockito.when(mockDynamoDBOperations.scan(userClassCaptor.capture(), scanEnhancedCaptor.capture(), null))
				.thenReturn(mockUserScanResults);

		// Execute the query
		Object[] parameters = new Object[]{joinYear};
		Object o = partTreeDynamoDBQuery.execute(parameters);

		// Assert that we obtain the expected list of results
		assertEquals(o, mockUserScanResults);

		// Assert that the list of results contains the correct elements
		assertEquals(1, mockUserScanResults.items().stream().toList().size());
		assertEquals(mockUser, mockUserScanResults.items().stream().toList().get(0));

		// Assert that we scanned DynamoDB for the correct class
		assertEquals(userClassCaptor.getValue(), User.class);

		// Assert that we have only one filter condition, for the name of the
		// property
		// TODO
//		Map<String, Condition> filterConditions = scanCaptor.getValue().getScanFilter();
//		assertEquals(1, filterConditions.size());
//		Condition filterCondition = filterConditions.get("joinYear");
//		assertNotNull(filterCondition);
//
//		assertEquals(ComparisonOperator.EQ, filterCondition.comparisonOperator());
//
//		// Assert we only have one attribute value for this filter condition
//		assertEquals(1, filterCondition.attributeValueList().size());
//
//		// Assert that there the attribute value type for this attribute value
//		// is String,
//		// and its value is the parameter expected
//		assertEquals(joinYearString, filterCondition.attributeValueList().get(0).s());
//
//		// Assert that all other attribute value types other than String type
//		// are null
//		assertNull(filterCondition.attributeValueList().get(0).ss());
//		assertNull(filterCondition.attributeValueList().get(0).n());
//		assertNull(filterCondition.attributeValueList().get(0).ns());
//		assertNull(filterCondition.attributeValueList().get(0).b());
//		assertNull(filterCondition.attributeValueList().get(0).bs());

		// Verify that the expected DynamoDBOperations method was called
		Mockito.verify(mockDynamoDBOperations).scan(userClassCaptor.getValue(), scanEnhancedCaptor.getValue(), null);
	}

	// Global Secondary Index Test 1
	@Test
	public void testExecute_WhenFinderMethodIsFindingEntityList_WithSingleDateParameter_WithCustomMarshaller_WhenFindingByGlobalSecondaryHashIndexHashKey()
			throws ParseException {
		String joinYearString = "2013";
		DateFormat dateFormat = new SimpleDateFormat("yyyy");
		Date joinYear = dateFormat.parse(joinYearString);

		setupCommonMocksForThisRepositoryMethod(mockUserEntityMetadata, mockDynamoDBUserQueryMethod, User.class,
				"findByJoinYear", 1, "id", null);
		Mockito.when(mockDynamoDBUserQueryMethod.isCollectionQuery()).thenReturn(true);
		DynamoDBYearMarshaller marshaller = new DynamoDBYearMarshaller();
		Mockito.when(mockUserEntityMetadata.isGlobalIndexHashKeyProperty("joinYear")).thenReturn(true);

		Map<String, String[]> indexRangeKeySecondaryIndexNames = new HashMap<String, String[]>();
		indexRangeKeySecondaryIndexNames.put("joinYear", new String[]{"JoinYear-index"});
		Mockito.when(mockUserEntityMetadata.getGlobalSecondaryIndexNamesByPropertyName())
				.thenReturn(indexRangeKeySecondaryIndexNames);

		Mockito.when(mockUserEntityMetadata.getDynamoDBTableName()).thenReturn("user");

		// Mock out specific QueryRequestMapper behavior expected by this method
		Mockito.when(mockUserQueryResults.items().stream().toList().get(0)).thenReturn(mockUser);
		Mockito.when(mockUserQueryResults.items().stream().toList().size()).thenReturn(1);
		Mockito.when(mockDynamoDBOperations.query(userClassCaptor.capture(), queryEnhancedRequestCaptor.capture(), null))
				.thenReturn(mockUserQueryResults);

		// Execute the query
		Object[] parameters = new Object[]{joinYear};
		Object o = partTreeDynamoDBQuery.execute(parameters);

		// Assert that we obtain the expected results
		assertEquals(mockUserQueryResults, o);
		assertEquals(1, mockUserQueryResults.items().stream().toList().size());
		assertEquals(mockUser, mockUserQueryResults.items().stream().toList().get(0));

		// Assert that we scanned DynamoDB for the correct class
		assertEquals(userClassCaptor.getValue(), User.class);

		String indexName = queryResultCaptor.getValue().indexName();
		assertNotNull(indexName);
		assertEquals("JoinYear-index", indexName);

		assertEquals("user", queryResultCaptor.getValue().tableName());

		// Assert that we have only one range condition for the global secondary index
		// hash key
		// TODO
//		assertEquals(1, queryResultCaptor.getValue().getKeyConditions().size());
//		Condition condition = queryResultCaptor.getValue().getKeyConditions().get("joinYear");
//		assertEquals(ComparisonOperator.EQ, condition.comparisonOperator());
//		assertEquals(1, condition.attributeValueList().size());
//		assertEquals(joinYearString, condition.attributeValueList().get(0).s());
//
//		// Assert that all other attribute value types other than String type
//		// are null
//		assertNull(condition.attributeValueList().get(0).ss());
//		assertNull(condition.attributeValueList().get(0).n());
//		assertNull(condition.attributeValueList().get(0).ns());
//		assertNull(condition.attributeValueList().get(0).b());
//		assertNull(condition.attributeValueList().get(0).bs());

		// Verify that the expected DynamoDBOperations method was called
		Mockito.verify(mockDynamoDBOperations).query(userClassCaptor.getValue(), queryEnhancedRequestCaptor.getValue(), null);
	}

	// Global Secondary Index Test 2
	@Test
	public void testExecute_WhenFinderMethodIsFindingEntityList_WithDateParameterAndStringParameter_WithCustomMarshaller_WhenFindingByGlobalSecondaryHashAndRangeIndexHashAndRangeKey()
			throws ParseException {
		String joinYearString = "2013";
		DateFormat dateFormat = new SimpleDateFormat("yyyy");
		Date joinYear = dateFormat.parse(joinYearString);

		setupCommonMocksForThisRepositoryMethod(mockUserEntityMetadata, mockDynamoDBUserQueryMethod, User.class,
				"findByJoinYearAndPostCode", 2, "id", null);
		lenient().when(mockDynamoDBUserQueryMethod.isCollectionQuery()).thenReturn(true);
		DynamoDBYearMarshaller marshaller = new DynamoDBYearMarshaller();

		lenient().when(mockUserEntityMetadata.isGlobalIndexHashKeyProperty("joinYear")).thenReturn(true);
		lenient().when(mockUserEntityMetadata.isGlobalIndexRangeKeyProperty("postCode")).thenReturn(true);

		Map<String, String[]> indexRangeKeySecondaryIndexNames = new HashMap<String, String[]>();
		indexRangeKeySecondaryIndexNames.put("joinYear", new String[]{"JoinYear-index"});
		indexRangeKeySecondaryIndexNames.put("postCode", new String[]{"JoinYear-index"});

		lenient().when(mockUserEntityMetadata.getGlobalSecondaryIndexNamesByPropertyName())
				.thenReturn(indexRangeKeySecondaryIndexNames);

		// Mock out specific QueryRequestMapper behavior expected by this method
		lenient().when(mockUserQueryResults.items().stream().toList().get(0)).thenReturn(mockUser);
		lenient().when(mockUserQueryResults.items().stream().toList().size()).thenReturn(1);
		lenient().when(mockDynamoDBOperations.query(userClassCaptor.capture(), queryEnhancedRequestCaptor.capture(), null))
				.thenReturn(mockUserQueryResults);
		lenient().when(mockUserEntityMetadata.getDynamoDBTableName()).thenReturn("user");

		// Execute the query
		Object[] parameters = new Object[]{joinYear, "nw1"};
		Object o = partTreeDynamoDBQuery.execute(parameters);

		// Assert that we obtain the expected results
		assertEquals(mockUserQueryResults, o);
		assertEquals(1, mockUserQueryResults.items().stream().toList().size());
		assertEquals(mockUser, mockUserQueryResults.items().stream().toList().get(0));

		// Assert that we scanned DynamoDB for the correct class
		assertEquals(userClassCaptor.getValue(), User.class);

		String indexName = queryResultCaptor.getValue().indexName();
		assertNotNull(indexName);
		assertEquals("JoinYear-index", indexName);

		// Assert that we have only two range conditions for the global secondary index
		// hash key and range key
		// TODO
//		assertEquals(2, queryResultCaptor.getValue().getKeyConditions().size());
//		Condition yearCondition = queryResultCaptor.getValue().getKeyConditions().get("joinYear");
//		assertEquals(ComparisonOperator.EQ, yearCondition.comparisonOperator());
//		assertEquals(1, yearCondition.attributeValueList().size());
//		assertEquals(joinYearString, yearCondition.attributeValueList().get(0).s());
//		Condition postCodeCondition = queryResultCaptor.getValue().getKeyConditions().get("postCode");
//		assertEquals(ComparisonOperator.EQ, postCodeCondition.comparisonOperator());
//		assertEquals(1, postCodeCondition.attributeValueList().size());
//		assertEquals("nw1", postCodeCondition.attributeValueList().get(0).s());
//
//		assertEquals("user", queryResultCaptor.getValue().tableName());
//
//		// Assert that all other attribute value types other than String type
//		// are null
//		assertNull(yearCondition.attributeValueList().get(0).ss());
//		assertNull(yearCondition.attributeValueList().get(0).n());
//		assertNull(yearCondition.attributeValueList().get(0).ns());
//		assertNull(yearCondition.attributeValueList().get(0).b());
//		assertNull(yearCondition.attributeValueList().get(0).bs());
//		assertNull(postCodeCondition.attributeValueList().get(0).ss());
//		assertNull(postCodeCondition.attributeValueList().get(0).n());
//		assertNull(postCodeCondition.attributeValueList().get(0).ns());
//		assertNull(postCodeCondition.attributeValueList().get(0).b());
//		assertNull(postCodeCondition.attributeValueList().get(0).bs());

		// Verify that the expected DynamoDBOperations method was called
		Mockito.verify(mockDynamoDBOperations).query(userClassCaptor.getValue(), queryEnhancedRequestCaptor.getValue(), null);
	}

	// Global Secondary Index Test 3
	@Test
	public void testExecute_WhenFinderMethodIsFindingEntityWithCompositeKeyList_WhenFindingByGlobalSecondaryHashIndexHashKey()
			throws ParseException {

		setupCommonMocksForThisRepositoryMethod(mockPlaylistEntityMetadata, mockDynamoDBPlaylistQueryMethod,
				Playlist.class, "findByDisplayNameOrderByDisplayNameDesc", 1, "userName", "playlistName");
		Mockito.when(mockDynamoDBPlaylistQueryMethod.isCollectionQuery()).thenReturn(true);

		Mockito.when(mockPlaylistEntityMetadata.isGlobalIndexHashKeyProperty("displayName")).thenReturn(true);
		Map<String, String[]> indexRangeKeySecondaryIndexNames = new HashMap<String, String[]>();
		indexRangeKeySecondaryIndexNames.put("displayName", new String[]{"DisplayName-index"});
		Mockito.when(mockPlaylistEntityMetadata.getGlobalSecondaryIndexNamesByPropertyName())
				.thenReturn(indexRangeKeySecondaryIndexNames);

		Mockito.when(mockPlaylistEntityMetadata.getDynamoDBTableName()).thenReturn("playlist");

		// Mock out specific QueryRequestMapper behavior expected by this method
		Mockito.when(mockPlaylistQueryResults.items().stream().toList().get(0)).thenReturn(mockPlaylist);
		Mockito.when(mockPlaylistQueryResults.items().stream().toList().size()).thenReturn(1);
		Mockito.when(mockDynamoDBOperations.query(playlistClassCaptor.capture(), queryEnhancedRequestCaptor.capture(), null))
				.thenReturn(mockPlaylistQueryResults);

		// Execute the query
		Object[] parameters = new Object[]{"Michael"};
		Object o = partTreeDynamoDBQuery.execute(parameters);

		// Assert that we obtain the expected results
		assertEquals(mockPlaylistQueryResults, o);
		assertEquals(1, mockPlaylistQueryResults.items().stream().toList().size());
		assertEquals(mockPlaylist, mockPlaylistQueryResults.items().stream().toList().get(0));

		// Assert that we scanned DynamoDB for the correct class
		assertEquals(playlistClassCaptor.getValue(), Playlist.class);

		String indexName = queryResultCaptor.getValue().indexName();
		assertNotNull(indexName);
		assertEquals("DisplayName-index", indexName);

		assertEquals("playlist", queryResultCaptor.getValue().tableName());

		// Assert that we have only one range condition for the global secondary index
		// hash key
		// TODO
//		assertEquals(1, queryResultCaptor.getValue().getKeyConditions().size());
//		Condition condition = queryResultCaptor.getValue().getKeyConditions().get("displayName");
//		assertEquals(ComparisonOperator.EQ, condition.comparisonOperator());
//		assertEquals(1, condition.attributeValueList().size());
//		assertEquals("Michael", condition.attributeValueList().get(0).s());
//
//		// Assert that all other attribute value types other than String type
//		// are null
//		assertNull(condition.attributeValueList().get(0).ss());
//		assertNull(condition.attributeValueList().get(0).n());
//		assertNull(condition.attributeValueList().get(0).ns());
//		assertNull(condition.attributeValueList().get(0).b());
//		assertNull(condition.attributeValueList().get(0).bs());

		// Verify that the expected DynamoDBOperations method was called
		Mockito.verify(mockDynamoDBOperations).query(playlistClassCaptor.getValue(), queryEnhancedRequestCaptor.getValue(), null);
	}

	// Global Secondary Index Test 3a
	@Test
	public void testExecute_WhenFinderMethodIsFindingEntityWithCompositeKeyList_WhenFindingByGlobalSecondaryHashIndexHashKey_WhereSecondaryHashKeyIsPrimaryRangeKey()
			throws ParseException {

		setupCommonMocksForThisRepositoryMethod(mockPlaylistEntityMetadata, mockDynamoDBPlaylistQueryMethod,
				Playlist.class, "findByPlaylistName", 1, "userName", "playlistName");
		Mockito.when(mockDynamoDBPlaylistQueryMethod.isCollectionQuery()).thenReturn(true);

		Map<String, String[]> indexRangeKeySecondaryIndexNames = new HashMap<String, String[]>();
		indexRangeKeySecondaryIndexNames.put("playlistName", new String[]{"PlaylistName-index"});
		Mockito.when(mockPlaylistEntityMetadata.getGlobalSecondaryIndexNamesByPropertyName())
				.thenReturn(indexRangeKeySecondaryIndexNames);

		Mockito.when(mockPlaylistEntityMetadata.getDynamoDBTableName()).thenReturn("playlist");

		// Mock out specific QueryRequestMapper behavior expected by this method
		Mockito.when(mockPlaylistQueryResults.items().stream().toList().get(0)).thenReturn(mockPlaylist);
		Mockito.when(mockPlaylistQueryResults.items().stream().toList().size()).thenReturn(1);
		Mockito.when(mockDynamoDBOperations.query(playlistClassCaptor.capture(), queryEnhancedRequestCaptor.capture(), null))
				.thenReturn(mockPlaylistQueryResults);

		// Execute the query
		Object[] parameters = new Object[]{"Some Playlist"};
		Object o = partTreeDynamoDBQuery.execute(parameters);

		// Assert that we obtain the expected results
		assertEquals(mockPlaylistQueryResults, o);
		assertEquals(1, mockPlaylistQueryResults.items().stream().toList().size());
		assertEquals(mockPlaylist, mockPlaylistQueryResults.items().stream().toList().get(0));

		// Assert that we scanned DynamoDB for the correct class
		assertEquals(playlistClassCaptor.getValue(), Playlist.class);

		String indexName = queryResultCaptor.getValue().indexName();
		assertNotNull(indexName);
		assertEquals("PlaylistName-index", indexName);

		assertEquals("playlist", queryResultCaptor.getValue().tableName());

		// Assert that we have the correct conditions
		// TODO
//		assertEquals(1, queryResultCaptor.getValue().getKeyConditions().size());
//		Condition condition = queryResultCaptor.getValue().getKeyConditions().get("playlistName");
//		assertEquals(ComparisonOperator.EQ, condition.comparisonOperator());
//		assertEquals(1, condition.attributeValueList().size());
//		assertEquals("Some Playlist", condition.attributeValueList().get(0).s());
//
//		// Assert that all other attribute value types other than String type
//		// are null
//		assertNull(condition.attributeValueList().get(0).ss());
//		assertNull(condition.attributeValueList().get(0).n());
//		assertNull(condition.attributeValueList().get(0).ns());
//		assertNull(condition.attributeValueList().get(0).b());
//		assertNull(condition.attributeValueList().get(0).bs());

		// Verify that the expected DynamoDBOperations method was called
		Mockito.verify(mockDynamoDBOperations).query(playlistClassCaptor.getValue(), queryEnhancedRequestCaptor.getValue(), null);
	}

	// Global Secondary Index Test 4
	@Test
	public void testExecute_WhenFinderMethodIsFindingEntityWithCompositeKeyList_WhenFindingByGlobalSecondaryHashAndRangeIndexHashAndRangeKey_WhereSecondaryHashKeyIsPrimaryHashKey()
			throws ParseException {

		setupCommonMocksForThisRepositoryMethod(mockPlaylistEntityMetadata, mockDynamoDBPlaylistQueryMethod,
				Playlist.class, "findByUserNameAndDisplayName", 2, "userName", "playlistName");
		lenient().when(mockDynamoDBPlaylistQueryMethod.isCollectionQuery()).thenReturn(true);

		lenient().when(mockPlaylistEntityMetadata.isGlobalIndexHashKeyProperty("userName")).thenReturn(true);
		// Mockito.when(mockPlaylistEntityMetadata.isGlobalIndexRangeKeyProperty("displayName")).thenReturn(true);

		Map<String, String[]> indexRangeKeySecondaryIndexNames = new HashMap<String, String[]>();
		indexRangeKeySecondaryIndexNames.put("displayName", new String[]{"UserName-DisplayName-index"});
		indexRangeKeySecondaryIndexNames.put("userName", new String[]{"UserName-DisplayName-index"});

		lenient().when(mockPlaylistEntityMetadata.getGlobalSecondaryIndexNamesByPropertyName())
				.thenReturn(indexRangeKeySecondaryIndexNames);

		lenient().when(mockPlaylistEntityMetadata.getDynamoDBTableName()).thenReturn("playlist");

		// Mock out specific QueryRequestMapper behavior expected by this method
		lenient().when(mockPlaylistQueryResults.items().stream().toList().get(0)).thenReturn(mockPlaylist);
		lenient().when(mockPlaylistQueryResults.items().stream().toList().size()).thenReturn(1);
		lenient().when(mockDynamoDBOperations.query(playlistClassCaptor.capture(), queryEnhancedRequestCaptor.capture(), null))
				.thenReturn(mockPlaylistQueryResults);

		// Execute the query
		Object[] parameters = new Object[]{"1", "Michael"};
		Object o = partTreeDynamoDBQuery.execute(parameters);

		// Assert that we obtain the expected results
		assertEquals(mockPlaylistQueryResults, o);
		assertEquals(1, mockPlaylistQueryResults.items().stream().toList().size());
		assertEquals(mockPlaylist, mockPlaylistQueryResults.items().stream().toList().get(0));

		// Assert that we scanned DynamoDB for the correct class
		assertEquals(playlistClassCaptor.getValue(), Playlist.class);

		String indexName = queryResultCaptor.getValue().indexName();
		assertNotNull(indexName);
		assertEquals("UserName-DisplayName-index", indexName);

		assertEquals("playlist", queryResultCaptor.getValue().tableName());

		// Assert that we the correct conditions
		// TODO
//		assertEquals(2, queryResultCaptor.getValue().getKeyConditions().size());
//		Condition globalRangeKeyCondition = queryResultCaptor.getValue().getKeyConditions().get("displayName");
//		assertEquals(ComparisonOperator.EQ, globalRangeKeyCondition.comparisonOperator());
//		assertEquals(1, globalRangeKeyCondition.attributeValueList().size());
//		assertEquals("Michael", globalRangeKeyCondition.attributeValueList().get(0).s());
//		Condition globalHashKeyCondition = queryResultCaptor.getValue().getKeyConditions().get("userName");
//		assertEquals(ComparisonOperator.EQ, globalHashKeyCondition.comparisonOperator());
//		assertEquals(1, globalHashKeyCondition.attributeValueList().size());
//		assertEquals("1", globalHashKeyCondition.attributeValueList().get(0).s());
//
//		// Assert that all other attribute value types other than String type
//		// are null
//		assertNull(globalRangeKeyCondition.attributeValueList().get(0).ss());
//		assertNull(globalRangeKeyCondition.attributeValueList().get(0).n());
//		assertNull(globalRangeKeyCondition.attributeValueList().get(0).ns());
//		assertNull(globalRangeKeyCondition.attributeValueList().get(0).b());
//		assertNull(globalRangeKeyCondition.attributeValueList().get(0).bs());
//
//		assertNull(globalHashKeyCondition.attributeValueList().get(0).ss());
//		assertNull(globalHashKeyCondition.attributeValueList().get(0).n());
//		assertNull(globalHashKeyCondition.attributeValueList().get(0).ns());
//		assertNull(globalHashKeyCondition.attributeValueList().get(0).b());
//		assertNull(globalHashKeyCondition.attributeValueList().get(0).bs());

		// Verify that the expected DynamoDBOperations method was called
		Mockito.verify(mockDynamoDBOperations).query(playlistClassCaptor.getValue(), queryEnhancedRequestCaptor.getValue(), null);
	}

	// Global Secondary Index Test 4b
	@Test
	public void testExecute_WhenFinderMethodIsFindingEntityWithCompositeKeyList_WhenFindingByGlobalSecondaryHashAndRangeIndexHashAndRangeKey_WhereSecondaryHashKeyIsPrimaryRangeKey()
			throws ParseException {

		setupCommonMocksForThisRepositoryMethod(mockPlaylistEntityMetadata, mockDynamoDBPlaylistQueryMethod,
				Playlist.class, "findByPlaylistNameAndDisplayName", 2, "userName", "playlistName");
		Mockito.when(mockDynamoDBPlaylistQueryMethod.isCollectionQuery()).thenReturn(true);

		lenient().when(mockPlaylistEntityMetadata.isGlobalIndexHashKeyProperty("playlistName")).thenReturn(true);
		// Mockito.when(mockPlaylistEntityMetadata.isGlobalIndexRangeKeyProperty("displayName")).thenReturn(true);
		Map<String, String[]> indexRangeKeySecondaryIndexNames = new HashMap<String, String[]>();
		indexRangeKeySecondaryIndexNames.put("playlistName", new String[]{"PlaylistName-DisplayName-index"});
		indexRangeKeySecondaryIndexNames.put("displayName", new String[]{"PlaylistName-DisplayName-index"});

		Mockito.when(mockPlaylistEntityMetadata.getGlobalSecondaryIndexNamesByPropertyName())
				.thenReturn(indexRangeKeySecondaryIndexNames);

		Mockito.when(mockPlaylistEntityMetadata.getDynamoDBTableName()).thenReturn("playlist");

		// Mock out specific QueryRequestMapper behavior expected by this method
		Mockito.when(mockPlaylistQueryResults.items().stream().toList().get(0)).thenReturn(mockPlaylist);
		Mockito.when(mockPlaylistQueryResults.items().stream().toList().size()).thenReturn(1);
		Mockito.when(mockDynamoDBOperations.query(playlistClassCaptor.capture(), queryEnhancedRequestCaptor.capture(), null))
				.thenReturn(mockPlaylistQueryResults);

		// Execute the query
		Object[] parameters = new Object[]{"SomePlaylistName", "Michael"};
		Object o = partTreeDynamoDBQuery.execute(parameters);

		// Assert that we obtain the expected results
		assertEquals(mockPlaylistQueryResults, o);
		assertEquals(1, mockPlaylistQueryResults.items().stream().toList().size());
		assertEquals(mockPlaylist, mockPlaylistQueryResults.items().stream().toList().get(0));

		// Assert that we scanned DynamoDB for the correct class
		assertEquals(playlistClassCaptor.getValue(), Playlist.class);

		String indexName = queryResultCaptor.getValue().indexName();
		assertNotNull(indexName);
		assertEquals("PlaylistName-DisplayName-index", indexName);

		assertEquals("playlist", queryResultCaptor.getValue().tableName());

		// Assert that we have the correct conditions
		// TODO
//		assertEquals(2, queryResultCaptor.getValue().getKeyConditions().size());
//		Condition globalRangeKeyCondition = queryResultCaptor.getValue().getKeyConditions().get("displayName");
//		assertEquals(ComparisonOperator.EQ, globalRangeKeyCondition.comparisonOperator());
//		assertEquals(1, globalRangeKeyCondition.attributeValueList().size());
//		assertEquals("Michael", globalRangeKeyCondition.attributeValueList().get(0).s());
//		Condition globalHashKeyCondition = queryResultCaptor.getValue().getKeyConditions().get("playlistName");
//		assertEquals(ComparisonOperator.EQ, globalHashKeyCondition.comparisonOperator());
//		assertEquals(1, globalHashKeyCondition.attributeValueList().size());
//		assertEquals("SomePlaylistName", globalHashKeyCondition.attributeValueList().get(0).s());

		// Assert that all other attribute value types other than String type
		// are null
//		assertNull(globalRangeKeyCondition.attributeValueList().get(0).ss());
//		assertNull(globalRangeKeyCondition.attributeValueList().get(0).n());
//		assertNull(globalRangeKeyCondition.attributeValueList().get(0).ns());
//		assertNull(globalRangeKeyCondition.attributeValueList().get(0).b());
//		assertNull(globalRangeKeyCondition.attributeValueList().get(0).bs());
//
//		assertNull(globalHashKeyCondition.attributeValueList().get(0).ss());
//		assertNull(globalHashKeyCondition.attributeValueList().get(0).n());
//		assertNull(globalHashKeyCondition.attributeValueList().get(0).ns());
//		assertNull(globalHashKeyCondition.attributeValueList().get(0).b());
//		assertNull(globalHashKeyCondition.attributeValueList().get(0).bs());

		// Verify that the expected DynamoDBOperations method was called
		Mockito.verify(mockDynamoDBOperations).query(playlistClassCaptor.getValue(), queryEnhancedRequestCaptor.getValue(), null);
	}

	// Global Secondary Index Test 4c
	@Test
	public void testExecute_WhenFinderMethodIsFindingEntityWithCompositeKeyList_WhenFindingByGlobalSecondaryHashAndRangeIndexHashAndRangeKey_WhereSecondaryRangeKeyIsPrimaryRangeKey()
			throws ParseException {

		setupCommonMocksForThisRepositoryMethod(mockPlaylistEntityMetadata, mockDynamoDBPlaylistQueryMethod,
				Playlist.class, "findByDisplayNameAndPlaylistName", 2, "userName", "playlistName");
		Mockito.when(mockDynamoDBPlaylistQueryMethod.isCollectionQuery()).thenReturn(true);

		Mockito.when(mockPlaylistEntityMetadata.isGlobalIndexHashKeyProperty("displayName")).thenReturn(true);
		// Mockito.when(mockPlaylistEntityMetadata.isGlobalIndexRangeKeyProperty("playlistName")).thenReturn(true);

		Map<String, String[]> indexRangeKeySecondaryIndexNames = new HashMap<String, String[]>();
		indexRangeKeySecondaryIndexNames.put("displayName", new String[]{"DisplayName-PlaylistName-index"});
		indexRangeKeySecondaryIndexNames.put("playlistName", new String[]{"DisplayName-PlaylistName-index"});

		Mockito.when(mockPlaylistEntityMetadata.getGlobalSecondaryIndexNamesByPropertyName())
				.thenReturn(indexRangeKeySecondaryIndexNames);

		Mockito.when(mockPlaylistEntityMetadata.getDynamoDBTableName()).thenReturn("playlist");

		// Mock out specific QueryRequestMapper behavior expected by this method
		Mockito.when(mockPlaylistQueryResults.items().stream().toList().get(0)).thenReturn(mockPlaylist);
		Mockito.when(mockPlaylistQueryResults.items().stream().toList().size()).thenReturn(1);
		Mockito.when(mockDynamoDBOperations.query(playlistClassCaptor.capture(), queryEnhancedRequestCaptor.capture(), null))
				.thenReturn(mockPlaylistQueryResults);

		// Execute the query
		Object[] parameters = new Object[]{"SomeDisplayName", "SomePlaylistName"};
		Object o = partTreeDynamoDBQuery.execute(parameters);

		// Assert that we obtain the expected results
		assertEquals(mockPlaylistQueryResults, o);
		assertEquals(1, mockPlaylistQueryResults.items().stream().toList().size());
		assertEquals(mockPlaylist, mockPlaylistQueryResults.items().stream().toList().get(0));

		// Assert that we scanned DynamoDB for the correct class
		assertEquals(playlistClassCaptor.getValue(), Playlist.class);

		String indexName = queryResultCaptor.getValue().indexName();
		assertNotNull(indexName);
		assertEquals("DisplayName-PlaylistName-index", indexName);

		assertEquals("playlist", queryResultCaptor.getValue().tableName());

		// Assert that we have the correct conditions
		// TODO

//		assertEquals(2, queryResultCaptor.getValue().getKeyConditions().size());
//		Condition globalRangeKeyCondition = queryResultCaptor.getValue().getKeyConditions().get("displayName");
//		assertEquals(ComparisonOperator.EQ, globalRangeKeyCondition.comparisonOperator());
//		assertEquals(1, globalRangeKeyCondition.attributeValueList().size());
//		assertEquals("SomeDisplayName", globalRangeKeyCondition.attributeValueList().get(0).s());
//		Condition globalHashKeyCondition = queryResultCaptor.getValue().getKeyConditions().get("playlistName");
//		assertEquals(ComparisonOperator.EQ, globalHashKeyCondition.comparisonOperator());
//		assertEquals(1, globalHashKeyCondition.attributeValueList().size());
//		assertEquals("SomePlaylistName", globalHashKeyCondition.attributeValueList().get(0).s());
//
//		// Assert that all other attribute value types other than String type
//		// are null
//		assertNull(globalRangeKeyCondition.attributeValueList().get(0).ss());
//		assertNull(globalRangeKeyCondition.attributeValueList().get(0).n());
//		assertNull(globalRangeKeyCondition.attributeValueList().get(0).ns());
//		assertNull(globalRangeKeyCondition.attributeValueList().get(0).b());
//		assertNull(globalRangeKeyCondition.attributeValueList().get(0).bs());
//
//		assertNull(globalHashKeyCondition.attributeValueList().get(0).ss());
//		assertNull(globalHashKeyCondition.attributeValueList().get(0).n());
//		assertNull(globalHashKeyCondition.attributeValueList().get(0).ns());
//		assertNull(globalHashKeyCondition.attributeValueList().get(0).b());
//		assertNull(globalHashKeyCondition.attributeValueList().get(0).bs());

		// Verify that the expected DynamoDBOperations method was called
		Mockito.verify(mockDynamoDBOperations).query(playlistClassCaptor.getValue(), queryEnhancedRequestCaptor.getValue(), null);
	}

	// Global Secondary Index Test 4d
	@Test
	public void testExecute_WhenFinderMethodIsFindingEntityWithCompositeKeyList_WhenFindingByGlobalSecondaryHashAndRangeIndexHashAndRangeKey_WhereSecondaryRangeKeyIsPrimaryHashKey()
			throws ParseException {

		setupCommonMocksForThisRepositoryMethod(mockPlaylistEntityMetadata, mockDynamoDBPlaylistQueryMethod,
				Playlist.class, "findByDisplayNameAndUserName", 2, "userName", "playlistName");
		Mockito.when(mockDynamoDBPlaylistQueryMethod.isCollectionQuery()).thenReturn(true);

		Mockito.when(mockPlaylistEntityMetadata.isGlobalIndexHashKeyProperty("displayName")).thenReturn(true);
		// Mockito.when(mockPlaylistEntityMetadata.isGlobalIndexRangeKeyProperty("userName")).thenReturn(true);

		Map<String, String[]> indexRangeKeySecondaryIndexNames = new HashMap<String, String[]>();
		indexRangeKeySecondaryIndexNames.put("displayName", new String[]{"DisplayName-UserName-index"});
		indexRangeKeySecondaryIndexNames.put("userName", new String[]{"DisplayName-UserName-index"});

		Mockito.when(mockPlaylistEntityMetadata.getGlobalSecondaryIndexNamesByPropertyName())
				.thenReturn(indexRangeKeySecondaryIndexNames);

		Mockito.when(mockPlaylistEntityMetadata.getDynamoDBTableName()).thenReturn("playlist");

		// Mock out specific QueryRequestMapper behavior expected by this method
		Mockito.when(mockPlaylistQueryResults.items().stream().toList().get(0)).thenReturn(mockPlaylist);
		Mockito.when(mockPlaylistQueryResults.items().stream().toList().size()).thenReturn(1);
		Mockito.when(mockDynamoDBOperations.query(playlistClassCaptor.capture(), queryEnhancedRequestCaptor.capture(), null))
				.thenReturn(mockPlaylistQueryResults);

		// Execute the query
		Object[] parameters = new Object[]{"SomeDisplayName", "SomeUserName"};
		Object o = partTreeDynamoDBQuery.execute(parameters);

		// Assert that we obtain the expected results
		assertEquals(mockPlaylistQueryResults, o);
		assertEquals(1, mockPlaylistQueryResults.items().stream().toList().size());
		assertEquals(mockPlaylist, mockPlaylistQueryResults.items().stream().toList().get(0));

		// Assert that we scanned DynamoDB for the correct class
		assertEquals(playlistClassCaptor.getValue(), Playlist.class);

		String indexName = queryResultCaptor.getValue().indexName();
		assertNotNull(indexName);
		assertEquals("DisplayName-UserName-index", indexName);

		assertEquals("playlist", queryResultCaptor.getValue().tableName());

		// Assert that we have the correct conditions
		// TODO

//		assertEquals(2, queryResultCaptor.getValue().getKeyConditions().size());
//		Condition globalRangeKeyCondition = queryResultCaptor.getValue().getKeyConditions().get("displayName");
//		assertEquals(ComparisonOperator.EQ, globalRangeKeyCondition.comparisonOperator());
//		assertEquals(1, globalRangeKeyCondition.attributeValueList().size());
//		assertEquals("SomeDisplayName", globalRangeKeyCondition.attributeValueList().get(0).s());
//		Condition globalHashKeyCondition = queryResultCaptor.getValue().getKeyConditions().get("userName");
//		assertEquals(ComparisonOperator.EQ, globalHashKeyCondition.comparisonOperator());
//		assertEquals(1, globalHashKeyCondition.attributeValueList().size());
//		assertEquals("SomeUserName", globalHashKeyCondition.attributeValueList().get(0).s());
//
//		// Assert that all other attribute value types other than String type
//		// are null
//		assertNull(globalRangeKeyCondition.attributeValueList().get(0).ss());
//		assertNull(globalRangeKeyCondition.attributeValueList().get(0).n());
//		assertNull(globalRangeKeyCondition.attributeValueList().get(0).ns());
//		assertNull(globalRangeKeyCondition.attributeValueList().get(0).b());
//		assertNull(globalRangeKeyCondition.attributeValueList().get(0).bs());
//
//		assertNull(globalHashKeyCondition.attributeValueList().get(0).ss());
//		assertNull(globalHashKeyCondition.attributeValueList().get(0).n());
//		assertNull(globalHashKeyCondition.attributeValueList().get(0).ns());
//		assertNull(globalHashKeyCondition.attributeValueList().get(0).b());
//		assertNull(globalHashKeyCondition.attributeValueList().get(0).bs());

		// Verify that the expected DynamoDBOperations method was called
		Mockito.verify(mockDynamoDBOperations).query(playlistClassCaptor.getValue(), queryEnhancedRequestCaptor.getValue(), null);
	}

	// Global Secondary Index Test 4e
	@Test
	public void testExecute_WhenFinderMethodIsFindingEntityWithCompositeKeyList_WhenFindingByGlobalSecondaryHashAndRangeIndexHashAndRangeKeyNonEqualityCondition_WhereSecondaryHashKeyIsPrimaryHashKey()
			throws ParseException {

		setupCommonMocksForThisRepositoryMethod(mockPlaylistEntityMetadata, mockDynamoDBPlaylistQueryMethod,
				Playlist.class, "findByUserNameAndDisplayNameAfter", 2, "userName", "playlistName");
		lenient().when(mockDynamoDBPlaylistQueryMethod.isCollectionQuery()).thenReturn(true);

		lenient().when(mockPlaylistEntityMetadata.isGlobalIndexHashKeyProperty("userName")).thenReturn(true);
		// Mockito.when(mockPlaylistEntityMetadata.isGlobalIndexRangeKeyProperty("displayName")).thenReturn(true);

		Map<String, String[]> indexRangeKeySecondaryIndexNames = new HashMap<String, String[]>();
		indexRangeKeySecondaryIndexNames.put("displayName", new String[]{"UserName-DisplayName-index"});
		indexRangeKeySecondaryIndexNames.put("userName", new String[]{"UserName-DisplayName-index"});

		lenient().when(mockPlaylistEntityMetadata.getGlobalSecondaryIndexNamesByPropertyName())
				.thenReturn(indexRangeKeySecondaryIndexNames);

		lenient().when(mockPlaylistEntityMetadata.getDynamoDBTableName()).thenReturn("playlist");

		// Mock out specific QueryRequestMapper behavior expected by this method
		lenient().when(mockPlaylistQueryResults.items().stream().toList().get(0)).thenReturn(mockPlaylist);
		lenient().when(mockPlaylistQueryResults.items().stream().toList().size()).thenReturn(1);
		lenient().when(mockDynamoDBOperations.query(playlistClassCaptor.capture(), queryEnhancedRequestCaptor.capture(), null))
				.thenReturn(mockPlaylistQueryResults);

		// Execute the query
		Object[] parameters = new Object[]{"1", "Michael"};
		Object o = partTreeDynamoDBQuery.execute(parameters);

		// Assert that we obtain the expected results
		assertEquals(mockPlaylistQueryResults, o);
		assertEquals(1, mockPlaylistQueryResults.items().stream().toList().size());
		assertEquals(mockPlaylist, mockPlaylistQueryResults.items().stream().toList().get(0));

		// Assert that we scanned DynamoDB for the correct class
		assertEquals(playlistClassCaptor.getValue(), Playlist.class);

		String indexName = queryResultCaptor.getValue().indexName();
		assertNotNull(indexName);
		assertEquals("UserName-DisplayName-index", indexName);

		assertEquals("playlist", queryResultCaptor.getValue().tableName());

		// Assert that we the correct conditions
		// TODO
//		assertEquals(2, queryResultCaptor.getValue().getKeyConditions().size());
//		Condition globalRangeKeyCondition = queryResultCaptor.getValue().getKeyConditions().get("displayName");
//		assertEquals(ComparisonOperator.GT, globalRangeKeyCondition.comparisonOperator());
//		assertEquals(1, globalRangeKeyCondition.attributeValueList().size());
//		assertEquals("Michael", globalRangeKeyCondition.attributeValueList().get(0).s());
//		Condition globalHashKeyCondition = queryResultCaptor.getValue().getKeyConditions().get("userName");
//		assertEquals(ComparisonOperator.EQ, globalHashKeyCondition.comparisonOperator());
//		assertEquals(1, globalHashKeyCondition.attributeValueList().size());
//		assertEquals("1", globalHashKeyCondition.attributeValueList().get(0).s());
//
//		// Assert that all other attribute value types other than String type
//		// are null
//		assertNull(globalRangeKeyCondition.attributeValueList().get(0).ss());
//		assertNull(globalRangeKeyCondition.attributeValueList().get(0).n());
//		assertNull(globalRangeKeyCondition.attributeValueList().get(0).ns());
//		assertNull(globalRangeKeyCondition.attributeValueList().get(0).b());
//		assertNull(globalRangeKeyCondition.attributeValueList().get(0).bs());
//
//		assertNull(globalHashKeyCondition.attributeValueList().get(0).ss());
//		assertNull(globalHashKeyCondition.attributeValueList().get(0).n());
//		assertNull(globalHashKeyCondition.attributeValueList().get(0).ns());
//		assertNull(globalHashKeyCondition.attributeValueList().get(0).b());
//		assertNull(globalHashKeyCondition.attributeValueList().get(0).bs());

		// Verify that the expected DynamoDBOperations method was called
		Mockito.verify(mockDynamoDBOperations).query(playlistClassCaptor.getValue(), queryEnhancedRequestCaptor.getValue(), null);
	}

	// Global Secondary Index Test 4e2
	@Test
	public void testExecute_WhenFinderMethodIsFindingEntityWithCompositeKeyList_WhenFindingByGlobalSecondaryHashAndRangeIndexHashAndRangeKeyNonEqualityCondition_WhereSecondaryHashKeyIsPrimaryHashKey_WhenAccessingPropertyViaCompositeIdPath()
			throws ParseException {

		setupCommonMocksForThisRepositoryMethod(mockPlaylistEntityMetadata, mockDynamoDBPlaylistQueryMethod,
				Playlist.class, "findByPlaylistIdUserNameAndDisplayNameAfter", 2, "userName", "playlistName");
		lenient().when(mockDynamoDBPlaylistQueryMethod.isCollectionQuery()).thenReturn(true);

		lenient().when(mockPlaylistEntityMetadata.isGlobalIndexHashKeyProperty("userName")).thenReturn(true);
		// Mockito.when(mockPlaylistEntityMetadata.isGlobalIndexRangeKeyProperty("displayName")).thenReturn(true);

		Map<String, String[]> indexRangeKeySecondaryIndexNames = new HashMap<String, String[]>();
		indexRangeKeySecondaryIndexNames.put("displayName", new String[]{"UserName-DisplayName-index"});
		indexRangeKeySecondaryIndexNames.put("userName", new String[]{"UserName-DisplayName-index"});

		lenient().when(mockPlaylistEntityMetadata.getGlobalSecondaryIndexNamesByPropertyName())
				.thenReturn(indexRangeKeySecondaryIndexNames);

		lenient().when(mockPlaylistEntityMetadata.getDynamoDBTableName()).thenReturn("playlist");

		// Mock out specific QueryRequestMapper behavior expected by this method
		lenient().when(mockPlaylistQueryResults.items().stream().toList().get(0)).thenReturn(mockPlaylist);
		lenient().when(mockPlaylistQueryResults.items().stream().toList().size()).thenReturn(1);
		lenient().when(mockDynamoDBOperations.query(playlistClassCaptor.capture(), queryEnhancedRequestCaptor.capture(), null))
				.thenReturn(mockPlaylistQueryResults);

		// Execute the query
		Object[] parameters = new Object[]{"1", "Michael"};
		Object o = partTreeDynamoDBQuery.execute(parameters);

		// Assert that we obtain the expected results
		assertEquals(mockPlaylistQueryResults, o);
		assertEquals(1, mockPlaylistQueryResults.items().stream().toList().size());
		assertEquals(mockPlaylist, mockPlaylistQueryResults.items().stream().toList().get(0));

		// Assert that we scanned DynamoDB for the correct class
		assertEquals(playlistClassCaptor.getValue(), Playlist.class);

		String indexName = queryResultCaptor.getValue().indexName();
		assertNotNull(indexName);
		assertEquals("UserName-DisplayName-index", indexName);

		assertEquals("playlist", queryResultCaptor.getValue().tableName());

		// Assert that we the correct conditions
		// TODO
//		assertEquals(2, queryResultCaptor.getValue().getKeyConditions().size());
//		Condition globalRangeKeyCondition = queryResultCaptor.getValue().getKeyConditions().get("displayName");
//		assertEquals(ComparisonOperator.GT, globalRangeKeyCondition.comparisonOperator());
//		assertEquals(1, globalRangeKeyCondition.attributeValueList().size());
//		assertEquals("Michael", globalRangeKeyCondition.attributeValueList().get(0).s());
//		Condition globalHashKeyCondition = queryResultCaptor.getValue().getKeyConditions().get("userName");
//		assertEquals(ComparisonOperator.EQ, globalHashKeyCondition.comparisonOperator());
//		assertEquals(1, globalHashKeyCondition.attributeValueList().size());
//		assertEquals("1", globalHashKeyCondition.attributeValueList().get(0).s());
//
//		// Assert that all other attribute value types other than String type
//		// are null
//		assertNull(globalRangeKeyCondition.attributeValueList().get(0).ss());
//		assertNull(globalRangeKeyCondition.attributeValueList().get(0).n());
//		assertNull(globalRangeKeyCondition.attributeValueList().get(0).ns());
//		assertNull(globalRangeKeyCondition.attributeValueList().get(0).b());
//		assertNull(globalRangeKeyCondition.attributeValueList().get(0).bs());
//
//		assertNull(globalHashKeyCondition.attributeValueList().get(0).ss());
//		assertNull(globalHashKeyCondition.attributeValueList().get(0).n());
//		assertNull(globalHashKeyCondition.attributeValueList().get(0).ns());
//		assertNull(globalHashKeyCondition.attributeValueList().get(0).b());
//		assertNull(globalHashKeyCondition.attributeValueList().get(0).bs());

		// Verify that the expected DynamoDBOperations method was called
		Mockito.verify(mockDynamoDBOperations).query(playlistClassCaptor.getValue(), queryEnhancedRequestCaptor.getValue(), null);
	}

	// Global Secondary Index Test 4f
	@Test
	public void testExecute_WhenFinderMethodIsFindingEntityWithCompositeKeyList_WhenFindingByGlobalSecondaryHashAndRangeIndexHashAndRangeKeyNonEqualityCondition_WhereSecondaryHashKeyIsPrimaryRangeKey()
			throws ParseException {

		setupCommonMocksForThisRepositoryMethod(mockPlaylistEntityMetadata, mockDynamoDBPlaylistQueryMethod,
				Playlist.class, "findByPlaylistNameAndDisplayNameAfter", 2, "userName", "playlistName");
		Mockito.when(mockDynamoDBPlaylistQueryMethod.isCollectionQuery()).thenReturn(true);

		lenient().when(mockPlaylistEntityMetadata.isGlobalIndexHashKeyProperty("playlistName")).thenReturn(true);
		// Mockito.when(mockPlaylistEntityMetadata.isGlobalIndexRangeKeyProperty("displayName")).thenReturn(true);
		Map<String, String[]> indexRangeKeySecondaryIndexNames = new HashMap<String, String[]>();
		indexRangeKeySecondaryIndexNames.put("playlistName", new String[]{"PlaylistName-DisplayName-index"});
		indexRangeKeySecondaryIndexNames.put("displayName", new String[]{"PlaylistName-DisplayName-index"});

		Mockito.when(mockPlaylistEntityMetadata.getGlobalSecondaryIndexNamesByPropertyName())
				.thenReturn(indexRangeKeySecondaryIndexNames);

		Mockito.when(mockPlaylistEntityMetadata.getDynamoDBTableName()).thenReturn("playlist");

		// Mock out specific QueryRequestMapper behavior expected by this method
		Mockito.when(mockPlaylistQueryResults.items().stream().toList().get(0)).thenReturn(mockPlaylist);
		Mockito.when(mockPlaylistQueryResults.items().stream().toList().size()).thenReturn(1);
		Mockito.when(mockDynamoDBOperations.query(playlistClassCaptor.capture(), queryEnhancedRequestCaptor.capture(), null))
				.thenReturn(mockPlaylistQueryResults);

		// Execute the query
		Object[] parameters = new Object[]{"SomePlaylistName", "Michael"};
		Object o = partTreeDynamoDBQuery.execute(parameters);

		// Assert that we obtain the expected results
		assertEquals(mockPlaylistQueryResults, o);
		assertEquals(1, mockPlaylistQueryResults.items().stream().toList().size());
		assertEquals(mockPlaylist, mockPlaylistQueryResults.items().stream().toList().get(0));

		// Assert that we scanned DynamoDB for the correct class
		assertEquals(playlistClassCaptor.getValue(), Playlist.class);

		String indexName = queryResultCaptor.getValue().indexName();
		assertNotNull(indexName);
		assertEquals("PlaylistName-DisplayName-index", indexName);

		assertEquals("playlist", queryResultCaptor.getValue().tableName());

		// Assert that we have the correct conditions
		// TODO
//		assertEquals(2, queryResultCaptor.getValue().getKeyConditions().size());
//		Condition globalRangeKeyCondition = queryResultCaptor.getValue().getKeyConditions().get("displayName");
//		assertEquals(ComparisonOperator.GT, globalRangeKeyCondition.comparisonOperator());
//		assertEquals(1, globalRangeKeyCondition.attributeValueList().size());
//		assertEquals("Michael", globalRangeKeyCondition.attributeValueList().get(0).s());
//		Condition globalHashKeyCondition = queryResultCaptor.getValue().getKeyConditions().get("playlistName");
//		assertEquals(ComparisonOperator.EQ, globalHashKeyCondition.comparisonOperator());
//		assertEquals(1, globalHashKeyCondition.attributeValueList().size());
//		assertEquals("SomePlaylistName", globalHashKeyCondition.attributeValueList().get(0).s());
//
//		// Assert that all other attribute value types other than String type
//		// are null
//		assertNull(globalRangeKeyCondition.attributeValueList().get(0).ss());
//		assertNull(globalRangeKeyCondition.attributeValueList().get(0).n());
//		assertNull(globalRangeKeyCondition.attributeValueList().get(0).ns());
//		assertNull(globalRangeKeyCondition.attributeValueList().get(0).b());
//		assertNull(globalRangeKeyCondition.attributeValueList().get(0).bs());
//
//		assertNull(globalHashKeyCondition.attributeValueList().get(0).ss());
//		assertNull(globalHashKeyCondition.attributeValueList().get(0).n());
//		assertNull(globalHashKeyCondition.attributeValueList().get(0).ns());
//		assertNull(globalHashKeyCondition.attributeValueList().get(0).b());
//		assertNull(globalHashKeyCondition.attributeValueList().get(0).bs());

		// Verify that the expected DynamoDBOperations method was called
		Mockito.verify(mockDynamoDBOperations).query(playlistClassCaptor.getValue(), queryEnhancedRequestCaptor.getValue(), null);
	}

	// Global Secondary Index Test 4g
	@Test
	public void testExecute_WhenFinderMethodIsFindingEntityWithCompositeKeyList_WhenFindingByGlobalSecondaryHashAndRangeIndexHashAndRangeKeyNonEqualityCondition_WhereSecondaryRangeKeyIsPrimaryRangeKey()
			throws ParseException {

		setupCommonMocksForThisRepositoryMethod(mockPlaylistEntityMetadata, mockDynamoDBPlaylistQueryMethod,
				Playlist.class, "findByDisplayNameAndPlaylistNameAfter", 2, "userName", "playlistName");
		Mockito.when(mockDynamoDBPlaylistQueryMethod.isCollectionQuery()).thenReturn(true);

		lenient().when(mockPlaylistEntityMetadata.isGlobalIndexHashKeyProperty("displayName")).thenReturn(true);
		lenient().when(mockPlaylistEntityMetadata.isGlobalIndexRangeKeyProperty("playlistName")).thenReturn(true);

		Map<String, String[]> indexRangeKeySecondaryIndexNames = new HashMap<String, String[]>();
		indexRangeKeySecondaryIndexNames.put("displayName", new String[]{"DisplayName-PlaylistName-index"});
		indexRangeKeySecondaryIndexNames.put("playlistName", new String[]{"DisplayName-PlaylistName-index"});

		Mockito.when(mockPlaylistEntityMetadata.getGlobalSecondaryIndexNamesByPropertyName())
				.thenReturn(indexRangeKeySecondaryIndexNames);

		Mockito.when(mockPlaylistEntityMetadata.getDynamoDBTableName()).thenReturn("playlist");

		// Mock out specific QueryRequestMapper behavior expected by this method
		Mockito.when(mockPlaylistQueryResults.items().stream().toList().get(0)).thenReturn(mockPlaylist);
		Mockito.when(mockPlaylistQueryResults.items().stream().toList().size()).thenReturn(1);
		Mockito.when(mockDynamoDBOperations.query(playlistClassCaptor.capture(), queryEnhancedRequestCaptor.capture(), null))
				.thenReturn(mockPlaylistQueryResults);

		// Execute the query
		Object[] parameters = new Object[]{"SomeDisplayName", "SomePlaylistName"};
		Object o = partTreeDynamoDBQuery.execute(parameters);

		// Assert that we obtain the expected results
		assertEquals(mockPlaylistQueryResults, o);
		assertEquals(1, mockPlaylistQueryResults.items().stream().toList().size());
		assertEquals(mockPlaylist, mockPlaylistQueryResults.items().stream().toList().get(0));

		// Assert that we scanned DynamoDB for the correct class
		assertEquals(playlistClassCaptor.getValue(), Playlist.class);

		String indexName = queryResultCaptor.getValue().indexName();
		assertNotNull(indexName);
		assertEquals("DisplayName-PlaylistName-index", indexName);

		assertEquals("playlist", queryResultCaptor.getValue().tableName());

		// Assert that we have the correct conditions
		// TODO

//		assertEquals(2, queryResultCaptor.getValue().getKeyConditions().size());
//		Condition globalRangeKeyCondition = queryResultCaptor.getValue().getKeyConditions().get("displayName");
//		assertEquals(ComparisonOperator.EQ, globalRangeKeyCondition.comparisonOperator());
//		assertEquals(1, globalRangeKeyCondition.attributeValueList().size());
//		assertEquals("SomeDisplayName", globalRangeKeyCondition.attributeValueList().get(0).s());
//		Condition globalHashKeyCondition = queryResultCaptor.getValue().getKeyConditions().get("playlistName");
//		assertEquals(ComparisonOperator.GT, globalHashKeyCondition.comparisonOperator());
//		assertEquals(1, globalHashKeyCondition.attributeValueList().size());
//		assertEquals("SomePlaylistName", globalHashKeyCondition.attributeValueList().get(0).s());
//
//		// Assert that all other attribute value types other than String type
//		// are null
//		assertNull(globalRangeKeyCondition.attributeValueList().get(0).ss());
//		assertNull(globalRangeKeyCondition.attributeValueList().get(0).n());
//		assertNull(globalRangeKeyCondition.attributeValueList().get(0).ns());
//		assertNull(globalRangeKeyCondition.attributeValueList().get(0).b());
//		assertNull(globalRangeKeyCondition.attributeValueList().get(0).bs());
//
//		assertNull(globalHashKeyCondition.attributeValueList().get(0).ss());
//		assertNull(globalHashKeyCondition.attributeValueList().get(0).n());
//		assertNull(globalHashKeyCondition.attributeValueList().get(0).ns());
//		assertNull(globalHashKeyCondition.attributeValueList().get(0).b());
//		assertNull(globalHashKeyCondition.attributeValueList().get(0).bs());

		// Verify that the expected DynamoDBOperations method was called
		Mockito.verify(mockDynamoDBOperations).query(playlistClassCaptor.getValue(), queryEnhancedRequestCaptor.getValue(), null);
	}

	// Global Secondary Index Test 4h
	@Test
	public void testExecute_WhenFinderMethodIsFindingEntityWithCompositeKeyList_WhenFindingByGlobalSecondaryHashAndRangeIndexHashAndRangeKeyNonEqualityCondition_WhereSecondaryRangeKeyIsPrimaryHashKey()
			throws ParseException {

		setupCommonMocksForThisRepositoryMethod(mockPlaylistEntityMetadata, mockDynamoDBPlaylistQueryMethod,
				Playlist.class, "findByDisplayNameAndUserNameAfter", 2, "userName", "playlistName");
		lenient().when(mockDynamoDBPlaylistQueryMethod.isCollectionQuery()).thenReturn(true);

		lenient().when(mockPlaylistEntityMetadata.isGlobalIndexHashKeyProperty("displayName")).thenReturn(true);
		lenient().when(mockPlaylistEntityMetadata.isGlobalIndexRangeKeyProperty("userName")).thenReturn(true);

		Map<String, String[]> indexRangeKeySecondaryIndexNames = new HashMap<String, String[]>();
		indexRangeKeySecondaryIndexNames.put("displayName", new String[]{"DisplayName-UserName-index"});
		indexRangeKeySecondaryIndexNames.put("userName", new String[]{"DisplayName-UserName-index"});

		lenient().when(mockPlaylistEntityMetadata.getGlobalSecondaryIndexNamesByPropertyName())
				.thenReturn(indexRangeKeySecondaryIndexNames);

		lenient().when(mockPlaylistEntityMetadata.getDynamoDBTableName()).thenReturn("playlist");

		// Mock out specific QueryRequestMapper behavior expected by this method
		lenient().when(mockPlaylistQueryResults.items().stream().toList().get(0)).thenReturn(mockPlaylist);
		lenient().when(mockPlaylistQueryResults.items().stream().toList().size()).thenReturn(1);
		lenient().when(mockDynamoDBOperations.query(playlistClassCaptor.capture(), queryEnhancedRequestCaptor.capture(), null))
				.thenReturn(mockPlaylistQueryResults);

		// Execute the query
		Object[] parameters = new Object[]{"SomeDisplayName", "SomeUserName"};
		Object o = partTreeDynamoDBQuery.execute(parameters);

		// Assert that we obtain the expected results
		assertEquals(mockPlaylistQueryResults, o);
		assertEquals(1, mockPlaylistQueryResults.items().stream().toList().size());
		assertEquals(mockPlaylist, mockPlaylistQueryResults.items().stream().toList().get(0));

		// Assert that we scanned DynamoDB for the correct class
		assertEquals(playlistClassCaptor.getValue(), Playlist.class);

		String indexName = queryResultCaptor.getValue().indexName();
		assertNotNull(indexName);
		assertEquals("DisplayName-UserName-index", indexName);

		assertEquals("playlist", queryResultCaptor.getValue().tableName());

		// Assert that we have the correct conditions
		// TODO

//		assertEquals(2, queryResultCaptor.getValue().getKeyConditions().size());
//		Condition globalRangeKeyCondition = queryResultCaptor.getValue().getKeyConditions().get("displayName");
//		assertEquals(ComparisonOperator.EQ, globalRangeKeyCondition.comparisonOperator());
//		assertEquals(1, globalRangeKeyCondition.attributeValueList().size());
//		assertEquals("SomeDisplayName", globalRangeKeyCondition.attributeValueList().get(0).s());
//		Condition globalHashKeyCondition = queryResultCaptor.getValue().getKeyConditions().get("userName");
//		assertEquals(ComparisonOperator.GT, globalHashKeyCondition.comparisonOperator());
//		assertEquals(1, globalHashKeyCondition.attributeValueList().size());
//		assertEquals("SomeUserName", globalHashKeyCondition.attributeValueList().get(0).s());
//
//		// Assert that all other attribute value types other than String type
//		// are null
//		assertNull(globalRangeKeyCondition.attributeValueList().get(0).ss());
//		assertNull(globalRangeKeyCondition.attributeValueList().get(0).n());
//		assertNull(globalRangeKeyCondition.attributeValueList().get(0).ns());
//		assertNull(globalRangeKeyCondition.attributeValueList().get(0).b());
//		assertNull(globalRangeKeyCondition.attributeValueList().get(0).bs());
//
//		assertNull(globalHashKeyCondition.attributeValueList().get(0).ss());
//		assertNull(globalHashKeyCondition.attributeValueList().get(0).n());
//		assertNull(globalHashKeyCondition.attributeValueList().get(0).ns());
//		assertNull(globalHashKeyCondition.attributeValueList().get(0).b());
//		assertNull(globalHashKeyCondition.attributeValueList().get(0).bs());

		// Verify that the expected DynamoDBOperations method was called
		Mockito.verify(mockDynamoDBOperations).query(playlistClassCaptor.getValue(), queryEnhancedRequestCaptor.getValue(), null);
	}

	// Global Secondary Index Test 4i
	@Test
	public void testExecute_WhenFinderMethodIsFindingEntityByGlobalSecondaryHashAndRangeIndexHashAndRangeKeyNonEqualityCondition_WhereBothSecondaryHashKeyAndSecondaryIndexRangeKeyMembersOfMultipleIndexes()
			throws ParseException {

		setupCommonMocksForThisRepositoryMethod(mockUserEntityMetadata, mockDynamoDBUserQueryMethod, User.class,
				"findByNameAndPostCodeAfter", 2, "id", null);
		lenient().when(mockDynamoDBUserQueryMethod.isCollectionQuery()).thenReturn(true);

		lenient().when(mockUserEntityMetadata.isGlobalIndexHashKeyProperty("name")).thenReturn(true);
		lenient().when(mockUserEntityMetadata.isGlobalIndexRangeKeyProperty("postCode")).thenReturn(true);

		Map<String, String[]> indexRangeKeySecondaryIndexNames = new HashMap<String, String[]>();
		indexRangeKeySecondaryIndexNames.put("name", new String[]{"Name-PostCode-index", "Name-JoinYear-index"});
		indexRangeKeySecondaryIndexNames.put("postCode", new String[]{"Name-PostCode-index", "Id-PostCode-index"});

		lenient().when(mockUserEntityMetadata.getGlobalSecondaryIndexNamesByPropertyName())
				.thenReturn(indexRangeKeySecondaryIndexNames);

		lenient().when(mockUserEntityMetadata.getDynamoDBTableName()).thenReturn("user");

		// Mock out specific QueryRequestMapper behavior expected by this method
		lenient().when(mockUserQueryResults.items().stream().toList().get(0)).thenReturn(mockUser);
		lenient().when(mockUserQueryResults.items().stream().toList().size()).thenReturn(1);
		lenient().when(mockDynamoDBOperations.query(userClassCaptor.capture(), queryEnhancedRequestCaptor.capture(), null))
				.thenReturn(mockUserQueryResults);

		// Execute the query
		Object[] parameters = new Object[]{"SomeName", "SomePostCode"};
		Object o = partTreeDynamoDBQuery.execute(parameters);

		// Assert that we obtain the expected results
		assertEquals(mockUserQueryResults, o);
		assertEquals(1, mockUserQueryResults.items().stream().toList().size());
		assertEquals(mockUser, mockUserQueryResults.items().stream().toList().get(0));

		// Assert that we scanned DynamoDB for the correct class
		assertEquals(userClassCaptor.getValue(), User.class);

		String indexName = queryResultCaptor.getValue().indexName();
		assertNotNull(indexName);
		assertEquals("Name-PostCode-index", indexName);

		assertEquals("user", queryResultCaptor.getValue().tableName());

		// Assert that we have the correct conditions
		// TODO

//		assertEquals(2, queryResultCaptor.getValue().getKeyConditions().size());
//		Condition globalRangeKeyCondition = queryResultCaptor.getValue().getKeyConditions().get("name");
//		assertEquals(ComparisonOperator.EQ, globalRangeKeyCondition.comparisonOperator());
//		assertEquals(1, globalRangeKeyCondition.attributeValueList().size());
//		assertEquals("SomeName", globalRangeKeyCondition.attributeValueList().get(0).s());
//		Condition globalHashKeyCondition = queryResultCaptor.getValue().getKeyConditions().get("postCode");
//		assertEquals(ComparisonOperator.GT, globalHashKeyCondition.comparisonOperator());
//		assertEquals(1, globalHashKeyCondition.attributeValueList().size());
//		assertEquals("SomePostCode", globalHashKeyCondition.attributeValueList().get(0).s());
//
//		// Assert that all other attribute value types other than String type
//		// are null
//		assertNull(globalRangeKeyCondition.attributeValueList().get(0).ss());
//		assertNull(globalRangeKeyCondition.attributeValueList().get(0).n());
//		assertNull(globalRangeKeyCondition.attributeValueList().get(0).ns());
//		assertNull(globalRangeKeyCondition.attributeValueList().get(0).b());
//		assertNull(globalRangeKeyCondition.attributeValueList().get(0).bs());
//
//		assertNull(globalHashKeyCondition.attributeValueList().get(0).ss());
//		assertNull(globalHashKeyCondition.attributeValueList().get(0).n());
//		assertNull(globalHashKeyCondition.attributeValueList().get(0).ns());
//		assertNull(globalHashKeyCondition.attributeValueList().get(0).b());
//		assertNull(globalHashKeyCondition.attributeValueList().get(0).bs());

		// Verify that the expected DynamoDBOperations method was called
		Mockito.verify(mockDynamoDBOperations).query(userClassCaptor.getValue(), queryEnhancedRequestCaptor.getValue(), null);
	}
	// Global Secondary Index Test 4j
	@Test
	public void testExecute_WhenFinderMethodIsFindingEntityByGlobalSecondaryHashAndRangeIndexHashCondition_WhereSecondaryHashKeyMemberOfMultipleIndexes()
			throws ParseException {

		setupCommonMocksForThisRepositoryMethod(mockUserEntityMetadata, mockDynamoDBUserQueryMethod, User.class,
				"findByName", 1, "id", null);
		Mockito.when(mockDynamoDBUserQueryMethod.isCollectionQuery()).thenReturn(true);

		Mockito.when(mockUserEntityMetadata.isGlobalIndexHashKeyProperty("name")).thenReturn(true);
		// Mockito.when(mockUserEntityMetadata.isGlobalIndexRangeKeyProperty("postCode")).thenReturn(true);
		// Mockito.when(mockUserEntityMetadata.isGlobalIndexRangeKeyProperty("joinYear")).thenReturn(true);
		// Mockito.when(mockUserEntityMetadata.isGlobalIndexHashKeyProperty("id")).thenReturn(true);

		Map<String, String[]> indexRangeKeySecondaryIndexNames = new HashMap<String, String[]>();
		indexRangeKeySecondaryIndexNames.put("name", new String[]{"Name-PostCode-index", "Name-JoinYear-index"});
		indexRangeKeySecondaryIndexNames.put("postCode", new String[]{"Name-PostCode-index", "Id-PostCode-index"});
		indexRangeKeySecondaryIndexNames.put("joinYear", new String[]{"Name-JoinYear-index"});
		indexRangeKeySecondaryIndexNames.put("id", new String[]{"Id-PostCode-index"});

		Mockito.when(mockUserEntityMetadata.getGlobalSecondaryIndexNamesByPropertyName())
				.thenReturn(indexRangeKeySecondaryIndexNames);

		Mockito.when(mockUserEntityMetadata.getDynamoDBTableName()).thenReturn("user");

		// Mock out specific QueryRequestMapper behavior expected by this method
		ArgumentCaptor<QueryRequest> queryResultCaptor = ArgumentCaptor.forClass(QueryRequest.class);
		Mockito.when(mockUserQueryResults.items().stream().toList().get(0)).thenReturn(mockUser);
		Mockito.when(mockUserQueryResults.items().stream().toList().size()).thenReturn(1);
		Mockito.when(mockDynamoDBOperations.query(userClassCaptor.capture(), queryEnhancedRequestCaptor.capture(), null))
				.thenReturn(mockUserQueryResults);

		// Execute the query
		Object[] parameters = new Object[]{"SomeName"};
		Object o = partTreeDynamoDBQuery.execute(parameters);

		// Assert that we obtain the expected results
		assertEquals(mockUserQueryResults, o);
		assertEquals(1, mockUserQueryResults.items().stream().toList().size());
		assertEquals(mockUser, mockUserQueryResults.items().stream().toList().get(0));

		// Assert that we scanned DynamoDB for the correct class
		assertEquals(userClassCaptor.getValue(), User.class);

		String indexName = queryResultCaptor.getValue().indexName();
		assertNotNull(indexName);
		assertEquals("Name-PostCode-index", indexName);

		assertEquals("user", queryResultCaptor.getValue().tableName());

		// Assert that we have the correct conditions
		// TODO
//		Condition globalHashKeyCondition = queryResultCaptor.getValue().getKeyConditions().get("name");
//		assertEquals(ComparisonOperator.EQ, globalHashKeyCondition.comparisonOperator());
//		assertEquals(1, globalHashKeyCondition.attributeValueList().size());
//		assertEquals("SomeName", globalHashKeyCondition.attributeValueList().get(0).s());
//
//		assertNull(globalHashKeyCondition.attributeValueList().get(0).ss());
//		assertNull(globalHashKeyCondition.attributeValueList().get(0).n());
//		assertNull(globalHashKeyCondition.attributeValueList().get(0).ns());
//		assertNull(globalHashKeyCondition.attributeValueList().get(0).b());
//		assertNull(globalHashKeyCondition.attributeValueList().get(0).bs());

		// Verify that the expected DynamoDBOperations method was called
		Mockito.verify(mockDynamoDBOperations).query(userClassCaptor.getValue(), queryEnhancedRequestCaptor.getValue(), null);
	}

	// Global Secondary Index Test 4k
	@Test
	public void testExecute_WhenFinderMethodIsFindingEntityByGlobalSecondaryHashAndRangeIndexHashCondition_WhereSecondaryHashKeyMemberOfMultipleIndexes_WhereOneIndexIsExactMatch()
			throws ParseException {

		setupCommonMocksForThisRepositoryMethod(mockUserEntityMetadata, mockDynamoDBUserQueryMethod, User.class,
				"findByName", 1, "id", null);
		Mockito.when(mockDynamoDBUserQueryMethod.isCollectionQuery()).thenReturn(true);

		Mockito.when(mockUserEntityMetadata.isGlobalIndexHashKeyProperty("name")).thenReturn(true);
		// Mockito.when(mockUserEntityMetadata.isGlobalIndexRangeKeyProperty("postCode")).thenReturn(true);
		// Mockito.when(mockUserEntityMetadata.isGlobalIndexRangeKeyProperty("joinYear")).thenReturn(true);
		// Mockito.when(mockUserEntityMetadata.isGlobalIndexHashKeyProperty("id")).thenReturn(true);

		Map<String, String[]> indexRangeKeySecondaryIndexNames = new HashMap<String, String[]>();
		indexRangeKeySecondaryIndexNames.put("name",
				new String[]{"Name-PostCode-index", "Name-index", "Name-JoinYear-index"});
		indexRangeKeySecondaryIndexNames.put("postCode", new String[]{"Name-PostCode-index", "Id-PostCode-index"});
		indexRangeKeySecondaryIndexNames.put("joinYear", new String[]{"Name-JoinYear-index"});
		indexRangeKeySecondaryIndexNames.put("id", new String[]{"Id-PostCode-index"});

		Mockito.when(mockUserEntityMetadata.getGlobalSecondaryIndexNamesByPropertyName())
				.thenReturn(indexRangeKeySecondaryIndexNames);

		Mockito.when(mockUserEntityMetadata.getDynamoDBTableName()).thenReturn("user");

		// Mock out specific QueryRequestMapper behavior expected by this method
		Mockito.when(mockUserQueryResults.items().stream().toList().get(0)).thenReturn(mockUser);
		Mockito.when(mockUserQueryResults.items().stream().toList().size()).thenReturn(1);
		Mockito.when(mockDynamoDBOperations.query(userClassCaptor.capture(), queryEnhancedRequestCaptor.capture(), null))
				.thenReturn(mockUserQueryResults);

		// Execute the query
		Object[] parameters = new Object[]{"SomeName"};
		Object o = partTreeDynamoDBQuery.execute(parameters);

		// Assert that we obtain the expected results
		assertEquals(mockUserQueryResults, o);
		assertEquals(1, mockUserQueryResults.items().stream().toList().size());
		assertEquals(mockUser, mockUserQueryResults.items().stream().toList().get(0));

		// Assert that we scanned DynamoDB for the correct class
		assertEquals(userClassCaptor.getValue(), User.class);

		String indexName = queryResultCaptor.getValue().indexName();
		assertNotNull(indexName);
		assertEquals("Name-index", indexName);

		assertEquals("user", queryResultCaptor.getValue().tableName());

		// Assert that we have the correct conditions
		// TODO
//		Condition globalHashKeyCondition = queryResultCaptor.getValue().getKeyConditions().get("name");
//		assertEquals(ComparisonOperator.EQ, globalHashKeyCondition.comparisonOperator());
//		assertEquals(1, globalHashKeyCondition.attributeValueList().size());
//		assertEquals("SomeName", globalHashKeyCondition.attributeValueList().get(0).s());
//
//		assertNull(globalHashKeyCondition.attributeValueList().get(0).ss());
//		assertNull(globalHashKeyCondition.attributeValueList().get(0).n());
//		assertNull(globalHashKeyCondition.attributeValueList().get(0).ns());
//		assertNull(globalHashKeyCondition.attributeValueList().get(0).b());
//		assertNull(globalHashKeyCondition.attributeValueList().get(0).bs());

		// Verify that the expected DynamoDBOperations method was called
		Mockito.verify(mockDynamoDBOperations).query(userClassCaptor.getValue(), queryEnhancedRequestCaptor.getValue(), null);
	}

	@Test
	public void testExecute_WhenFinderMethodIsFindingEntityList_WithSingleStringParameter_WithCustomMarshaller_WhenNotFindingByHashKey()
			throws ParseException {

		String postcode = "N1";

		setupCommonMocksForThisRepositoryMethod(mockUserEntityMetadata, mockDynamoDBUserQueryMethod, User.class,
				"findByPostCode", 1, "id", null);
		Mockito.when(mockDynamoDBUserQueryMethod.isCollectionQuery()).thenReturn(true);
		CaseChangingMarshaller marshaller = new CaseChangingMarshaller();

		Mockito.when(mockUserScanResults.items().stream().toList().get(0)).thenReturn(mockUser);
		Mockito.when(mockUserScanResults.items().stream().toList().size()).thenReturn(1);
		Mockito.when(mockDynamoDBOperations.scan(userClassCaptor.capture(), scanEnhancedCaptor.capture(), null))
				.thenReturn(mockUserScanResults);

		// Execute the query
		Object[] parameters = new Object[]{postcode};
		Object o = partTreeDynamoDBQuery.execute(parameters);

		// Assert that we obtain the expected list of results
		assertEquals(o, mockUserScanResults);

		// Assert that the list of results contains the correct elements
		assertEquals(1, mockUserScanResults.items().stream().toList().size());
		assertEquals(mockUser, mockUserScanResults.items().stream().toList().get(0));

		// Assert that we scanned DynamoDB for the correct class
		assertEquals(userClassCaptor.getValue(), User.class);

		// Assert that we have only one filter condition, for the name of the
		// property
		// TODO
//		Map<String, Condition> filterConditions = scanCaptor.getValue().getScanFilter();
//		assertEquals(1, filterConditions.size());
//		Condition filterCondition = filterConditions.get("postCode");
//		assertNotNull(filterCondition);
//
//		assertEquals(ComparisonOperator.EQ, filterCondition.comparisonOperator());
//
//		// Assert we only have one attribute value for this filter condition
//		assertEquals(1, filterCondition.attributeValueList().size());
//
//		// Assert that there the attribute value type for this attribute value
//		// is String,
//		// and its value is the parameter expected
//		assertEquals("n1", filterCondition.attributeValueList().get(0).s());
//
//		// Assert that all other attribute value types other than String type
//		// are null
//		assertNull(filterCondition.attributeValueList().get(0).ss());
//		assertNull(filterCondition.attributeValueList().get(0).n());
//		assertNull(filterCondition.attributeValueList().get(0).ns());
//		assertNull(filterCondition.attributeValueList().get(0).b());
//		assertNull(filterCondition.attributeValueList().get(0).bs());

		// Verify that the expected DynamoDBOperations method was called
		Mockito.verify(mockDynamoDBOperations).scan(userClassCaptor.getValue(), scanEnhancedCaptor.getValue(), null);
	}

	@Test
	public void testExecute_WhenFinderMethodIsFindingEntityList_WithSingleIntegerParameter_WhenNotFindingByHashKey() {
		int numberOfPlaylists = 5;

		setupCommonMocksForThisRepositoryMethod(mockUserEntityMetadata, mockDynamoDBUserQueryMethod, User.class,
				"findByNumberOfPlaylists", 1, "id", null);
		Mockito.when(mockDynamoDBUserQueryMethod.isCollectionQuery()).thenReturn(true);

		Mockito.when(mockUserScanResults.items().stream().toList().get(0)).thenReturn(mockUser);
		Mockito.when(mockUserScanResults.items().stream().toList().size()).thenReturn(1);
		Mockito.when(mockDynamoDBOperations.scan(userClassCaptor.capture(), scanEnhancedCaptor.capture(), null))
				.thenReturn(mockUserScanResults);

		// Execute the query
		Object[] parameters = new Object[]{numberOfPlaylists};
		Object o = partTreeDynamoDBQuery.execute(parameters);

		// Assert that we obtain the expected list of results
		assertEquals(o, mockUserScanResults);

		// Assert that the list of results contains the correct elements
		assertEquals(1, mockUserScanResults.items().stream().toList().size());
		assertEquals(mockUser, mockUserScanResults.items().stream().toList().get(0));

		// Assert that we scanned DynamoDB for the correct class
		assertEquals(userClassCaptor.getValue(), User.class);

		// Assert that we have only one filter condition, for the name of the
		// property
		// TODO
//		Map<String, Condition> filterConditions = scanCaptor.getValue().getScanFilter();
//		assertEquals(1, filterConditions.size());
//		Condition filterCondition = filterConditions.get("numberOfPlaylists");
//		assertNotNull(filterCondition);
//
//		assertEquals(ComparisonOperator.EQ, filterCondition.comparisonOperator());
//
//		// Assert we only have one attribute value for this filter condition
//		assertEquals(1, filterCondition.attributeValueList().size());
//
//		// Assert that there the attribute value type for this attribute value
//		// is Number,
//		// and its Dynamo value is the number as a string
//		assertEquals("5", filterCondition.attributeValueList().get(0).n());
//
//		// Assert that all other attribute value types other than String type
//		// are null
//		assertNull(filterCondition.attributeValueList().get(0).ns());
//		assertNull(filterCondition.attributeValueList().get(0).s());
//		assertNull(filterCondition.attributeValueList().get(0).ss());
//		assertNull(filterCondition.attributeValueList().get(0).b());
//		assertNull(filterCondition.attributeValueList().get(0).bs());

		// Verify that the expected DynamoDBOperations method was called
		Mockito.verify(mockDynamoDBOperations).scan(userClassCaptor.getValue(), scanEnhancedCaptor.getValue(), null);
	}

	@Test
	public void testExecute_WhenFinderMethodIsFindingEntityList_WithSingleStringParameter_WhenNotFindingByNotHashKey() {
		setupCommonMocksForThisRepositoryMethod(mockUserEntityMetadata, mockDynamoDBUserQueryMethod, User.class,
				"findByIdNot", 1, "id", null);
		Mockito.when(mockDynamoDBUserQueryMethod.isCollectionQuery()).thenReturn(true);

		Mockito.when(mockUserScanResults.items().stream().toList().get(0)).thenReturn(mockUser);
		Mockito.when(mockUserScanResults.items().stream().toList().size()).thenReturn(1);
		Mockito.when(mockDynamoDBOperations.scan(userClassCaptor.capture(), scanEnhancedCaptor.capture(), null))
				.thenReturn(mockUserScanResults);

		// Execute the query
		Object[] parameters = new Object[]{"someId"};
		Object o = partTreeDynamoDBQuery.execute(parameters);

		// Assert that we obtain the expected list of results
		assertEquals(o, mockUserScanResults);

		// Assert that the list of results contains the correct elements
		assertEquals(1, mockUserScanResults.items().stream().toList().size());
		assertEquals(mockUser, mockUserScanResults.items().stream().toList().get(0));

		// Assert that we scanned DynamoDB for the correct class
		assertEquals(userClassCaptor.getValue(), User.class);

		// Assert that we have only one filter condition, for the name of the
		// property
		// TODO
//		Map<String, Condition> filterConditions = scanCaptor.getValue().getScanFilter();
//		assertEquals(1, filterConditions.size());
//		Condition filterCondition = filterConditions.get("id");
//		assertNotNull(filterCondition);
//
//		assertEquals(ComparisonOperator.NE, filterCondition.comparisonOperator());
//
//		// Assert we only have one attribute value for this filter condition
//		assertEquals(1, filterCondition.attributeValueList().size());
//
//		// Assert that there the attribute value type for this attribute value
//		// is String,
//		// and its value is the parameter expected
//		assertEquals("someId", filterCondition.attributeValueList().get(0).s());
//
//		// Assert that all other attribute value types other than String type
//		// are null
//		assertNull(filterCondition.attributeValueList().get(0).ss());
//		assertNull(filterCondition.attributeValueList().get(0).n());
//		assertNull(filterCondition.attributeValueList().get(0).ns());
//		assertNull(filterCondition.attributeValueList().get(0).b());
//		assertNull(filterCondition.attributeValueList().get(0).bs());

		// Verify that the expected DynamoDBOperations method was called
		Mockito.verify(mockDynamoDBOperations).scan(userClassCaptor.getValue(), scanEnhancedCaptor.getValue(), null);
	}

	@Test
	public void testExecute_WhenFinderMethodIsFindingEntityList_WithSingleStringParameter_WhenNotFindingByNotAProperty() {
		setupCommonMocksForThisRepositoryMethod(mockUserEntityMetadata, mockDynamoDBUserQueryMethod, User.class,
				"findByNameNot", 1, "id", null);
		Mockito.when(mockDynamoDBUserQueryMethod.isCollectionQuery()).thenReturn(true);

		Mockito.when(mockUserScanResults.items().stream().toList().get(0)).thenReturn(mockUser);
		Mockito.when(mockUserScanResults.items().stream().toList().size()).thenReturn(1);
		Mockito.when(mockDynamoDBOperations.scan(userClassCaptor.capture(), scanEnhancedCaptor.capture(), null))
				.thenReturn(mockUserScanResults);

		// Execute the query
		Object[] parameters = new Object[]{"someName"};
		Object o = partTreeDynamoDBQuery.execute(parameters);

		// Assert that we obtain the expected list of results
		assertEquals(o, mockUserScanResults);

		// Assert that the list of results contains the correct elements
		assertEquals(1, mockUserScanResults.items().stream().toList().size());
		assertEquals(mockUser, mockUserScanResults.items().stream().toList().get(0));

		// Assert that we scanned DynamoDB for the correct class
		assertEquals(userClassCaptor.getValue(), User.class);

		// Assert that we have only one filter condition, for the name of the
		// property
		// TODO
//		Map<String, Condition> filterConditions = scanCaptor.getValue().getScanFilter();
//		assertEquals(1, filterConditions.size());
//		Condition filterCondition = filterConditions.get("name");
//		assertNotNull(filterCondition);
//
//		assertEquals(ComparisonOperator.NE, filterCondition.comparisonOperator());
//
//		// Assert we only have one attribute value for this filter condition
//		assertEquals(1, filterCondition.attributeValueList().size());
//
//		// Assert that there the attribute value type for this attribute value
//		// is String,
//		// and its value is the parameter expected
//		assertEquals("someName", filterCondition.attributeValueList().get(0).s());
//
//		// Assert that all other attribute value types other than String type
//		// are null
//		assertNull(filterCondition.attributeValueList().get(0).ss());
//		assertNull(filterCondition.attributeValueList().get(0).n());
//		assertNull(filterCondition.attributeValueList().get(0).ns());
//		assertNull(filterCondition.attributeValueList().get(0).b());
//		assertNull(filterCondition.attributeValueList().get(0).bs());

		// Verify that the expected DynamoDBOperations method was called
		Mockito.verify(mockDynamoDBOperations).scan(userClassCaptor.getValue(), scanEnhancedCaptor.getValue(), null);
	}

	@Test
	public void testExecute_WhenExistsQueryFindsNoEntity() {
		setupCommonMocksForThisRepositoryMethod(mockUserEntityMetadata, mockDynamoDBUserQueryMethod, User.class,
				"existsByName", 1, "id", null);
		Mockito.when(mockUserEntityMetadata.getOverriddenAttributeName("name")).thenReturn(Optional.of("Name"));

		// Mockito.when(mockUserScanResults.size()).thenReturn(0);
		Mockito.when(mockUserScanResults.stream().toList().isEmpty()).thenReturn(true);
		Mockito.when(mockDynamoDBOperations.scan(userClassCaptor.capture(), scanEnhancedCaptor.capture(), null))
				.thenReturn(mockUserScanResults);

		// Execute the query
		Object[] parameters = new Object[]{"someName"};
		Object o = partTreeDynamoDBQuery.execute(parameters);

		// Assert that we obtain the expected single result
		assertEquals(false, o);

		// Assert that we scanned DynamoDB for the correct class
		assertEquals(User.class, userClassCaptor.getValue());

		// Assert that we have only one filter condition, for the name of the
		// property
		// TODO
//		Map<String, Condition> filterConditions = scanCaptor.getValue().getScanFilter();
//		assertEquals(1, filterConditions.size());
//		Condition filterCondition = filterConditions.get("Name");
//		assertNotNull(filterCondition);
//
//		assertEquals(ComparisonOperator.EQ, filterCondition.comparisonOperator());
//
//		// Assert we only have one attribute value for this filter condition
//		assertEquals(1, filterCondition.attributeValueList().size());
//
//		// Assert that there the attribute value type for this attribute value
//		// is String,
//		// and its value is the parameter expected
//		assertEquals("someName", filterCondition.attributeValueList().get(0).s());
//
//		// Assert that all other attribute value types other than String type
//		// are null
//		assertNull(filterCondition.attributeValueList().get(0).ss());
//		assertNull(filterCondition.attributeValueList().get(0).n());
//		assertNull(filterCondition.attributeValueList().get(0).ns());
//		assertNull(filterCondition.attributeValueList().get(0).b());
//		assertNull(filterCondition.attributeValueList().get(0).bs());

		// Verify that the expected DynamoDBOperations method was called
		Mockito.verify(mockDynamoDBOperations).scan(userClassCaptor.getValue(), scanEnhancedCaptor.getValue(), null);
	}

	@Test
	public void testExecute_WhenExistsQueryFindsOneEntity() {
		setupCommonMocksForThisRepositoryMethod(mockUserEntityMetadata, mockDynamoDBUserQueryMethod, User.class,
				"existsByName", 1, "id", null);
		Mockito.when(mockUserEntityMetadata.getOverriddenAttributeName("name")).thenReturn(Optional.of("Name"));

		// Mockito.when(mockUserScanResults.get(0)).thenReturn(mockUser);
		// Mockito.when(mockUserScanResults.size()).thenReturn(1);
		Mockito.when(mockUserScanResults.items().stream().toList().isEmpty()).thenReturn(false);
		Mockito.when(mockDynamoDBOperations.scan(userClassCaptor.capture(), scanEnhancedCaptor.capture(), null))
				.thenReturn(mockUserScanResults);

		// Execute the query
		Object[] parameters = new Object[]{"someName"};
		Object o = partTreeDynamoDBQuery.execute(parameters);

		// Assert that we obtain the expected single result
		assertEquals(true, o);

		// Assert that we scanned DynamoDB for the correct class
		assertEquals(userClassCaptor.getValue(), User.class);

		// Assert that we have only one filter condition, for the name of the
		// property
		// TODO
//		Map<String, Condition> filterConditions = scanCaptor.getValue().getScanFilter();
//		assertEquals(1, filterConditions.size());
//		Condition filterCondition = filterConditions.get("Name");
//		assertNotNull(filterCondition);
//
//		assertEquals(ComparisonOperator.EQ, filterCondition.comparisonOperator());
//
//		// Assert we only have one attribute value for this filter condition
//		assertEquals(1, filterCondition.attributeValueList().size());
//
//		// Assert that there the attribute value type for this attribute value
//		// is String,
//		// and its value is the parameter expected
//		assertEquals("someName", filterCondition.attributeValueList().get(0).s());
//
//		// Assert that all other attribute value types other than String type
//		// are null
//		assertNull(filterCondition.attributeValueList().get(0).ss());
//		assertNull(filterCondition.attributeValueList().get(0).n());
//		assertNull(filterCondition.attributeValueList().get(0).ns());
//		assertNull(filterCondition.attributeValueList().get(0).b());
//		assertNull(filterCondition.attributeValueList().get(0).bs());

		// Verify that the expected DynamoDBOperations method was called
		Mockito.verify(mockDynamoDBOperations).scan(userClassCaptor.getValue(), scanEnhancedCaptor.getValue(), null);
	}

	@Test
	public void testExecute_WhenExistsQueryFindsMultipleEntities() {
		setupCommonMocksForThisRepositoryMethod(mockUserEntityMetadata, mockDynamoDBUserQueryMethod, User.class,
				"existsByName", 1, "id", null);
		Mockito.when(mockUserEntityMetadata.getOverriddenAttributeName("name")).thenReturn(Optional.of("Name"));

		// Mockito.when(mockUserScanResults.get(0)).thenReturn(mockUser);
		// Mockito.when(mockUserScanResults.get(1)).thenReturn(mockUser);
		// Mockito.when(mockUserScanResults.size()).thenReturn(2);
		Mockito.when(mockUserScanResults.stream().toList().isEmpty()).thenReturn(false);
		Mockito.when(mockDynamoDBOperations.scan(userClassCaptor.capture(), scanEnhancedCaptor.capture(), null))
				.thenReturn(mockUserScanResults);

		// Execute the query
		Object[] parameters = new Object[]{"someName"};
		Object o = partTreeDynamoDBQuery.execute(parameters);

		// Assert that we obtain the expected single result
		assertEquals(true, o);

		// Assert that we scanned DynamoDB for the correct class
		assertEquals(userClassCaptor.getValue(), User.class);

		// Assert that we have only one filter condition, for the name of the
		// property
		// TODO
//		Map<String, Condition> filterConditions = scanCaptor.getValue().getScanFilter();
//		assertEquals(1, filterConditions.size());
//		Condition filterCondition = filterConditions.get("Name");
//		assertNotNull(filterCondition);
//
//		assertEquals(ComparisonOperator.EQ, filterCondition.comparisonOperator());
//
//		// Assert we only have one attribute value for this filter condition
//		assertEquals(1, filterCondition.attributeValueList().size());
//
//		// Assert that there the attribute value type for this attribute value
//		// is String,
//		// and its value is the parameter expected
//		assertEquals("someName", filterCondition.attributeValueList().get(0).s());
//
//		// Assert that all other attribute value types other than String type
//		// are null
//		assertNull(filterCondition.attributeValueList().get(0).ss());
//		assertNull(filterCondition.attributeValueList().get(0).n());
//		assertNull(filterCondition.attributeValueList().get(0).ns());
//		assertNull(filterCondition.attributeValueList().get(0).b());
//		assertNull(filterCondition.attributeValueList().get(0).bs());

		// Verify that the expected DynamoDBOperations method was called
		Mockito.verify(mockDynamoDBOperations).scan(userClassCaptor.getValue(), scanEnhancedCaptor.getValue(), null);
	}

	@Test
	public void testExecute_WhenExistsWithLimitQueryFindsNoEntity() {
		setupCommonMocksForThisRepositoryMethod(mockUserEntityMetadata, mockDynamoDBUserQueryMethod, User.class,
				"existsTop1ByName", 1, "id", null);
		Mockito.when(mockUserEntityMetadata.getOverriddenAttributeName("name")).thenReturn(Optional.of("Name"));

		// Mockito.when(mockUserScanResults.size()).thenReturn(0);
		Mockito.when(mockUserScanResults.stream().toList().isEmpty()).thenReturn(true);
		Mockito.when(mockDynamoDBOperations.scan(userClassCaptor.capture(), scanEnhancedCaptor.capture(), null))
				.thenReturn(mockUserScanResults);

		// Execute the query
		Object[] parameters = new Object[]{"someName"};
		Object o = partTreeDynamoDBQuery.execute(parameters);

		// Assert that we obtain the expected single result
		assertEquals(false, o);

		// Assert that we scanned DynamoDB for the correct class
		assertEquals(User.class, userClassCaptor.getValue());

		// Assert that we have only one filter condition, for the name of the
		// property
		// TODO
//		Map<String, Condition> filterConditions = scanCaptor.getValue().getScanFilter();
//		assertEquals(1, filterConditions.size());
//		Condition filterCondition = filterConditions.get("Name");
//		assertNotNull(filterCondition);
//
//		assertEquals(ComparisonOperator.EQ, filterCondition.comparisonOperator());
//
//		// Assert we only have one attribute value for this filter condition
//		assertEquals(1, filterCondition.attributeValueList().size());
//
//		// Assert that there the attribute value type for this attribute value
//		// is String,
//		// and its value is the parameter expected
//		assertEquals("someName", filterCondition.attributeValueList().get(0).s());
//
//		// Assert that all other attribute value types other than String type
//		// are null
//		assertNull(filterCondition.attributeValueList().get(0).ss());
//		assertNull(filterCondition.attributeValueList().get(0).n());
//		assertNull(filterCondition.attributeValueList().get(0).ns());
//		assertNull(filterCondition.attributeValueList().get(0).b());
//		assertNull(filterCondition.attributeValueList().get(0).bs());

		// Verify that the expected DynamoDBOperations method was called
		Mockito.verify(mockDynamoDBOperations).scan(userClassCaptor.getValue(), scanEnhancedCaptor.getValue(), null);
	}

	@Test
	public void testExecute_WhenExistsWithLimitQueryFindsOneEntity() {
		setupCommonMocksForThisRepositoryMethod(mockUserEntityMetadata, mockDynamoDBUserQueryMethod, User.class,
				"existsTop1ByName", 1, "id", null);
		Mockito.when(mockUserEntityMetadata.getOverriddenAttributeName("name")).thenReturn(Optional.of("Name"));

		// Mockito.when(mockUserScanResults.get(0)).thenReturn(mockUser);
		// Mockito.when(mockUserScanResults.size()).thenReturn(1);
		Mockito.when(mockUserScanResults.stream().toList().isEmpty()).thenReturn(false);
		Mockito.when(mockDynamoDBOperations.scan(userClassCaptor.capture(), scanEnhancedCaptor.capture(), null))
				.thenReturn(mockUserScanResults);

		// Execute the query
		Object[] parameters = new Object[]{"someName"};
		Object o = partTreeDynamoDBQuery.execute(parameters);

		// Assert that we obtain the expected single result
		assertEquals(true, o);

		// Assert that we scanned DynamoDB for the correct class
		assertEquals(userClassCaptor.getValue(), User.class);

		// Assert that we have only one filter condition, for the name of the
		// property
		// TODO
//		Map<String, Condition> filterConditions = scanCaptor.getValue().getScanFilter();
//		assertEquals(1, filterConditions.size());
//		Condition filterCondition = filterConditions.get("Name");
//		assertNotNull(filterCondition);
//
//		assertEquals(ComparisonOperator.EQ, filterCondition.comparisonOperator());
//
//		// Assert we only have one attribute value for this filter condition
//		assertEquals(1, filterCondition.attributeValueList().size());
//
//		// Assert that there the attribute value type for this attribute value
//		// is String,
//		// and its value is the parameter expected
//		assertEquals("someName", filterCondition.attributeValueList().get(0).s());
//
//		// Assert that all other attribute value types other than String type
//		// are null
//		assertNull(filterCondition.attributeValueList().get(0).ss());
//		assertNull(filterCondition.attributeValueList().get(0).n());
//		assertNull(filterCondition.attributeValueList().get(0).ns());
//		assertNull(filterCondition.attributeValueList().get(0).b());
//		assertNull(filterCondition.attributeValueList().get(0).bs());

		// Verify that the expected DynamoDBOperations method was called
		Mockito.verify(mockDynamoDBOperations).scan(userClassCaptor.getValue(), scanEnhancedCaptor.getValue(), null);
	}
}
