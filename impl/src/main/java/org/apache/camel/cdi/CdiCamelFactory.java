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
import org.apache.camel.FluentProducerTemplate;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.TypeConverter;
import org.apache.camel.component.mock.MockEndpoint;

import javax.annotation.Priority;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.Typed;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.interceptor.Interceptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;

import static org.apache.camel.cdi.CdiEventEndpoint.eventEndpointUri;
import static org.apache.camel.cdi.CdiSpiHelper.isAnnotationType;

@Priority(Interceptor.Priority.LIBRARY_BEFORE)
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
    @Default @Uri("")
    // Qualifiers are dynamically added in CdiCamelExtension
    private static FluentProducerTemplate fluentProducerTemplate(InjectionPoint ip, @Any Instance<CamelContext> instance, CdiCamelExtension extension) {
        return getQualifierByType(ip, Uri.class)
            .map(uri -> fluentProducerTemplateFromUri(ip, instance, extension, uri))
            .orElseGet(() -> defaultFluentProducerTemplate(ip, instance, extension));
    }

    private static FluentProducerTemplate fluentProducerTemplateFromUri(InjectionPoint ip, @Any Instance<CamelContext> instance, CdiCamelExtension extension, Uri uri) {
        CamelContext context = selectContext(ip, instance, extension);
        FluentProducerTemplate producerTemplate = context.createFluentProducerTemplate();
        Endpoint endpoint = context.getEndpoint(uri.value(), Endpoint.class);
        producerTemplate.setDefaultEndpoint(endpoint);
        return producerTemplate;
    }

    private static FluentProducerTemplate defaultFluentProducerTemplate(InjectionPoint ip, @Any Instance<CamelContext> instance, CdiCamelExtension extension) {
        return selectContext(ip, instance, extension).createFluentProducerTemplate();
    }

    @Produces
    @Typed(MockEndpoint.class)
    // Alternative is dynamically added in CdiCamelExtension
    private static MockEndpoint mockEndpointFromMember(InjectionPoint ip, @Any Instance<CamelContext> instance, CdiCamelExtension extension) {
        String uri = "mock:" + ip.getMember().getName();
        return selectContext(ip, instance, extension).getEndpoint(uri, MockEndpoint.class);
    }

    @Uri("")
    @Produces
    @Typed(MockEndpoint.class)
    // Alternative is dynamically added in CdiCamelExtension
    private static MockEndpoint mockEndpointFromUri(InjectionPoint ip, @Any Instance<CamelContext> instance, CdiCamelExtension extension) {
        Uri uri = getQualifierByType(ip, Uri.class).get();
        return selectContext(ip, instance, extension).getEndpoint(uri.value(), MockEndpoint.class);
    }

    @Uri("")
    @Produces
    // Alternative is dynamically added in CdiCamelExtension
    private static Endpoint endpoint(InjectionPoint ip, @Any Instance<CamelContext> instance, CdiCamelExtension extension) {
        Uri uri = getQualifierByType(ip, Uri.class).get();
        return selectContext(ip, instance, extension).getEndpoint(uri.value(), Endpoint.class);
    }

    @Produces
    @SuppressWarnings("unchecked")
    // Alternative is dynamically added in CdiCamelExtension
    private static <T> CdiEventEndpoint<T> cdiEventEndpoint(InjectionPoint ip, @Any Instance<CamelContext> instance, CdiCamelExtension extension) throws Exception {
        CamelContext context = selectContext(ip, instance, extension);
        Type type = Object.class;
        if (ip.getType() instanceof ParameterizedType)
            type = ((ParameterizedType) ip.getType()).getActualTypeArguments()[0];
        String uri = eventEndpointUri(type, ip.getQualifiers());
        if (context.hasEndpoint(uri) == null)
            context.addEndpoint(uri, extension.getEventEndpoint(uri));
        return context.getEndpoint(uri, CdiEventEndpoint.class);
    }

    private static <T extends CamelContext> T selectContext(InjectionPoint ip, Instance<T> instance, CdiCamelExtension extension) {
        Collection<Annotation> qualifiers = new HashSet<>(ip.getQualifiers());
        qualifiers.retainAll(extension.getContextQualifiers());
        if (qualifiers.isEmpty() && !instance.select(Default.Literal.INSTANCE).isUnsatisfied())
            return instance.select(Default.Literal.INSTANCE).get();
        return instance.select(qualifiers.stream().toArray(Annotation[]::new)).get();
    }

    private static <T extends Annotation> Optional<T> getQualifierByType(InjectionPoint ip, Class<T> type) {
        return ip.getQualifiers().stream()
            .filter(isAnnotationType(type))
            .findAny()
            .map(type::cast);
    }
}
