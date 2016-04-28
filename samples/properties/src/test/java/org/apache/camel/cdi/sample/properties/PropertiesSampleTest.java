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
package org.apache.camel.cdi.sample.properties;

import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.cdi.CdiCamelExtension;
import org.apache.camel.cdi.Uri;
import org.apache.camel.cdi.test.LogVerifier;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.management.event.CamelContextStartingEvent;
import org.apache.camel.model.ModelCamelContext;
import org.apache.deltaspike.core.api.config.ConfigProperty;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;

import javax.enterprise.event.Observes;

import static org.hamcrest.Matchers.containsInRelativeOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

@RunWith(Arquillian.class)
public class PropertiesSampleTest {

    @Deployment
    public static Archive<?> deployment() {
        return ShrinkWrap.create(JavaArchive.class)
            // Camel CDI
            .addPackage(CdiCamelExtension.class.getPackage())
            // DeltaSpike
            .addPackages(true, "org.apache.deltaspike.core.impl")
            // Test classes
            .addPackage(PropertiesSampleTest.class.getPackage())
            // Bean archive deployment descriptor
            .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @ClassRule
    public static TestRule verifier = new LogVerifier() {
        @Override
        protected void verify() {
            assertThat("Log messages not found!", getMessages(),
                containsInRelativeOrder(
                    containsString("(CamelContext: hello) is starting"),
                    equalTo("Hello from CamelContext (hello)"),
                    containsString("(CamelContext: hello) is shutdown"))
            );
        }
    };

    static void advice(@Observes CamelContextStartingEvent event,
                       ModelCamelContext context) throws Exception {
        // Add a mock endpoint to the end of the route
        context.getRouteDefinitions().get(0).adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() {
                weaveAddLast().to("mock:outbound");
            }
        });
    }

    @Test
    public void testRouteMessage(@Uri("mock:outbound") MockEndpoint outbound) {
        assertThat("Exchange count is incorrect!",
            outbound.getExchanges(),
            hasSize(1));
        assertThat("Exchange body is incorrect!",
            outbound.getExchanges().get(0).getIn().getBody(String.class),
            is(equalTo("Hello")));
    }

    @Test
    public void testProperties(@ConfigProperty(name = "destination") String destination,
                               @ConfigProperty(name = "message") String message) {
        assertThat("Property 'destination' value is incorrect!", destination,
            is(equalTo("direct:hello")));
        assertThat("Property 'message' value is incorrect!", message,
            is(equalTo("Hello")));
    }
}
