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
import org.mockito.junit.jupiter.MockitoExtension;
import org.socialsignin.spring.data.dynamodb.domain.sample.Playlist;
import org.socialsignin.spring.data.dynamodb.domain.sample.PlaylistId;
import org.socialsignin.spring.data.dynamodb.domain.sample.User;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unused")
public class DynamoDBIdIsHashKeyEntityInformationImplUnitTest {

	private DynamoDBIdIsHashKeyEntityInformationImpl<Playlist, PlaylistId> dynamoDBPlaylistEntityInformation;

	private DynamoDBIdIsHashKeyEntityInformationImpl<User, String> dynamoDBUserEntityInformation;

	@Mock
	private DynamoDBHashAndRangeKeyExtractingEntityMetadata<Playlist, String> mockPlaylistEntityMetadata;

	@Mock
	private DynamoDBHashKeyExtractingEntityMetadata<User> mockUserEntityMetadata;

	@Mock
	private Object mockHashKey;

	@Mock
	private User mockUserPrototype;

	@Mock
	private Playlist mockPlaylistPrototype;

	@BeforeEach
	public void setup() {

		lenient().when(mockUserEntityMetadata.getHashKeyPropertyName()).thenReturn("userHashKeyPropertyName");
		lenient().when(mockPlaylistEntityMetadata.getHashKeyPropertyName()).thenReturn("playlistHashKeyPropertyName");
		lenient().when(mockUserEntityMetadata.getOverriddenAttributeName("overriddenProperty"))
				.thenReturn(Optional.of("modifiedPropertyName"));
		lenient().when(mockPlaylistEntityMetadata.getOverriddenAttributeName("overriddenProperty"))
				.thenReturn(Optional.of("modifiedPropertyName"));

		lenient().when(mockUserEntityMetadata.isHashKeyProperty("hashKeyProperty")).thenReturn(true);
		lenient().when(mockPlaylistEntityMetadata.isHashKeyProperty("nonHashKeyProperty")).thenReturn(false);

		dynamoDBPlaylistEntityInformation = new DynamoDBIdIsHashKeyEntityInformationImpl<>(Playlist.class,
				mockPlaylistEntityMetadata);
		dynamoDBUserEntityInformation = new DynamoDBIdIsHashKeyEntityInformationImpl<>(User.class,
				mockUserEntityMetadata);
	}

	@Test
	public void testGetId_WhenHashKeyTypeSameAsIdType_InvokesHashKeyMethod_AndReturnedIdIsAssignableToIdType_AndIsValueExpected() {
		User user = new User();
		user.setId("someUserId");
		String id = dynamoDBUserEntityInformation.getId(user);
		Assertions.assertEquals("someUserId", id);

	}

	@Test
	public void testGetId_WhenHashKeyMethodNotSameAsIdType_InvokesHashKeyMethod_AndReturnedIdIsNotAssignableToIdType() {
		Playlist playlist = new Playlist();
		playlist.setUserName("someUserName");
		playlist.setPlaylistName("somePlaylistName");
		assertThrows(ClassCastException.class, () -> {
			PlaylistId id = dynamoDBPlaylistEntityInformation.getId(playlist);
		});
	}

	@Test
	public void testGetHashKeyGivenId_WhenHashKeyTypeSameAsIdType_ReturnsId() {
		Object hashKey = dynamoDBUserEntityInformation.getHashKey("someUserId");
		Assertions.assertNotNull(hashKey);
		Assertions.assertEquals("someUserId", hashKey);
	}

	@Test
	public void testGetHashKeyGivenId_WhenHashKeyTypeNotSameAsIdType_ThrowsIllegalArgumentException() {
		PlaylistId id = new PlaylistId();
		assertThrows(IllegalArgumentException.class, () -> {
			Object hashKey = dynamoDBPlaylistEntityInformation.getHashKey(id);
			Assertions.assertNotNull(hashKey);
			Assertions.assertEquals(id, hashKey);
		});
	}

	@Test
	public void testGetJavaType_WhenEntityIsInstanceWithHashAndRangeKey_ReturnsEntityClass() {
		Assertions.assertEquals(Playlist.class, dynamoDBPlaylistEntityInformation.getJavaType());
	}

