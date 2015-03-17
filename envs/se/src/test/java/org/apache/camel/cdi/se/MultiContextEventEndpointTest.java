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
package org.apache.camel.cdi.se;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.cdi.CdiCamelExtension;
import org.apache.camel.cdi.ContextName;
import org.apache.camel.cdi.Uri;
import org.apache.camel.cdi.se.bean.FirstCamelContextBean;
import org.apache.camel.cdi.se.bean.FirstCamelContextEventConsumingRoute;
import org.apache.camel.cdi.se.bean.FirstCamelContextEventProducingRoute;
import org.apache.camel.cdi.se.bean.SecondCamelContextBean;
import org.apache.camel.cdi.se.bean.SecondCamelContextEventConsumingRoute;
import org.apache.camel.cdi.se.bean.SecondCamelContextEventProducingRoute;
import org.apache.camel.component.mock.MockEndpoint;
import org.hamcrest.Matchers;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.apache.camel.component.mock.MockEndpoint.assertIsSatisfied;
import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

@RunWith(Arquillian.class)
public class MultiContextEventEndpointTest {

    @Deployment
    public static Archive<?> deployment() {
        return ShrinkWrap.create(JavaArchive.class)
            // Camel CDI
            .addPackage(CdiCamelExtension.class.getPackage())
            // Test classes
            .addClass(FirstCamelContextBean.class)
            .addClass(FirstCamelContextEventConsumingRoute.class)
            .addClass(FirstCamelContextEventProducingRoute.class)
            .addClass(SecondCamelContextBean.class)
            .addClass(SecondCamelContextEventConsumingRoute.class)
            .addClass(SecondCamelContextEventProducingRoute.class)
            // Bean archive deployment descriptor
            .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @Inject
    @ContextName("first")
    @Uri("mock:consumeString")
    private MockEndpoint firstConsumeString;

    @Inject
    @ContextName("second")
    @Uri("mock:consumeString")
    private MockEndpoint secondConsumeString;

    @Inject
    @ContextName("first")
    @Uri("direct:produceString")
    private ProducerTemplate firstProduceString;

    @Inject
    @ContextName("second")
    @Uri("direct:produceString")
    private ProducerTemplate secondProduceString;

    @Inject
    Event<Object> objectEvent;

    @Inject
    private EventObserver observer;

    @Test
    @InSequence(1)
    public void startCamelContexts(@ContextName("first") CamelContext firstContext, @ContextName("second") CamelContext secondContext) throws Exception {
        firstContext.start();
        secondContext.start();
    }

    @Test
    @InSequence(2)
    public void sendEventsToConsumers() throws InterruptedException {
        firstConsumeString.expectedMessageCount(1);
        firstConsumeString.expectedBodiesReceived("testFirst");

        secondConsumeString.expectedMessageCount(2);
        secondConsumeString.expectedBodiesReceived("testSecond1", "testSecond2");

        objectEvent.select(String.class, new ContextName.Literal("first")).fire("testFirst");
        objectEvent.select(String.class, new ContextName.Literal("second")).fire("testSecond1");
        objectEvent.select(String.class, new ContextName.Literal("second")).fire("testSecond2");

        assertIsSatisfied(2L, TimeUnit.SECONDS, firstConsumeString, secondConsumeString);
    }

    @Test
    @InSequence(3)
    public void sendMessagesToProducers() {
        firstProduceString.sendBody("testFirst");
        secondProduceString.sendBody("testSecond");

        assertThat(observer.getObjectEvents(), Matchers.<Object>contains("testFirst", "testSecond"));
        assertThat(observer.getStringEvents(), contains("testFirst", "testSecond"));
        assertThat(observer.getFirstStringEvents(), contains("testFirst"));
        assertThat(observer.secondStringEvents(), contains("testSecond"));
    }

    @Test
    @InSequence(4)
    public void stopCamelContexts(@ContextName("first") CamelContext firstContext, @ContextName("second") CamelContext secondContext) throws Exception {
        firstContext.stop();
        secondContext.stop();
    }

    @Before
    public void resetCollectedEvents() {
        observer.reset();
    }

    @ApplicationScoped
    static class EventObserver {

        private final List<Object> objectEvents = new ArrayList<Object>();

        private final List<String> stringEvents = new ArrayList<String>();

        private final List<String> firstStringEvents = new ArrayList<String>();

        private final List<String> secondStringEvents = new ArrayList<String>();

        void collectObjectEvents(@Observes Object event) {
            objectEvents.add(event);
        }

        void collectStringEvents(@Observes String event) {
            stringEvents.add(event);
        }

        void collectFirstStringEvents(@Observes @ContextName("first") String event) {
            firstStringEvents.add(event);
        }

        void collectSecondStringEvents(@Observes @ContextName("second") String event) {
            secondStringEvents.add(event);
        }

        List<Object> getObjectEvents() {
            return objectEvents;
        }

        List<String> getStringEvents() {
            return stringEvents;
        }

        List<String> getFirstStringEvents() {
            return firstStringEvents;
        }

        List<String> secondStringEvents() {
            return secondStringEvents;
        }

        void reset() {
            objectEvents.clear();
            stringEvents.clear();
            firstStringEvents.clear();
            secondStringEvents.clear();
        }
    }
}
