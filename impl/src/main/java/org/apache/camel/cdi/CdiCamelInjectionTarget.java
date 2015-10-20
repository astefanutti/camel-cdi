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


import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.InjectionException;
import javax.enterprise.inject.Vetoed;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.InjectionTarget;
import java.util.Set;

@Vetoed
final class CdiCamelInjectionTarget<T> implements InjectionTarget<T> {

    private final InjectionTarget<T> delegate;

    private final CdiCamelBeanPostProcessor processor;

    CdiCamelInjectionTarget(InjectionTarget<T> delegate, BeanManager manager) {
        this.delegate = delegate;
        this.processor = new CdiCamelBeanPostProcessor(manager);
    }

    @Override
    public void inject(T instance, CreationalContext<T> ctx) {
        delegate.inject(instance, ctx);
        try {
            // TODO: see how to retrieve the bean name
            processor.postProcessBeforeInitialization(instance, instance.getClass().getName());
            processor.postProcessAfterInitialization(instance, instance.getClass().getName());
        } catch (Exception cause) {
            throw new InjectionException("Camel annotations post processing of [" + delegate + "] failed!", cause);
        }
    }

    @Override
    public void postConstruct(T instance) {
        delegate.postConstruct(instance);
    }

    @Override
    public void preDestroy(T instance) {
        delegate.preDestroy(instance);
    }

    @Override
    public T produce(CreationalContext<T> ctx) {
        return delegate.produce(ctx);
    }

    @Override
    public void dispose(T instance) {
        delegate.dispose(instance);
    }

    @Override
    public Set<InjectionPoint> getInjectionPoints() {
        return delegate.getInjectionPoints();
    }

    @Override
    public String toString() {
        return delegate.toString();
    }

    @Override
    public int hashCode() {
        return delegate.hashCode();
    }

    @Override
    public boolean equals(Object object) {
        return delegate.equals(object);
    }
}
