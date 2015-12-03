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
import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.CommandSession;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.karaf.options.LogLevelOption;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

import javax.inject.Inject;
import java.io.File;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.configureConsole;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.features;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.karafDistributionConfiguration;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.keepRuntimeFolder;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.logLevel;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class SjmsPaxExamTest {

    @Configuration
    public Option[] config() {
        return new Option[] {
            karafDistributionConfiguration()
                .frameworkUrl(
                    maven().groupId("org.apache.karaf").artifactId("apache-karaf").versionAsInProject().type("zip"))
                .name("Apache Karaf")
                .useDeployFolder(false)
                .unpackDirectory(new File("target/pax-exam/unpack/")),
            keepRuntimeFolder(),
            // Don't bother with local console output as it just ends up cluttering the logs
            configureConsole().ignoreLocalConsole(),
            // Force the log level to INFO so we have more details during the test. It defaults to WARN.
            logLevel(LogLevelOption.LogLevel.INFO),

            // Option to be used to do remote debugging
            //debugConfiguration("5005", true),

            // JUnit and Hamcrest
            junitBundles(),
            // PAX CDI Weld
            features(maven("org.ops4j.pax.cdi", "pax-cdi-features")
                .type("xml").classifier("features").versionAsInProject(),
                "pax-cdi-weld"),
            // Camel CDI
            features(maven("io.astefanutti.camel.cdi", "camel-cdi")
                .type("xml").classifier("features").versionAsInProject(),
                "camel-cdi"),
            // Karaf Camel commands
            mavenBundle("org.apache.camel.karaf", "camel-karaf-commands").versionAsInProject(),
            mavenBundle("org.apache.camel", "camel-commands-core").versionAsInProject(),
            mavenBundle("org.apache.camel", "camel-catalog").versionAsInProject(),

            // SJMS sample
            mavenBundle("io.astefanutti.camel.cdi", "camel-cdi-sample-sjms").versionAsInProject(),
            features(maven("org.apache.karaf.features", "enterprise")
                .type("xml").classifier("features").versionAsInProject(),
                "jms"),
            features(maven("org.apache.activemq", "activemq-karaf")
                .type("xml").classifier("features").versionAsInProject(),
                "activemq-broker"),
            mavenBundle("org.apache.camel", "camel-sjms").versionAsInProject(),
            mavenBundle("commons-pool", "commons-pool").versionAsInProject()
        };
    }

    @Inject
    private CamelContext context;

    @Inject
    private CommandProcessor commandProcessor;

    @Test
    public void getRouteStatus() {
        assertThat(context.getRouteStatus("consumer-route"), equalTo(ServiceStatus.Started));
    }

    @Test
    public void executeCommands() throws Exception {
        CommandSession session = commandProcessor.createSession(System.in, System.out, System.err);
        session.execute("camel:context-list");
        session.execute("camel:route-list");
        session.execute("camel:route-info consumer-route");
    }
}
