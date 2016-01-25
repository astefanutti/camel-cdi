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

import org.apache.camel.impl.DefaultCamelContext;

import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.BeanManager;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

final class CamelContextBeanAnnotated implements Annotated {

    private final Set<Type> types;

    private final Set<Annotation> annotations;

    CamelContextBeanAnnotated(BeanManager manager, Annotation... annotations) {
        this.types = Collections.unmodifiableSet(manager.createAnnotatedType(DefaultCamelContext.class).getTypeClosure());
        this.annotations = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(annotations)));
    }

    @Override
    public Type getBaseType() {
        return DefaultCamelContext.class;
    }

    @Override
    public Set<Type> getTypeClosure() {
        return types;
    }

    @Override
    public Set<Annotation> getAnnotations() {
        return annotations;
    }

    @Override
    public <U extends Annotation> U getAnnotation(Class<U> type) {
        return annotations.stream()
            .filter(a -> a.annotationType().equals(type))
            .findAny()
            .map(type::cast)
            .orElse(null);
    }

    @Override
    public boolean isAnnotationPresent(Class<? extends Annotation> type) {
        return annotations.stream()
            .filter(a -> a.annotationType().equals(type))
            .findAny()
            .isPresent();
    }
}
