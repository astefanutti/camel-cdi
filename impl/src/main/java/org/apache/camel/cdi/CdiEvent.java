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

/**
 * <p>
 * Consume all CDI events with this endpoint and route them to Camel dynamically,
 * based on event (payload) type. Additional qualifiers are currently not supported,
 * only the desired event (payload) type can be constrained.
 * </p>
 * <p>
 * Produce and fire CDI events in Camel exchanges with this destination endpoint.
 * The exchange input message body is the event type and payload. You will get an
 * exception if the declared type of the producing endpoint doesn't match the
 * message payload. An <code>Annotation[]</code> of additional qualifiers
 * can be added as message header {@link #CDI_EVENT_QUALIFIERS}.
 * </p>
 * <p>
 * Usage: <code>cdi-event://some.event.CustomType</code> or all with <code>cdi-event:///</code>
 * </p>
 * <p>
 * Alternatively, call {@link CdiEvent#endpoint(Class)} to generate the string.
 * </p>
 * <p>
 *     TODO: Support <code>@Inject @Uri(CDI_EVENT_URI) @SomeQualifier Endpoint|ProducerTemplate</code>
 * </p>
 */
public final class CdiEvent {

    public static final String CDI_EVENT = "cdi-event";
    public static final String CDI_EVENT_URI = CDI_EVENT + ":///";
    public static final String CDI_EVENT_QUALIFIERS = "CamelCdiQualifiers";

    public static String endpoint() {
        return endpoint(null);
    }

    public static String endpoint(Class<?> clazz) {
        return CDI_EVENT + "://" + (clazz != null ? clazz.getName() : "") + "/";
    }
}
