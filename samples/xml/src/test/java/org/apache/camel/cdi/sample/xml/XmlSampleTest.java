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
package org.apache.camel.cdi.sample.xml;

import java.util.concurrent.TimeUnit;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.camel.CamelContext;
import org.apache.camel.Endpoint;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.ServiceStatus;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.cdi.CdiCamelExtension;
import org.apache.camel.cdi.Uri;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.management.event.CamelContextStartingEvent;
import org.apache.camel.model.ModelCamelContext;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.apache.camel.component.mock.MockEndpoint.assertIsSatisfied;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

@RunWith(Arquillian.class)
public class XmlSampleTest {

    @Deployment
    public static Archive<?> deployment() {
        return ShrinkWrap.create(JavaArchive.class)
            // Camel CDI
            .addPackage(CdiCamelExtension.class.getPackage())
            // Test classes
            .addPackage(XmlSampleTest.class.getPackage())
            // Bean archive deployment descriptor
            .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    void pipeMatrixStream(@Observes CamelContextStartingEvent event,
                          ModelCamelContext context) throws Exception {
        context.getRouteDefinition("matrix")
            .adviceWith(context, new AdviceWithRouteBuilder() {
                @Override
                public void configure() {
                    weaveAddLast().to("mock:matrix");
                }
            });
    }

    @Named
    @Inject
    private Endpoint neo;

    @Inject
    private ProducerTemplate prompt;

    static class RescueMission extends RouteBuilder {

        @Override
        public void configure() {
            from("seda:rescue?multipleConsumers=true").routeId("rescue mission").to("mock:zion");
        }
    }

    @Test
    @InSequence(1)
    public void takeTheBluePill(@Uri("mock:matrix") MockEndpoint matrix) throws InterruptedException {
        matrix.expectedMessageCount(1);
        matrix.expectedBodiesReceived("Matrix » Take the blue pill!");

        prompt.sendBody(neo, "Take the blue pill!");

        assertIsSatisfied(2L, TimeUnit.SECONDS, matrix);
    }

    @Test
    @InSequence(2)
    public void takeTheRedPill(@Uri("mock:zion") MockEndpoint zion) throws InterruptedException {
        zion.expectedMessageCount(1);
        zion.expectedHeaderReceived("location", "matrix");

        prompt.sendBody(neo, "Take the red pill!");

        assertIsSatisfied(2L, TimeUnit.SECONDS, zion);
    }

    @Test
    @InSequence(3)
    public void verifyRescue(CamelContext context) {
        assertThat("Neo is still in the matrix!",
            context.getRouteStatus("terminal"), is(equalTo(ServiceStatus.Stopped)));
    }
}
