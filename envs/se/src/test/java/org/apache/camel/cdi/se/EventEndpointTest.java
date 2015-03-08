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
import org.apache.camel.cdi.Uri;
import org.apache.camel.cdi.se.bean.EventConsumingRoute;
import org.apache.camel.cdi.se.bean.EventPayload;
import org.apache.camel.cdi.se.bean.EventProducingRoute;
import org.apache.camel.component.mock.MockEndpoint;
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

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.enterprise.util.TypeLiteral;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.apache.camel.component.mock.MockEndpoint.assertIsSatisfied;
import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertThat;

@RunWith(Arquillian.class)
public class EventEndpointTest {

    @Deployment
    public static Archive<?> deployment() {
        return ShrinkWrap.create(JavaArchive.class)
            // Camel CDI
            .addPackage(CdiCamelExtension.class.getPackage())
            // Test classes
            .addClass(EventConsumingRoute.class)
            .addClass(EventProducingRoute.class)
            // Bean archive deployment descriptor
            .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @Inject
    @Uri("mock:consumeObject")
    private MockEndpoint consumeObject;

    @Inject
    @Uri("mock:consumeString")
    private MockEndpoint consumeString;

    @Inject
    @Uri("mock:consumeStringPayload")
    private MockEndpoint consumeStringPayload;

    @Inject
    @Uri("mock:consumeIntegerPayload")
    private MockEndpoint consumeIntegerPayload;

    @Inject
    @Uri("direct:produceObject")
    private ProducerTemplate produceObject;

    @Inject
    @Uri("direct:produceString")
    private ProducerTemplate produceString;

    @Inject
    @Uri("direct:produceStringPayload")
    private ProducerTemplate produceStringPayload;

    @Inject
    @Uri("direct:produceIntegerPayload")
    private ProducerTemplate produceIntegerPayload;

    @Inject
    Event<Object> objectEvent;

    @Inject
    Event<EventPayload<String>> stringPayloadEvent;

    @Inject
    Event<EventPayload<Integer>> integerPayloadEvent;

    @Inject
    private EventObserver observer;

    @Test
    @InSequence(1)
    public void startCamelContext(CamelContext context) throws Exception {
        context.start();
    }

    @Test
    @InSequence(2)
    public void sendEventsToConsumers() throws InterruptedException {
        consumeObject.expectedMessageCount(4);
        consumeObject.expectedBodiesReceived(1234, new EventPayload<>("foo"), new EventPayload<>("bar"), "test", new EventPayload<>(1), new EventPayload<>(2));

        consumeString.expectedMessageCount(1);
        consumeString.expectedBodiesReceived("test");

        consumeStringPayload.expectedMessageCount(2);
        consumeStringPayload.expectedBodiesReceived(new EventPayload<>("foo"), new EventPayload<>("bar"));

        consumeIntegerPayload.expectedMessageCount(2);
        consumeIntegerPayload.expectedBodiesReceived(new EventPayload<>(1), new EventPayload<>(2));

        objectEvent.select(Integer.class).fire(1234);
        objectEvent.select(new TypeLiteral<EventPayload<String>>() {}).fire(new EventPayload<>("foo"));
        stringPayloadEvent.fire(new EventPayload<>("bar"));
        objectEvent.select(String.class).fire("test");
        integerPayloadEvent.fire(new EventPayload<>(1));
        integerPayloadEvent.fire(new EventPayload<>(2));

        assertIsSatisfied(2L, TimeUnit.SECONDS, consumeObject);
        assertIsSatisfied(2L, TimeUnit.SECONDS, consumeString);
        assertIsSatisfied(2L, TimeUnit.SECONDS, consumeStringPayload);
        assertIsSatisfied(2L, TimeUnit.SECONDS, consumeIntegerPayload);
    }

    @Test
    @InSequence(3)
    public void sendMessagesToProducers() {
        produceObject.sendBody("string");
        EventPayload foo = new EventPayload<>("foo");
        produceStringPayload.sendBody(foo);
        produceObject.sendBody(1234);
        produceString.sendBody("test");
        EventPayload<Integer> bar = new EventPayload<>(2);
        produceIntegerPayload.sendBody(bar);
        EventPayload<Integer> baz = new EventPayload<>(12);
        produceIntegerPayload.sendBody(baz);

        assertThat(observer.getObjectEvents(), contains("string", foo, 1234, "test", bar, baz));
        assertThat(observer.getStringEvents(), contains("string", "test"));
        assertThat(observer.getStringPayloadEvents(), contains(foo));
        assertThat(observer.getIntegerPayloadEvents(), contains(bar, baz));
    }

    @Test
    @InSequence(4)
    public void stopCamelContext(CamelContext context) throws Exception {
        context.stop();
    }

    @Before
    public void resetCollectedEvents() {
        observer.reset();
    }

    @ApplicationScoped
    static class EventObserver {

        private final List<Object> objectEvents = new ArrayList<>();

        private final List<String> stringEvents = new ArrayList<>();

        private final List<EventPayload<String>> stringPayloadEvents = new ArrayList<>();

        private final List<EventPayload<Integer>> integerPayloadEvents = new ArrayList<>();

        void collectObjectEvents(@Observes Object event) {
            objectEvents.add(event);
        }

        void collectStringEvents(@Observes String event) {
            stringEvents.add(event);
        }

        void collectStringPayloadEvents(@Observes EventPayload<String> event) {
            stringPayloadEvents.add(event);
        }

        void collectIntegerPayloadEvents(@Observes EventPayload<Integer> event) {
            integerPayloadEvents.add(event);
        }

        List<Object> getObjectEvents() {
            return objectEvents;
        }

        List<String> getStringEvents() {
            return stringEvents;
        }

        List<EventPayload<String>> getStringPayloadEvents() {
            return stringPayloadEvents;
        }

        List<EventPayload<Integer>> getIntegerPayloadEvents() {
            return integerPayloadEvents;
        }

        void reset() {
            objectEvents.clear();
            stringEvents.clear();
            stringPayloadEvents.clear();
            integerPayloadEvents.clear();
        }
    }
}
