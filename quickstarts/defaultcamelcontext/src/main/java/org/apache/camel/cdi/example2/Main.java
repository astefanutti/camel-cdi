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
package org.apache.camel.cdi.example2;


import org.apache.camel.CamelContext;
import org.apache.deltaspike.cdise.api.CdiContainer;
import org.apache.deltaspike.cdise.api.CdiContainerLoader;

import javax.enterprise.inject.Vetoed;
import javax.enterprise.inject.spi.CDI;
import java.util.concurrent.CountDownLatch;

@Vetoed
public class Main {

    public static void main(String[] args) throws Exception {
        // Since version 2.3.0.Final and WELD-1915, Weld SE registers a shutdown hook. See WELD-2051. The system property above is available starting Weld 2.3.1.Final to deactivate the registration of the shutdown hook so that the example behave consistently between Weld and OpenWebBeans.
        System.setProperty("org.jboss.weld.se.shutdownHook", "false");

        CdiContainer container = CdiContainerLoader.getCdiContainer();
        ShutdownHook shutdownHook = new ShutdownHook(container);
        Runtime.getRuntime().addShutdownHook(shutdownHook);
        container.boot();

        // Start the Camel context
        CDI.current().select(CamelContext.class).get().start();

        // Wait until the JVM exits
        shutdownHook.latch.await();
    }

    @Vetoed
    private static final class ShutdownHook extends Thread {

        private final CountDownLatch latch = new CountDownLatch(1);
        private final CdiContainer container;

        private ShutdownHook(CdiContainer container) {
            this.container = container;
        }

        @Override
        public void run() {
            try {
                // Camel context is gracefully shut down by Camel CDI
                container.shutdown();
            } catch (Exception cause) {
                cause.printStackTrace();
            } finally {
                latch.countDown();
            }
        }
    }
}
