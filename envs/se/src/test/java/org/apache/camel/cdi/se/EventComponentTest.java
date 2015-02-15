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
import org.apache.camel.cdi.CdiEvent;
import org.apache.camel.cdi.Uri;
import org.apache.camel.cdi.se.bean.EventConsumingRoute;
import org.apache.camel.cdi.se.bean.SampleBean;
import org.apache.camel.component.mock.MockEndpoint;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.enterprise.event.Event;
import javax.inject.Inject;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.apache.camel.component.mock.MockEndpoint.assertIsSatisfied;

@RunWith(Arquillian.class)
public class EventComponentTest {

    @Deployment
    public static Archive<?> deployment() {
        return ShrinkWrap.create(JavaArchive.class)
            // Camel CDI
            .addPackage(CdiCamelExtension.class.getPackage())
            // Test class
            .addClass(EventConsumingRoute.class)
            // Bean archive deployment descriptor
            .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @Inject
    @Uri(CdiEvent.CDI_EVENT_URI)
    private ProducerTemplate inbound;

    @Inject
    Event<SampleBean> sampleBeanEvent;

    @Inject
    @Uri("mock:resultAll")
    private MockEndpoint outboundAll;

    @Inject
    @Uri("mock:resultBean")
    private MockEndpoint outboundBean;

    @Inject
    @Uri("mock:resultString")
    private MockEndpoint outboundString;

    @Test
    @InSequence(1)
    public void startCamelContext(CamelContext context, List<Class> events) throws Exception {
        context.start();
    }

    @Test
    @InSequence(2)
    public void sendMessageToInbound(List<Class> events) throws InterruptedException {
        outboundAll.expectedMessageCount(4);
        outboundAll.expectedBodiesReceived(1234, new SampleBean("foo"), new SampleBean("bar"), "test");

        outboundBean.expectedMessageCount(2);
        outboundBean.expectedBodiesReceived(new SampleBean("foo"), new SampleBean("bar"));

        outboundString.expectedMessageCount(1);
        outboundString.expectedBodiesReceived("test");

        inbound.sendBody(1234);
        inbound.sendBody(new SampleBean("foo"));
        sampleBeanEvent.fire(new SampleBean("bar"));
        inbound.sendBody("test");

        assertIsSatisfied(2L, TimeUnit.SECONDS, outboundAll);
        assertIsSatisfied(2L, TimeUnit.SECONDS, outboundBean);
        assertIsSatisfied(2L, TimeUnit.SECONDS, outboundString);
    }

    @Test
    @InSequence(3)
    public void stopCamelContext(CamelContext context, List<Class> events) throws Exception {
        context.stop();
    }
}
