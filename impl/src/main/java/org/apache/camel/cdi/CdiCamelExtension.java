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
import org.apache.camel.ServiceStatus;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.management.event.AbstractExchangeEvent;
import org.apache.camel.model.RouteContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.Any;
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
import javax.enterprise.inject.spi.InjectionTargetFactory;
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
import javax.inject.Named;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EventObject;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Collections.newSetFromMap;

public class CdiCamelExtension implements Extension {

    private final Logger logger = LoggerFactory.getLogger(CdiCamelExtension.class);

    private final CdiCamelEnvironment environment = new CdiCamelEnvironment();

    private final Set<Class<?>> converters = newSetFromMap(new ConcurrentHashMap<>());

    private final Set<AnnotatedType<?>> camelBeans = newSetFromMap(new ConcurrentHashMap<>());

    private final Set<AnnotatedType<?>> eagerBeans = newSetFromMap(new ConcurrentHashMap<>());

    private final Map<InjectionPoint, ForwardingObserverMethod<?>> cdiEventEndpoints = new ConcurrentHashMap<>();

    private final Set<Annotation> contextQualifiers = newSetFromMap(new ConcurrentHashMap<>());

    private final Set<Annotation> eventQualifiers = newSetFromMap(new ConcurrentHashMap<>());

    ForwardingObserverMethod<?> getObserverMethod(InjectionPoint ip) {
        return cdiEventEndpoints.get(ip);
    }

    Set<Annotation> getObserverEvents() {
        return eventQualifiers;
    }

    Set<Annotation> getContextQualifiers() {
        return contextQualifiers;
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
        pit.setInjectionTarget(environment.camelContextInjectionTarget(pit.getInjectionTarget(), pit.getAnnotatedType(), manager));
    }

    private <T extends CamelContext> void camelContextProducers(@Observes ProcessProducer<?, T> pp, BeanManager manager) {
        pp.setProducer(environment.camelContextProducer(pp.getProducer(), pp.getAnnotatedMember(), manager));
    }

    private <T> void camelBeansPostProcessor(@Observes ProcessInjectionTarget<T> pit, BeanManager manager) {
        if (camelBeans.contains(pit.getAnnotatedType()))
            pit.setInjectionTarget(new CamelBeanInjectionTarget<>(pit.getInjectionTarget(), manager));
    }

