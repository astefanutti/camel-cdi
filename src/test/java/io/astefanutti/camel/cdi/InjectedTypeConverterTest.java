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
import io.astefanutti.camel.cdi.pojo.TypeConverterInput;
import io.astefanutti.camel.cdi.pojo.TypeConverterOutput;
import io.astefanutti.camel.cdi.converter.InjectedTypeConverter;
import org.apache.camel.CamelContext;
import org.apache.camel.NoTypeConversionAvailableException;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.TypeConverter;
import org.apache.camel.component.mock.MockEndpoint;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.Filters;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.enterprise.inject.Produces;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static org.apache.camel.component.mock.MockEndpoint.assertIsSatisfied;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

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
            // No need as Camel CDI automatically registers the type converter bean
            //.addAsManifestResource(new StringAsset("io.astefanutti.camel.cdi.converter"), ArchivePaths.create("services/org/apache/camel/TypeConverter"))
            // Bean archive deployment descriptor
            .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @Config
    @Produces
    private static Properties configuration() {
        Properties configuration = new Properties();
        configuration.put("property1", "value 1");
        configuration.put("property2", "value 2");
        return configuration;
    }

    @Test
    @InSequence(1)
    public void startCamelContext(CamelContext context) throws Exception {
        context.start();
    }

    @Test
    @InSequence(2)
    public void sendMessageToInboundConsumer(@Uri("direct:inbound") ProducerTemplate inbound, @Uri("mock:outbound") MockEndpoint outbound) throws InterruptedException {
        outbound.expectedMessageCount(1);

        TypeConverterInput input = new TypeConverterInput();
        input.setProperty("property value is [{{property1}}]");
        
        inbound.sendBody(input);

        assertIsSatisfied(2L, TimeUnit.SECONDS, outbound);
        assertThat(outbound.getExchanges().get(0).getIn().getBody(TypeConverterOutput.class).getProperty(), is(equalTo("property value is [value 1]")));
    }

    @Test
    @InSequence(3)
    public void convertWithTypeConverter(TypeConverter converter) throws NoTypeConversionAvailableException {
        TypeConverterInput input = new TypeConverterInput();
        input.setProperty("property value is [{{property2}}]");

        TypeConverterOutput output = converter.mandatoryConvertTo(TypeConverterOutput.class, input);

        assertThat(output.getProperty(), is(equalTo("property value is [value 2]")));
    }

    @Test
    @InSequence(4)
    public void stopCamelContext(CamelContext context) throws Exception {
        context.stop();
    }
}
