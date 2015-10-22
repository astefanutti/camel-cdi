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

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.main.MainSupport;
import org.apache.deltaspike.core.api.provider.BeanProvider;

import javax.enterprise.inject.Vetoed;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Camel CDI boot integration. Allows Camel and CDI to be booted up on the command line as a JVM process.
 * See http://camel.apache.org/camel-boot.html.
 */
@Vetoed
public class Main extends MainSupport {

    private static Main instance;

    private Object cdiContainer; // we don't want to use cdictrl API in OSGi

    public static void main(String... args) throws Exception {
        Main main = new Main();
        instance = main;
        main.enableHangupSupport();
        main.run(args);
    }

    /**
     * Returns the currently executing instance.
     *
     * @return the current running instance
     */
    public static Main getInstance() {
        return instance;
    }

    @Override
    protected ProducerTemplate findOrCreateCamelTemplate() {
        BeanManager manager = ((org.apache.deltaspike.cdise.api.CdiContainer) cdiContainer).getBeanManager();
        Bean<?> bean = manager.resolve(manager.getBeans(CamelContext.class));
        if (bean == null)
            throw new IllegalStateException("No default Camel context is deployed so cannot create a ProducerTemplate!");

        CamelContext context = (CamelContext) manager.getReference(bean, CamelContext.class, manager.createCreationalContext(bean));
        return context.createProducerTemplate();
    }

    @Override
    protected Map<String, CamelContext> getCamelContextMap() {
        List<CamelContext> contexts = BeanProvider.getContextualReferences(CamelContext.class, true);
        Map<String, CamelContext> answer = new HashMap<>();
        for (CamelContext context : contexts) {
            String name = context.getName();
            answer.put(name, context);
        }
        return answer;
    }

    @Override
    protected void doStart() throws Exception {
        // TODO: Use standard CDI Java SE support when CDI 2.0 becomes a prerequisite
        org.apache.deltaspike.cdise.api.CdiContainer container = org.apache.deltaspike.cdise.api.CdiContainerLoader.getCdiContainer();
        container.boot();
        container.getContextControl().startContexts();
        cdiContainer = container;
        super.doStart();
        postProcessContext();
        for (CamelContext context : getCamelContexts())
            context.start();
    }

    @Override
    protected void doStop() throws Exception {
        // FIXME: since version 2.3.0.Final and WELD-1915, Weld always register a shutdown hook that conflicts with Camel main support. See WELD-2051.
        for (CamelContext context : getCamelContexts())
            context.stop();
        super.doStop();
        if (cdiContainer != null)
            ((org.apache.deltaspike.cdise.api.CdiContainer) cdiContainer).shutdown();
    }
}
