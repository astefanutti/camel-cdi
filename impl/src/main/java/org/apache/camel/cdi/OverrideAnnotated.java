package org.apache.camel.cdi;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import javax.enterprise.inject.spi.Annotated;

class OverrideAnnotated implements Annotated {

    private final Annotated delegate;

    private final Set<Annotation> annotations;

    OverrideAnnotated(Annotated delegate, Set<Annotation> annotations) {
        this.delegate = delegate;
        this.annotations = new HashSet<Annotation>(annotations);
    }

    OverrideAnnotated(Annotated delegate) {
        this.delegate = delegate;
        annotations = Collections.emptySet();
    }

    @Override
    public <T extends Annotation> T getAnnotation(Class<T> annotationType) {
        for (Annotation a : annotations) 
            if (a.annotationType().equals(annotationType))
                return (T) a;

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

    @Override
    public String toString() {
        return delegate.toString();
    }
    
    @Override
    public int hashCode() {
        return delegate.hashCode();
    }
    
    @Override
    public boolean equals(Object object) {
        return delegate.equals(object);
    }
}
