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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.socialsignin.spring.data.dynamodb.domain.sample.Playlist;
import org.socialsignin.spring.data.dynamodb.domain.sample.PlaylistId;
import org.socialsignin.spring.data.dynamodb.domain.sample.User;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
public class DynamoDBIdIsHashAndRangeKeyEntityInformationImplUnitTest {

	private DynamoDBIdIsHashAndRangeKeyEntityInformationImpl<Playlist, PlaylistId> dynamoDBPlaylistEntityInformation;

	@Mock
	private DynamoDBHashAndRangeKeyExtractingEntityMetadata<Playlist, PlaylistId> mockPlaylistEntityMetadata;

	@Mock
	private DynamoDBHashAndRangeKeyExtractingEntityMetadata<User, String> mockUserEntityMetadata;

	@Mock
	private Object mockHashKey;

	@Mock
	private Object mockRangeKey;

	@SuppressWarnings("rawtypes")
	@Mock
	private HashAndRangeKeyExtractor mockHashAndRangeKeyExtractor;

	@Mock
	private User mockUserPrototype;

	@Mock
	private Playlist mockPlaylistPrototype;

	@Mock
	private PlaylistId mockPlaylistId;

	@SuppressWarnings("unchecked")
	@BeforeEach
	public void setup() {
		lenient().when(mockPlaylistEntityMetadata.getHashAndRangeKeyExtractor(PlaylistId.class))
				.thenReturn(mockHashAndRangeKeyExtractor);
		lenient().when(mockHashAndRangeKeyExtractor.getHashKey(mockPlaylistId)).thenReturn(mockHashKey);
		lenient().when(mockHashAndRangeKeyExtractor.getRangeKey(mockPlaylistId)).thenReturn(mockRangeKey);

		lenient().when(mockPlaylistEntityMetadata.getHashKeyPropertyName()).thenReturn("playlistHashKeyPropertyName");
		lenient().when(mockPlaylistEntityMetadata.getHashKeyPropotypeEntityForHashKey("somePlaylistHashKey"))
				.thenReturn(mockPlaylistPrototype);
		lenient().when(mockPlaylistEntityMetadata.getOverriddenAttributeName("overriddenProperty"))
				.thenReturn(Optional.of("modifiedPropertyName"));

		lenient().when(mockPlaylistEntityMetadata.isHashKeyProperty("nonHashKeyProperty")).thenReturn(false);
		lenient().when(mockPlaylistEntityMetadata.isCompositeHashAndRangeKeyProperty("compositeIdProperty"))
				.thenReturn(true);
		lenient().when(mockPlaylistEntityMetadata.isCompositeHashAndRangeKeyProperty("nonCompositeIdProperty"))
				.thenReturn(false);

		dynamoDBPlaylistEntityInformation = new DynamoDBIdIsHashAndRangeKeyEntityInformationImpl<>(Playlist.class,
				mockPlaylistEntityMetadata);

	}

	@Test
	public void testConstruct_WhenEntityDoesNotHaveFieldAnnotatedWithId_ThrowsIllegalArgumentException() {
		assertThrows(IllegalArgumentException.class, () -> {
			new DynamoDBIdIsHashAndRangeKeyEntityInformationImpl<User, String>(User.class, mockUserEntityMetadata);
		});
	}

	@Test
	public void testGetId_WhenHashKeyMethodSameAsIdType_InvokesHashKeyMethod_AndReturnedIdIsAssignableToIdType_AndIsValueExpected() {
		Playlist playlist = new Playlist();
		playlist.setUserName("someUserName");
		playlist.setPlaylistName("somePlaylistName");
		PlaylistId id = dynamoDBPlaylistEntityInformation.getId(playlist);
		Assertions.assertNotNull(id);
		Assertions.assertEquals("someUserName", id.getUserName());
		Assertions.assertEquals("somePlaylistName", id.getPlaylistName());
	}

	@Test
	public void testGetJavaType_WhenEntityIsInstanceWithHashAndRangeKey_ReturnsEntityClass() {
		Assertions.assertEquals(Playlist.class, dynamoDBPlaylistEntityInformation.getJavaType());
	}

