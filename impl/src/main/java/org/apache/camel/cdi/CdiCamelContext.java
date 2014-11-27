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
package org.apache.camel.cdi;

import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.util.ObjectHelper;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.inject.Vetoed;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;

@Vetoed
public class CdiCamelContext extends DefaultCamelContext {

    @Inject
    private BeanManager manager;

    @PostConstruct
    @Override
    public void start() {
        try {
            // Add BeanRegistry & Camel Injector
            setRegistry(new CdiBeanRegistry(manager));
            setInjector(new CdiInjector(getInjector(), manager));
            // Explicitly load the properties component as NPE can be thrown when the Camel context is interacted with but not started yet
            lookupPropertiesComponent();

            // Start CamelContext
            super.start();
        } catch (Exception e) {
            throw ObjectHelper.wrapRuntimeCamelException(e);
        }
    }

    @PreDestroy
    @Override
    public void stop() {
        try {
            super.stop();
        } catch (Exception e) {
            throw ObjectHelper.wrapRuntimeCamelException(e);
        }
    }
}
