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

import org.apache.camel.cdi.xml.ApplicationContextFactoryBean;
import org.apache.camel.cdi.xml.BeanManagerAware;
import org.apache.camel.cdi.xml.CamelContextFactoryBean;
import org.apache.camel.cdi.xml.CamelProxyFactoryDefinition;
import org.apache.camel.cdi.xml.CamelServiceExporterDefinition;
import org.apache.camel.core.xml.AbstractCamelFactoryBean;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.model.RoutesDefinition;

import javax.enterprise.inject.CreationException;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import static org.apache.camel.cdi.AnyLiteral.ANY;
import static org.apache.camel.cdi.ApplicationScopedLiteral.APPLICATION_SCOPED;
import static org.apache.camel.cdi.DefaultLiteral.DEFAULT;
import static org.apache.camel.cdi.Startup.Literal.STARTUP;

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

    Set<SyntheticBean<?>> beansFrom(URL url) throws JAXBException, IOException {
        try (InputStream xml = url.openStream()) {
            Object node = XmlCdiJaxbContexts.CAMEL_CDI.instance()
                .createUnmarshaller()
                .unmarshal(xml);
            if (node instanceof RoutesDefinition) {
                RoutesDefinition routes = (RoutesDefinition) node;
                SyntheticBean<?> bean = new SyntheticBean<>(manager,
                    new SyntheticAnnotated(manager, RoutesDefinition.class, ANY, DEFAULT),
                    RoutesDefinition.class,
                    new SyntheticInjectionTarget<>(() -> routes),
                    ba -> "imported routes definition "
                        + (routes.getId() != null ? "[" + routes.getId() + "] " : "")
                        + "from resource [" + url + "]");
                return Collections.singleton(bean);
            } else if (node instanceof ApplicationContextFactoryBean) {
                ApplicationContextFactoryBean app = (ApplicationContextFactoryBean) node;
                Set<SyntheticBean<?>> beans = new HashSet<>();
                for (CamelContextFactoryBean factory : app.getContexts()) {
                    SyntheticBean<?> bean = camelContextBean(factory, url);
                    beans.add(bean);
                    beans.addAll(camelContextBeans(factory, bean, url));
                }
                return beans;
            } else if (node instanceof CamelContextFactoryBean) {
                CamelContextFactoryBean factory = (CamelContextFactoryBean) node;
                Set<SyntheticBean<?>> beans = new HashSet<>();
                SyntheticBean<?> bean = camelContextBean(factory, url);
                beans.add(bean);
                beans.addAll(camelContextBeans(factory, bean, url));
                return beans;
            }
        }
        return Collections.emptySet();
    }

    private SyntheticBean<?> camelContextBean(CamelContextFactoryBean factory, URL url) {
        Set<Annotation> annotations = new HashSet<>();
        annotations.add(ANY);
        if (factory.getId() != null) {
            Collections.addAll(annotations,
                ContextName.Literal.of(factory.getId()), NamedLiteral.of(factory.getId()));
        } else {
            annotations.add(DEFAULT);
            factory.setImplicitId(true);
            factory.setId(new CdiCamelContextNameStrategy().getNextName());
        }

        annotations.add(APPLICATION_SCOPED);
        SyntheticAnnotated annotated = new SyntheticAnnotated(manager, DefaultCamelContext.class, annotations);
        return new SyntheticBean<>(manager, annotated, DefaultCamelContext.class,
            environment.camelContextInjectionTarget(
                new SyntheticInjectionTarget<>(() -> {
                    DefaultCamelContext context = new DefaultCamelContext();
                    factory.setContext(context);
                    factory.setBeanManager(manager);
                    return context;
                }, context -> {
                    try {
                        factory.afterPropertiesSet();
                    } catch (Exception cause) {
                        throw new CreationException(cause);
                    }
                }),
                annotated, manager),
            ba -> "imported Camel context with "
                + (factory.isImplicitId() ? "implicit " : "")
                + "id [" + factory.getId() + "] "
                + "from resource [" + url + "] "
                + "with qualifiers " + ba.getQualifiers());
    }

    private Set<SyntheticBean<?>> camelContextBeans(CamelContextFactoryBean factory, Bean<?> context, URL url) {
        // TODO: WARN log if the definition doesn't have an id
        Set<SyntheticBean<?>> beans = new HashSet<>();
        // Only generate a bean for named endpoint definition
        if (factory.getEndpoints() != null)
            factory.getEndpoints().stream()
                .filter(endpoint -> endpoint.getId() != null)
                .map(endpoint -> camelContextBean(context, endpoint, url))
                .forEach(beans::add);

        if (factory.getBeans() != null)
            factory.getBeans().stream()
                .filter(bean -> AbstractCamelFactoryBean.class.isAssignableFrom(bean.getClass()))
                .map(AbstractCamelFactoryBean.class::cast)
                .filter(bean -> bean.getId() != null)
                .map(bean -> camelContextBean(context, bean, url))
                .forEach(beans::add);

        if (factory.getProxies() != null)
            factory.getProxies().stream()
                .filter(proxy -> proxy.getId() != null)
                .map(proxy -> proxyFactoryBean(context, proxy, url))
                .forEach(beans::add);

        if (factory.getExports() != null)
            factory.getExports().stream()
                .map(export -> serviceExporterBean(context, export, url))
                .forEach(beans::add);

        if (factory.getThreadPools() != null)
            factory.getThreadPools().stream()
                .map(pool -> camelContextBean(context, pool, url))
                .forEach(beans::add);

        return beans;
    }

    private SyntheticBean<?> camelContextBean(Bean<?> context, AbstractCamelFactoryBean<?> bean, URL url) {
        if (bean instanceof BeanManagerAware)
            ((BeanManagerAware) bean).setBeanManager(manager);

        Set<Annotation> annotations = new HashSet<>();
        annotations.add(ANY);
        // FIXME: should add @ContextName if the Camel context bean has it
        annotations.add(bean.getId() != null ? NamedLiteral.of(bean.getId()) : DEFAULT);

        // TODO: should that be @Singleton to enable injection points with bean instance type?
        if (bean.isSingleton())
            annotations.add(APPLICATION_SCOPED);

        return new SyntheticBean<>(manager,
            new SyntheticAnnotated(manager, bean.getObjectType(), annotations),
            bean.getObjectType(),
            new XmlFactoryBeanInjectionTarget<>(manager, bean, context),
            ba -> "imported bean [" + bean.getId() + "] "
                + "from resource [" + url + "] "
                + "with qualifiers " + ba.getQualifiers());
    }

    private SyntheticBean<?> proxyFactoryBean(Bean<?> context, CamelProxyFactoryDefinition proxy, URL url) {
        return new XmlProxyFactoryBean<>(manager,
            new SyntheticAnnotated(manager, proxy.getServiceInterface(),
                APPLICATION_SCOPED, ANY, NamedLiteral.of(proxy.getId())),
            proxy.getServiceInterface(),
            ba -> "imported bean [" + proxy.getId() + "] "
                + "from resource [" + url + "] "
                + "with qualifiers " + ba.getQualifiers(),
            context, proxy);
    }

    private SyntheticBean<?> serviceExporterBean(Bean<?> context, CamelServiceExporterDefinition exporter, URL url) {
        Objects.requireNonNull(exporter.getServiceRef(),
            () -> String.format("Missing [%s] attribute for imported bean [%s] from resource [%s]",
                "serviceRef", Objects.toString(exporter.getId(), "export"), url));

        Class<?> type;
        if (exporter.getServiceInterface() != null) {
            type = exporter.getServiceInterface();
        } else {
            Bean<?> bean = manager.resolve(manager.getBeans(exporter.getServiceRef()));
            if (bean != null) {
                type = bean.getBeanClass();
            } else {
                Objects.requireNonNull(exporter.getServiceInterface(),
                    () -> String.format("Missing [%s] attribute for imported bean [%s] from resource [%s]",
                        "serviceInterface", Objects.toString(exporter.getId(), "export"), url));
                type = exporter.getServiceInterface();
            }
        }

        return new XmlServiceExporterBean<>(manager,
            new SyntheticAnnotated(manager, type,
                APPLICATION_SCOPED, ANY, STARTUP,
                exporter.getId() != null ? NamedLiteral.of(exporter.getId()) : DEFAULT),
            type,
            ba -> "imported bean [" + Objects.toString(exporter.getId(), "export") + "] "
                + "from resource [" + url + "] "
                + "with qualifiers " + ba.getQualifiers(),
            context, exporter);
    }
}
