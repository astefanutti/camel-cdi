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
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.cdi.CdiCamelExtension;
import org.apache.camel.cdi.CdiPropertiesComponent;
import org.apache.camel.cdi.Uri;
import org.apache.camel.cdi.se.bean.PropertyInjectBean;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.component.properties.PropertiesComponent;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Named;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static org.apache.camel.component.mock.MockEndpoint.assertIsSatisfied;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

@RunWith(Arquillian.class)
public class PropertyInjectTest {

    @Deployment
    public static Archive<?> deployment() {
        return ShrinkWrap.create(JavaArchive.class)
            // Camel CDI
            .addPackage(CdiCamelExtension.class.getPackage())
            // Test class
            .addClass(PropertyInjectBean.class)
            // Bean archive deployment descriptor
            .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @Produces
    @ApplicationScoped
    @Named("properties")
    private static PropertiesComponent configuration() {
        Properties configuration = new Properties();
        configuration.put("property", "value");
        return new CdiPropertiesComponent(configuration);
    }

    @Test
    @InSequence(1)
    public void configureAndStartCamelContext(CamelContext context) throws Exception {
        context.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:in").bean(PropertyInjectBean.class).to("mock:out");
            }
        });

        context.start();
    }

    @Test
    @InSequence(2)
    public void sendMessageToInbound(@Uri("direct:in") ProducerTemplate in, @Uri("mock:out") MockEndpoint out) throws InterruptedException {
        out.expectedMessageCount(1);
        out.expectedBodiesReceived("test");
        out.expectedHeaderReceived("header", "value");
        
        in.sendBody("test");

        assertIsSatisfied(2L, TimeUnit.SECONDS, out);
    }

    @Test
    @InSequence(3)
    public void retrieveContextualReference(PropertyInjectBean bean) {
        assertThat(bean.getProperty(), is(equalTo("value")));
    }

    @Test
    @InSequence(4)
    public void stopCamelContext(CamelContext context) throws Exception {
        context.stop();
    }
}
