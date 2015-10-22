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
package org.apache.camel.cdi.example2;


import org.apache.camel.CamelContext;
import org.apache.deltaspike.cdise.api.CdiContainer;

import javax.enterprise.inject.spi.BeanManager;

public class BootStrap {

    public static void main(String[] args) throws Exception {
        final CdiContainer container = org.apache.deltaspike.cdise.api.CdiContainerLoader.getCdiContainer();
        container.boot();

        BeanManager manager = container.getBeanManager();
        final CamelContext context = (CamelContext) manager.getReference(manager.resolve(manager.getBeans(CamelContext.class)), CamelContext.class, manager.createCreationalContext(null));

        // FIXME: since version 2.3.0.Final and WELD-1915, Weld always register a shutdown hook that conflicts with Camel main support. See WELD-2051.
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                try {
                    context.stop();
                    container.shutdown();
                } catch (Exception cause) {
                    cause.printStackTrace();
                }
            }
        });

        context.start();
    }
}
