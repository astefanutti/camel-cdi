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
import org.apache.camel.cdi.CdiCamelExtension;
import org.apache.camel.cdi.CdiPropertiesComponent;
import org.apache.camel.component.properties.PropertiesComponent;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
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

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

@RunWith(Arquillian.class)
public class UndefinedPropertyTest {

    @Deployment
    public static Archive<?> deployment() {
        return ShrinkWrap.create(JavaArchive.class)
            // Camel CDI
            .addPackage(CdiCamelExtension.class.getPackage())
            // Bean archive deployment descriptor
            .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @Produces
    @ApplicationScoped
    @Named("properties")
    private static PropertiesComponent configuration() {
        Properties configuration = new Properties();
        configuration.put("from", "inbound");
        // Do not add the looked up property for test purpose
        //configuration.put("to", "mock:outbound");
        return new CdiPropertiesComponent(configuration);
    }

    @Test
    public void lookupDefinedProperty(CamelContext context) throws Exception {
        assertThat("Resolved property value is incorrect", context.resolvePropertyPlaceholders("{{from}}"), is(equalTo("inbound")));
    }

    @Test
    public void lookupUndefinedProperty(CamelContext context) {
        try {
            context.resolvePropertyPlaceholders("{{to}}");
            fail("No exception is thrown!");
        } catch (Exception cause) {
            assertThat("Exception thrown is incorrect", cause, is(instanceOf(IllegalArgumentException.class)));
            assertThat("Exception message is incorrect", cause.getMessage(), is(equalTo("Property with key [to] not found in properties from text: {{to}}")));
        }
    }
}