	@Test
	public void testGetIdType_WhenEntityIsInstanceWithHashAndRangeKey_ReturnsReturnTypeOfIdMethod() {
		Assertions.assertEquals(PlaylistId.class, dynamoDBPlaylistEntityInformation.getIdType());
	}

	// The following tests ensure that invarient methods such as those always
	// retuning constants, or
	// that delegate to metadata, behave the same irrespective of the setup of the
	// EntityInformation

	@Test
	public void testIsRangeKeyAware_ReturnsTrue() {
		Assertions.assertTrue(dynamoDBPlaylistEntityInformation.isRangeKeyAware());
	}

	@Test
	public void testGetHashKeyGivenId_WhenIdMethodFoundOnEntity_DelegatesToHashAndRangeKeyExtractorWithGivenIdValue() {
		Object hashKey = dynamoDBPlaylistEntityInformation.getHashKey(mockPlaylistId);
		Assertions.assertNotNull(hashKey);
		Assertions.assertEquals(mockHashKey, hashKey);
	}

	@Test
	public void testGetRangeKeyGivenId_WhenIdMethodFoundOnEntity_DelegatesToHashAndRangeKeyExtractorWithGivenIdValue() {
		Object rangeKey = dynamoDBPlaylistEntityInformation.getRangeKey(mockPlaylistId);
		Assertions.assertNotNull(rangeKey);
		Assertions.assertEquals(mockRangeKey, rangeKey);
	}

	@Test
	public void testGetPrototypeEntityForHashKey_DelegatesToDynamoDBEntityMetadata_IrrespectiveOfEntityInformationSetup() {
		Playlist playlistPrototypeEntity = new Playlist();
		Mockito.when(mockPlaylistEntityMetadata.getHashKeyPropotypeEntityForHashKey("someHashKey"))
				.thenReturn(playlistPrototypeEntity);

		Object returnedPlaylistEntity = dynamoDBPlaylistEntityInformation
				.getHashKeyPropotypeEntityForHashKey("someHashKey");

		Assertions.assertEquals(playlistPrototypeEntity, returnedPlaylistEntity);
		Mockito.verify(mockPlaylistEntityMetadata).getHashKeyPropotypeEntityForHashKey("someHashKey");

	}

	@Test
	public void testGetHashKeyPropertyName_DelegatesToEntityMetadata_IrrespectiveOfEntityInformationSetup() {
		Assertions.assertEquals("playlistHashKeyPropertyName", dynamoDBPlaylistEntityInformation.getHashKeyPropertyName());

	}

	@Test
	public void testGetHashKeyPrototypeEntityForHashKey_DelegatesToEntityMetadata_IrrespectiveOfEntityInformationSetup() {

		Object hashKeyPrototype2 = dynamoDBPlaylistEntityInformation
				.getHashKeyPropotypeEntityForHashKey("somePlaylistHashKey");
		Assertions.assertEquals(mockPlaylistPrototype, hashKeyPrototype2);
	}

	@Test
	public void testGetOverriddenAttributeName_DelegatesToEntityMetadata_IrrespectiveOfEntityInformationSetup() {

		Optional<String> propertyName2 = dynamoDBPlaylistEntityInformation
				.getOverriddenAttributeName("overriddenProperty");
		Assertions.assertEquals(Optional.of("modifiedPropertyName"), propertyName2);
	}

	@Test
	public void testGetIsHashKeyProperty_DelegatesToEntityMetadata_IrrespectiveOfEntityInformationSetup() {

		Assertions.assertFalse(dynamoDBPlaylistEntityInformation.isHashKeyProperty("nonHashKeyProperty"));
		Assertions.assertFalse(dynamoDBPlaylistEntityInformation.isHashKeyProperty("nonHashKeyProperty"));
	}

	@Test
	public void testGetIsCompositeIdProperty_DelegatesToEntityMetadata_IrrespectiveOfEntityInformationSetup() {

		Assertions.assertTrue(dynamoDBPlaylistEntityInformation.isCompositeHashAndRangeKeyProperty("compositeIdProperty"));
		Assertions.assertFalse(
				dynamoDBPlaylistEntityInformation.isCompositeHashAndRangeKeyProperty("nonCompositeIdProperty"));
	}

}
