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

import javax.enterprise.inject.UnsatisfiedResolutionException;
import javax.enterprise.inject.Vetoed;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.CDI;
import javax.enterprise.inject.spi.CDIProvider;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.stream.StreamSupport;

import static java.util.Collections.singletonMap;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static org.apache.camel.cdi.AnyLiteral.ANY;
import static org.apache.camel.cdi.BeanManagerHelper.getReference;
import static org.apache.camel.cdi.BeanManagerHelper.getReferenceByType;

/**
 * Camel CDI boot integration. Allows Camel and CDI to be booted up on the command line as a JVM process.
 * See http://camel.apache.org/camel-boot.html.
 */
@Vetoed
public class Main extends MainSupport {

    private static Main instance;

    private CDI container;

    public static void main(String... args) throws Exception {
        Main main = new Main();
        instance = main;
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
        return getReferenceByType(container.getBeanManager(), CamelContext.class)
            .orElseThrow(
                () -> new UnsatisfiedResolutionException("No default Camel context is deployed, "
                    + "cannot create default ProducerTemplate!"))
            .createProducerTemplate();
    }

    @Override
    protected Map<String, CamelContext> getCamelContextMap() {
        BeanManager manager = container.getBeanManager();
        return manager.getBeans(CamelContext.class, ANY).stream()
            .map(bean -> getReference(manager, CamelContext.class, bean))
            .collect(toMap(CamelContext::getName, identity()));
    }

    @Override
    protected void doStart() throws Exception {
        container = StreamSupport.stream(
            ServiceLoader.load(CDIProvider.class, Main.class.getClassLoader()).spliterator(), false)
            .findFirst()
            // Since version 2.3.0.Final and WELD-1915, Weld SE registers a shutdown hook
            // that conflicts with Camel main support. See WELD-2051. The parameter below
            // is available starting Weld 2.3.1.Final to deactivate the registration of
            // the shutdown hook.
            .map(provider -> provider.initialize(singletonMap("org.jboss.weld.se.shutdownHook", false)))
            .orElseThrow(() -> new IllegalStateException("No CDIProvider found in the classpath!"));
        super.doStart();
        postProcessContext();
        warnIfNoCamelFound();
    }

    private void warnIfNoCamelFound() {
        BeanManager manager = container.getBeanManager();
        Set<Bean<?>> contexts = manager.getBeans(CamelContext.class, ANY);
        // Warn if there is no CDI Camel contexts
        if (contexts.isEmpty())
            LOG.warn("Camel CDI main has started with no Camel context!");
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();
        if (container != null)
            container.shutdown();
    }
}
