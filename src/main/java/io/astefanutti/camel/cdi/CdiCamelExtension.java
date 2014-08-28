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

import org.apache.camel.CamelContext;
import org.apache.camel.RoutesBuilder;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterDeploymentValidation;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Extension;

public class CdiCamelExtension implements Extension {

    void configureCamelContext(@Observes AfterDeploymentValidation event, BeanManager beanManager) {
        CamelContext context = BeanManagerUtil.getContextualReference(beanManager, CamelContext.class, false);

        for (RoutesBuilder builder : BeanManagerUtil.getContextualReferences(beanManager, RoutesBuilder.class)) {
            try {
                context.addRoutes(builder);
            } catch (Exception exception) {
                event.addDeploymentProblem(exception);
            }
        }
    }
}
