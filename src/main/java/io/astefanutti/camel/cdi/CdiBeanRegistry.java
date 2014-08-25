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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.enterprise.inject.Vetoed;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;

import org.apache.camel.spi.Registry;
import org.apache.camel.util.ObjectHelper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Vetoed
class CdiBeanRegistry implements Registry {

    private final BeanManager beanManager;

    CdiBeanRegistry(BeanManager beanManager) {
        this.beanManager = beanManager;
    }

    @Override
    public Object lookupByName(String name) {
        ObjectHelper.notEmpty(name, "name");
        return BeanManagerUtil.getContextualReference(beanManager, name, true, Object.class);
    }

    @Override
    public <T> T lookupByNameAndType(String name, Class<T> type) {
        ObjectHelper.notEmpty(name, "name");
        ObjectHelper.notNull(type, "type");
        return BeanManagerUtil.getContextualReference(beanManager, name, true, type);
    }

    @Override
    public <T> Map<String, T> findByTypeWithName(Class<T> type) {
        ObjectHelper.notNull(type, "type");
        Map<String, T> references = new HashMap<>();
        Set<Bean<?>> beans = beanManager.getBeans(type, AnyLiteral.INSTANCE);

        if (beans == null)
            return references;

        for (Bean<?> bean : beans)
            if (bean.getName() != null)
                references.put(bean.getName(), BeanManagerUtil.getContextualReference(beanManager, type, bean));

        return references;
    }

    @Override
    public <T> Set<T> findByType(Class<T> type) {
        ObjectHelper.notNull(type, "type");
        return BeanManagerUtil.getContextualReferences(beanManager, type, AnyLiteral.INSTANCE);
    }

    @Override
    public Object lookup(String name) {
        return lookupByName(name);
    }

    @Override
    public <T> T lookup(String name, Class<T> type) {
        return lookupByNameAndType(name, type);
    }

    @Override
    public <T> Map<String, T> lookupByType(Class<T> type) {
        return findByTypeWithName(type);
    }

    @Override
    public String toString() {
        return "CDI bean registry";
    }
}
