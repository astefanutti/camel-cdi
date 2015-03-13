package org.apache.camel.cdi;

import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.Set;
import javax.enterprise.inject.spi.AnnotatedConstructor;
import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.AnnotatedMember;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedType;

/**
 *
 */
public class ReannotedAnnotatedType<T> extends ReannotedAnnotated implements AnnotatedType<T> {

    private final AnnotatedMember<T> target;
    private final Set<Annotation> targetAnnotations;
    
    public ReannotedAnnotatedType(AnnotatedType<T> delegate, Set<Annotation> toChange) {
        super(delegate, toChange);
        target = null;
        targetAnnotations = null;
    }

    public ReannotedAnnotatedType(AnnotatedType<T> delegate, AnnotatedMember<T> target, Set<Annotation> toChange) {
        super(delegate);
        this.target = target;
        annotations = new HashSet<Annotation>(delegate.getAnnotations());
        targetAnnotations = new HashSet<Annotation>(toChange);
    }

    private AnnotatedType<T> getDelegate() {
        return (AnnotatedType<T>) delegate;
    }

    @Override
    public Set<AnnotatedConstructor<T>> getConstructors() {
        return getDelegate().getConstructors();
    }

    @Override
    public Set<AnnotatedField<? super T>> getFields() {
        if (getDelegate().getFields().contains(target)) {
            Set<AnnotatedField<? super T>> res = new HashSet<AnnotatedField<? super T>>(getDelegate().getFields());
            res.remove(target);
            res.add(new ReannotedAnnotatedField<T>((AnnotatedField<T>) target, targetAnnotations));
            return res;
        }
        return getDelegate().getFields();
    }

    @Override
    public Class<T> getJavaClass() {
        return getDelegate().getJavaClass();
    }

    @Override
    public Set<AnnotatedMethod<? super T>> getMethods() {
        if (getDelegate().getMethods().contains(target)) {
            Set<AnnotatedMethod<? super T>> res = new HashSet<AnnotatedMethod<? super T>>(getDelegate().getMethods());
            res.remove(target);
            res.add(new ReannotedAnnotatedMethod<T>((AnnotatedMethod<T>) target, targetAnnotations));
            return res;
        }
        return getDelegate().getMethods();
    }
    
}
