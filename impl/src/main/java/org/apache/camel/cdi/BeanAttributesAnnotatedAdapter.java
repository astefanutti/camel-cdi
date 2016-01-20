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

import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.BeanAttributes;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Set;

final class BeanAttributesAnnotatedAdapter<T> implements Annotated {

    private final BeanAttributes<T> delegate;

    private final Class<T> type;

    BeanAttributesAnnotatedAdapter(BeanAttributes<T> delegate, Class<T> type) {
        this.delegate = delegate;
        this.type = type;
    }

    @Override
    public Type getBaseType() {
        return type;
    }

    @Override
    public Set<Type> getTypeClosure() {
        return delegate.getTypes();
    }

    @Override
    public <U extends Annotation> U getAnnotation(Class<U> annotationType) {
        return CdiSpiHelper.getFirstElementOfType(delegate.getQualifiers(), annotationType);
    }

    @Override
    public Set<Annotation> getAnnotations() {
        return delegate.getQualifiers();
    }

    @Override
    public boolean isAnnotationPresent(Class<? extends Annotation> annotationType) {
        return getAnnotation(annotationType) != null;
    }
}
