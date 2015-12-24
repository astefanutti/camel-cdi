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

import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.karaf.options.LogLevelOption;
import org.ops4j.pax.exam.options.DefaultCompositeOption;

import java.io.File;

import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.url;
import static org.ops4j.pax.exam.CoreOptions.vmOption;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.configureConsole;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.features;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.karafDistributionConfiguration;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.keepRuntimeFolder;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.logLevel;

public enum CommonPaxExamOptions {

    KARAF (
        karafDistributionConfiguration()
            .frameworkUrl(
                maven()
                    .groupId("org.apache.karaf")
                    .artifactId("apache-karaf")
                    .versionAsInProject()
                    .type("zip"))
            .name("Apache Karaf")
            .useDeployFolder(false)
            .unpackDirectory(new File("target/pax-exam/unpack/")),
        keepRuntimeFolder(),
        // Don't bother with local console output as it just ends up cluttering the logs
        configureConsole().ignoreLocalConsole(),
        // Force the log level to INFO so we have more details during the test. It defaults to WARN.
        logLevel(LogLevelOption.LogLevel.INFO),
        // JaCoCo code coverage
        System.getProperty("jacoco.agent", "").isEmpty() ?
            new Option() {} :
            vmOption(System.getProperty("jacoco.agent")),
        // JUnit and Hamcrest
        junitBundles()
    ),
    CAMEL_COMMANDS(
        mavenBundle()
            .groupId("org.apache.camel.karaf")
            .artifactId("camel-karaf-commands")
            .versionAsInProject(),
        mavenBundle()
            .groupId("org.apache.camel")
            .artifactId("camel-commands-core")
            .versionAsInProject(),
        mavenBundle()
            .groupId("org.apache.camel")
            .artifactId("camel-catalog")
            .versionAsInProject()
    ),
    PAX_CDI (
        features(
            maven()
                .groupId("org.ops4j.pax.cdi")
                .artifactId("pax-cdi-features")
                .type("xml")
                .classifier("features")
                .versionAsInProject(),
            "pax-cdi")
    ),
    PAX_CDI_IMPL (
        // TODO: use when()...
        System.getProperty("pax.cdi.implementation", "").isEmpty() ?
            features(
                maven()
                    .groupId("org.ops4j.pax.cdi")
                    .artifactId("pax-cdi-features")
                    .type("xml")
                    .classifier("features")
                    .versionAsInProject(),
                "pax-cdi-weld") :
            features(
                url("file:" + System.getProperty("user.dir") + "/src/test/features/features.xml"),
                System.getProperty("pax.cdi.implementation"))
    ),
    CAMEL_CDI (
        features(
            maven()
                .groupId("io.astefanutti.camel.cdi")
                .artifactId("camel-cdi")
                .type("xml")
                .classifier("features")
                .versionAsInProject(),
            "camel-cdi")
    ),
    JMS_FEATURE (
        features(
            maven()
                .groupId("org.apache.karaf.features")
                .artifactId("enterprise")
                .type("xml")
                .classifier("features")
                .versionAsInProject(),
            "jms")
    ),
    CAMEL_SJMS (
        mavenBundle()
            .groupId("org.apache.camel")
            .artifactId("camel-sjms")
            .versionAsInProject(),
        mavenBundle()
            .groupId("commons-pool")
            .artifactId("commons-pool")
            .versionAsInProject()
    ),
    ACTIVEMQ (
        features(
            maven()
                .groupId("org.apache.karaf.features")
                .artifactId("enterprise")
                .type("xml")
                .classifier("features")
                .versionAsInProject(),
            "jms"),
        features(
            maven()
                .groupId("org.apache.activemq")
                .artifactId("activemq-karaf")
                .type("xml")
                .classifier("features")
                .versionAsInProject(),
            "activemq-broker")
    ),
    CAMEL_METRICS (
        mavenBundle()
            .groupId("org.apache.camel")
            .artifactId( "camel-metrics")
            .versionAsInProject(),
        mavenBundle()
            .groupId("io.dropwizard.metrics")
            .artifactId("metrics-json")
            .versionAsInProject(),
        mavenBundle()
            .groupId("com.fasterxml.jackson.core")
            .artifactId("jackson-core")
            .versionAsInProject(),
        mavenBundle()
            .groupId("com.fasterxml.jackson.core")
            .artifactId("jackson-databind")
            .versionAsInProject(),
        mavenBundle()
            .groupId("com.fasterxml.jackson.core")
            .artifactId("jackson-annotations")
            .versionAsInProject()
    ),
    METRICS_CDI (
        mavenBundle()
            .groupId("io.dropwizard.metrics")
            .artifactId("metrics-core")
            .versionAsInProject(),
        mavenBundle()
            .groupId("io.dropwizard.metrics")
            .artifactId("metrics-annotation")
            .versionAsInProject(),
        mavenBundle()
            .groupId("io.astefanutti.metrics.cdi")
            .artifactId("metrics-cdi")
            .versionAsInProject()
    );

    private final Option[] options;

    CommonPaxExamOptions(Option... options) {
        this.options = options;
    }

    public Option option() {
        return new DefaultCompositeOption(options);
    }
}
