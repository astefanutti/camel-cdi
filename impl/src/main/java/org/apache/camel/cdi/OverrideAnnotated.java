package org.apache.camel.cdi;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;
import javax.enterprise.inject.spi.Annotated;

/**
 *
 */
public class OverrideAnnotated implements Annotated {

    protected final Annotated delegate;
    protected Set<Annotation> annotations;
    

    public OverrideAnnotated(Annotated delegate, Set<Annotation> toChange) {
        this.delegate = delegate;
        annotations = new HashSet<Annotation>(toChange);
    }

    OverrideAnnotated(Annotated delegate) {
        this.delegate = delegate;
    }

    @Override
    public <T extends Annotation> T getAnnotation(Class<T> annotationType) {
        for (Annotation a : annotations) {
            if (a.getClass().equals(annotationType))
                return (T) a;
        }
        return null;
    }

    @Override
    public Set<Annotation> getAnnotations() {
        return annotations;
    }

    @Override
    public Type getBaseType() {
        return delegate.getBaseType();
    }

    @Override
    public Set<Type> getTypeClosure() {
        return delegate.getTypeClosure();
    }

    @Override
    public boolean isAnnotationPresent(Class<? extends Annotation> annotationType) {
        return getAnnotation(annotationType) != null;
    }
}
