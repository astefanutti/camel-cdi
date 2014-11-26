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
package org.apache.camel.cdi.karaf;

import org.apache.camel.CamelContext;
import org.apache.camel.cdi.CdiCamelContext;
import org.apache.camel.cdi.ContextName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.util.logging.Level;

public class Bootstrap {

    @Inject
    @ContextName("simple")
    CdiCamelContext context;

    Logger logger = LoggerFactory.getLogger(Bootstrap.class);

    @PostConstruct
    public void init() {

        logger.info(">> Create CamelContext and register Camel Route.");

        try {
            context.addRoutes(new SimpleCamelRoute());

            // Start Camel Context
            context.start();

            logger.info(">> CamelContext created and camel route started.");
        } catch (Exception ex) {
            java.util.logging.Logger.getLogger(Bootstrap.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @PreDestroy
    public void shutdown() {
        // Graceful Shutdown Camel Context
        try {
            context.stop();
            logger.info(">> CamelContext stopped .");
        } catch (Exception ex) {
            java.util.logging.Logger.getLogger(Bootstrap.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
