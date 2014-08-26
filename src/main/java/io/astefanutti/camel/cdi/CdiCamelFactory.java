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
import javax.enterprise.inject.spi.InjectionPoint;

import org.apache.camel.CamelContext;
import org.apache.camel.Endpoint;
import org.apache.camel.NoSuchEndpointException;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.TypeConverter;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.util.CamelContextHelper;
import org.apache.camel.util.ObjectHelper;

class CdiCamelFactory {

    @Produces
    private static TypeConverter createTypeConverter(CamelContext camelContext) {
        return camelContext.getTypeConverter();
    }

    @Mock
    @Produces
    private static MockEndpoint createMockEndpoint(InjectionPoint point, CamelContext camelContext) {
        String uri = point.getAnnotated().getAnnotation(Mock.class).value();
        if (ObjectHelper.isEmpty(uri))
            uri = "mock:" + point.getMember().getName();

        MockEndpoint endpoint = camelContext.getEndpoint(uri, MockEndpoint.class);
        if (endpoint == null)
            throw new NoSuchEndpointException(uri);

        return endpoint;
    }

    @Uri("")
    @Produces
    private static Endpoint createEndpoint(InjectionPoint point, CamelContext camelContext) {
        String uri = point.getAnnotated().getAnnotation(Mock.class).value();
        return CamelContextHelper.getMandatoryEndpoint(camelContext, uri);
    }

    @Uri("")
    @Produces
    private static ProducerTemplate createProducerTemplate(InjectionPoint point, CamelContext camelContext) {
        Uri uri = point.getAnnotated().getAnnotation(Uri.class);
        ProducerTemplate producerTemplate = camelContext.createProducerTemplate();
        Endpoint endpoint = CamelContextHelper.getMandatoryEndpoint(camelContext, uri.value());
        producerTemplate.setDefaultEndpoint(endpoint);
        return producerTemplate;
    }
}
