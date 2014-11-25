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
public class CdiKarafTest {

    protected static final transient Logger LOG = LoggerFactory.getLogger(CdiKarafTest.class);

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
                        "weld"
                ),
                features(
                        maven().groupId("org.apache.camel.karaf").artifactId("apache-camel").type("xml").classifier("features").version("2.14.0"),
                        "camel"
                ),
                features(
                        maven().groupId("io.astefanutti.camel.cdi").artifactId("karaf").type("xml").classifier("features").version("1.1-SNAPSHOT"),
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
