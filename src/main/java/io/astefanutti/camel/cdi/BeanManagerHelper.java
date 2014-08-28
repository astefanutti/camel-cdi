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

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.Vetoed;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Vetoed
final class BeanManagerHelper {

    @SuppressWarnings("unchecked")
    static <T> Set<T> getContextualReferences(BeanManager beanManager, Class<T> type, Annotation... qualifiers) {
        Set<T> references = new HashSet<>();
        for (Bean<?> bean : beanManager.getBeans(type, qualifiers))
            references.add((T) beanManager.getReference(bean, type, beanManager.createCreationalContext(bean)));

        return references;
    }

    static <T> T getContextualReference(BeanManager manager, String name, boolean optional, Class<T> type) {
        Set<Bean<?>> beans = manager.getBeans(name);

        if ((beans == null) || beans.isEmpty()) {
            if (optional)
                return null;

            throw new IllegalStateException("Could not find beans for type [" + type + "] and name [" + name + "]");
        }

        return getContextualReference(manager, type, beans);
    }

    static <T> T getContextualReference(BeanManager manager, Class<T> type, boolean optional, Annotation... qualifiers) {
        Set<Bean<?>> beans = manager.getBeans(type, qualifiers);

        if ((beans == null) || beans.isEmpty()) {
            if (optional)
                return null;

            throw new IllegalStateException("Could not find beans for type [" + type + "] and qualifiers " + Arrays.toString(qualifiers));
        }

        return getContextualReference(manager, type, beans);
    }

    static <T> T getContextualReference(BeanManager manager, Class<T> type, Bean<?> bean) {
        return getContextualReference(manager, type, new HashSet<>(Arrays.<Bean<?>>asList(bean)));
    }

    @SuppressWarnings("unchecked")
    private static <T> T getContextualReference(BeanManager manager, Class<T> type, Set<Bean<?>> beans) {
        Bean<?> bean = manager.resolve(beans);
        CreationalContext<?> context = manager.createCreationalContext(bean);
        return (T) manager.getReference(bean, type, context);
    }
}
