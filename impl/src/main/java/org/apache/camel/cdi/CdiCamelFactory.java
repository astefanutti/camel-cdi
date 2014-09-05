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
package org.apache.camel.cdi;

import org.apache.camel.CamelContext;
import org.apache.camel.Endpoint;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.TypeConverter;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.util.CamelContextHelper;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.Typed;
import javax.enterprise.inject.spi.InjectionPoint;

final class CdiCamelFactory {

    @Produces
    private static TypeConverter typeConverter(InjectionPoint ip, @Any Instance<CamelContext> instance) {
        ContextName name = CdiSpiHelper.getQualifierByType(ip, ContextName.class);
        CamelContext context = instance.select(name != null ? name : DefaultLiteral.INSTANCE).get();
        return context.getTypeConverter();
    }

    @Produces
    @Typed(MockEndpoint.class)
    private static MockEndpoint mockEndpointFromMember(InjectionPoint ip, @Any Instance<CamelContext> instance) {
        ContextName name = CdiSpiHelper.getQualifierByType(ip, ContextName.class);
        CamelContext context = instance.select(name != null ? name : DefaultLiteral.INSTANCE).get();
        String uri = "mock:" + ip.getMember().getName();
        return CamelContextHelper.getMandatoryEndpoint(context, uri, MockEndpoint.class);
    }

    @Uri("")
    @Produces
    @Typed(MockEndpoint.class)
    private static MockEndpoint mockEndpointFromUri(InjectionPoint ip, @Any Instance<CamelContext> instance) {
        ContextName name = CdiSpiHelper.getQualifierByType(ip, ContextName.class);
        CamelContext context = instance.select(name != null ? name : DefaultLiteral.INSTANCE).get();
        String uri = CdiSpiHelper.getQualifierByType(ip, Uri.class).value();
        return CamelContextHelper.getMandatoryEndpoint(context, uri, MockEndpoint.class);
    }

    @Uri("")
    @Produces
    private static Endpoint endpoint(InjectionPoint ip, @Any Instance<CamelContext> instance) {
        ContextName name = CdiSpiHelper.getQualifierByType(ip, ContextName.class);
        CamelContext context = instance.select(name != null ? name : DefaultLiteral.INSTANCE).get();
        String uri = CdiSpiHelper.getQualifierByType(ip, Uri.class).value();
        return CamelContextHelper.getMandatoryEndpoint(context, uri);
    }

    @Uri("")
    @Produces
    private static ProducerTemplate producerTemplate(InjectionPoint ip, @Any Instance<CamelContext> instance) {
        ContextName name = CdiSpiHelper.getQualifierByType(ip, ContextName.class);
        CamelContext context = instance.select(name != null ? name : DefaultLiteral.INSTANCE).get();
        ProducerTemplate producerTemplate = context.createProducerTemplate();
        String uri = CdiSpiHelper.getQualifierByType(ip, Uri.class).value();
        Endpoint endpoint = CamelContextHelper.getMandatoryEndpoint(context, uri);
        producerTemplate.setDefaultEndpoint(endpoint);
        return producerTemplate;
    }
}
