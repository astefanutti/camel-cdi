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

import org.apache.camel.core.osgi.OsgiCamelContextHelper;
import org.apache.camel.core.osgi.OsgiCamelContextPublisher;
import org.apache.camel.core.osgi.utils.BundleContextUtils;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.DefaultCamelContextNameStrategy;
import org.apache.camel.spi.CamelContextNameStrategy;
import org.osgi.framework.BundleContext;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.BeanManager;

final class OsgiCdiCamelContextBean extends CdiCamelContextBean {

    OsgiCdiCamelContextBean(BeanManager manager) {
        super(manager);
    }

    @Override
    public DefaultCamelContext create(CreationalContext<DefaultCamelContext> creational) {
        DefaultCamelContext context = super.create(creational);

        BundleContext bundle = BundleContextUtils.getBundleContext(getClass());
        context.getManagementStrategy().addEventNotifier(new OsgiCamelContextPublisher(bundle));
        context.setRegistry(OsgiCamelContextHelper.wrapRegistry(context, context.getRegistry(), bundle));
        CamelContextNameStrategy strategy = context.getNameStrategy();
        OsgiCamelContextHelper.osgiUpdate(context, bundle);
        // FIXME: the above call should not override explicit strategies provided by the end user or should decorate them instead of overriding them completely
        if (!(strategy instanceof DefaultCamelContextNameStrategy))
            context.setNameStrategy(strategy);

        return context;
    }
}
