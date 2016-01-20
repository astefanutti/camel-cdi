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

import org.apache.camel.util.ObjectHelper;

import javax.enterprise.inject.Vetoed;
import javax.enterprise.inject.spi.InjectionPoint;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

@Vetoed
final class CdiSpiHelper {

    static <T extends Annotation> T getQualifierByType(InjectionPoint ip, Class<T> type) {
        return getFirstElementOfType(ip.getQualifiers(), type);
    }

    static <E, T extends E> T getFirstElementOfType(Collection<E> collection, Class<T> type) {
        for (E item : collection)
            if ((item != null) && type.isAssignableFrom(item.getClass()))
                return ObjectHelper.cast(type, item);

        return null;
    }

    @SafeVarargs
    static <T> Set<T> excludeElementOfTypes(Set<T> annotations, Class<? extends T>... exclusions) {
        Set<T> set = new HashSet<>();
        for (T qualifier : annotations) {
            boolean exclude = false;
            for (Class<? extends T> exclusion : exclusions) {
                if (exclusion.isAssignableFrom(qualifier.getClass())) {
                    exclude = true;
                    break;
                }
            }
            if (!exclude)
                set.add(qualifier);
        }
        return set;
    }

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