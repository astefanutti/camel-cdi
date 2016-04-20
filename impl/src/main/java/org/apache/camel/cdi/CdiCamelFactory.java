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
import org.apache.camel.ConsumerTemplate;
import org.apache.camel.Endpoint;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.TypeConverter;
import org.apache.camel.component.mock.MockEndpoint;

import javax.enterprise.event.Event;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.Typed;
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

import static java.util.stream.Collectors.joining;
import static org.apache.camel.cdi.CdiSpiHelper.isAnnotationType;
import static org.apache.camel.cdi.DefaultLiteral.DEFAULT;

final class CdiCamelFactory {

    @Produces
    private static TypeConverter typeConverter(InjectionPoint ip, @Any Instance<CamelContext> instance, CdiCamelExtension extension) {
        return selectContext(ip, instance, extension).getTypeConverter();
    }

    @Produces
    // Qualifiers are dynamically added in CdiCamelExtension
    private static ConsumerTemplate consumerTemplate(InjectionPoint ip, @Any Instance<CamelContext> instance, CdiCamelExtension extension) {
        return selectContext(ip, instance, extension).createConsumerTemplate();
    }

    @Produces
    @Default @Uri("")
    // Qualifiers are dynamically added in CdiCamelExtension
    private static ProducerTemplate producerTemplate(InjectionPoint ip, @Any Instance<CamelContext> instance, CdiCamelExtension extension) {
        return getQualifierByType(ip, Uri.class)
            .map(uri -> producerTemplateFromUri(ip, instance, extension, uri))
            .orElseGet(() -> defaultProducerTemplate(ip, instance, extension));
    }

    private static ProducerTemplate producerTemplateFromUri(InjectionPoint ip, @Any Instance<CamelContext> instance, CdiCamelExtension extension, Uri uri) {
        CamelContext context = selectContext(ip, instance, extension);
        ProducerTemplate producerTemplate = context.createProducerTemplate();
        Endpoint endpoint = context.getEndpoint(uri.value(), Endpoint.class);
        producerTemplate.setDefaultEndpoint(endpoint);
        return producerTemplate;
    }

    private static ProducerTemplate defaultProducerTemplate(InjectionPoint ip, @Any Instance<CamelContext> instance, CdiCamelExtension extension) {
        return selectContext(ip, instance, extension).createProducerTemplate();
    }

    @Produces
    @Typed(MockEndpoint.class)
    // Qualifiers are dynamically added in CdiCamelExtension
    private static MockEndpoint mockEndpointFromMember(InjectionPoint ip, @Any Instance<CamelContext> instance, CdiCamelExtension extension) {
        String uri = "mock:" + ip.getMember().getName();
        return selectContext(ip, instance, extension).getEndpoint(uri, MockEndpoint.class);
    }

    @Uri("")
    @Produces
    @Typed(MockEndpoint.class)
    // Qualifiers are dynamically added in CdiCamelExtension
    private static MockEndpoint mockEndpointFromUri(InjectionPoint ip, @Any Instance<CamelContext> instance, CdiCamelExtension extension) {
        Uri uri = getQualifierByType(ip, Uri.class).get();
        return selectContext(ip, instance, extension).getEndpoint(uri.value(), MockEndpoint.class);
    }

    @Uri("")
    @Produces
    // Qualifiers are dynamically added in CdiCamelExtension
    private static Endpoint endpoint(InjectionPoint ip, @Any Instance<CamelContext> instance, CdiCamelExtension extension) {
        Uri uri = getQualifierByType(ip, Uri.class).get();
        return selectContext(ip, instance, extension).getEndpoint(uri.value(), Endpoint.class);
    }

    @Produces
    @SuppressWarnings("unchecked")
    // Qualifiers are dynamically added in CdiCamelExtension
    private static <T> CdiEventEndpoint<T> cdiEventEndpoint(InjectionPoint ip, @Any Instance<CamelContext> instance, CdiCamelExtension extension, @Any Event<Object> event) throws Exception {
        CamelContext context = selectContext(ip, instance, extension);
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
            context.addEndpoint(uri,
                new CdiEventEndpoint<>(
                    event.select(literal, ip.getQualifiers().stream().toArray(Annotation[]::new)),
                    uri, context, (ForwardingObserverMethod<T>) extension.getObserverMethod(ip)));
        }
        return context.getEndpoint(uri, CdiEventEndpoint.class);
    }

    private static <T extends CamelContext> T selectContext(InjectionPoint ip, Instance<T> instance, CdiCamelExtension extension) {
        Collection<Annotation> qualifiers = new HashSet<>(ip.getQualifiers());
        qualifiers.retainAll(extension.getContextQualifiers());
        if (qualifiers.isEmpty() && !instance.select(DEFAULT).isUnsatisfied())
            return instance.select(DEFAULT).get();
        return instance.select(qualifiers.stream().toArray(Annotation[]::new)).get();
    }

    private static <T extends Annotation> Optional<T> getQualifierByType(InjectionPoint ip, Class<T> type) {
        return ip.getQualifiers().stream()
            .filter(isAnnotationType(type))
            .findAny()
            .map(type::cast);
    }

    private static String eventEndpointUri(Type type, Set<Annotation> qualifiers) {
        return "cdi-event://" + authorityFromType(type) + qualifiers.stream()
            .map(Annotation::annotationType)
            .map(Class::getCanonicalName)
            .collect(joining("%2C", qualifiers.size() > 0 ? "?qualifiers=" : "", ""));
    }

    private static String authorityFromType(Type type) {
        if (type instanceof Class)
            return Class.class.cast(type).getName();

        if (type instanceof ParameterizedType)
            return Stream.of(((ParameterizedType) type).getActualTypeArguments())
                .map(CdiCamelFactory::authorityFromType)
                .collect(joining("%2C", "%3C", "%3E"));

        if (type instanceof GenericArrayType)
            return authorityFromType(((GenericArrayType) type).getGenericComponentType()) + "%5B%5D";

        throw new IllegalArgumentException("Cannot create URI authority for event type [" + type + "]");
    }
}
