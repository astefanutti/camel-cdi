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

import javax.enterprise.inject.Produces;
import javax.enterprise.inject.Typed;
import javax.enterprise.inject.spi.InjectionPoint;

import org.apache.camel.CamelContext;
import org.apache.camel.Endpoint;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.TypeConverter;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.util.CamelContextHelper;
import org.apache.camel.util.ObjectHelper;

import java.util.Collection;

class CdiCamelFactory {

    @Produces
    private static TypeConverter typeConverter(CamelContext context) {
        return context.getTypeConverter();
    }

    @Produces
    private static MockEndpoint mockEndpointFromMember(InjectionPoint point, CamelContext context) {
        String uri = "mock:" + point.getMember().getName();
        return CamelContextHelper.getMandatoryEndpoint(context, uri, MockEndpoint.class);
    }

    @Uri("")
    @Produces
    @Typed(MockEndpoint.class)
    private static MockEndpoint mockEndpointFromUri(InjectionPoint point, CamelContext context) {
        String uri = getFirstElementOfType(point.getQualifiers(), Uri.class).value();
        return CamelContextHelper.getMandatoryEndpoint(context, uri, MockEndpoint.class);
    }

    @Uri("")
    @Produces
    private static Endpoint endpoint(InjectionPoint point, CamelContext context) {
        String uri = getFirstElementOfType(point.getQualifiers(), Uri.class).value();
        return CamelContextHelper.getMandatoryEndpoint(context, uri);
    }

    @Uri("")
    @Produces
    private static ProducerTemplate producerTemplate(InjectionPoint point, CamelContext context) {
        Uri uri = getFirstElementOfType(point.getQualifiers(), Uri.class);
        ProducerTemplate producerTemplate = context.createProducerTemplate();
        Endpoint endpoint = CamelContextHelper.getMandatoryEndpoint(context, uri.value());
        producerTemplate.setDefaultEndpoint(endpoint);
        return producerTemplate;
    }

    private static <E, T extends E> T getFirstElementOfType(Collection<E> collection, Class<T> type) {
        for (E item : collection)
            if ((item != null) && type.isAssignableFrom(item.getClass()))
                return ObjectHelper.cast(type, item);

        throw new IllegalArgumentException("No element of type [" + type.getName() + "] in [" + collection + "]");
    }
}
