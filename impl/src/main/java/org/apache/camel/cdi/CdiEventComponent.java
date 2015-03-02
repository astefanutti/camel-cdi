/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.cdi;

import org.apache.camel.Endpoint;
import org.apache.camel.impl.DefaultComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Named("cdi-event")
@ApplicationScoped
/* package-private */ class CdiEventComponent extends DefaultComponent {

    private final Logger logger = LoggerFactory.getLogger(CdiEventComponent.class);

    private final Map<Class, Set<CdiEventEndpoint>> endpoints = new HashMap<>();

    @Inject
    private BeanManager manager;

    BeanManager getBeanManager() {
        return manager;
    }

    private void notify(@Observes Object event) {
        // TODO: should ideally replicate the CDI observer resolution mechanism and be dynamic given the configured endpoints
        Set<CdiEventEndpoint> destinationEndpoints = new HashSet<>();
        // Block only for finding the endpoints
        synchronized (endpoints) {
            logger.debug("Handling CDI event of type '{}', possible endpoints: {}", event.getClass().getName(), endpoints.size());
            for (Map.Entry<Class, Set<CdiEventEndpoint>> entry : endpoints.entrySet()) {
                if (entry.getKey().isAssignableFrom(event.getClass()))
                    destinationEndpoints.addAll(entry.getValue());
            }
        }
        // Then notify them after leaving the monitor
        for (CdiEventEndpoint destinationEndpoint : destinationEndpoints) {
            logger.debug("Found endpoint handling event (super)type '{}': {}", event.getClass().getName(), destinationEndpoint);
            destinationEndpoint.notify(event);
        }
    }

    @Override
    protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters) throws Exception {
        logger.debug("New endpoint for URI: {}, remaining: {}, parameters: {}", new Object[]{uri, remaining, parameters});
        if (remaining == null || remaining.length() == 0 || "/".equals(remaining)) {
            logger.info("Creating new catchall (java.lang.Object) event endpoint");
            return new CdiEventEndpoint<>(Object.class, uri, this);
        } else {
            remaining = remaining.endsWith("/") ? remaining.substring(0, remaining.length() - 1) : remaining;
            logger.info("Creating new endpoint for event type: {}", remaining);
            Class type = CdiEventComponent.class.getClassLoader().loadClass(remaining);
            return new CdiEventEndpoint<>(type, uri, this);
        }
    }

    void registerEndpoint(CdiEventEndpoint endpoint) {
        synchronized (endpoints) {
            Set<CdiEventEndpoint> registered = endpoints.get(endpoint.getType());
            if (registered == null) {
                registered = new HashSet<>();
                endpoints.put(endpoint.getType(), registered);
            }
            registered.add(endpoint);
        }
    }

    void unregisterEndpoint(CdiEventEndpoint endpoint) {
        synchronized (endpoints) {
            if (endpoints.containsKey(endpoint.getType()))
                endpoints.get(endpoint.getType()).remove(endpoint);
        }
    }
}

