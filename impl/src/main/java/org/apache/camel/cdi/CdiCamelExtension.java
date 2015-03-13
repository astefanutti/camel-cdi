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
import static org.apache.camel.cdi.CdiSpiHelper.getAnnotationsWithMeta;
import static org.apache.camel.cdi.CdiSpiHelper.hasAnnotation;
import static org.apache.camel.cdi.CdiSpiHelper.removeQualifiersFromAnnotated;

import java.lang.annotation.Annotation;
import java.lang.reflect.Member;
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
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.AfterDeploymentValidation;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.AnnotatedMember;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.ObserverMethod;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.inject.spi.ProcessBean;
import javax.enterprise.inject.spi.ProcessInjectionTarget;
import javax.enterprise.inject.spi.ProcessObserverMethod;
import javax.enterprise.inject.spi.ProcessProducerField;
import javax.enterprise.inject.spi.ProcessProducerMethod;
import javax.inject.Qualifier;

public class CdiCamelExtension implements Extension {

    private final Set<Class<?>> converters = Collections.newSetFromMap(new ConcurrentHashMap<Class<?>, Boolean>());

    private final Set<AnnotatedType<?>> camelBeans = Collections.newSetFromMap(new ConcurrentHashMap<AnnotatedType<?>, Boolean>());

    private final Set<AnnotatedType<?>> eagerBeans = Collections.newSetFromMap(new ConcurrentHashMap<AnnotatedType<?>, Boolean>());

    private final Map<InjectionPoint, ForwardingObserverMethod<?>> cdiEventEndpoints = new ConcurrentHashMap<InjectionPoint, ForwardingObserverMethod<?>>();

    private final Map<ContextName, EnumSet<ContextInfo>> namedContexts = new ConcurrentHashMap<ContextName, EnumSet<ContextInfo>>();

    private final EnumSet<ContextInfo> defaultContext = EnumSet.noneOf(ContextInfo.class);
    
    boolean customCamelContextPresent = false;
    
    private Map<Annotated, Bean<? extends Endpoint>> endPointsBeans = new ConcurrentHashMap<Annotated, Bean<? extends Endpoint>>();
    
    private Set<Bean<ProducerTemplate>> producerTemplateBeans = new HashSet<Bean<ProducerTemplate>>();
    
    private final Map<Member,Set<Annotation>> memberToQualifiers = new ConcurrentHashMap<Member, Set<Annotation>>();
    
    private final Map<Bean<?>, Member> beanToMember = new ConcurrentHashMap<Bean<?>, Member>();

    EnumSet<ContextInfo> getContextInfo(ContextName name) {
        return name != null ? namedContexts.get(name) : defaultContext;
    }

    ForwardingObserverMethod<?> getObserverMethod(InjectionPoint ip) {
        return cdiEventEndpoints.get(ip);
    }

    
    private void typeConverters(@Observes ProcessAnnotatedType<?> pat) {
        AnnotatedType<?> at = pat.getAnnotatedType();
        if (hasAnnotation(at, Converter.class))
            converters.add(at.getJavaClass());
    }


    private void veto(@Observes ProcessAnnotatedType<?> pat) {
        if (pat.getAnnotatedType().isAnnotationPresent(ToVeto.class))
            pat.veto();
    }

    private void checkCustomCamelContext(@Observes ProcessAnnotatedType<CamelContext> pat) {
        AnnotatedType<CamelContext> at = pat.getAnnotatedType();
        customCamelContextPresent = !at.isAnnotationPresent(ToVeto.class);
    }
    
    private void camelContextAware(@Observes ProcessAnnotatedType<? extends CamelContextAware> pat) {
        camelBeans.add(pat.getAnnotatedType());
    }

    private void camelAnnotations(@Observes ProcessAnnotatedType<?> pat) {
        AnnotatedType<?> at = pat.getAnnotatedType();
        if (hasAnnotation(at, BeanInject.class) || hasAnnotation(at, Consume.class) ||
                hasAnnotation(at, EndpointInject.class) || hasAnnotation(at, Produce.class) || hasAnnotation(at,
                PropertyInject.class))
            camelBeans.add(pat.getAnnotatedType());
    }

    private void consumeBeans(@Observes ProcessAnnotatedType<?> pat) {
        if ((hasAnnotation(pat.getAnnotatedType(), Consume.class)))
            eagerBeans.add(pat.getAnnotatedType());
    }

    private void camelContextNames(@Observes ProcessAnnotatedType<? extends CamelContext> pat) {
        if (pat.getAnnotatedType().isAnnotationPresent(ContextName.class))
            namedContexts.put(pat.getAnnotatedType().getAnnotation(ContextName.class), EnumSet.noneOf(ContextInfo.class));
    }

    private <T> void camelBeansPostProcessor(@Observes ProcessInjectionTarget<T> pit, BeanManager manager) {
        if (camelBeans.contains(pit.getAnnotatedType()))
            pit.setInjectionTarget(new CdiCamelInjectionTarget<T>(pit.getInjectionTarget(), manager));
    }
    

