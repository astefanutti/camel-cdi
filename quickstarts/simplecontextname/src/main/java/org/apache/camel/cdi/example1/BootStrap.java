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
package org.apache.camel.cdi.example1;


import org.apache.camel.CamelContext;
import org.apache.camel.cdi.ContextName;
import org.jboss.weld.environment.se.StartMain;
import org.jboss.weld.environment.se.WeldContainer;
import org.jboss.weld.environment.se.bindings.Parameters;
import org.jboss.weld.environment.se.events.ContainerInitialized;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import javax.enterprise.event.Observes;

public class BootStrap {

    void start(@Observes ContainerInitialized event, @Parameters List<String> parameters) {
        System.out.println("Camel CDI :: Example 1 has started");
        // The context is started in the @PostConstruct lifecycle callback (see class SimpleCamelContext)
    }
    

    public static void main(String[] args) throws Exception {
        WeldContainer container = new StartMain(args).go();
        // Get a reference to the Camel context named "simple"
        CamelContext context = container.instance().select(CamelContext.class, new ContextName.Literal("simple")).get();
        System.out.println("Camel CDI :: " + context + " started!");
        // And wait until the JVM exits
        new CountDownLatch(1).await();
    }
}
