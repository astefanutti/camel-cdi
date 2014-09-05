/**
 * Copyright (C) 2014 Antonin Stefanutti (antonin.stefanutti@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
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
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

final class BeanAttributesDecorator<T> implements BeanAttributes<T> {

    private final BeanAttributes<T> attributes;

    private final Set<Annotation> qualifiers;

    BeanAttributesDecorator(BeanAttributes<T> attributes, Set<Annotation> qualifiers) {
        this.attributes = attributes;
        Set<Annotation> annotations = new HashSet<>(attributes.getQualifiers());
        annotations.addAll(qualifiers);
        this.qualifiers = Collections.unmodifiableSet(annotations);
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
