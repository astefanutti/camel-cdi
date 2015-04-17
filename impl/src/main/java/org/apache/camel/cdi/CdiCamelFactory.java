/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
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

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Iterator;
import java.util.Set;
import javax.enterprise.event.Event;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.Typed;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.util.TypeLiteral;

import java.lang.reflect.GenericArrayType;
import java.util.Arrays;

final class CdiCamelFactory {

    @Produces
    private static TypeConverter typeConverter(InjectionPoint ip, @Any Instance<CamelContext> instance) {
        return selectContext(ip, instance).getTypeConverter();
    }

    @Uri("")
    @Produces
    // Qualifiers are dynamically added in CdiCamelExtension.producerTemplates()
    private static ProducerTemplate producerTemplate(InjectionPoint ip, @Any Instance<CamelContext> instance) {
        CamelContext context = selectContext(ip, instance);
        ProducerTemplate producerTemplate = context.createProducerTemplate();
        // FIXME: avoid NPE caused by missing @Uri qualifier when injection point is @ContextName qualified
        String uri = CdiSpiHelper.getQualifierByType(ip, Uri.class).value();
        Endpoint endpoint = context.getEndpoint(uri, Endpoint.class);
        producerTemplate.setDefaultEndpoint(endpoint);
        return producerTemplate;
    }

    @Produces
    @Typed(MockEndpoint.class)
    // Qualifiers are dynamically added in CdiCamelExtension.endpointProducers()
    private static MockEndpoint mockEndpointFromMember(InjectionPoint ip, @Any Instance<CamelContext> instance) {
        String uri = "mock:" + ip.getMember().getName();
        return selectContext(ip, instance).getEndpoint(uri, MockEndpoint.class);
    }

    @Uri("")
    @Produces
    @Typed(MockEndpoint.class)
    // Qualifiers are dynamically added in CdiCamelExtension.endpointProducers()
    private static MockEndpoint mockEndpointFromUri(InjectionPoint ip, @Any Instance<CamelContext> instance) {
        String uri = CdiSpiHelper.getQualifierByType(ip, Uri.class).value();
        return selectContext(ip, instance).getEndpoint(uri, MockEndpoint.class);
    }

    // Maintained for backward compatibility reason though this is redundant with @Uri, see https://issues.apache.org/jira/browse/CAMEL-5553?focusedCommentId=13445936&page=com.atlassian.jira.plugin.system.issuetabpanels:comment-tabpanel#comment-13445936
    @Mock("")
    @Produces
    @Typed(MockEndpoint.class)
    // Qualifiers are dynamically added in CdiCamelExtension.endpointProducers()
    private static MockEndpoint createMockEndpoint(InjectionPoint ip, @Any Instance<CamelContext> instance) {
        String uri = CdiSpiHelper.getQualifierByType(ip, Mock.class).value();
        return selectContext(ip, instance).getEndpoint(uri, MockEndpoint.class);
    }

    @Uri("")
    @Produces
    // Qualifiers are dynamically added in CdiCamelExtension.endpointProducers()
    private static Endpoint endpoint(InjectionPoint ip, @Any Instance<CamelContext> instance) {
        String uri = CdiSpiHelper.getQualifierByType(ip, Uri.class).value();
        return selectContext(ip, instance).getEndpoint(uri, Endpoint.class);
    }

    @Produces
    @SuppressWarnings("unchecked")
    // Qualifiers are dynamically added in CdiCamelExtension.endpointProducers()
    private static <T> CdiEventEndpoint<T> cdiEventEndpoint(InjectionPoint ip, @Any Instance<CamelContext> instance, CdiCamelExtension extension, @Any Event<Object> event) throws Exception {
        CamelContext context = selectContext(ip, instance);
        Type type = ((ParameterizedType) ip.getType()).getActualTypeArguments()[0];
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
            context.addEndpoint(uri, new CdiEventEndpoint<T>(event.select(literal, ip.getQualifiers().toArray(new Annotation[ip.getQualifiers().size()])), uri, context, (ForwardingObserverMethod<T>) extension.getObserverMethod(ip)));
        }
        return context.getEndpoint(uri, CdiEventEndpoint.class);
    }

    private static <T extends CamelContext> T selectContext(InjectionPoint ip, Instance<T> instance) {
        ContextName name = CdiSpiHelper.getQualifierByType(ip, ContextName.class);
        return instance.select(name != null ? name : DefaultLiteral.INSTANCE).get();
    }

    private static String eventEndpointUri(Type type, Set<Annotation> qualifiers) {
        String uri = "cdi-event://" + authorityFromType(type);
        StringBuilder parameters = new StringBuilder();
        Iterator<Annotation> it = qualifiers.iterator();
        while (it.hasNext()) {
            Annotation qualifier = it.next();
            // Skip the ContextName qualifier as the endpoint already belongs to its Camel context
            if (!qualifier.annotationType().equals(ContextName.class))
                parameters.append(qualifier.annotationType().getCanonicalName());
            if (it.hasNext())
                parameters.append("%2C");
        }

        if (parameters.length() > 0)
            uri += "?qualifiers=" + parameters.toString();

        return uri;
    }

    private static String authorityFromType(Type type) {
        if (type instanceof Class) {
            return Class.class.cast(type).getName();
        }
        if (type instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) type;
            StringBuilder builder = new StringBuilder(authorityFromType(pt.getRawType()));
            Iterator<Type> it = Arrays.asList(pt.getActualTypeArguments()).iterator();
            builder.append("%3C");
            while (it.hasNext()) {
                builder.append(authorityFromType(it.next()));
                if (it.hasNext())
                    builder.append("%2C");
            }
            builder.append("%3E");
            return builder.toString();
        }
        if (type instanceof GenericArrayType) {
            GenericArrayType arrayType = (GenericArrayType) type;
            return authorityFromType(arrayType.getGenericComponentType()) + "%5B%5D";
        }
        throw new IllegalArgumentException("Cannot create URI authority for event type [" + type + "]");
    }
}
