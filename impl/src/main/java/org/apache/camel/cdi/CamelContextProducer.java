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
import org.apache.camel.util.ObjectHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.DeploymentException;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.Producer;
import javax.inject.Named;
import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.Set;

class CamelContextProducer<T extends CamelContext> implements Producer<T> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final Producer<T> delegate;

    private final Annotated annotated;

    private final BeanManager manager;

    CamelContextProducer(Producer<T> delegate, Annotated annotated, BeanManager manager) {
        this.delegate = delegate;
        this.annotated = annotated;
        this.manager = manager;
    }

    @Override
    public T produce(CreationalContext<T> ctx) {
        T instance = delegate.produce(ctx);
        // Do not override the name if it's been already set (in the bean constructor for example)
        if (instance.getNameStrategy() instanceof DefaultCamelContextNameStrategy) {
            if (annotated.isAnnotationPresent(ContextName.class)) {
                instance.setNameStrategy(new ExplicitCamelContextNameStrategy(annotated.getAnnotation(ContextName.class).value()));
            }
            else if (annotated.isAnnotationPresent(Named.class)) {
                String name = annotated.getAnnotation(Named.class).value();
                if (name.isEmpty()) {
                    name = CdiSpiHelper.getRawType(annotated.getBaseType()).getSimpleName();
                    name = Character.toLowerCase(name.charAt(0)) + name.substring(1);
                }
                instance.setNameStrategy(new ExplicitCamelContextNameStrategy(name));
            } else {
                // Use a specific naming strategy for Camel CDI as the default one increments the suffix for each CDI proxy created
                instance.setNameStrategy(new CdiCamelContextNameStrategy());
            }
        }

        // Add bean registry and Camel injector
        if (instance instanceof DefaultCamelContext) {
            instance.adapt(DefaultCamelContext.class).setRegistry(new CdiCamelRegistry(manager));
            instance.adapt(DefaultCamelContext.class).setInjector(new CdiCamelInjector(instance.getInjector(), manager));
        } else {
            // Fail fast for the moment to avoid side effects by the time these two methods get declared on the CamelContext interface
            throw new DeploymentException("Camel CDI requires " + instance + " to be a subtype of DefaultCamelContext");
        }

        // Add event notifier if at least one observer is present
        CdiCamelExtension extension = manager.getExtension(CdiCamelExtension.class);
        Set<Annotation> events = new HashSet<>(extension.getObserverEvents());
        // Annotated must be wrapped because of OWB-1099
        Set<Annotation> qualifiers = extension.getContextBean(new AnnotatedWrapper(annotated)).getQualifiers();
        events.retainAll(qualifiers);
        if (!events.isEmpty())
            instance.getManagementStrategy().addEventNotifier(new CdiEventNotifier(manager, qualifiers));

        return instance;
    }

    @Override
    public void dispose(T instance) {
        delegate.dispose(instance);
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
    public Set<InjectionPoint> getInjectionPoints() {
        return delegate.getInjectionPoints();
    }
}
