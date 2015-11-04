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

import org.apache.camel.ProducerTemplate;
import org.apache.camel.cdi.CdiCamelExtension;
import org.apache.camel.cdi.Mock;
import org.apache.camel.cdi.Uri;
import org.apache.camel.cdi.se.bean.DefaultCamelContextBean;
import org.apache.camel.cdi.se.bean.MockAnnotationRoute;
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

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

import static org.apache.camel.component.mock.MockEndpoint.assertIsSatisfied;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

@RunWith(Arquillian.class)
public class MockEndpointTest {

    @Deployment
    public static Archive<?> deployment() {
        return ShrinkWrap.create(JavaArchive.class)
            // Camel CDI
            .addPackage(CdiCamelExtension.class.getPackage())
            // Test classes
            .addClasses(DefaultCamelContextBean.class,
                MockAnnotationRoute.class)
            // Bean archive deployment descriptor
            .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @Inject
    private DefaultCamelContextBean defaultCamelContext;

    @Inject
    @Uri("direct:start")
    private ProducerTemplate defaultInbound;

    @Inject
    @Mock("mock:result")
    private MockEndpoint defaultOutbound;

    @Test
    @InSequence(1)
    public void verifyCamelContext() {
        assertThat(defaultCamelContext.getName(), is(equalTo("camel-cdi")));
        assertThat(defaultOutbound.getCamelContext().getName(), is(equalTo(defaultCamelContext.getName())));
    }

    @Test
    @InSequence(2)
    public void configureAndStartCamelContexts() throws Exception {
        defaultCamelContext.start();
    }

    @Test
    @InSequence(3)
    public void sendMessageToInbound() throws InterruptedException {
        defaultOutbound.expectedMessageCount(1);
        defaultOutbound.expectedBodiesReceived("test");
        defaultOutbound.expectedHeaderReceived("foo","bar");

        defaultInbound.sendBodyAndHeader("test","foo","bar");

        assertIsSatisfied(2L, TimeUnit.SECONDS, defaultOutbound);
    }


    @Test
    @InSequence(4)
    public void stopCamelContext() throws Exception {
        defaultCamelContext.stop();
    }
}
