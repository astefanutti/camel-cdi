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

import org.apache.camel.CamelContext;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.DefaultCamelContextNameStrategy;
import org.apache.camel.impl.ExplicitCamelContextNameStrategy;
import org.apache.camel.spi.CamelContextNameStrategy;
import org.apache.camel.util.ObjectHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.DeploymentException;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.Producer;
import javax.inject.Named;
import javax.inject.Provider;
import java.beans.Introspector;
import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.Set;

class CamelContextProducer<T extends CamelContext> implements Producer<T> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final Producer<T> delegate;

    private final BeanManager manager;

    // We need to delay the logic until the ProcessBean lifecycle event is fired. To be replaced with the Supplier functional interface when Java 8 becomes a pre-requisite.
    private final Provider<Set<Annotation>> qualifiers;

    private final CamelContextNameStrategy strategy;

    CamelContextProducer(Producer<T> delegate, final Set<Annotation> qualifiers, CamelContextNameStrategy strategy, BeanManager manager) {
        this.delegate = delegate;
        this.qualifiers = new Provider<Set<Annotation>>() {
            @Override
            public Set<Annotation> get() {
                return qualifiers;
            }
        };
        this.strategy = strategy;
        this.manager = manager;
    }

    CamelContextProducer(Producer<T> delegate, final Annotated annotated, final BeanManager manager) {
        this.delegate = delegate;
        this.qualifiers = new Provider<Set<Annotation>>() {
            @Override
            public Set<Annotation> get() {
                return manager.getExtension(CdiCamelExtension.class).getContextBean(new AnnotatedWrapper(annotated)).getQualifiers();
            }
        };
        this.strategy = nameStrategy(annotated);
        this.manager = manager;
    }

    private static CamelContextNameStrategy nameStrategy(Annotated annotated) {
        if (annotated.isAnnotationPresent(ContextName.class)) {
            return new ExplicitCamelContextNameStrategy(annotated.getAnnotation(ContextName.class).value());
        }
        // TODO: support stereotype with empty @Named annotation
        else if (annotated.isAnnotationPresent(Named.class)) {
            String name = annotated.getAnnotation(Named.class).value();
            if (name.isEmpty()) {
                if (annotated instanceof AnnotatedField) {
                    name = ((AnnotatedField) annotated).getJavaMember().getName();
                } else if (annotated instanceof AnnotatedMethod) {
                    name = ((AnnotatedMethod) annotated).getJavaMember().getName();
                    if (name.startsWith("get"))
                        name = Introspector.decapitalize(name.substring(3));
                } else {
                    name = Introspector.decapitalize(CdiSpiHelper.getRawType(annotated.getBaseType()).getSimpleName());
                }
            }
            return new ExplicitCamelContextNameStrategy(name);
        } else {
            // Use a specific naming strategy for Camel CDI as the default one increments the suffix for each CDI proxy created
            return new CdiCamelContextNameStrategy();
        }
    }

    @Override
    public T produce(CreationalContext<T> ctx) {
        T instance = delegate.produce(ctx);
        // Do not override the name if it's been already set (in the bean constructor for example)
        if (instance.getNameStrategy() instanceof DefaultCamelContextNameStrategy)
            instance.setNameStrategy(strategy);

        // Add bean registry and Camel injector
        if (instance instanceof DefaultCamelContext) {
            DefaultCamelContext adapted = instance.adapt(DefaultCamelContext.class);
            adapted.setRegistry(new CdiCamelRegistry(manager));
            adapted.setInjector(new CdiCamelInjector(instance.getInjector(), manager));
        } else {
            // Fail fast for the moment to avoid side effects by the time these two methods get declared on the CamelContext interface
            throw new DeploymentException("Camel CDI requires " + instance + " to be a subtype of DefaultCamelContext");
        }

        // Add event notifier if at least one observer is present
        Set<Annotation> events = new HashSet<>(manager.getExtension(CdiCamelExtension.class).getObserverEvents());
        events.retainAll(qualifiers.get());
        if (!events.isEmpty())
            instance.getManagementStrategy().addEventNotifier(new CdiEventNotifier(manager, qualifiers.get()));

        return instance;
    }

    @Override
    public void dispose(T instance) {
        delegate.dispose(instance);
        stopCamelContext(instance);
    }

    @Override
    public Set<InjectionPoint> getInjectionPoints() {
        return delegate.getInjectionPoints();
    }

    protected void stopCamelContext(T context) {
        if (!context.getStatus().isStopped()) {
            logger.info("Camel CDI is stopping {}", context);
            try {
                context.stop();
            } catch (Exception cause) {
                throw ObjectHelper.wrapRuntimeCamelException(cause);
            }
        }
    }
}
