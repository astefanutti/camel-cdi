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

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.spi.BeanAttributes;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Named;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

class SyntheticBeanAttributes<T> implements BeanAttributes<T> {

    private final BeanManager manager;

    private final SyntheticAnnotated annotated;

    private final Function<BeanAttributes<T>, String> toString;

    SyntheticBeanAttributes(BeanManager manager, SyntheticAnnotated annotated, Function<BeanAttributes<T>, String> toString) {
        this.manager = manager;
        this.annotated = annotated;
        this.toString = toString;
    }

    <A extends Annotation> void addQualifier(A qualifier) {
        annotated.addAnnotation(qualifier);
    }

    @Override
    public Class<? extends Annotation> getScope() {
        return annotated.getAnnotations().stream()
            .map(Annotation::annotationType)
            .filter(manager::isScope)
            .findAny()
            .orElse(Dependent.class);
    }

    @Override
    public Set<Annotation> getQualifiers() {
        return annotated.getAnnotations().stream()
            .filter(a -> manager.isQualifier(a.annotationType()))
            .collect(Collectors.toSet());
    }

    @Override
    public String getName() {
        return annotated.getAnnotations().stream()
            .filter(a -> Named.class.equals(a.annotationType()))
            .map(Named.class::cast)
            .map(Named::value)
            .findFirst()
            .orElse(null);
    }

    @Override
    public Set<Class<? extends Annotation>> getStereotypes() {
        return Collections.emptySet();
    }

    @Override
    public Set<Type> getTypes() {
        return annotated.getTypeClosure();
    }

    @Override
    public boolean isAlternative() {
        return false;
    }

    @Override
    public String toString() {
        return toString.apply(this);
    }
}
