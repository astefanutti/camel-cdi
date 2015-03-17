package org.apache.camel.cdi;

import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.Set;
import javax.enterprise.inject.spi.AnnotatedConstructor;
import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedType;

final class OverrideAnnotatedType<T> extends OverrideAnnotated implements AnnotatedType<T> {

    private final AnnotatedMethod<? super T> target;

    private final Set<Annotation> annotations;

    private final AnnotatedType<T> delegate;

    OverrideAnnotatedType(AnnotatedType<T> delegate, AnnotatedMethod<? super T> target, Set<Annotation> annotations) {
        super(delegate);
        this.delegate = delegate;
        this.target = target;
        this.annotations = new HashSet<Annotation>(annotations);
    }

    @Override
    public Set<AnnotatedConstructor<T>> getConstructors() {
        return delegate.getConstructors();
    }

    @Override
    public Set<AnnotatedField<? super T>> getFields() {
        return delegate.getFields();
    }

    @Override
    public Class<T> getJavaClass() {
        return delegate.getJavaClass();
    }

    @Override
    public Set<AnnotatedMethod<? super T>> getMethods() {
        Set<AnnotatedMethod<? super T>> methods = new HashSet<AnnotatedMethod<? super T>>(delegate.getMethods());
        methods.remove(target);
        methods.add(new OverrideAnnotatedMethod(target, annotations));
        return methods;
    }
}
