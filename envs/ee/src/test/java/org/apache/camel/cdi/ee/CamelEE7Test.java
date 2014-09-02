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
package org.apache.camel.cdi.ee;

import org.apache.camel.cdi.ee.category.Integration;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
@Category(Integration.class)
public class CamelEE7Test {

    @Deployment
    public static Archive<?> deployment() {
        return ShrinkWrap.create(EnterpriseArchive.class, "camel-ee7.ear")
            .addAsLibraries(
                Maven.configureResolver()
                    .workOffline()
                    .loadPomFromFile("pom.xml")
                    .resolve("io.astefanutti.camel.cdi:camel-cdi")
                    .withTransitivity()
                    .as(JavaArchive.class))
            .addAsModule(
                ShrinkWrap.create(JavaArchive.class)
                    .addClass(Bootstrap.class)
                    .addClass(HelloCamel.class)
                    // FIXME: Test class must be added until ARQ-659 is fixed
                    .addClass(CamelEE7Test.class)
                    .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml"));
    }

    @Test
    @InSequence(1)
    public void pauseWhileLogging() throws InterruptedException {
        Thread.sleep(10000L);
    }
}