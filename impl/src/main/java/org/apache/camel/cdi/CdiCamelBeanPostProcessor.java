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

import org.apache.camel.BeanInject;
import org.apache.camel.CamelContext;
import org.apache.camel.EndpointInject;
import org.apache.camel.Produce;
import org.apache.camel.PropertyInject;
import org.apache.camel.impl.CamelPostProcessorHelper;
import org.apache.camel.impl.DefaultCamelBeanPostProcessor;
import org.apache.camel.util.ReflectionHelper;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import javax.enterprise.inject.spi.BeanManager;

@ToVeto
final class CdiCamelBeanPostProcessor extends DefaultCamelBeanPostProcessor {

    private final BeanManager manager;

    private final Map<String, CamelPostProcessorHelper> postProcessorHelpers = new HashMap<String, CamelPostProcessorHelper>();

    CdiCamelBeanPostProcessor(BeanManager manager) {
        this.manager = manager;
    }

    protected void injectFields(final Object bean, final String beanName) {
        ReflectionHelper.doWithFields(bean.getClass(), new ReflectionHelper.FieldCallback() {
            public void doWith(Field field) throws IllegalAccessException {
                PropertyInject propertyInject = field.getAnnotation(PropertyInject.class);
                if (propertyInject != null)
                    injectFieldProperty(field, propertyInject.value(), propertyInject.defaultValue(), propertyInject.context(), bean, beanName);

                BeanInject beanInject = field.getAnnotation(BeanInject.class);
                if (beanInject != null && getPostProcessorHelper().matchContext(beanInject.context()))
                    injectFieldBean(field, beanInject.value(), bean, beanName);

                EndpointInject endpointInject = field.getAnnotation(EndpointInject.class);
                if (endpointInject != null)
                    injectField(field, endpointInject.uri(), endpointInject.ref(), endpointInject.property(), endpointInject.context(), bean, beanName);

                Produce produce = field.getAnnotation(Produce.class);
                if (produce != null)
                    injectField(field, produce.uri(), produce.ref(), produce.property(), produce.context(), bean, beanName);
            }
        });
    }

    public void injectField(Field field, String uri, String ref, String property, String context, Object bean, String beanName) {
        ReflectionHelper.setField(field, bean, getPostProcessorHelper(context).getInjectionValue(field.getType(), uri, ref, property, field.getName(), bean, beanName));
    }

    public void injectFieldProperty(Field field, String property, String defaultValue, String context, Object bean, String beanName) {
        ReflectionHelper.setField(field, bean, getPostProcessorHelper(context).getInjectionPropertyValue(field.getType(), property, defaultValue, field.getName(), bean, beanName));
    }

    public CamelPostProcessorHelper getPostProcessorHelper(String context) {
        CamelPostProcessorHelper helper = postProcessorHelpers.get(context);
        if (helper == null) {
            helper = new CamelPostProcessorHelper(getOrLookupCamelContext(context));
            postProcessorHelpers.put(context, helper);
        }
        return helper;
    }

    public CamelContext getOrLookupCamelContext(String context) {
        return BeanManagerHelper.getReferenceByType(manager, CamelContext.class, context.isEmpty() ? DefaultLiteral.INSTANCE : new ContextName.Literal(context));
    }

    public CamelContext getOrLookupCamelContext() {
        return BeanManagerHelper.getReferenceByType(manager, CamelContext.class);
    }
}
