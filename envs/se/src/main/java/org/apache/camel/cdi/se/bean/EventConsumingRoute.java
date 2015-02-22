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

import org.apache.camel.Endpoint;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.cdi.CdiEvent;
import org.apache.camel.cdi.Uri;
import org.apache.camel.component.mock.MockEndpoint;

import javax.inject.Inject;

public class EventConsumingRoute extends RouteBuilder {

    @Inject
    @Uri(CdiEvent.CDI_EVENT_URI)
    private Endpoint allCdiEventsEndpoint;

    @Inject
    @Uri("mock:resultAll")
    private MockEndpoint resultAllEndpoint;

    @Inject
    @Uri("mock:resultBean")
    private MockEndpoint resultBeanEndpoint;

    @Inject
    @Uri("mock:resultString")
    private MockEndpoint resultStringEndpoint;

    @Override
    public void configure() {
        from(allCdiEventsEndpoint).to(resultAllEndpoint);

        from(CdiEvent.endpoint(SampleBean.class))
            .to(resultBeanEndpoint);

        from(CdiEvent.endpoint(String.class))
            .to(resultStringEndpoint);

    }
}
