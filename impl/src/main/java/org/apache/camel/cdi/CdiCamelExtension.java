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

import org.apache.camel.BeanInject;
import org.apache.camel.CamelContext;
import org.apache.camel.CamelContextAware;
import org.apache.camel.Consume;
import org.apache.camel.Converter;
import org.apache.camel.Endpoint;
import org.apache.camel.EndpointInject;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.PropertyInject;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.management.event.AbstractExchangeEvent;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.AfterDeploymentValidation;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanAttributes;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.DeploymentException;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.ObserverMethod;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.inject.spi.ProcessBeanAttributes;
import javax.enterprise.inject.spi.ProcessInjectionPoint;
import javax.enterprise.inject.spi.ProcessInjectionTarget;
import javax.enterprise.inject.spi.ProcessObserverMethod;
import javax.enterprise.inject.spi.ProcessProducerMethod;
import javax.enterprise.inject.spi.WithAnnotations;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.EnumSet;
import java.util.EventObject;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class CdiCamelExtension implements Extension {

    private final Set<Class<?>> converters = Collections.newSetFromMap(new ConcurrentHashMap<Class<?>, Boolean>());

    private final Set<AnnotatedType<?>> camelBeans = Collections.newSetFromMap(new ConcurrentHashMap<AnnotatedType<?>, Boolean>());

    private final Set<AnnotatedType<?>> eagerBeans = Collections.newSetFromMap(new ConcurrentHashMap<AnnotatedType<?>, Boolean>());

    private final Map<InjectionPoint, ForwardingObserverMethod<?>> cdiEventEndpoints = new ConcurrentHashMap<>();

    private final Map<ContextName, EnumSet<ContextInfo>> namedContexts = new ConcurrentHashMap<>();

    private final EnumSet<ContextInfo> defaultContext = EnumSet.noneOf(ContextInfo.class);

    EnumSet<ContextInfo> getContextInfo(ContextName name) {
        return name != null ? namedContexts.get(name) : defaultContext;
    }

    ForwardingObserverMethod<?> getObserverMethod(InjectionPoint ip) {
        return cdiEventEndpoints.get(ip);
    }

    private void typeConverters(@Observes @WithAnnotations(Converter.class) ProcessAnnotatedType<?> pat) {
        converters.add(pat.getAnnotatedType().getJavaClass());
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

    private void camelContextNames(@Observes ProcessAnnotatedType<? extends CamelContext> pat) {
        if (pat.getAnnotatedType().isAnnotationPresent(ContextName.class))
            namedContexts.put(pat.getAnnotatedType().getAnnotation(ContextName.class), EnumSet.noneOf(ContextInfo.class));
    }

    private <T> void camelBeansPostProcessor(@Observes ProcessInjectionTarget<T> pit, BeanManager manager) {
        if (camelBeans.contains(pit.getAnnotatedType()))
            pit.setInjectionTarget(new CdiCamelInjectionTarget<>(pit.getInjectionTarget(), manager));
    }

    private <T> void cdiEventEndpoints(@Observes ProcessInjectionPoint<?, CdiEventEndpoint<T>> pip) {
        InjectionPoint ip = pip.getInjectionPoint();
        // TODO: refine the key to the type and qualifiers instead of the whole injection point as it leads to registering redundant observers
        cdiEventEndpoints.put(ip, new ForwardingObserverMethod<T>(((ParameterizedType) ip.getType()).getActualTypeArguments()[0], ip.getQualifiers()));
    }

    private <T extends Endpoint> void endpointProducers(@Observes ProcessBeanAttributes<T> pba) {
        if (CdiEventEndpoint.class.equals(CdiSpiHelper.getRawType(pba.getAnnotated().getBaseType()))) {
            Set<Annotation> qualifiers = new HashSet<>();
            for (InjectionPoint ip : cdiEventEndpoints.keySet())
                qualifiers.addAll(ip.getQualifiers());
            pba.setBeanAttributes(new BeanAttributesDecorator<>(pba.getBeanAttributes(), qualifiers));
        } else {
            pba.setBeanAttributes(new BeanAttributesDecorator<>(pba.getBeanAttributes(), namedContexts.keySet()));
        }
    }

    private void producerTemplates(@Observes ProcessBeanAttributes<ProducerTemplate> pba) {
        pba.setBeanAttributes(new BeanAttributesDecorator<>(pba.getBeanAttributes(), namedContexts.keySet()));
    }

    private void camelEventNotifiers(@Observes ProcessObserverMethod<? extends EventObject, ?> pom) {
        Type type = pom.getObserverMethod().getObservedType();
        // Camel events are raw types
        if (type instanceof Class && Class.class.cast(type).getPackage().equals(AbstractExchangeEvent.class.getPackage())) {
            Set<Annotation> qualifiers = pom.getObserverMethod().getObservedQualifiers();
            if (qualifiers.isEmpty()) {
                defaultContext.add(ContextInfo.EventNotifierSupport);
                for (EnumSet<ContextInfo> info : namedContexts.values())
                    info.add(ContextInfo.EventNotifierSupport);
            } else {
                for (Annotation qualifier : qualifiers)
                    if (qualifier instanceof ContextName)
                        namedContexts.get(qualifier).add(ContextInfo.EventNotifierSupport);
                    else if (qualifier instanceof Default)
                        defaultContext.add(ContextInfo.EventNotifierSupport);
            }
        }
    }

    private void addDefaultCamelContext(@Observes AfterBeanDiscovery abd, BeanManager manager) {
        if (manager.getBeans(CamelContext.class, AnyLiteral.INSTANCE).isEmpty())
            abd.addBean(new CdiCamelContextBean(manager));
    }

    private void addCdiEventObserverMethods(@Observes AfterBeanDiscovery abd) {
        // TODO: it happens that, while the observer method is declared @Default, it's treated as if it is declared @Any. Check Weld and OWB implementations.
        for (ObserverMethod method : cdiEventEndpoints.values())
            abd.addObserverMethod(method);
    }

    private void configureCamelContexts(@Observes AfterDeploymentValidation adv, BeanManager manager) {
        String defaultContextName = "camel-cdi";
        // Instantiate the Camel contexts
        Map<String, CamelContext> camelContexts = new HashMap<>();
        for (Bean<?> bean : manager.getBeans(CdiCamelContext.class, AnyLiteral.INSTANCE)) {
            ContextName name = CdiSpiHelper.getQualifierByType(bean, ContextName.class);
            CdiCamelContext context = BeanManagerHelper.getReferenceByType(manager, CdiCamelContext.class, name != null ? name : DefaultLiteral.INSTANCE);
            if (name == null)
                defaultContextName = context.getName();
            camelContexts.put(context.getName(), context);
            // TODO: register the Camel context into the registry for the context component (see http://camel.apache.org/context.html)
        }

        // Add type converters to the Camel contexts
        CdiTypeConverterLoader loader = new CdiTypeConverterLoader();
        for (Class<?> converter : converters)
            for (CamelContext context : camelContexts.values())
                loader.loadConverterMethods(context.getTypeConverterRegistry(), converter);

        // Instantiate route builders and add them to the corresponding Camel contexts
        // This should ideally be done in the @PostConstruct callback of the CdiCamelContext class as this would enable custom Camel contexts that inherit from CdiCamelContext and start the context in their own @PostConstruct callback with their routes already added. However, that leads to circular dependencies between the RouteBuilder beans and the CdiCamelContext bean itself.
        for (Bean<?> bean : manager.getBeans(RoutesBuilder.class, AnyLiteral.INSTANCE)) {
            ContextName name = CdiSpiHelper.getQualifierByType(bean, ContextName.class);
            CamelContext context = camelContexts.get(name != null ? name.value() : defaultContextName);
            if (context != null)
                addRouteToContext(bean, context, manager, adv);
            else
                adv.addDeploymentProblem(new DeploymentException("No corresponding Camel context found for RouteBuilder bean [" + bean + "]"));
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
}
