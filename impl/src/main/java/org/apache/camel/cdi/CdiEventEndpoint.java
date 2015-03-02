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
import org.apache.camel.Consumer;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.impl.DefaultEndpoint;

import java.util.ArrayList;
import java.util.List;

/* package-private */ final class CdiEventEndpoint<T> extends DefaultEndpoint {
    
    private final Class<T> type;
    
    private final List<CdiEventConsumer<T>> consumers = new ArrayList<>();

    CdiEventEndpoint(Class<T> type, String endpointUri, Component component) {
        super(endpointUri, component);
        this.type = type;
    }

    Class<?> getType() {
        return type;
    }

    @Override
    public Consumer createConsumer(Processor processor) {
        return new CdiEventConsumer<>(this, processor);
    }

    @Override
    public Producer createProducer() {
        return new CdiEventProducer<>(this);
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

    void registerConsumer(CdiEventConsumer<T> consumer) {
        synchronized (consumers) {
            consumers.add(consumer);
        }
    }

     void unregisterConsumer(CdiEventConsumer<T> consumer) {
        synchronized (consumers) {
            consumers.remove(consumer);
        }
    }

    void notify(T t) {
        synchronized (consumers) {
            for (CdiEventConsumer<T> consumer : consumers) {
                consumer.notify(t);
            }
        }
    }
}