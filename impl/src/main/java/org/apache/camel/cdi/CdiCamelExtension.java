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
package org.apache.camel.cdi;

import org.apache.camel.BeanInject;
import org.apache.camel.CamelContext;
import org.apache.camel.CamelContextAware;
import org.apache.camel.Consume;
import org.apache.camel.Converter;
import org.apache.camel.EndpointInject;
import org.apache.camel.Produce;
import org.apache.camel.PropertyInject;
import org.apache.camel.RoutesBuilder;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.AfterDeploymentValidation;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.inject.spi.ProcessBean;
import javax.enterprise.inject.spi.ProcessInjectionTarget;
import javax.enterprise.inject.spi.WithAnnotations;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class CdiCamelExtension implements Extension {

    private boolean hasDefaultCamelContext;

    private final Set<Class<?>> typeConverters = Collections.newSetFromMap(new ConcurrentHashMap<Class<?>, Boolean>());

    private final Set<AnnotatedType<?>> camelBeans = Collections.newSetFromMap(new ConcurrentHashMap<AnnotatedType<?>, Boolean>());

    private final Set<AnnotatedType<?>> eagerBeans = Collections.newSetFromMap(new ConcurrentHashMap<AnnotatedType<?>, Boolean>());

    private final Map<String, CamelContext> camelContexts = new ConcurrentHashMap<>();

    private void typeConverters(@Observes @WithAnnotations(Converter.class) ProcessAnnotatedType<?> pat) {
        typeConverters.add(pat.getAnnotatedType().getJavaClass());
    }

    private void camelContextAware(@Observes ProcessAnnotatedType<? extends CamelContextAware> pat) {
        camelBeans.add(pat.getAnnotatedType());
    }

    private void camelAnnotations(@Observes @WithAnnotations({BeanInject.class, Consume.class, EndpointInject.class, Produce.class, PropertyInject.class}) ProcessAnnotatedType<?> pat) {
        camelBeans.add(pat.getAnnotatedType());
    }

    private void consumeBeans(@Observes @WithAnnotations(Consume.class) ProcessAnnotatedType<?> pat) {
        eagerBeans.add(pat.getAnnotatedType());
    }

    private <T> void camelBeansPostProcessor(@Observes ProcessInjectionTarget<T> pit, BeanManager manager) {
        if (camelBeans.contains(pit.getAnnotatedType()))
            pit.setInjectionTarget(new CdiCamelInjectionTarget<>(pit.getInjectionTarget(), manager));
    }

    // FIXME: remove when WELD-1729 is fixed
    private void camelContextBean(@Observes ProcessBean<? extends CamelContext> pb) {
        if (pb.getBean().getQualifiers().contains(DefaultLiteral.INSTANCE))
            hasDefaultCamelContext = true;
    }

    private void addDefaultCamelContext(@Observes AfterBeanDiscovery abd, BeanManager manager) {
        // FIXME: not working with Weld 2.x, see WELD-1729
        //if (manager.getBeans(CamelContext.class).isEmpty())
        if (!hasDefaultCamelContext)
            abd.addBean(new CdiCamelContextBean(manager));
    }

    private void configureCamelContext(@Observes AfterDeploymentValidation adv, BeanManager manager) {
        // Instantiate the Camel contexts
        CamelContext defaultContext = BeanManagerHelper.getReferenceByType(manager, CamelContext.class);
        for (Bean<?> bean : manager.getBeans(CamelContext.class, AnyLiteral.INSTANCE, ContextNameLiteral.INSTANCE)) {
            ContextName name = CdiCamelFactory.getFirstElementOfType(bean.getQualifiers(), ContextName.class);
            camelContexts.put(name.value(), BeanManagerHelper.getReferenceByType(manager, CamelContext.class, bean));
        }

        // Add type converter beans to the Camel contexts
        CdiTypeConverterLoader loader = new CdiTypeConverterLoader();
        for (Class<?> typeConverter : typeConverters) {
            loader.loadConverterMethods(defaultContext.getTypeConverterRegistry(), typeConverter);
            for (CamelContext context : camelContexts.values())
                loader.loadConverterMethods(context.getTypeConverterRegistry(), typeConverter);
        }

        // Instantiate route builders and add them to the corresponding Camel contexts
        for (Bean<?> bean : manager.getBeans(RoutesBuilder.class, AnyLiteral.INSTANCE)) {
            ContextName name = CdiCamelFactory.getFirstElementOfType(bean.getQualifiers(), ContextName.class);
            addRouteToContext(bean, name != null ? camelContexts.get(name.value()) : defaultContext, manager, adv);
        }

        // Trigger eager beans instantiation
        for (AnnotatedType<?> type : eagerBeans)
            // Calling toString is necessary to force the initialization of normal-scoped beans
            BeanManagerHelper.getReferencesByType(manager, type.getBaseType(), AnyLiteral.INSTANCE).toString();
    }

    private void addRouteToContext(Bean<?> route, CamelContext context, BeanManager manager, AfterDeploymentValidation adv) {
        try {
            context.addRoutes(BeanManagerHelper.getReferenceByType(manager, RoutesBuilder.class, route));
        } catch (Exception exception) {
            adv.addDeploymentProblem(exception);
        }
    }

    CamelContext getCamelContext(String name) {
        return camelContexts.get(name);
    }
}
