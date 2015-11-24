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
package org.apache.camel.cdi.ee;

import org.apache.camel.CamelContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;

@Startup
@Singleton
public class Bootstrap {

    @Inject
    CamelContext context;

    Logger logger = LoggerFactory.getLogger(Bootstrap.class);

    @PostConstruct
    void init() {
        try {
            logger.info("Starting {}...", context);
            context.start();
        } catch (Exception cause) {
            logger.error("Error while starting {}", context, cause);
        }
    }

    @PreDestroy
    void shutdown() {
        try {
            logger.info("Gracefully shutting down {}...", context);
            context.stop();
        } catch (Exception cause) {
            logger.error("Error while stopping {}", context, cause);
        }
    }
}
