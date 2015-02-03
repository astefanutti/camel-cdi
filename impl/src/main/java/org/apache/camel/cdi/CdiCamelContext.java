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

import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.DefaultCamelContextNameStrategy;

import javax.annotation.PostConstruct;
import javax.enterprise.inject.Vetoed;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;

@Vetoed
public class CdiCamelContext extends DefaultCamelContext {

    @Inject
    private BeanManager manager;

    @Inject
    private CdiCamelExtension extension;

    @PostConstruct
    void postConstruct() {
        AnnotatedType<?> type = manager.createAnnotatedType(getClass());
        ContextName name = type.getAnnotation(ContextName.class);
        // Do not override the name if it's been already set (in the bean constructor for example)
        if (getNameStrategy() instanceof DefaultCamelContextNameStrategy)
            setName(name != null ? name.value() : "camel-cdi");

        // Add bean registry and Camel injector
        setRegistry(new CdiCamelRegistry(manager));
        setInjector(new CdiCamelInjector(getInjector(), manager));

        // Add event notifier if at least one observer is present
        if (extension.getContextInfo(name).contains(ContextInfo.EventNotifierSupport))
            getManagementStrategy().addEventNotifier(new CdiEventNotifier(manager, name));
    }
}
