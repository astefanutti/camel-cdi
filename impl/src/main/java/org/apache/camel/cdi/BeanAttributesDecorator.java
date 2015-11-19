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


import javax.enterprise.inject.spi.BeanAttributes;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

final class BeanAttributesDecorator<T> implements BeanAttributes<T> {

    private final BeanAttributes<T> attributes;

    private final Set<Annotation> qualifiers;

    BeanAttributesDecorator(BeanAttributes<T> attributes, Set<? extends Annotation> qualifiers) {
        this(attributes, qualifiers, Collections.<Class<? extends Annotation>>emptySet());
    }

    BeanAttributesDecorator(BeanAttributes<T> attributes, Set<? extends Annotation> qualifiers, Collection<Class<? extends Annotation>> exclusions) {
        this.attributes = attributes;
        Set<Annotation> set = new HashSet<>(attributes.getQualifiers());
        for (Annotation qualifier : qualifiers) {
            boolean exclude = false;
            for (Class<? extends Annotation> exclusion : exclusions) {
                if (exclusion.isAssignableFrom(qualifier.annotationType())) {
                    exclude = true;
                    break;
                }
            }
            if (!exclude)
                set.add(qualifier);
        }
        this.qualifiers = Collections.unmodifiableSet(set);
    }

    @Override
    public Set<Type> getTypes() {
        return attributes.getTypes();
    }

    @Override
    public Set<Annotation> getQualifiers() {
        return qualifiers;
    }

    @Override
    public Class<? extends Annotation> getScope() {
        return attributes.getScope();
    }

    @Override
    public String getName() {
        return attributes.getName();
    }

    @Override
    public Set<Class<? extends Annotation>> getStereotypes() {
        return attributes.getStereotypes();
    }

    @Override
    public boolean isAlternative() {
        return attributes.isAlternative();
    }
}
