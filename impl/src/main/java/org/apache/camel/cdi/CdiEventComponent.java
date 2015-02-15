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
package org.apache.camel.cdi;

import org.apache.camel.Endpoint;
import org.apache.camel.impl.DefaultComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.event.Reception;
import javax.enterprise.event.TransactionPhase;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.ObserverMethod;
import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Singleton;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.*;

/**
 * @author Christian Bauer
 */
public class CdiEventComponent
    extends DefaultComponent
    implements Bean<CdiEventComponent>, ObserverMethod {

    private final Logger logger = LoggerFactory.getLogger(CdiEventComponent.class);

    final protected BeanManager beanManager;
    final protected Map<Class, Set<CdiEventEndpoint>> endpoints = new HashMap<>();

    public CdiEventComponent(BeanManager beanManager) {
        this.beanManager = beanManager;
    }

    public BeanManager getBeanManager() {
        return beanManager;
    }

    @Override
    public Class<?> getBeanClass() {
        return CdiEventComponent.class;
    }

    @Override
    public Type getObservedType() {
        return Object.class;
    }

    @Override
    public Set<Annotation> getObservedQualifiers() {
        return new HashSet<Annotation>() {{
            add(new AnnotationLiteral<Any>() {
            });
        }};
    }

    @Override
    public Reception getReception() {
        return Reception.ALWAYS;
    }

    @Override
    public TransactionPhase getTransactionPhase() {
        return TransactionPhase.IN_PROGRESS;
    }

    @Override
    public Set<InjectionPoint> getInjectionPoints() {
        return Collections.emptySet();
    }

    @Override
    public boolean isNullable() {
        return false;
    }

    @Override
    public Set<Type> getTypes() {
        Set<Type> types = new HashSet<Type>();
        types.add(CdiEventComponent.class);
        types.add(Object.class);
        return types;
    }

    @Override
    public Set<Annotation> getQualifiers() {
        return new HashSet<Annotation>() {{
            add(new AnnotationLiteral<Any>() {
            });
            add(new AnnotationLiteral<Default>() {
            });
        }};
    }

    @Override
    public Class<? extends Annotation> getScope() {
        return Singleton.class;
    }

    @Override
    public String getName() {
        return CdiEvent.CDI_EVENT;
    }

    @Override
    public Set<Class<? extends Annotation>> getStereotypes() {
        return Collections.emptySet();
    }

    @Override
    public boolean isAlternative() {
        return false;
    }

    @Override
    public CdiEventComponent create(CreationalContext<CdiEventComponent> creationalContext) {
        return this;
    }

    @Override
    public void destroy(CdiEventComponent instance, CreationalContext<CdiEventComponent> creationalContext) {
    }

    @SuppressWarnings("unchecked")
    @Override
    public void notify(Object o) {
        try {
            Set<CdiEventEndpoint> destinationEndpoints = new HashSet<>();

            // Block only for finding the endpoints
            synchronized (endpoints) {
                logger.debug("Handling CDI event of type '{}', possible endpoints: {}", o.getClass().getName(), endpoints.size());
                for (Map.Entry<Class, Set<CdiEventEndpoint>> entry : endpoints.entrySet()) {
                    if (entry.getKey().isAssignableFrom(o.getClass()))
                        destinationEndpoints.addAll(entry.getValue());
                }
            }

            // Then notify them after leaving the monitor
            for (CdiEventEndpoint destinationEndpoint : destinationEndpoints) {
                logger.debug("Found endpoint handling event (super)type '{}': {}", o.getClass().getName(), destinationEndpoint);
                destinationEndpoint.notify(o);
            }

        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters) throws Exception {
        logger.debug("New endpoint for URI: {}, remaining: {}, parameters: {}", new Object[] {uri, remaining, parameters});
        if (remaining == null || remaining.length() == 0 || "/".equals(remaining)) {
            logger.info("Creating new catchall (java.lang.Object) event endpoint");
            return createEndpoint(Object.class, uri);
        } else {
            remaining = remaining.endsWith("/") ? remaining.substring(0, remaining.length() - 1) : remaining;
            logger.info("Creating new endpoint for event type: {}", remaining);
            Class type = CdiEventComponent.class.getClassLoader().loadClass(remaining);
            return createEndpoint(type, uri);
        }
    }

    protected <T> CdiEventEndpoint<T> createEndpoint(Class<T> type, String uri) throws Exception {
        return new CdiEventEndpoint<T>(type, uri, this);
    }

    public void registerEndpoint(CdiEventEndpoint endpoint) {
        synchronized (endpoints) {
            Set<CdiEventEndpoint> registered = endpoints.get(endpoint.getType());
            if (registered == null) {
                registered = new HashSet<>();
                endpoints.put(endpoint.getType(), registered);
            }
            registered.add(endpoint);
        }
    };

    public void unregisterEndpoint(CdiEventEndpoint endpoint) {
        synchronized (endpoints) {
            if (endpoints.containsKey(endpoint.getType()))
                endpoints.get(endpoint.getType()).remove(endpoint);
        }
    };
}

