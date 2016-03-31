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
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;
import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.Set;

final class BeanDecorator<T> extends BeanAttributesDecorator<T> implements Bean<T> {

    private final Bean<T> bean;

    BeanDecorator(Bean<T> bean, Annotation qualifier) {
        this(bean, Collections.singleton(qualifier));
    }

    BeanDecorator(Bean<T> bean, Set<? extends Annotation> qualifiers) {
        super(bean, qualifiers);
        this.bean = bean;
    }

    @Override
    public Class<?> getBeanClass() {
        return bean.getBeanClass();
    }

    @Override
    public Set<InjectionPoint> getInjectionPoints() {
        return bean.getInjectionPoints();
    }

    @Override
    public boolean isNullable() {
        return bean.isNullable();
    }

    @Override
    public T create(CreationalContext<T> creationalContext) {
        return bean.create(creationalContext);
    }

    @Override
    public void destroy(T instance, CreationalContext<T> creationalContext) {
        bean.destroy(instance, creationalContext);
    }

    @Override
    public String toString() {
        return bean.toString();
    }
}
