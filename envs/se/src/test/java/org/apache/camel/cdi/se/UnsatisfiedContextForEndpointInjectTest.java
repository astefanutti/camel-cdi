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

import org.apache.camel.cdi.CdiCamelExtension;
import org.apache.camel.cdi.se.bean.EndpointInjectWrongContextRoute;
import org.apache.camel.cdi.test.LogVerifier;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.junit.runners.model.Statement;

import javax.enterprise.inject.spi.DeploymentException;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.assertThat;

@RunWith(Arquillian.class)
public class UnsatisfiedContextForEndpointInjectTest {

    @Deployment
    public static Archive<?> deployment() {
        return ShrinkWrap.create(JavaArchive.class)
            // Camel CDI
            .addPackage(CdiCamelExtension.class.getPackage())
            // Test class
            .addClass(EndpointInjectWrongContextRoute.class)
            // Bean archive deployment descriptor
            .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    private static final LogVerifier log = new LogVerifier();

    @ClassRule
    public static TestRule rules = RuleChain
        .outerRule(log)
        .around(new TestRule() {
            @Override
            public Statement apply(final Statement base, Description description) {
                return new Statement() {
                    @Override
                    public void evaluate() throws Throwable {
                        try {
                            base.evaluate();
                        } catch (Throwable exception) {
                            assertThat(exception, is(instanceOf(DeploymentException.class)));
                            try {
                                // OpenWebBeans logs the deployment exception details
                                assertThat(log.getMessages(), hasItem("No Camel context with name [foo] is deployed!"));
                            } catch (AssertionError error) {
                                // Weld stores the deployment exception details in the exception message
                                assertThat(exception.getMessage(), containsString("No Camel context with name [foo] is deployed!"));
                            }
                        }
                    }
                };
            }
        });

    @Test
    public void test() {
    }
}