    private void cdiEventEndpoints(@Observes ProcessInjectionPoint<?, CdiEventEndpoint> pip) {
        InjectionPoint ip = pip.getInjectionPoint();
        // FIXME: to be removed when OWB-1102 is fixed
        if (!CdiEventEndpoint.class.isAssignableFrom(CdiSpiHelper.getRawType(ip.getType())))
            return;
        // TODO: refine the key to the type and qualifiers instead of the whole injection point as it leads to registering redundant observers
        if (ip.getType() instanceof ParameterizedType)
            cdiEventEndpoints.put(ip, new ForwardingObserverMethod<>(((ParameterizedType) ip.getType()).getActualTypeArguments()[0], ip.getQualifiers()));
        else if (ip.getType() instanceof Class)
            cdiEventEndpoints.put(ip, new ForwardingObserverMethod<>(Object.class, ip.getQualifiers()));
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

    private <T extends EventObject> void camelEventNotifiers(@Observes ProcessObserverMethod<T, ?> pom) {
        // Only activate Camel event notifiers for explicit Camel event observers, that is, an observer method for a super type won't activate notifiers.
        Type type = pom.getObserverMethod().getObservedType();
        // Camel events are raw types
        if (type instanceof Class && Class.class.cast(type).getPackage().equals(AbstractExchangeEvent.class.getPackage()))
            eventQualifiers.addAll(pom.getObserverMethod().getObservedQualifiers().isEmpty() ? Collections.singleton(AnyLiteral.INSTANCE) : pom.getObserverMethod().getObservedQualifiers());
    }

    private <T extends CamelContext> void camelContextBeans(@Observes ProcessBean<T> pb) {
        contextQualifiers.addAll(pb.getBean().getQualifiers());
    }

    private <T extends CamelContext> void camelContextProducerFields(@Observes ProcessProducerField<T, ?> pb) {
        contextQualifiers.addAll(pb.getBean().getQualifiers());
    }

    private <T extends CamelContext> void camelContextProducerMethods(@Observes ProcessProducerMethod<T, ?> pb) {
        contextQualifiers.addAll(pb.getBean().getQualifiers());
    }

    private void afterBeanDiscovery(@Observes AfterBeanDiscovery abd, BeanManager manager) {
        // Add @ContextName Camel context beans if missing
        long contexts = manager.getBeans(CamelContext.class, AnyLiteral.INSTANCE).size();
        contexts += manager.getBeans(RoutesBuilder.class, AnyLiteral.INSTANCE).stream()
            .flatMap(bean -> bean.getQualifiers().stream())
            .filter(qualifier -> ContextName.class.equals(qualifier.annotationType()))
            .filter(name -> manager.getBeans(CamelContext.class, name).isEmpty())
            .peek(contextQualifiers::add)
            .map(name -> camelContextBean(manager, AnyLiteral.INSTANCE, name))
            .peek(abd::addBean)
            .count();
        // Add a default Camel context bean if any
        if (contexts == 0)
            abd.addBean(camelContextBean(manager, AnyLiteral.INSTANCE, DefaultLiteral.INSTANCE));

        // Update the CDI Camel factory beans
        Bean<CdiCamelFactory> bean = (Bean<CdiCamelFactory>) manager.resolve(manager.getBeans(CdiCamelFactory.class));
        for (AnnotatedMethod<? super CdiCamelFactory> am : abd.getAnnotatedType(CdiCamelFactory.class, null).getMethods()) {
            if (!am.isAnnotationPresent(Produces.class))
                continue;
            // TODO: would be more correct to add a bean for each Camel context bean
            Class<?> type = CdiSpiHelper.getRawType(am.getBaseType());
            if (CdiEventEndpoint.class.equals(type)) {
                Set<Annotation> qualifiers = new HashSet<>();
                for (InjectionPoint ip : cdiEventEndpoints.keySet())
                    qualifiers.addAll(ip.getQualifiers());
                abd.addBean(manager.createBean(new BeanAttributesDecorator<>(manager.createBeanAttributes(am), qualifiers), CdiCamelFactory.class, manager.getProducerFactory(am, bean)));
            } else if (Endpoint.class.isAssignableFrom(type) || ProducerTemplate.class.isAssignableFrom(type)) {
                abd.addBean(manager.createBean(new BeanAttributesDecorator<>(manager.createBeanAttributes(am), CdiSpiHelper.excludeElementOfTypes(contextQualifiers, Any.class, Default.class, Named.class)), CdiCamelFactory.class, manager.getProducerFactory(am, bean)));
            }
        }

        // Add CDI event endpoint observer methods
        cdiEventEndpoints.values().forEach(abd::addObserverMethod);
    }

    private Bean<?> camelContextBean(BeanManager manager, Annotation... qualifiers) {
        CamelContextBeanAnnotated annotated = new CamelContextBeanAnnotated(manager, qualifiers);
        return manager.createBean(new CamelContextBeanAttributes(annotated), DefaultCamelContext.class, (InjectionTargetFactory<DefaultCamelContext>) bean -> environment.camelContextInjectionTarget(new CamelContextDefaultProducer(), annotated, manager));
    }

    private void afterDeploymentValidation(@Observes AfterDeploymentValidation adv, BeanManager manager) {
        Collection<CamelContext> contexts = new ArrayList<>();
        for (Bean<?> context : manager.getBeans(CamelContext.class, AnyLiteral.INSTANCE))
            contexts.add(BeanManagerHelper.getReference(manager, CamelContext.class, context));

        // Add type converters to Camel contexts
        CdiTypeConverterLoader loader = new CdiTypeConverterLoader();
        for (Class<?> converter : converters)
            for (CamelContext context : contexts)
                loader.loadConverterMethods(context.getTypeConverterRegistry(), converter);

        // Add routes to Camel contexts
        boolean deploymentException = false;
        Set<Bean<?>> routes = new HashSet<>(manager.getBeans(RoutesBuilder.class, AnyLiteral.INSTANCE));
        routes.addAll(manager.getBeans(RouteContainer.class, AnyLiteral.INSTANCE));
        for (Bean<?> context : manager.getBeans(CamelContext.class, AnyLiteral.INSTANCE))
            for (Bean<?> route : routes) {
                Set<Annotation> qualifiers = new HashSet<>(context.getQualifiers());
                qualifiers.retainAll(route.getQualifiers());
                if (qualifiers.size() > 1)
                    deploymentException |= !addRouteToContext(route, context, manager, adv);
            }
        // Let's return to avoid starting misconfigured contexts
        if (deploymentException)
            return;

        // Trigger eager beans instantiation
        for (AnnotatedType<?> type : eagerBeans)
            // Calling toString is necessary to force the initialization of normal-scoped beans
            BeanManagerHelper.getReferencesByType(manager, type.getJavaClass(), AnyLiteral.INSTANCE).toString();

        // Start Camel contexts
        for (CamelContext context : contexts) {
            if (ServiceStatus.Started.equals(context.getStatus()))
                continue;
            logger.info("Camel CDI is starting Camel context [{}]", context.getName());
            try {
                context.start();
            } catch (Exception exception) {
                adv.addDeploymentProblem(exception);
            }
        }

        // Clean-up
        converters.clear();
        camelBeans.clear();
        eagerBeans.clear();
    }

    private boolean addRouteToContext(Bean<?> routeBean, Bean<?> contextBean, BeanManager manager, AfterDeploymentValidation adv) {
        try {
            CamelContext context = BeanManagerHelper.getReference(manager, CamelContext.class, contextBean);
            try {
                Object route = BeanManagerHelper.getReference(manager, Object.class, routeBean);
                if (route instanceof RoutesBuilder)
                    context.addRoutes((RoutesBuilder) route);
                else if (route instanceof RouteContainer)
                    context.addRouteDefinitions(((RouteContainer) route).getRoutes());
                else
                    throw new IllegalArgumentException("Invalid routes type [" + routeBean.getBeanClass().getName() + "], must be either of type RoutesBuilder or RouteContainer!");
                return true;
            } catch (Exception cause) {
                adv.addDeploymentProblem(new DeploymentException("Error adding routes of type [" + routeBean.getBeanClass().getName() + "] to Camel context [" + context.getName() + "]", cause));
            }
        } catch (Exception exception) {
            adv.addDeploymentProblem(exception);
        }
        return false;
    }
}
