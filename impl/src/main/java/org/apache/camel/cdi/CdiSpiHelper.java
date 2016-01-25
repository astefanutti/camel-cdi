/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.cdi;

import javax.enterprise.inject.Vetoed;
import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;

@Vetoed
final class CdiSpiHelper {

    static Class<?> getRawType(Type type) {
        if (type instanceof Class<?>) {
            return Class.class.cast(type);
        }
        else if (type instanceof ParameterizedType) {
            return getRawType(ParameterizedType.class.cast(type).getRawType());
        }
        else if (type instanceof TypeVariable<?>) {
            return getBound(TypeVariable.class.cast(type).getBounds());
        }
        else if (type instanceof WildcardType) {
            return getBound(WildcardType.class.cast(type).getUpperBounds());
        }
        else if (type instanceof GenericArrayType) {
            Class<?> rawType = getRawType(GenericArrayType.class.cast(type).getGenericComponentType());
            if (rawType != null)
                return Array.newInstance(rawType, 0).getClass();
        }
        throw new UnsupportedOperationException("Unable to retrieve raw type for [" + type + "]");
    }

    private static Class<?> getBound(Type[] bounds) {
        if (bounds.length == 0)
            return Object.class;
        else
            return getRawType(bounds[0]);
    }
}