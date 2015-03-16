package org.apache.camel.cdi;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Set;
import javax.enterprise.inject.spi.AnnotatedField;

/**
 *
 */
public class OverrideAnnotatedField<T> extends OverrideAnnotatedMember<T> implements AnnotatedField<T> {

    public OverrideAnnotatedField(AnnotatedField<T> delegate, Set<Annotation> enrichment) {
        super(delegate, enrichment);
    }

    private AnnotatedField<T> getDelegate() {
        return (AnnotatedField<T>) delegate;
    }

    @Override
    public Field getJavaMember() {
        return getDelegate().getJavaMember();
    }
}
