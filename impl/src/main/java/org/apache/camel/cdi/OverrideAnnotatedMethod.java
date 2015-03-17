package org.apache.camel.cdi;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedParameter;

/**
 *
 */
public class OverrideAnnotatedMethod<T> extends OverrideAnnotatedMember<T> implements
        AnnotatedMethod<T> {


    public OverrideAnnotatedMethod(AnnotatedMethod<T> delegate, Set<Annotation> enrichment) {
        super(delegate, enrichment);
    }

    private AnnotatedMethod<T> getDelegate() {
        return (AnnotatedMethod<T>) delegate;
    }

    @Override
    public List<AnnotatedParameter<T>> getParameters() {
        return getDelegate().getParameters();
    }

    @Override
    public Method getJavaMember() {
        return getDelegate().getJavaMember();
    }


}