    private <T> void enrichAnnotatedTypeForClass(ProcessAnnotatedType<T> pat, Class<?> clazz) {
        AnnotatedType<T> at = pat.getAnnotatedType();
        Set<Annotation> annotations;
       
        if (at.getTypeClosure().contains(clazz)) {
            annotations = removeQualifiersFromAnnotated(at);
            annotations.add(HiddenLiteral.INSTANCE);
            pat.setAnnotatedType(new ReannotedAnnotatedType(at, annotations));
        } else {
            Set<AnnotatedMember<? super T>> def = CdiSpiHelper.getProducerMemberForType(at, clazz);
            if (!def.isEmpty()) {
                for (AnnotatedMember<? super T> member : def) {
                     memberToQualifiers.put(member.getJavaMember(), getAnnotationsWithMeta(member, Qualifier.class));
                    annotations = removeQualifiersFromAnnotated(member);
                    annotations.add(HiddenLiteral.INSTANCE);
                    at = (new ReannotedAnnotatedType(at, member, annotations));
                }
                pat.setAnnotatedType(at);
            }
        }
    }
    
  
    private <T> void collectCdiEventEndpointInjectionPoint(@Observes ProcessBean<?> pb) {
        Set<InjectionPoint> ipSet= pb.getBean().getInjectionPoints();
        for (InjectionPoint ip : ipSet) {
            if(CdiSpiHelper.getRawType(ip.getType()).equals(CdiEventEndpoint.class)) {
                cdiEventEndpoints.put(ip, new ForwardingObserverMethod<T>(((ParameterizedType) ip.getType()).getActualTypeArguments()[0], ip.getQualifiers()));
            }
        }
        
    }

    
    private void collectAnnotatedMethodForBeans(@Observes ProcessProducerMethod<?, ?> pb) {
        collectAnnotatedFromProcessBean(pb.getBean(),pb.getAnnotatedProducerMethod().getJavaMember());
    }

    private void collectAnnotatedFieldForBeans(@Observes ProcessProducerField<?, ?> pb) {
        collectAnnotatedFromProcessBean(pb.getBean(),pb.getAnnotatedProducerField().getJavaMember());
    }

    private <T> void collectAnnotatedFromProcessBean(Bean<T> bean, Member member) {
        if (memberToQualifiers.keySet().contains(member))
            beanToMember.put(bean, member);
    }


    private void endpointObserver(@Observes ProcessProducerMethod<Endpoint, ?> pb) {
        endPointsBeans.put(pb.getAnnotated(), (Bean<? extends Endpoint>) pb.getBean());
    }
    
    private void processEndPointsProducer(@Observes ProcessAnnotatedType<?> pat) {
        enrichAnnotatedTypeForClass(pat, Endpoint.class);
    }

    private void processProducerTemplateProducer(@Observes ProcessAnnotatedType<?> pat) {
        enrichAnnotatedTypeForClass(pat, ProducerTemplate.class);

    }
    
    
    private void captureProducerTemplateBeans(@Observes ProcessProducerMethod<ProducerTemplate,?> pb) {
        if(pb.getAnnotated().isAnnotationPresent(Hidden.class))
            producerTemplateBeans.add((Bean<ProducerTemplate>) pb.getBean());
    }

    private void registerEndpoints(@Observes AfterBeanDiscovery abd, BeanManager bm) {
        for (Annotated annotated : endPointsBeans.keySet()) {

            final Bean<Endpoint> bean = (Bean<Endpoint>) endPointsBeans.get(annotated);
            Set<Annotation> qualifiers = new HashSet<Annotation>(bean.getQualifiers());
            qualifiers.addAll(memberToQualifiers.get(beanToMember.get(bean)));
            qualifiers.remove(HiddenLiteral.INSTANCE);
            if (CdiEventEndpoint.class.equals(CdiSpiHelper.getRawType(annotated.getBaseType()))) {
                for (InjectionPoint ip : cdiEventEndpoints.keySet()) {
                    qualifiers.addAll(ip.getQualifiers());
                }
                /*if (!cdiEventEndpoints.isEmpty()) {
                    qualifiers.remove(DefaultLiteral.INSTANCE);
                }*/
            } else {
                if (!namedContexts.isEmpty()) {
                   /* qualifiers.remove(DefaultLiteral.INSTANCE);*/
                    qualifiers.addAll(namedContexts.keySet());
                }
            }
            if(qualifiers.size() == 1 && qualifiers.contains(AnyLiteral.INSTANCE))
                qualifiers.add(DefaultLiteral.INSTANCE);
            abd.addBean(new RequalifiedBean<Endpoint>(bean, qualifiers));
        }
        for (Bean<ProducerTemplate> bean : producerTemplateBeans) {
            Set<Annotation> qualifiers = new HashSet<Annotation>(bean.getQualifiers());
            qualifiers.addAll(memberToQualifiers.get(beanToMember.get(bean)));
            qualifiers.remove(HiddenLiteral.INSTANCE);
            qualifiers.addAll(namedContexts.keySet());
            abd.addBean(new RequalifiedBean<ProducerTemplate>(bean, qualifiers));
        }

    }
    
    private void addDefaultCamelContext(@Observes AfterBeanDiscovery abd, BeanManager manager) {
        if (!customCamelContextPresent)
            abd.addBean(new CdiCamelContextBean(manager));

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
    

    private void addCdiEventObserverMethods(@Observes AfterBeanDiscovery abd) {
        // TODO: it happens that, while the observer method is declared @Default, it's treated as if it is declared @Any. Check Weld and OWB implementations.
        for (ObserverMethod method : cdiEventEndpoints.values())
            abd.addObserverMethod(method);
    }

    private void configureCamelContexts(@Observes AfterDeploymentValidation adv, BeanManager manager) {
        String defaultContextName = "camel-cdi";
        // Instantiate the Camel contexts
        Map<String, CamelContext> camelContexts = new HashMap<String, CamelContext>();
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
                adv.addDeploymentProblem(new IllegalStateException("No corresponding Camel context found for RouteBuilder bean "
                        + "[" + bean + "]"));
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
