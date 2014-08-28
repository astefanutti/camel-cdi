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
import org.apache.camel.Converter;
import org.apache.camel.RoutesBuilder;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.AfterDeploymentValidation;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.inject.spi.WithAnnotations;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class CdiCamelExtension implements Extension {

    private final Set<Class<?>> typeConverters = Collections.newSetFromMap(new ConcurrentHashMap<Class<?>, Boolean>());

    void processTypeConverters(@Observes @WithAnnotations(Converter.class) ProcessAnnotatedType<?> event) {
        typeConverters.add(event.getAnnotatedType().getJavaClass());
    }

    private void addDefaultCamelContext(@Observes AfterBeanDiscovery abd, BeanManager manager) {
        if (manager.getBeans(CamelContext.class, AnyLiteral.INSTANCE, DefaultLiteral.INSTANCE).isEmpty())
            abd.addBean(new CdiCamelContextBean(manager));
    }

    void configureCamelContext(@Observes AfterDeploymentValidation event, BeanManager manager) {
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
                event.addDeploymentProblem(exception);
            }
        }
    }
}
