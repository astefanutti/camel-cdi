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
package org.apache.camel.cdi.se;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Expression;
import org.apache.camel.Predicate;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.cdi.CdiCamelExtension;
import org.apache.camel.cdi.ContextName;
import org.apache.camel.cdi.Uri;
import org.apache.camel.cdi.se.bean.DefaultCamelContextBean;
import org.apache.camel.cdi.se.bean.FirstCamelContextBean;
import org.apache.camel.cdi.se.bean.FirstCamelContextRoute;
import org.apache.camel.cdi.se.bean.SecondCamelContextBean;
import org.apache.camel.cdi.se.bean.UriEndpointRoute;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.util.PredicateToExpressionAdapter;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

import static org.apache.camel.component.mock.MockEndpoint.assertIsSatisfied;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

@RunWith(Arquillian.class)
public class MultiCamelContextTest {

    @Deployment
    public static Archive<?> deployment() {
        return ShrinkWrap.create(JavaArchive.class)
            // Camel CDI
            .addPackage(CdiCamelExtension.class.getPackage())
            // Test class
            .addClass(DefaultCamelContextBean.class)
            .addClass(UriEndpointRoute.class)
            .addClass(FirstCamelContextBean.class)
            .addClass(FirstCamelContextRoute.class)
            .addClass(SecondCamelContextBean.class)
            // Bean archive deployment descriptor
            .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @Inject
    // Support bean class injection support for custom beans
    DefaultCamelContextBean defaultCamelContext;

    @Inject @Uri("direct:inbound")
    private ProducerTemplate defaultInbound;

    @Inject @Uri("mock:outbound")
    private MockEndpoint defaultOutbound;

    @Inject @ContextName("first")
    CamelContext firstCamelContext;

    @Inject @ContextName("first") @Uri("direct:inbound")
    private ProducerTemplate firstInbound;

    @Inject @ContextName("first") @Uri("mock:outbound")
    private MockEndpoint firstOutbound;

    @Inject
    @ContextName("second")
    CamelContext secondCamelContext;

    @Inject @ContextName("second") @Uri("direct:inbound")
    private ProducerTemplate secondInbound;

    @Inject @ContextName("second") @Uri("mock:outbound")
    private MockEndpoint secondOutbound;

    @Test
    @InSequence(1)
    public void configureAndStartCamelContexts() throws Exception {
        secondCamelContext.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:inbound").setHeader("context").constant("second").to("mock:outbound");
            }
        });

        defaultCamelContext.start();
        firstCamelContext.start();
        secondCamelContext.start();
    }

    @Test
    @InSequence(2)
    public void verifyCamelContexts() {
        assertThat(defaultOutbound.getCamelContext().getName(), is(equalTo(defaultCamelContext.getName())));
        assertThat(firstOutbound.getCamelContext().getName(), is(equalTo(firstCamelContext.getName())));
        assertThat(secondOutbound.getCamelContext().getName(), is(equalTo(secondCamelContext.getName())));
    }

    @Test
    @InSequence(3)
    public void sendMessageToDefaultCamelContextInbound() throws InterruptedException {
        defaultOutbound.expectedMessageCount(1);
        defaultOutbound.expectedBodiesReceived("test-default");
        defaultOutbound.message(0).exchange().matches(fromCamelContext(defaultCamelContext));

        defaultInbound.sendBody("test-default");

        assertIsSatisfied(2L, TimeUnit.SECONDS, defaultOutbound);
    }

    @Test
    @InSequence(4)
    public void sendMessageToFirstCamelContextInbound() throws InterruptedException {
        firstOutbound.expectedMessageCount(1);
        firstOutbound.expectedBodiesReceived("test-first");
        firstOutbound.expectedHeaderReceived("context", "first");
        firstOutbound.message(0).exchange().matches(fromCamelContext(firstCamelContext));

        firstInbound.sendBody("test-first");

        assertIsSatisfied(2L, TimeUnit.SECONDS, firstOutbound);
    }

    @Test
    @InSequence(5)
    public void sendMessageToSecondCamelContextInbound() throws InterruptedException {
        secondOutbound.expectedMessageCount(1);
        secondOutbound.expectedBodiesReceived("test-second");
        secondOutbound.expectedHeaderReceived("context", "second");
        secondOutbound.message(0).exchange().matches(fromCamelContext(secondCamelContext));

        secondInbound.sendBody("test-second");

        assertIsSatisfied(2L, TimeUnit.SECONDS, secondOutbound);
    }

    @Test
    @InSequence(6)
    public void stopCamelContexts() throws Exception {
        defaultCamelContext.stop();
        firstCamelContext.stop();
        secondCamelContext.stop();
    }

    private static Expression fromCamelContext(final CamelContext context) {
        return new PredicateToExpressionAdapter(new Predicate() {
            @Override
            public boolean matches(Exchange exchange) {
                return exchange.getContext().getName().equals(context.getName());
            }
        });
    }
}
