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

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.InjectionTarget;
import javax.enterprise.inject.spi.PassivationCapable;

final class CdiCamelContextBean implements Bean<CdiCamelContext>, PassivationCapable {

    private final Set<Annotation> qualifiers;

    private final Set<Type> types;

    private final InjectionTarget<CdiCamelContext> target;

    CdiCamelContextBean(BeanManager manager) {
        this.qualifiers = Collections.unmodifiableSet(new HashSet<Annotation>(Arrays.asList(AnyLiteral.INSTANCE, DefaultLiteral.INSTANCE)));
        AnnotatedType<CdiCamelContext> annotatedType = manager.createAnnotatedType(CdiCamelContext.class);
        this.types = Collections.unmodifiableSet(annotatedType.getTypeClosure());
        this.target = manager.createInjectionTarget(annotatedType);
    }

    @Override
    public Class<? extends Annotation> getScope() {
        return ApplicationScoped.class;
    }

    @Override
    public Set<Annotation> getQualifiers() {
        return qualifiers;
    }

    @Override
    public CdiCamelContext create(CreationalContext<CdiCamelContext> creational) {
        CdiCamelContext context = target.produce(creational);
        target.inject(context, creational);
        target.postConstruct(context);
        creational.push(context);
        return context;
    }

    @Override
    public void destroy(CdiCamelContext instance, CreationalContext<CdiCamelContext> creational) {
        target.preDestroy(instance);
        target.dispose(instance);
        creational.release();
    }

    @Override
    public Class<CdiCamelContext> getBeanClass() {
        return CdiCamelContext.class;
    }

    @Override
    public Set<InjectionPoint> getInjectionPoints() {
        return Collections.emptySet();
    }

    @Override
    public String getName() {
        // Not called as this is not a named bean
        return null;
    }

    @Override
    public String toString() {
        return "Default CdiCamelContext Bean";
    }

    @Override
    public Set<Class<? extends Annotation>> getStereotypes() {
        return Collections.emptySet();
    }

    @Override
    public Set<Type> getTypes() {
        return types;
    }

    @Override
    public boolean isAlternative() {
        return false;
    }

    @Override
    public boolean isNullable() {
        return false;
    }

    @Override
    public String getId() {
        return getClass().getName();
    }
}
