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

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.RuntimeExchangeException;
import org.apache.camel.impl.DefaultConsumer;

/* package-private */ final class CdiEventConsumer<T> extends DefaultConsumer {

    private final CdiEventEndpoint<T> endpoint;

    CdiEventConsumer(CdiEventEndpoint<T> endpoint, Processor processor) {
        super(endpoint, processor);
        this.endpoint = endpoint;
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();
        endpoint.registerConsumer(this);
    }

    @Override
    protected void doStop() throws Exception {
        endpoint.unregisterConsumer(this);
        super.doStop();
    }

    void notify(T event) {
        // TODO: add debug logging
        Exchange exchange = getEndpoint().createExchange();
        exchange.getIn().setBody(event);
        try {
            getProcessor().process(exchange);
        } catch (Exception cause) {
            throw new RuntimeExchangeException("Error while processing CDI event", exchange, cause);
        }
    }
}
