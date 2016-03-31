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

import org.apache.camel.Endpoint;
import org.apache.camel.cdi.xml.ApplicationContextFactoryBean;
import org.apache.camel.cdi.xml.CamelContextFactoryBean;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.model.RoutesDefinition;

import javax.enterprise.inject.CreationException;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.InjectionTargetFactory;
import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.apache.camel.cdi.AnyLiteral.ANY;
import static org.apache.camel.cdi.ApplicationScopedLiteral.APPLICATION_SCOPED;
import static org.apache.camel.cdi.DefaultLiteral.DEFAULT;

final class XmlCdiBeanFactory {

    private final BeanManager manager;

    private final CdiCamelEnvironment environment;

    private XmlCdiBeanFactory(BeanManager manager, CdiCamelEnvironment environment) {
        this.manager = manager;
        this.environment = environment;
    }

    static XmlCdiBeanFactory with(BeanManager manager, CdiCamelEnvironment environment) {
        return new XmlCdiBeanFactory(manager, environment);
    }

    Set<Bean<?>> beansFrom(URL url) throws JAXBException, IOException {
        try (InputStream xml = url.openStream()) {
            Object node = XmlCdiJaxbContexts.CAMEL_CDI.instance()
                .createUnmarshaller()
                .unmarshal(xml);
            if (node instanceof RoutesDefinition) {
                RoutesDefinition routes = (RoutesDefinition) node;
                Bean<?> bean = manager.createBean(
                    new SyntheticBeanAttributes<>(manager,
                        // TODO: scope based on singleton attribute
                        new SyntheticAnnotated(manager, RoutesDefinition.class, ANY, DEFAULT),
                        ba -> "Imported routes definition " +
                            (routes.getId() != null ? "[" + routes.getId() + "]" : "") +
                            " from resource [" + url + "]"),
                    RoutesDefinition.class,
                    (InjectionTargetFactory<RoutesDefinition>) b ->new SyntheticInjectionTarget<>(() -> routes));
                return Collections.singleton(bean);
            } else if (node instanceof ApplicationContextFactoryBean) {
                ApplicationContextFactoryBean app = (ApplicationContextFactoryBean) node;
                Set<Bean<?>> beans = new HashSet<>();
                for (CamelContextFactoryBean factory : app.getContexts()) {
                    beans.add(camelContextBean(factory, url));
                    beans.addAll(camelContextBeans(factory, url));
                }
                return beans;
            } else if (node instanceof CamelContextFactoryBean) {
                CamelContextFactoryBean factory = (CamelContextFactoryBean) node;
                Set<Bean<?>> beans = camelContextBeans(factory, url);
                beans.add(camelContextBean(factory, url));
                return beans;
            }
        }
        return Collections.emptySet();
    }

    private Bean<?> camelContextBean(CamelContextFactoryBean factory, URL url) {
        Set<Annotation> annotations = new HashSet<>();
        annotations.add(ANY);
        if (factory.getId() != null)
            Collections.addAll(annotations,
                ContextName.Literal.of(factory.getId()), NamedLiteral.of(factory.getId()));
        else
            annotations.add(DEFAULT);
        annotations.add(APPLICATION_SCOPED);
        Annotated annotated = new SyntheticAnnotated(manager, DefaultCamelContext.class, annotations);
        return manager.createBean(
            new SyntheticBeanAttributes<>(manager, annotated,
                ba -> "Imported Camel context " +
                    (factory.getId() != null ? "[" + factory.getId() + "]" : "") +
                    " from resource [" + url + "]" +
                    " with qualifiers " + ba.getQualifiers()),
            DefaultCamelContext.class,
            (InjectionTargetFactory<DefaultCamelContext>) b -> environment.camelContextInjectionTarget(
                new SyntheticInjectionTarget<>(() -> {
                    DefaultCamelContext context = new DefaultCamelContext();
                    factory.setContext(context);
                    factory.setManager(manager);
                    return context;
                }, context -> {
                    try {
                        factory.afterPropertiesSet();
                    } catch (Exception cause) {
                        throw new CreationException(cause);
                    }
                }),
                annotated, manager));
    }

    private Set<Bean<?>> camelContextBeans(CamelContextFactoryBean factory, URL url) {
        Set<Bean<?>> beans = new HashSet<>();
        if (factory.getEndpoints() != null)
            factory.getEndpoints().stream()
                .peek(endpoint -> endpoint.setBeanManager(manager))
                .peek(endpoint -> {
                    if (endpoint.getCamelContextId() == null)
                        endpoint.setCamelContextId(factory.getId());
                })
                .map(endpoint -> manager.createBean(
                    new SyntheticBeanAttributes<>(manager,
                        // TODO: should be the qualifier of the Camel context
                        // TODO: scope based on singleton attribute
                        // TODO: test if id can be null
                        new SyntheticAnnotated(manager, endpoint.getObjectType(), ANY, DEFAULT, NamedLiteral.of(endpoint.getId())),
                        ba -> "Imported endpoint [" + endpoint.getId() + "]" +
                            " from resource [" + url + "]" +
                            " with qualifiers " + ba.getQualifiers()),
                    endpoint.getObjectType(),
                    (InjectionTargetFactory<Endpoint>) bean -> new XmlFactoryBeanInjectionTarget<>(endpoint)))
                .forEach(beans::add);

        return beans;
    }
}
