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

import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

public final class AttributeValueUtil {

    private AttributeValueUtil() {
    }

    public static AttributeValue toAttributeValue(Object o) {
        if (o instanceof AttributeValue) {
            return (AttributeValue) o;
        } else if (o instanceof Number) {
            return AttributeValue.builder().n(o.toString()).build();
        } else if (o instanceof SdkBytes) {
            return AttributeValue.builder().b((SdkBytes) o).build();
        } else if (o instanceof String) {
            return AttributeValue.builder().s((String) o).build();
        } else {
            return AttributeValue.builder().s(o.toString()).build();
        }
    }

}
