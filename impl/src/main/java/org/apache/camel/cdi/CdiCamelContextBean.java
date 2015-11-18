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
import org.apache.camel.util.ObjectHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.PassivationCapable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

final class CdiCamelContextBean implements Bean<DefaultCamelContext>, PassivationCapable {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final BeanManager manager;

    private final Set<Annotation> qualifiers;

    private final Set<Type> types;

    CdiCamelContextBean(BeanManager manager) {
        this.manager = manager;
        this.qualifiers = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(AnyLiteral.INSTANCE, DefaultLiteral.INSTANCE)));
        this.types = Collections.unmodifiableSet(manager.createAnnotatedType(DefaultCamelContext.class).getTypeClosure());
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
    public DefaultCamelContext create(CreationalContext<DefaultCamelContext> creational) {
        DefaultCamelContext context = new DefaultCamelContext();
        context.setName("camel-cdi");

        // Add bean registry and Camel injector
        context.setRegistry(new CdiCamelRegistry(manager));
        context.setInjector(new CdiCamelInjector(context.getInjector(), manager));

        // Add event notifier if at least one observer is present
        Set<Annotation> events = manager.getExtension(CdiCamelExtension.class).getObserverEvents();
        if (events.contains(AnyLiteral.INSTANCE) || events.contains(DefaultLiteral.INSTANCE))
            context.getManagementStrategy().addEventNotifier(new CdiEventNotifier(manager));

        return context;
    }

    @Override
    public void destroy(DefaultCamelContext instance, CreationalContext<DefaultCamelContext> creational) {
        if (!instance.getStatus().isStopped()) {
            logger.info("Camel CDI is stopping {}", instance);
            try {
                instance.stop();
            } catch (Exception cause) {
                throw ObjectHelper.wrapRuntimeCamelException(cause);
            }
        }
    }

    @Override
    public Class<DefaultCamelContext> getBeanClass() {
        return DefaultCamelContext.class;
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
        return "Default CDI CamelContext Bean";
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
