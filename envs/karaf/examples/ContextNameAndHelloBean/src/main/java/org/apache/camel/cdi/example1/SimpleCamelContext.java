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
package org.apache.camel.cdi.example1;

import org.apache.camel.cdi.CdiCamelContext;
import org.apache.camel.cdi.ContextName;
import org.apache.camel.component.properties.PropertiesComponent;
import org.apache.camel.util.ObjectHelper;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
@ContextName("simple")
public class SimpleCamelContext extends CdiCamelContext {

    @PostConstruct
    void postConstruct() {
        try {
            // Enable Tracing
            super.setTracing(false);

            // Define PropertyPlaceHolder
            getComponent("properties", PropertiesComponent.class).setLocation("classpath:placeholder.properties");

            // Start Context
            super.start();

        } catch (Exception cause) {
            throw ObjectHelper.wrapRuntimeCamelException(cause);
        }
    }

    @PreDestroy
    void preDestroy() {
        try {
            super.stop();
        } catch (Exception cause) {
            throw ObjectHelper.wrapRuntimeCamelException(cause);
        }
    }
}
