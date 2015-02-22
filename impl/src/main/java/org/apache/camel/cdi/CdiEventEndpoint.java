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

import org.apache.camel.Component;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.impl.DefaultConsumer;
import org.apache.camel.impl.DefaultEndpoint;
import org.apache.camel.impl.DefaultProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.inject.spi.BeanManager;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Christian Bauer
 */
public class CdiEventEndpoint<T> extends DefaultEndpoint {

    static private final Logger logger = LoggerFactory.getLogger(CdiEventEndpoint.class);

    public static class Consumer<T> extends DefaultConsumer {

        public Consumer(CdiEventEndpoint<T> endpoint, Processor processor) {
            super(endpoint, processor);
        }

        @SuppressWarnings("unchecked")
        @Override
        public CdiEventEndpoint<T> getEndpoint() {
            return (CdiEventEndpoint<T>) super.getEndpoint();
        }

        @Override
        protected void doStart() throws Exception {
            super.doStart();
            getEndpoint().registerConsumer(this);
        }

        @Override
        protected void doStop() throws Exception {
            getEndpoint().unregisterConsumer(this);
            super.doStop();
        }

        public void notify(T event) throws Exception {
            Exchange exchange = getEndpoint().createExchange();
            exchange.getIn().setBody(event);
            getProcessor().process(exchange);
        }
    }

    public static class Producer<T> extends DefaultProducer {

        public Producer(CdiEventEndpoint<T> endpoint) {
            super(endpoint);
        }

        @SuppressWarnings("unchecked")
        @Override
        public CdiEventEndpoint<T> getEndpoint() {
            return (CdiEventEndpoint<T>) super.getEndpoint();
        }

        @Override
        public void process(Exchange exchange) throws Exception {
            BeanManager beanManager = getEndpoint().getComponent().getBeanManager();
            Annotation[] qualifiers = exchange.getIn().getHeader(CdiEvent.CDI_EVENT_QUALIFIERS, Annotation[].class);
            Object event = exchange.getIn().getBody();
            if (!getEndpoint().getType().isAssignableFrom(event.getClass())) {
                throw new Exception(
                    "Exchange body of type '" + event.getClass().getName() + "'" +
                        " is not of destination endpoint event type: " + getEndpoint().getType()
                );
            }
            logger.debug("Firing CDI event of type '{}' with qualifiers: {} ", event.getClass().getName(), Arrays.toString(qualifiers));
            beanManager.fireEvent(event, qualifiers != null ? qualifiers : new Annotation[0]);
        }
    }

    final protected Class<T> type;
    final protected List<Consumer<T>> consumers = new ArrayList<>();

    public CdiEventEndpoint(Class<T> type, String endpointUri, Component component) {
        super(endpointUri, component);
        this.type = type;
    }

    public Class<?> getType() {
        return type;
    }

    @Override
    public Consumer<T> createConsumer(Processor processor) throws Exception {
        return new Consumer<T>(this, processor);
    }

    @Override
    public Producer<T> createProducer() throws Exception {
        return new Producer<T>(this);
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();
        getComponent().registerEndpoint(this);
    }

    @Override
    protected void doStop() throws Exception {
        getComponent().unregisterEndpoint(this);
        super.doStop();
    }

    @Override
    public CdiEventComponent getComponent() {
        return (CdiEventComponent) super.getComponent();
    }

    public void registerConsumer(Consumer<T> consumer) {
        synchronized (consumers) {
            consumers.add(consumer);
        }
    }

    public void unregisterConsumer(Consumer<T> consumer) {
        synchronized (consumers) {
            consumers.remove(consumer);
        }
    }

    public void notify(T t) throws Exception {
        synchronized (consumers) {
            for (Consumer<T> consumer : consumers) {
                consumer.notify(t);
            }
        }
    }
}