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

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.osgi.StartLevelAware;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.osgi.metadata.OSGiManifestBuilder;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.log.LogService;

import java.io.InputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(Arquillian.class)
public class CdiArquillianKarafTest {

    private final static String SYMBOLIC_NAME = "my-bundle";

    /**
     * Injects the bundle context.
     */
    @ArquillianResource
    public BundleContext bundleContext;

    /**
     * Injects the bundle.
     */
    @ArquillianResource
    public Bundle bundle;

    /**
     * Creates a new Java Archive.
     *
     * @return the archive
     */
    @Deployment
    @StartLevelAware(autostart = true)
    public static Archive<?> createdeployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "mybundle.jar");
        archive.setManifest(new Asset() {
            public InputStream openStream() {
                // Adds OSGi entries
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleManifestVersion(2);
                builder.addBundleSymbolicName(SYMBOLIC_NAME);
                builder.addBundleVersion("1.0.0");

                // Import the LogService package
                builder.addImportPackages(LogService.class);

                return builder.openStream();
            }
        });

        return archive;
    }

    @Test
    public void testBundleContextInjection() {
        assertNotNull("BundleContext injected", bundleContext);
    }

    @Test
    public void testBundleInjection() throws Exception {
        assertNotNull("Bundle injected", bundle);
        assertEquals("Bundle RESOLVED", Bundle.RESOLVED, bundle.getState());
        bundle.start();
        assertEquals("Bundle ACTIVE", Bundle.ACTIVE, bundle.getState());
    }

}
