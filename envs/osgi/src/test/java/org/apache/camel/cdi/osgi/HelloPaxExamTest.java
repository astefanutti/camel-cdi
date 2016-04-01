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
package org.apache.camel.cdi.osgi;

import org.apache.camel.CamelContext;
import org.apache.camel.api.management.mbean.ManagedRouteMBean;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;

import javax.inject.Inject;

import static org.apache.camel.cdi.osgi.CommonPaxExamOptions.CAMEL_CDI;
import static org.apache.camel.cdi.osgi.CommonPaxExamOptions.KARAF;
import static org.apache.camel.cdi.osgi.CommonPaxExamOptions.PAX_CDI_IMPL;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.when;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.debugConfiguration;

@RunWith(PaxExam.class)
public class HelloPaxExamTest {

    @Configuration
    public Option[] config() {
        return options(
            KARAF.option(),
            PAX_CDI_IMPL.option(),
            CAMEL_CDI.option(),
            // Hello sample
            mavenBundle()
                .groupId("io.astefanutti.camel.cdi")
                .artifactId("camel-cdi-sample-hello")
                .versionAsInProject(),
            when(false)
                .useOptions(
                    debugConfiguration("5005", true))
        );
    }

    @Inject
    private CamelContext context;

    @Test
    public void test() throws Exception {
        assertThat("Number of routes is incorrect!", context.getRoutes().size(), is(equalTo(1)));
        ManagedRouteMBean route = context.getManagedRoute(context.getRoutes().get(0).getId(), ManagedRouteMBean.class);
        assertThat("Number of exchanges completed is incorrect!", route.getExchangesCompleted(), is(equalTo(1L)));
    }
}
