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
package org.apache.camel.cdi.se.bean;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.cdi.CdiEventEndpoint;
import org.apache.camel.cdi.se.pojo.EventPayload;
import org.apache.camel.cdi.se.qualifier.BarQualifier;
import org.apache.camel.cdi.se.qualifier.FooQualifier;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Default;
import javax.inject.Inject;

@ApplicationScoped
public class EventConsumingRoute extends RouteBuilder {

    @Inject
    private CdiEventEndpoint<Object> objectCdiEventEndpoint;

    @Inject
    private CdiEventEndpoint<String> stringCdiEventEndpoint;

    @Inject
    private CdiEventEndpoint<EventPayload<String>> stringPayloadCdiEventEndpoint;

    @Inject
    private CdiEventEndpoint<EventPayload<Integer>> integerPayloadCdiEventEndpoint;

    @Inject
    private CdiEventEndpoint<String[]> stringArrayCdiEventEndpoint;

    @Inject
    @FooQualifier
    private CdiEventEndpoint<Long> fooQualifierCdiEventEndpoint;

    @Inject
    @BarQualifier
    private CdiEventEndpoint<Long> barQualifierCdiEventEndpoint;

    @Override
    public void configure() {
        from(objectCdiEventEndpoint).to("mock:consumeObject");

        from(stringCdiEventEndpoint).to("mock:consumeString");

        from(stringPayloadCdiEventEndpoint).to("mock:consumeStringPayload");

        from(integerPayloadCdiEventEndpoint).to("mock:consumeIntegerPayload");

        from(stringArrayCdiEventEndpoint).to("mock:consumeStringArray");

        from(fooQualifierCdiEventEndpoint).to("mock:consumeFooQualifier");

        from(barQualifierCdiEventEndpoint).to("mock:consumeBarQualifier");
    }
}
