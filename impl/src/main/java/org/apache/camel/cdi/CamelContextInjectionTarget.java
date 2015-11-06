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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.DeploymentException;
import javax.enterprise.inject.spi.InjectionTarget;

final class CamelContextInjectionTarget<T extends CamelContext> extends DelegateInjectionTarget<T> implements InjectionTarget<T> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final AnnotatedType<T> type;

    private final BeanManager manager;

    private final CdiCamelExtension extension;

    CamelContextInjectionTarget(AnnotatedType<T> type, InjectionTarget<T> delegate, BeanManager manager, CdiCamelExtension extension) {
        super(delegate);
        this.type = type;
        this.manager = manager;
        this.extension = extension;
    }

    @Override
    public T produce(CreationalContext<T> ctx) {
        T instance = super.produce(ctx);

        ContextName name = type.getAnnotation(ContextName.class);
        // Do not override the name if it's been already set (in the bean constructor for example)
        if (instance.getNameStrategy() instanceof DefaultCamelContextNameStrategy)
            instance.setNameStrategy(new ExplicitCamelContextNameStrategy(name != null ? name.value() : "camel-cdi"));

        // Add bean registry and Camel injector
        if (instance instanceof DefaultCamelContext) {
            instance.adapt(DefaultCamelContext.class).setRegistry(new CdiCamelRegistry(manager));
            instance.adapt(DefaultCamelContext.class).setInjector(new CdiCamelInjector(instance.getInjector(), manager));
        } else {
            // Fail fast for the moment to avoid side effects by the time these two methods get declared on the CamelContext interface
            throw new DeploymentException("Camel CDI requires " + instance + " to be a subtype of DefaultCamelContext");
        }

        // Add event notifier if at least one observer is present
        if (extension.getContextInfo(name).contains(ContextInfo.EventNotifierSupport))
            instance.getManagementStrategy().addEventNotifier(new CdiEventNotifier(manager, name));

        return instance;
    }

    @Override
    public void preDestroy(T instance) {
        super.preDestroy(instance);
        if (!instance.getStatus().isStopped())
            logger.warn("{} has not stopped!", instance);
    }
}
