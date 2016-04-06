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
import org.apache.camel.Consumer;
import org.apache.camel.Endpoint;
import org.apache.camel.cdi.xml.BeanManagerAware;
import org.apache.camel.cdi.xml.CamelContextFactoryBean;
import org.apache.camel.cdi.xml.CamelServiceExporterDefinition;
import org.apache.camel.component.bean.BeanProcessor;
import org.apache.camel.util.CamelContextHelper;
import org.apache.camel.util.ObjectHelper;
import org.apache.camel.util.ServiceHelper;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.CreationException;
import javax.enterprise.inject.UnsatisfiedResolutionException;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanAttributes;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.InjectionPoint;
import java.util.Collections;
import java.util.Set;

final class XmlServiceExporterBean<T> extends BeanAttributesDelegate<T> implements Bean<T> {

    private final BeanManager manager;

    private final CamelContextFactoryBean context;

    private final CamelServiceExporterDefinition exporter;

    private final Class<?> type;

    private Consumer consumer;

    XmlServiceExporterBean(BeanManager manager, CamelContextFactoryBean context, CamelServiceExporterDefinition exporter, BeanAttributes<T> attributes, Class<?> type) {
        super(attributes);
        this.manager = manager;
        this.context = context;
        this.exporter = exporter;
        this.type = type;
    }

    @Override
    public Class<?> getBeanClass() {
        return type;
    }

    @Override
    public Set<InjectionPoint> getInjectionPoints() {
        return Collections.emptySet();
    }

    @Override
    public boolean isNullable() {
        return false;
    }

    @Override
    public T create(CreationalContext<T> creationalContext) {
        try {
            CamelContext context = BeanManagerAware.getCamelContextById(manager,
                ObjectHelper.isNotEmpty(exporter.getCamelContextId())
                    ? exporter.getCamelContextId()
                    : this.context.getId());

            Bean<?> bean = manager.resolve(manager.getBeans(exporter.getServiceRef()));
            if (bean == null)
                throw new UnsatisfiedResolutionException("No bean with name [" + exporter.getServiceRef() + "] is deployed!");

            @SuppressWarnings("unchecked")
            T service = (T) manager.getReference(bean, type, manager.createCreationalContext(bean));

            Endpoint endpoint = CamelContextHelper.getMandatoryEndpoint(context, exporter.getUri());
            consumer = endpoint.createConsumer(new BeanProcessor(service, context));
            ServiceHelper.startService(consumer);

            return service;
        } catch (Exception cause) {
            throw new CreationException("Error while creating instance for " + this, cause);
        }
    }

    @Override
    public void destroy(T instance, CreationalContext<T> creationalContext) {
        try {
            ServiceHelper.stopService(consumer);
        } catch (Exception cause) {
            throw ObjectHelper.wrapRuntimeCamelException(cause);
        }
    }
}