	@Test
	public void testGetJavaType_WhenEntityIsInstanceWithHashKeyOnly_ReturnsEntityClass() {
		Assertions.assertEquals(User.class, dynamoDBUserEntityInformation.getJavaType());
	}

	@Test
	public void testGetIdType_WhenEntityIsInstanceWithHashAndRangeKey_ReturnsReturnTypeOfHashKeyMethod() {
		Assertions.assertEquals(String.class, dynamoDBPlaylistEntityInformation.getIdType());
	}

	@Test
	public void testGetIdType_WhenEntityIsInstanceWithHashKeyOnly_ReturnsReturnTypeOfHashKeyMethod() {
		Assertions.assertEquals(String.class, dynamoDBUserEntityInformation.getIdType());
	}

	// The following tests ensure that invarient methods such as those always
	// retuning constants, or
	// that delegate to metadata, behave the same irrespective of the setup of the
	// EntityInformation

	@Test
	public void testGetRangeKey_ReturnsNull_IrrespectiveOfEntityInformationSetup() {
		Object userRangeKey = dynamoDBUserEntityInformation.getRangeKey("someUserId");
		Assertions.assertNull(userRangeKey);

		Object playlistRangeKey = dynamoDBPlaylistEntityInformation.getRangeKey(new PlaylistId());
		Assertions.assertNull(playlistRangeKey);
	}

	@Test
	public void testIsRangeKeyAware_ReturnsFalse_IrrespectiveOfEntityInformationSetup() {
		Assertions.assertFalse(dynamoDBUserEntityInformation.isRangeKeyAware());

		Assertions.assertFalse(dynamoDBPlaylistEntityInformation.isRangeKeyAware());
	}

	@Test
	public void testGetHashKeyPropertyName_DelegatesToEntityMetadata_IrrespectiveOfEntityInformationSetup() {
		Assertions.assertEquals("userHashKeyPropertyName", dynamoDBUserEntityInformation.getHashKeyPropertyName());
		Assertions.assertEquals("playlistHashKeyPropertyName", dynamoDBPlaylistEntityInformation.getHashKeyPropertyName());

	}

	@Test
	public void testGetIsHashKeyProperty_DelegatesToEntityMetadata_IrrespectiveOfEntityInformationSetup() {
		Assertions.assertTrue(dynamoDBUserEntityInformation.isHashKeyProperty("hashKeyProperty"));
		Assertions.assertTrue(dynamoDBUserEntityInformation.isHashKeyProperty("hashKeyProperty"));

		Assertions.assertFalse(dynamoDBPlaylistEntityInformation.isHashKeyProperty("nonHashKeyProperty"));
		Assertions.assertFalse(dynamoDBPlaylistEntityInformation.isHashKeyProperty("nonHashKeyProperty"));
	}

	@Test
	public void testGetIsCompositeIdProperty_ReturnsFalse_IrrespectiveOfEntityInformationSetup() {
		Assertions.assertFalse(dynamoDBUserEntityInformation.isCompositeHashAndRangeKeyProperty("compositeIdProperty"));
		Assertions.assertFalse(dynamoDBUserEntityInformation.isCompositeHashAndRangeKeyProperty("compositeIdProperty"));

		Assertions.assertFalse(
				dynamoDBPlaylistEntityInformation.isCompositeHashAndRangeKeyProperty("nonCompositeIdProperty"));
		Assertions.assertFalse(
				dynamoDBPlaylistEntityInformation.isCompositeHashAndRangeKeyProperty("nonCompositeIdProperty"));
	}

	@Test
	public void testGetOverriddenAttributeName_DelegatesToEntityMetadata_IrrespectiveOfEntityInformationSetup() {
		Optional<String> propertyName1 = dynamoDBUserEntityInformation.getOverriddenAttributeName("overriddenProperty");
		Assertions.assertEquals(Optional.of("modifiedPropertyName"), propertyName1);

		Optional<String> propertyName2 = dynamoDBPlaylistEntityInformation
				.getOverriddenAttributeName("overriddenProperty");
		Assertions.assertEquals(Optional.of("modifiedPropertyName"), propertyName2);
	}

}
