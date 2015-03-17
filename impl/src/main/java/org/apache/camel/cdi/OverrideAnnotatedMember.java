package org.apache.camel.cdi;

import java.lang.annotation.Annotation;
import java.lang.reflect.Member;
import java.util.Set;
import javax.enterprise.inject.spi.AnnotatedMember;
import javax.enterprise.inject.spi.AnnotatedType;

/**
 *
 */
public class OverrideAnnotatedMember<T> extends OverrideAnnotated implements AnnotatedMember<T> {
    


    public OverrideAnnotatedMember(AnnotatedMember<T> delegate, Set<Annotation> enrichment) {
        super(delegate,enrichment);
    }

    private AnnotatedMember<T> getDelegate() {
        return (AnnotatedMember<T>) delegate;
    }
        

    @Override
    public AnnotatedType<T> getDeclaringType() {
        return getDelegate().getDeclaringType();
    }

    @Override
    public Member getJavaMember() {
        return getDelegate().getJavaMember();
    }

    @Override
    public boolean isStatic() {
        return getDelegate().isStatic();
    }
}
