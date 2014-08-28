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
package io.astefanutti.camel.cdi;

import org.apache.camel.CamelContext;
import org.apache.camel.impl.DefaultCamelBeanPostProcessor;
import org.apache.camel.spi.Injector;
import org.apache.camel.util.ObjectHelper;

import javax.enterprise.inject.spi.BeanManager;

final class CdiInjector implements Injector {

    private final Injector injector;

    private final BeanManager manager;

    private final DefaultCamelBeanPostProcessor processor;

    CdiInjector(Injector injector, BeanManager manager, CamelContext context) {
        this.injector = injector;
        this.manager = manager;
        this.processor = new DefaultCamelBeanPostProcessor(context);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T newInstance(Class<T> type) {
        T instance = BeanManagerHelper.getContextualReference(manager, type, true);
        if (instance != null) {
            try {
                instance = (T) processor.postProcessAfterInitialization(instance, type.getName());
                return (T) processor.postProcessBeforeInitialization(instance, type.getName());
            } catch (Exception cause) {
                throw ObjectHelper.wrapRuntimeCamelException(cause);
            }
        }

        return injector.newInstance(type);
    }

    @Override
    public <T> T newInstance(Class<T> type, Object instance) {
        return injector.newInstance(type, instance);
    }
}
