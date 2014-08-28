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
package io.astefanutti.camel.cdi;

import io.astefanutti.camel.cdi.bean.InjectedTypeConverterRoute;
import io.astefanutti.camel.cdi.bean.TypeConverterInput;

import io.astefanutti.camel.cdi.bean.TypeConverterOutput;
import io.astefanutti.camel.cdi.converter.InjectedTypeConverter;
import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.mock.MockEndpoint;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.Filters;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertThat;
import static org.hamcrest.Matchers.*;

import static org.apache.camel.component.mock.MockEndpoint.assertIsSatisfied;

@RunWith(Arquillian.class)
public class InjectedTypeConverterTest {

    @Deployment
    public static Archive<?> deployment() {
        return ShrinkWrap.create(JavaArchive.class)
            // Camel CDI
            .addPackages(false, Filters.exclude(".*Test.*"), CdiCamelExtension.class.getPackage()) 
            // Test class
            .addClass(InjectedTypeConverterRoute.class)
            // Type converter
            .addClass(InjectedTypeConverter.class)
            // TODO: use CDI to automatically register the converter
            .addAsManifestResource(new StringAsset("io.astefanutti.camel.cdi.converter"), ArchivePaths.create("services/org/apache/camel/TypeConverter"))
            // Bean archive deployment descriptor
            .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @Config
    @Produces
    private static Properties configuration() {
        Properties configuration = new Properties();
        configuration.put("property", "value");
        return configuration;
    }

    @Inject
    @Uri("direct:inbound")
    private ProducerTemplate inbound;

    @Inject
    @Uri("mock:outbound")
    private MockEndpoint outbound;

    @Test
    @InSequence(1)
    public void startCamelContext(CamelContext context) throws Exception {
        context.start();
    }

    @Test
    @InSequence(2)
    public void sendMessageToInboundConsumer() throws InterruptedException {
        outbound.expectedMessageCount(1);

        TypeConverterInput input = new TypeConverterInput();
        input.setProperty("property value is [{{property}}]");
        
        inbound.sendBody(input);

        assertIsSatisfied(2L, TimeUnit.SECONDS, outbound);

        assertThat(outbound.getExchanges().get(0).getIn().getBody(TypeConverterOutput.class).getProperty(), is(equalTo("property value is [value]")));
    }

    @Test
    @InSequence(3)
    public void stopCamelContext(CamelContext context) throws Exception {
        context.stop();
    }
}
