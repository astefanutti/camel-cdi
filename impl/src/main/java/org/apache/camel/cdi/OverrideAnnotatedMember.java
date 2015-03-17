package org.apache.camel.cdi;

import java.lang.annotation.Annotation;
import java.lang.reflect.Member;
import java.util.Set;
import javax.enterprise.inject.spi.AnnotatedMember;
import javax.enterprise.inject.spi.AnnotatedType;

class OverrideAnnotatedMember<T> extends OverrideAnnotated implements AnnotatedMember<T> {

    private final AnnotatedMember<T> delegate;
    
    OverrideAnnotatedMember(AnnotatedMember<T> delegate, Set<Annotation> annotations) {
        super(delegate, annotations);
        this.delegate = delegate;
    }

    @Override
    public AnnotatedType<T> getDeclaringType() {
        return delegate.getDeclaringType();
    }

    @Override
    public Member getJavaMember() {
        return delegate.getJavaMember();
    }

    @Override
    public boolean isStatic() {
        return delegate.isStatic();
    }
}
