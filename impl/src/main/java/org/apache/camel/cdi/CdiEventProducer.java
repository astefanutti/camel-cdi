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

import org.apache.camel.CamelExchangeException;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.inject.spi.BeanManager;
import java.lang.annotation.Annotation;
import java.util.Arrays;

/* package-private */ final class CdiEventProducer<T> extends DefaultProducer {

    private final Logger logger = LoggerFactory.getLogger(CdiEventComponent.class);
    
    private final CdiEventEndpoint<T> endpoint;
    
     CdiEventProducer(CdiEventEndpoint<T> endpoint) {
         super(endpoint);
         this.endpoint = endpoint;
    }

    @Override
    public void process(Exchange exchange) throws CamelExchangeException {
        Object event = exchange.getIn().getBody();
        if (!endpoint.getType().isAssignableFrom(event.getClass()))
            throw new CamelExchangeException("Exchange body of type '" + event.getClass().getName() + "' is not of destination endpoint event type: " + endpoint.getType(), exchange);

        Annotation[] qualifiers = exchange.getIn().getHeader(CdiEvent.CDI_EVENT_QUALIFIERS, Annotation[].class);
        logger.debug("Firing CDI event of type '{}' with qualifiers: {}", event.getClass().getName(), Arrays.toString(qualifiers));
        BeanManager beanManager = endpoint.getComponent().getBeanManager();
        beanManager.fireEvent(event, qualifiers != null ? qualifiers : new Annotation[0]);
    }
}
