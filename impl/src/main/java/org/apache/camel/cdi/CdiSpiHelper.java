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
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;
import java.lang.annotation.Annotation;
import java.util.Collection;

@Vetoed
final class CdiSpiHelper {

    static <T extends Annotation> T getQualifierByType(Bean<?> bean, Class<T> type) {
        return getFirstElementOfType(bean.getQualifiers(), type);
    }

    static <T extends Annotation> T getQualifierByType(InjectionPoint ip, Class<T> type) {
        return getFirstElementOfType(ip.getQualifiers(), type);
    }

    private static <E, T extends E> T getFirstElementOfType(Collection<E> collection, Class<T> type) {
        for (E item : collection)
            if ((item != null) && type.isAssignableFrom(item.getClass()))
                return ObjectHelper.cast(type, item);

        return null;
    }
}