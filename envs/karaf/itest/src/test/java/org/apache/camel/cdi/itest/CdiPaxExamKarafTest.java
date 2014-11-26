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
package org.apache.camel.cdi.itest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.ConfigurationManager;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.karaf.options.KarafDistributionOption;
import org.ops4j.pax.exam.karaf.options.LogLevelOption;
import org.ops4j.pax.exam.options.MavenArtifactUrlReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

//import static org.apache.karaf.tooling.exam.options.KarafDistributionOption.karafDistributionConfiguration;
import static org.junit.Assert.assertTrue;
import static org.ops4j.pax.exam.CoreOptions.bundle;
import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.*;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.features;

@RunWith(PaxExam.class)
public class CdiPaxExamKarafTest {

    protected static final transient Logger LOG = LoggerFactory.getLogger(CdiPaxExamKarafTest.class);

    private static final MavenArtifactUrlReference KARAF_URL = maven("org.apache.karaf", "apache-karaf", karafVersion()).type("zip");

    @Configuration
    public Option[] config() {
        return new Option[]{
                getKarafDistributionOption(),
                keepRuntimeFolder(),
                // Don't bother with local console output as it just ends up cluttering the logs
                configureConsole().ignoreLocalConsole(),
                // Force the log level to INFO so we have more details during the test.  It defaults to WARN.
                logLevel(LogLevelOption.LogLevel.INFO),

                // Option to be used to do remote debugging
                // debugConfiguration("5005", true),

                // Load Features Camel, Camel-cdi & PAX CDI, Weld
                features(
                        maven().groupId("org.ops4j.pax.cdi").artifactId("pax-cdi-features").type("xml").classifier("features").version("0.8.0"),
                        "pax-cdi-1.2-weld"
                ),
                features(
                        maven().groupId("org.apache.camel.karaf").artifactId("apache-camel").type("xml").classifier("features").version("2.14.0"),
                        "camel"
                ),
                features(
                        maven().groupId("io.astefanutti.camel.cdi").artifactId("features").type("xml").classifier("features").version("1.1-SNAPSHOT"),
                        "camel-cdi"
                ),
                // Install example
                bundle("mvn:io.astefanutti.camel.cdi/example/1.1-SNAPSHOT")
        };
    }

    public static String karafVersion() {
        ConfigurationManager cm = new ConfigurationManager();
        String karafVersion = cm.getProperty("pax.exam.karaf.version", "3.0.2");
        return karafVersion;
    }

    @Test
    public void test() throws Exception {
        assertTrue(true);
    }

    public static Option getKarafDistributionOption() {
        String karafVersion = karafVersion();
        LOG.info("*** The karaf version is " + karafVersion + " ***");
        return KarafDistributionOption.karafDistributionConfiguration()
                .frameworkUrl(KARAF_URL)
                .karafVersion(karafVersion)
                .name("Apache Karaf")
                .useDeployFolder(false)
                .unpackDirectory(new File("target/paxexam/unpack/"));
    }

}
