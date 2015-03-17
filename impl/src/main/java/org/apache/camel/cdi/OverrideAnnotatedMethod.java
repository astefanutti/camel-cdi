package org.apache.camel.cdi;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedParameter;

final class OverrideAnnotatedMethod<T> extends OverrideAnnotatedMember<T> implements AnnotatedMethod<T> {
    
    private final AnnotatedMethod<T> delegate;
    
    OverrideAnnotatedMethod(AnnotatedMethod<T> delegate, Set<Annotation> annotations) {
        super(delegate, annotations);
        this.delegate = delegate;
    }

    @Override
    public List<AnnotatedParameter<T>> getParameters() {
        return delegate.getParameters();
    }

    @Override
    public Method getJavaMember() {
        return delegate.getJavaMember();
    }
}
