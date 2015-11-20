/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.cdi.ee;

import org.apache.camel.cdi.ee.category.Integration;
import org.apache.camel.cdi.ee.category.WildFlyCamel;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.wildfly.extension.camel.CamelAware;

// Should ideally be removed so that there is no compile dependency on WildFly Camel
@CamelAware
@RunWith(Arquillian.class)
@Category({Integration.class, WildFlyCamel.class})
public class CamelWildFlyEarTest {

    @Deployment
    public static Archive<?> deployment() {
        return ShrinkWrap.create(EnterpriseArchive.class, "camel-wildfly.ear")
            .addAsManifestResource("jboss-all.xml")
            .addAsManifestResource("jboss-deployment-structure.xml")
            .addAsModule(
                ShrinkWrap.create(JavaArchive.class, "camel-wildfly.jar")
                    .addClass(Bootstrap.class)
                    .addClass(HelloCamel.class)
                    // FIXME: Test class must be added until ARQ-659 is fixed
                    .addClass(CamelWildFlyEarTest.class)
                    .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml"));
    }

    @Test
    @InSequence(1)
    public void pauseWhileLogging() throws InterruptedException {
        Thread.sleep(10000L);
    }
}