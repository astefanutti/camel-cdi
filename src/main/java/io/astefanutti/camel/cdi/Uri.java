/**
 * Copyright (C) 2014 Antonin Stefanutti (antonin.stefanutti@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.astefanutti.camel.cdi;

import javax.enterprise.util.Nonbinding;
import javax.inject.Qualifier;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An injection annotation to define the <a href="http://camel.apache.org/uris.html">Camel URI</a> used to reference the
 * underlying <a href="http://camel.apache.org/endpoint.html">Camel Endpoint</a>. This annotation can be used to
 * annotate an @Inject injection point for values of type {@link org.apache.camel.Endpoint} or {@link
 * org.apache.camel.ProducerTemplate} with a String URI. For example: <code>public class Foo { @Inject @Uri("mock:foo")
 * Endpoint endpoint;
 *
 * @Inject @Uri("seda:bar") ProducerTemplate producer; }</code>
 */
@Qualifier
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER})
public @interface Uri {

    /**
     * Returns the <a href="http://camel.apache.org/uris.html">Camel URI</a> of the endpoint
     */
    @Nonbinding String value();
}
