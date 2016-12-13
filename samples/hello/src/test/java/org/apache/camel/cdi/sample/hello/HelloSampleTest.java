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
package org.apache.camel.cdi.sample.hello;

import org.apache.camel.cdi.test.LogVerifier;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;

import static org.hamcrest.Matchers.containsInRelativeOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

@RunWith(Arquillian.class)
public class HelloSampleTest {

    @Deployment
    public static Archive<?> deployment() {
        return Maven.configureResolver().workOffline()
            // Camel CDI
            .loadPomFromFile("pom.xml")
            .resolve("io.astefanutti.camel.cdi:camel-cdi")
            .withoutTransitivity()
            .asSingle(JavaArchive.class)
            // Test classes
            .addPackage(HelloSampleTest.class.getPackage());
    }

    @ClassRule
    public static TestRule verifier = new LogVerifier() {
        @Override
        protected void verify() {
            assertThat("Log messages not found!", getMessages(),
                containsInRelativeOrder(
                    containsString("(CamelContext: camel-1) is starting"),
                    equalTo("Hello from CamelContext(camel-1)"),
                    containsString("(CamelContext: camel-1) is shutdown"))
            );
        }
    };

    @Test
    public void test() {
    }
}
