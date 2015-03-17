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

import org.apache.camel.util.ObjectHelper;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import javax.enterprise.context.Dependent;
import javax.enterprise.context.NormalScope;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.AnnotatedConstructor;
import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.AnnotatedMember;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Qualifier;
import javax.inject.Scope;

@ToVeto
final class CdiSpiHelper {

    static <T extends Annotation> T getQualifierByType(Bean<?> bean, Class<T> type) {
        return getFirstElementOfType(bean.getQualifiers(), type);
    }

    static <T extends Annotation> T getQualifierByType(InjectionPoint ip, Class<T> type) {
        return getFirstElementOfType(ip.getQualifiers(), type);
    }

    private static <E, T extends E> T getFirstElementOfType(Collection<E> collection, Class<T> type) {
        for (E item : collection)
            if ((item != null) && type.isAssignableFrom(item.getClass()))
                return ObjectHelper.cast(type, item);

        return null;
    }

    static Class<?> getRawType(Type type) {
        if (type instanceof Class<?>) {
            return Class.class.cast(type);
        }
        else if (type instanceof ParameterizedType) {
            return getRawType(ParameterizedType.class.cast(type).getRawType());
        }
        else if (type instanceof TypeVariable<?>) {
            return getBound(TypeVariable.class.cast(type).getBounds());
        }
        else if (type instanceof WildcardType) {
            return getBound(WildcardType.class.cast(type).getUpperBounds());
        }
        else if (type instanceof GenericArrayType) {
            Class<?> rawType = getRawType(GenericArrayType.class.cast(type).getGenericComponentType());
            if (rawType != null)
                return Array.newInstance(rawType, 0).getClass();
        }
        return null;
    }

    private static Class<?> getBound(Type[] bounds) {
        if (bounds.length == 0)
            return Object.class;
        else
            return getRawType(bounds[0]);
    }

    public static boolean hasAnnotation(AnnotatedType<?> at, Class<? extends Annotation> annotation) {

        if (at.isAnnotationPresent(annotation))
            return true;

        for (AnnotatedMethod<?> method : at.getMethods()) {
            if (method.isAnnotationPresent(annotation))
                return true;
        }
        for (AnnotatedConstructor<?> constructor : at.getConstructors()) {
            if (constructor.isAnnotationPresent(annotation))
                return true;
        }
        for (AnnotatedField<?> field : at.getFields()) {
            if (field.isAnnotationPresent(annotation))
                return true;
        }
        return false;

    }

    public static <T> Set<AnnotatedMember<? super T>> getProducerMemberForType(AnnotatedType<T> at, Class<?> lookedType) {
        Set<AnnotatedMember<? super T>> res = new HashSet<AnnotatedMember<? super T>>();
        for (AnnotatedMethod<? super T> method : at.getMethods()) {
            if (method.isAnnotationPresent(Produces.class) && method.getTypeClosure().contains(lookedType))
                res.add(method);
        }

        for (AnnotatedField<? super T> field : at.getFields()) {
            if (field.isAnnotationPresent(Produces.class) && field.getTypeClosure().contains(lookedType))
                res.add(field);
        }
        return res;
    }

    public static Set<Annotation> getAnnotationsWithMeta(Annotated element, final Class<? extends Annotation>
            metaAnnotationType) {
        return getAnnotationsWithMeta(element.getAnnotations(), metaAnnotationType);
    }

    public static Set<Annotation> getAnnotationsWithMeta(Set<Annotation> qualifiers, final Class<? extends Annotation>
            metaAnnotationType) {
        Set<Annotation> annotations = new HashSet<Annotation>();
        for (Annotation annotation : qualifiers) {
            if (annotation.annotationType().isAnnotationPresent(metaAnnotationType)) {
                annotations.add(annotation);
            }
        }
        return annotations;
    }

    public static Class<? extends Annotation> getScopeClass(Annotated element) {
        Set<Annotation> res = getAnnotationsWithMeta(element, NormalScope.class);
        if (!res.isEmpty())
            return getFirstElementOfType(res, Annotation.class).getClass();
        res = getAnnotationsWithMeta(element, Scope.class);
        if (!res.isEmpty())
            return getFirstElementOfType(res, Annotation.class).getClass();
        return Dependent.class;
    }
    
    public static Set<Annotation> removeQualifiersFromAnnotated(Annotated at) {
        Set<Annotation> res = new HashSet<Annotation>(at.getAnnotations());
        res.removeAll(CdiSpiHelper.getAnnotationsWithMeta(res, Qualifier.class));
        return res;
    }
}
