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
import org.apache.camel.ServiceStatus;
import org.apache.karaf.shell.api.console.Session;
import org.apache.karaf.shell.api.console.SessionFactory;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

import javax.inject.Inject;

import static org.apache.camel.cdi.osgi.CommonPaxExamOptions.ACTIVEMQ;
import static org.apache.camel.cdi.osgi.CommonPaxExamOptions.CAMEL_CDI;
import static org.apache.camel.cdi.osgi.CommonPaxExamOptions.CAMEL_COMMANDS;
import static org.apache.camel.cdi.osgi.CommonPaxExamOptions.CAMEL_SJMS;
import static org.apache.camel.cdi.osgi.CommonPaxExamOptions.JMS_FEATURE;
import static org.apache.camel.cdi.osgi.CommonPaxExamOptions.KARAF;
import static org.apache.camel.cdi.osgi.CommonPaxExamOptions.PAX_CDI_IMPL;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.when;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.debugConfiguration;

@Ignore("CAMEL-10405")
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class SjmsPaxExamTest {

    @Configuration
    public Option[] config() {
        return options(
            KARAF.option(),
            PAX_CDI_IMPL.option(),
            CAMEL_CDI.option(),
            CAMEL_COMMANDS.option(),
            // SJMS sample
            JMS_FEATURE.option(),
            CAMEL_SJMS.option(),
            ACTIVEMQ.option(),
            mavenBundle()
                .groupId("io.astefanutti.camel.cdi")
                .artifactId("camel-cdi-sample-sjms")
                .versionAsInProject(),
            when(false)
                .useOptions(
                    debugConfiguration("5005", true))
        );
    }

    @Inject
    private CamelContext context;

    @Inject
    private SessionFactory sessionFactory;

    @Test
    public void getRouteStatus() {
        assertThat("Route status is incorrect!", context.getRouteStatus("consumer-route"), equalTo(ServiceStatus.Started));
    }

    @Test
    public void executeCommands() throws Exception {
        Session session = sessionFactory.create(System.in, System.out, System.err);
        session.execute("camel:context-list");
        session.execute("camel:route-list");
        session.execute("camel:route-info consumer-route");
    }
}
