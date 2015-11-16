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
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.AfterDeploymentValidation;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.DeploymentException;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.ObserverMethod;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.inject.spi.ProcessBean;
import javax.enterprise.inject.spi.ProcessBeanAttributes;
import javax.enterprise.inject.spi.ProcessInjectionPoint;
import javax.enterprise.inject.spi.ProcessInjectionTarget;
import javax.enterprise.inject.spi.ProcessObserverMethod;
import javax.enterprise.inject.spi.ProcessProducer;
import javax.enterprise.inject.spi.ProcessProducerField;
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
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class CdiCamelExtension implements Extension {

    private final Set<Class<?>> converters = Collections.newSetFromMap(new ConcurrentHashMap<Class<?>, Boolean>());

    private final Set<AnnotatedType<?>> camelBeans = Collections.newSetFromMap(new ConcurrentHashMap<AnnotatedType<?>, Boolean>());

    private final Set<AnnotatedType<?>> eagerBeans = Collections.newSetFromMap(new ConcurrentHashMap<AnnotatedType<?>, Boolean>());

    private final Map<InjectionPoint, ForwardingObserverMethod<?>> cdiEventEndpoints = new ConcurrentHashMap<>();

    private final Map<ContextName, EnumSet<ContextInfo>> namedContexts = new ConcurrentHashMap<>();

    private final EnumSet<ContextInfo> anyContext = EnumSet.noneOf(ContextInfo.class);

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

    private <T extends CamelContext> void camelContextBeans(@Observes ProcessInjectionTarget<T> pit, BeanManager manager) {
        pit.setInjectionTarget(new CamelContextInjectionTarget<>(pit.getInjectionTarget(), pit.getAnnotatedType(), manager));
    }

    private <T extends CamelContext> void camelContextProducers(@Observes ProcessProducer<?, T> pp, BeanManager manager) {
        pp.setProducer(new CamelContextProducer<>(pp.getProducer(), pp.getAnnotatedMember(), manager));
    }

    private <T> void camelBeansPostProcessor(@Observes ProcessInjectionTarget<T> pit, BeanManager manager) {
        if (camelBeans.contains(pit.getAnnotatedType()))
            pit.setInjectionTarget(new CamelBeanInjectionTarget<>(pit.getInjectionTarget(), manager));
    }

    private <T> void cdiEventEndpoints(@Observes ProcessInjectionPoint<?, CdiEventEndpoint<T>> pip) {
        InjectionPoint ip = pip.getInjectionPoint();
        // TODO: refine the key to the type and qualifiers instead of the whole injection point as it leads to registering redundant observers
        cdiEventEndpoints.put(ip, new ForwardingObserverMethod<T>(((ParameterizedType) ip.getType()).getActualTypeArguments()[0], ip.getQualifiers()));
    }

    private <T extends Endpoint> void endpointProducers(@Observes ProcessBeanAttributes<T> pba) {
        // Veto the bean as we first need to collect the metadata during the bean discovery phase. The bean attributes decoration is done after the bean discovery.
        if (pba.getAnnotated() instanceof AnnotatedMethod && CdiCamelFactory.class.equals(((AnnotatedMethod) pba.getAnnotated()).getDeclaringType().getJavaClass()))
            pba.veto();
    }

    private void producerTemplates(@Observes ProcessBeanAttributes<ProducerTemplate> pba) {
        // Veto the bean as we first need to collect the metadata during the bean discovery phase. The bean attributes decoration is done after the bean discovery.
        if (pba.getAnnotated() instanceof AnnotatedMethod && CdiCamelFactory.class.equals(((AnnotatedMethod) pba.getAnnotated()).getDeclaringType().getJavaClass()))
            pba.veto();
    }

    private void camelEventNotifiers(@Observes ProcessObserverMethod<? extends EventObject, ?> pom) {
        Type type = pom.getObserverMethod().getObservedType();
        // Camel events are raw types
        if (type instanceof Class && Class.class.cast(type).getPackage().equals(AbstractExchangeEvent.class.getPackage())) {
            Set<Annotation> qualifiers = pom.getObserverMethod().getObservedQualifiers();
            if (qualifiers.isEmpty()) {
                defaultContext.add(ContextInfo.EventNotifierSupport);
                anyContext.add(ContextInfo.EventNotifierSupport);
            } else {
                for (Annotation qualifier : qualifiers)
                    if (qualifier instanceof ContextName)
                        // Use the functional API introduced in JDK 8 when it becomes a pre-requisite
                        if (namedContexts.containsKey(qualifier))
                            namedContexts.get(qualifier).add(ContextInfo.EventNotifierSupport);
                        else
                            namedContexts.put((ContextName) qualifier, EnumSet.of(ContextInfo.EventNotifierSupport));
                    else if (qualifier instanceof Default)
                        defaultContext.add(ContextInfo.EventNotifierSupport);
                    // FIXME: support for explicit @Any qualifier
            }
        }
    }

    private <T extends CamelContext> void camelContextBeans(@Observes ProcessBean<T> pb) {
        processCamelContextBean(pb.getBean());
    }

    private <T extends CamelContext> void camelContextProducerFields(@Observes ProcessProducerField<T, ?> pb) {
        processCamelContextBean(pb.getBean());
    }

    private <T extends CamelContext> void camelContextProducerMethods(@Observes ProcessProducerMethod<T, ?> pb) {
        processCamelContextBean(pb.getBean());
    }

    private void processCamelContextBean(Bean<?> bean) {
        ContextName name = CdiSpiHelper.getQualifierByType(bean, ContextName.class);
        if (name != null) {
            // Use the functional API introduced in JDK 8 when it becomes a pre-requisite
            if (namedContexts.containsKey(name))
                namedContexts.get(name).add(ContextInfo.ProcessBean);
            else
                namedContexts.put(name, EnumSet.of(ContextInfo.ProcessBean));
        } else {
            defaultContext.add(ContextInfo.ProcessBean);
        }
    }

    private void cdiCamelFactoryProducers(@Observes AfterBeanDiscovery abd, BeanManager manager) {
        // Do not take contexts info that do not correspond to any deployed bean into account
        Iterator<EnumSet<ContextInfo>> it = namedContexts.values().iterator();
        while (it.hasNext())
            if (!it.next().contains(ContextInfo.ProcessBean))
                it.remove();
        // Decorate the CDI Camel factory beans with the metadata gathered during the bean discovery phase
        Bean<CdiCamelFactory> bean = (Bean<CdiCamelFactory>) manager.resolve(manager.getBeans(CdiCamelFactory.class));
        for (AnnotatedMethod<? super CdiCamelFactory> am : abd.getAnnotatedType(CdiCamelFactory.class, null).getMethods()) {
            if (!am.isAnnotationPresent(Produces.class))
                continue;
            Class<?> type = CdiSpiHelper.getRawType(am.getBaseType());
            if (CdiEventEndpoint.class.equals(type)) {
                Set<Annotation> qualifiers = new HashSet<>();
                // TODO: exclude @ContextName qualifiers from injection points
                for (InjectionPoint ip : cdiEventEndpoints.keySet())
                    qualifiers.addAll(ip.getQualifiers());
                abd.addBean(manager.createBean(new BeanAttributesDecorator<>(manager.createBeanAttributes(am), qualifiers), CdiCamelFactory.class, manager.getProducerFactory(am, bean)));
            } else if (Endpoint.class.isAssignableFrom(type) || ProducerTemplate.class.isAssignableFrom(type)) {
                abd.addBean(manager.createBean(new BeanAttributesDecorator<>(manager.createBeanAttributes(am), namedContexts.keySet()), CdiCamelFactory.class, manager.getProducerFactory(am, bean)));
            }
        }
    }

    private void addDefaultCamelContext(@Observes AfterBeanDiscovery abd, BeanManager manager) {
        if (manager.getBeans(CamelContext.class, AnyLiteral.INSTANCE).isEmpty())
            abd.addBean(new CdiCamelContextBean(manager, defaultContext));
    }

    private void addCdiEventObserverMethods(@Observes AfterBeanDiscovery abd) {
        for (ObserverMethod method : cdiEventEndpoints.values())
            abd.addObserverMethod(method);
    }

    private void updateContextInfo(@Observes AfterBeanDiscovery abd) {
        if (anyContext.contains(ContextInfo.EventNotifierSupport)) {
            defaultContext.add(ContextInfo.EventNotifierSupport);
            for (EnumSet<ContextInfo> context : namedContexts.values())
                context.add(ContextInfo.EventNotifierSupport);
        }
    }

    private void createCamelContexts(@Observes AfterDeploymentValidation adv, BeanManager manager) {
        String defaultContextName = "camel-cdi";
        // Instantiate the Camel contexts
        Map<String, CamelContext> camelContexts = new HashMap<>();
        for (Bean<?> bean : manager.getBeans(CamelContext.class, AnyLiteral.INSTANCE)) {
            ContextName name = CdiSpiHelper.getQualifierByType(bean, ContextName.class);
            CamelContext context = BeanManagerHelper.getReferenceByType(manager, CamelContext.class, name != null ? name : DefaultLiteral.INSTANCE);
            if (name == null)
                defaultContextName = context.getName();
            camelContexts.put(context.getName(), context);
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
            BeanManagerHelper.getReferencesByType(manager, type.getJavaClass(), AnyLiteral.INSTANCE).toString();

        // Clean-up
        converters.clear();
        camelBeans.clear();
        eagerBeans.clear();
    }

    private void addRouteToContext(Bean<?> route, CamelContext context, BeanManager manager, AfterDeploymentValidation adv) {
        try {
            context.addRoutes(BeanManagerHelper.getReferenceByType(manager, RoutesBuilder.class, route));
        } catch (Exception exception) {
            adv.addDeploymentProblem(exception);
        }
    }
}
