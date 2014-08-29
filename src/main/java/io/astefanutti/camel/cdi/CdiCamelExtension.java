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

import org.apache.camel.BeanInject;
import org.apache.camel.CamelContext;
import org.apache.camel.CamelContextAware;
import org.apache.camel.Converter;
import org.apache.camel.EndpointInject;
import org.apache.camel.Produce;
import org.apache.camel.PropertyInject;
import org.apache.camel.RoutesBuilder;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.AfterDeploymentValidation;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.inject.spi.ProcessBean;
import javax.enterprise.inject.spi.ProcessInjectionTarget;
import javax.enterprise.inject.spi.WithAnnotations;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class CdiCamelExtension implements Extension {

    private boolean hasCamelContext;

    private final Set<Class<?>> typeConverters = Collections.newSetFromMap(new ConcurrentHashMap<Class<?>, Boolean>());

    private final Set<AnnotatedType<?>> camelBeans = Collections.newSetFromMap(new ConcurrentHashMap<AnnotatedType<?>, Boolean>());

    private void processTypeConverters(@Observes @WithAnnotations(Converter.class) ProcessAnnotatedType<?> pat) {
        typeConverters.add(pat.getAnnotatedType().getJavaClass());
    }

    private void processCamelContextAware(@Observes ProcessAnnotatedType<? extends CamelContextAware> pat) {
        camelBeans.add(pat.getAnnotatedType());
    }

    private void processCamelAnnotations(@Observes @WithAnnotations({BeanInject.class, EndpointInject.class, Produce.class, PropertyInject.class, }) ProcessAnnotatedType<?> pat) {
        camelBeans.add(pat.getAnnotatedType());
    }

    private <T> void camelBeanIntegrationPostProcessor(@Observes ProcessInjectionTarget<T> pit, BeanManager manager) {
        if (camelBeans.contains(pit.getAnnotatedType()))
            pit.setInjectionTarget(new CdiCamelInjectionTarget<>(pit.getInjectionTarget(), manager));
    }

    // FIXME: remove when bean manager solution with ProcessInjectionTarget decorator works
    private void processCamelContextBean(@Observes ProcessBean<? extends CamelContext> pb) {
        hasCamelContext = true;
    }

    private void addDefaultCamelContext(@Observes AfterBeanDiscovery abd, BeanManager manager) {
        // FIXME: understand why this is not working anymore when ProcessInjectionTarget is decorated in processCamelAnnotations
        //if (manager.getBeans(CamelContext.class, AnyLiteral.INSTANCE, DefaultLiteral.INSTANCE).isEmpty())
        if (!hasCamelContext)
            abd.addBean(new CdiCamelContextBean(manager));
    }

    private void configureCamelContext(@Observes AfterDeploymentValidation adv, BeanManager manager) {
        CamelContext context = BeanManagerHelper.getContextualReference(manager, CamelContext.class, false);

        // add type converter beans to the Camel context
        if (!typeConverters.isEmpty()) {
            CdiTypeConverterLoader loader = new CdiTypeConverterLoader();
            for (Class<?> typeConverter : typeConverters)
                loader.loadConverterMethods(context.getTypeConverterRegistry(), typeConverter);
        }

        // instantiate route builders and add them to the Camel context
        for (RoutesBuilder builder : BeanManagerHelper.getContextualReferences(manager, RoutesBuilder.class)) {
            try {
                context.addRoutes(builder);
            } catch (Exception exception) {
                adv.addDeploymentProblem(exception);
            }
        }
    }
}
