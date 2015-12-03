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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.karaf.options.LogLevelOption;

import java.io.File;

import static org.junit.Assert.assertTrue;
import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.configureConsole;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.features;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.karafDistributionConfiguration;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.keepRuntimeFolder;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.logLevel;

@RunWith(PaxExam.class)
public class HelloPaxExamTest {

    @Configuration
    public Option[] config() {
        return new Option[]{
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

            // TODO: camel-core-osgi requires OSGi compendium API though this is not actually necessary
            features(maven("org.apache.karaf.features", "standard")
                    .type("xml").classifier("features").versionAsInProject(),
                "eventadmin"),
            // PAX CDI Weld
            features(maven("org.ops4j.pax.cdi", "pax-cdi-features")
                    .type("xml").classifier("features").versionAsInProject(),
                "pax-cdi-weld"),
            // Camel CDI
            features(maven("io.astefanutti.camel.cdi", "camel-cdi")
                    .type("xml").classifier("features").versionAsInProject(),
                "camel-cdi"),
            // Hello sample
            mavenBundle("io.astefanutti.camel.cdi", "camel-cdi-sample-hello").versionAsInProject()
        };
    }

    @Test
    public void test() {
        assertTrue(true);
    }
}