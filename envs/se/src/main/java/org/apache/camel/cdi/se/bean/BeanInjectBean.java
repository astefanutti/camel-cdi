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
package org.apache.camel.cdi.se.bean;

import org.apache.camel.BeanInject;

public class BeanInjectBean {

    @BeanInject
    private PropertyInjectBean injectBeanField;
    
    private PropertyInjectBean injectBeanMethod;

    @BeanInject("beanName")
    private NamedCamelBean injectBeanNamed;

    public PropertyInjectBean getInjectBeanField() {
        return injectBeanField;
    }

    @BeanInject
    public void setInjectBeanMethod(PropertyInjectBean bean) {
        injectBeanMethod = bean;
    }

    public PropertyInjectBean getInjectBeanMethod() {
        return injectBeanMethod;
    }

    public NamedCamelBean getInjectBeanNamed() {
        return injectBeanNamed;
    }
}
