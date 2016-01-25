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
import org.apache.camel.Endpoint;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.TypeConverter;
import org.apache.camel.component.mock.MockEndpoint;

import javax.enterprise.event.Event;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.InjectionException;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.Typed;
import javax.enterprise.inject.UnsatisfiedResolutionException;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.util.TypeLiteral;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static org.apache.camel.cdi.DefaultLiteral.DEFAULT;

final class CdiCamelFactory {

    @Produces
    private static TypeConverter typeConverter(InjectionPoint ip, @Any Instance<CamelContext> instance, BeanManager manager) {
        return selectContext(ip, instance, manager).getTypeConverter();
    }

    @Uri("")
    @Produces
    // Qualifiers are dynamically added in CdiCamelExtension
    private static ProducerTemplate producerTemplate(InjectionPoint ip, @Any Instance<CamelContext> instance, BeanManager manager) {
        Uri uri = getQualifierByType(ip, Uri.class).get();
        try {
            CamelContext context = uri.context().isEmpty() ? selectContext(ip, instance, manager) : selectContext(uri.context(), instance);
            ProducerTemplate producerTemplate = context.createProducerTemplate();
            // FIXME: avoid NPE caused by missing @Uri qualifier when injection point is @ContextName qualified
            Endpoint endpoint = context.getEndpoint(uri.value(), Endpoint.class);
            producerTemplate.setDefaultEndpoint(endpoint);
            return producerTemplate;
        } catch (Exception cause) {
            throw new InjectionException("Error injecting producer template annotated with " + uri + " into " + ip, cause);
        }
    }

    @Produces
    @Typed(MockEndpoint.class)
    // Qualifiers are dynamically added in CdiCamelExtension
    private static MockEndpoint mockEndpointFromMember(InjectionPoint ip, @Any Instance<CamelContext> instance, BeanManager manager) {
        String uri = "mock:" + ip.getMember().getName();
        try {
            return selectContext(ip, instance, manager).getEndpoint(uri, MockEndpoint.class);
        } catch (Exception cause) {
            throw new InjectionException("Error injecting mock endpoint into " + ip, cause);
        }
    }

    @Uri("")
    @Produces
    @Typed(MockEndpoint.class)
    // Qualifiers are dynamically added in CdiCamelExtension
    private static MockEndpoint mockEndpointFromUri(InjectionPoint ip, @Any Instance<CamelContext> instance, BeanManager manager) {
        Uri uri = getQualifierByType(ip, Uri.class).get();
        try {
            CamelContext context = uri.context().isEmpty() ? selectContext(ip, instance, manager) : selectContext(uri.context(), instance);
            return context.getEndpoint(uri.value(), MockEndpoint.class);
        } catch (Exception cause) {
            throw new InjectionException("Error injecting mock endpoint annotated with " + uri + " into " + ip, cause);
        }
    }

    @Uri("")
    @Produces
    // Qualifiers are dynamically added in CdiCamelExtension
    private static Endpoint endpoint(InjectionPoint ip, @Any Instance<CamelContext> instance, BeanManager manager) {
        Uri uri = getQualifierByType(ip, Uri.class).get();
        try {
            CamelContext context = uri.context().isEmpty() ? selectContext(ip, instance, manager) : selectContext(uri.context(), instance);
            return context.getEndpoint(uri.value(), Endpoint.class);
        } catch (Exception cause) {
            throw new InjectionException("Error injecting endpoint annotated with " + uri + " into " + ip, cause);
        }
    }

    @Produces
    @SuppressWarnings("unchecked")
    // Qualifiers are dynamically added in CdiCamelExtension
    private static <T> CdiEventEndpoint<T> cdiEventEndpoint(InjectionPoint ip, @Any Instance<CamelContext> instance, BeanManager manager, @Any Event<Object> event) throws Exception {
        CamelContext context = selectContext(ip, instance, manager);
        Type type = Object.class;
        if (ip.getType() instanceof ParameterizedType)
            type = ((ParameterizedType) ip.getType()).getActualTypeArguments()[0];
        String uri = eventEndpointUri(type, ip.getQualifiers());
        if (context.hasEndpoint(uri) == null) {
            // FIXME: to be replaced once event firing with dynamic parameterized type is properly supported (see https://issues.jboss.org/browse/CDI-516)
            TypeLiteral<T> literal = new TypeLiteral<T>() {};
            for (Field field : TypeLiteral.class.getDeclaredFields()) {
                if (field.getType().equals(Type.class)) {
                    field.setAccessible(true);
                    field.set(literal, type);
                    break;
                }
            }
            context.addEndpoint(uri, new CdiEventEndpoint<>(event.select(literal, ip.getQualifiers().toArray(new Annotation[ip.getQualifiers().size()])), uri, context, (ForwardingObserverMethod<T>) manager.getExtension(CdiCamelExtension.class).getObserverMethod(ip)));
        }
        return context.getEndpoint(uri, CdiEventEndpoint.class);
    }

    private static <T extends CamelContext> T selectContext(String name, Instance<T> instance) {
        for (T context : instance)
            if (name.equals(context.getName())) return context;
        throw new UnsatisfiedResolutionException("No Camel context with name [" + name + "] is deployed!");
    }

    private static <T extends CamelContext> T selectContext(InjectionPoint ip, Instance<T> instance, BeanManager manager) {
        // TODO: understand why Weld / OSGi throws an IAE when the extension gets directly injected directly. In the meantime, retrieving the extension from the bean manager works as expected.
        Collection<Annotation> qualifiers = new HashSet<>(ip.getQualifiers());
        qualifiers.retainAll(manager.getExtension(CdiCamelExtension.class).getContextQualifiers());
        if (qualifiers.isEmpty() && !instance.select(DEFAULT).isUnsatisfied())
            return instance.select(DEFAULT).get();
        return instance.select(qualifiers.toArray(new Annotation[qualifiers.size()])).get();
    }

    static <T extends Annotation> Optional<T> getQualifierByType(InjectionPoint ip, Class<T> type) {
        return ip.getQualifiers().stream()
            .filter(q -> type.equals(q.annotationType()))
            .findAny()
            .map(type::cast);
    }

    private static String eventEndpointUri(Type type, Set<Annotation> qualifiers) {
        String uri = "cdi-event://" + authorityFromType(type);

        Optional<String> parameters = qualifiers.stream()
            .map(Annotation::annotationType)
            .map(Class::getCanonicalName)
            .reduce((q1, q2) -> q1 + "%2C" + q2);

        if (parameters.isPresent())
            uri += "?qualifiers=" + parameters.get();

        return uri;
    }

    private static String authorityFromType(Type type) {
        if (type instanceof Class)
            return Class.class.cast(type).getName();

        if (type instanceof ParameterizedType)
            return Stream.of(((ParameterizedType) type).getActualTypeArguments())
                .map(CdiCamelFactory::authorityFromType)
                .reduce((t1, t2) -> t1 + "%2C" + t2)
                .map(t -> "%3C" + t + "%3E").get();

        if (type instanceof GenericArrayType)
            return authorityFromType(((GenericArrayType) type).getGenericComponentType()) + "%5B%5D";

        throw new IllegalArgumentException("Cannot create URI authority for event type [" + type + "]");
    }
}
