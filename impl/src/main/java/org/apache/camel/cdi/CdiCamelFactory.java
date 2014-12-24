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
import org.apache.camel.util.CamelContextHelper;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.Typed;
import javax.enterprise.inject.spi.InjectionPoint;

final class CdiCamelFactory {

    @Produces
    private static TypeConverter typeConverter(InjectionPoint ip, @Any Instance<CamelContext> instance) {
        return selectContext(ip, instance).getTypeConverter();
    }

    @Produces
    @Typed(MockEndpoint.class)
    private static MockEndpoint mockEndpointFromMember(InjectionPoint ip, @Any Instance<CamelContext> instance) {
        String uri = "mock:" + ip.getMember().getName();
        return CamelContextHelper.getMandatoryEndpoint(selectContext(ip, instance), uri, MockEndpoint.class);
    }

    @Uri("")
    @Produces
    @Typed(MockEndpoint.class)
    private static MockEndpoint mockEndpointFromUri(InjectionPoint ip, @Any Instance<CamelContext> instance) {
        String uri = CdiSpiHelper.getQualifierByType(ip, Uri.class).value();
        return CamelContextHelper.getMandatoryEndpoint(selectContext(ip, instance), uri, MockEndpoint.class);
    }

    // TODO: confirm whether it can be removed as this is redundant with @Uri, see https://issues.apache.org/jira/browse/CAMEL-5553?focusedCommentId=13445936&page=com.atlassian.jira.plugin.system.issuetabpanels:comment-tabpanel#comment-13445936
    @Mock("")
    @Produces
    @Typed(MockEndpoint.class)
    private static MockEndpoint createMockEndpoint(InjectionPoint ip, @Any Instance<CamelContext> instance) {
        String uri = CdiSpiHelper.getQualifierByType(ip, Mock.class).value();
        return CamelContextHelper.getMandatoryEndpoint(selectContext(ip, instance), uri, MockEndpoint.class);
    }

    @Uri("")
    @Produces
    private static Endpoint endpoint(InjectionPoint ip, @Any Instance<CamelContext> instance) {
        String uri = CdiSpiHelper.getQualifierByType(ip, Uri.class).value();
        return CamelContextHelper.getMandatoryEndpoint(selectContext(ip, instance), uri);
    }

    @Uri("")
    @Produces
    private static ProducerTemplate producerTemplate(InjectionPoint ip, @Any Instance<CamelContext> instance) {
        CamelContext context = selectContext(ip, instance);
        ProducerTemplate producerTemplate = context.createProducerTemplate();
        String uri = CdiSpiHelper.getQualifierByType(ip, Uri.class).value();
        Endpoint endpoint = CamelContextHelper.getMandatoryEndpoint(context, uri);
        producerTemplate.setDefaultEndpoint(endpoint);
        return producerTemplate;
    }

    private static CamelContext selectContext(InjectionPoint ip, Instance<CamelContext> instance) {
        ContextName name = CdiSpiHelper.getQualifierByType(ip, ContextName.class);
        return instance.select(name != null ? name : DefaultLiteral.INSTANCE).get();
    }
}
