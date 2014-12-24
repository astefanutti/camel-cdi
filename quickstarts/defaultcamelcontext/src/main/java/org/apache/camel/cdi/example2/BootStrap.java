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
import org.jboss.weld.environment.se.StartMain;
import org.jboss.weld.environment.se.WeldContainer;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Destroyed;
import javax.enterprise.context.Initialized;
import javax.enterprise.event.Observes;
import java.util.concurrent.CountDownLatch;

public class BootStrap {

    void start(@Observes @Initialized(ApplicationScoped.class) Object event, CamelContext context) throws Exception {
        System.out.println("Camel CDI :: Example 2 will be started");
        context.start();
    }

    void shutdown(@Observes @Destroyed(ApplicationScoped.class) Object event, CamelContext context) throws Exception {
        System.out.println("Camel CDI :: Example 2 will be stopped");
        context.stop();
    }

    public static void main(String[] args) throws Exception {
        WeldContainer container = new StartMain(args).go();
        // Fet a reference to the default Camel context
        CamelContext context = container.instance().select(CamelContext.class).get();
        System.out.println("Camel CDI ::" + context + " started!");
        // And wait until the JVM exits
        new CountDownLatch(1).await();
    }
}
