package org.apache.camel.cdi;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Set;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.AnnotatedCallable;
import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;

/**
 *
 */
public class BeanNamesContextQualified<T> implements Bean<T> {

    private final Annotated target;

    private final AnnotatedType<T> delegate;

    private final Set<Annotation> qualifiers;

    private final Targets targetType;

    @Override
    public Set<Type> getTypes() {
        return delegate.getTypeClosure();
    }

    @Override
    public Set<Annotation> getQualifiers() {
        throw new IllegalStateException("not implemented");
    }

    @Override
    public Class<? extends Annotation> getScope() {
        return CdiSpiHelper.getScopeClass(target);
    }

    @Override
    public String getName() {
        throw new IllegalStateException("not implemented");
    }

    @Override
    public Set<Class<? extends Annotation>> getStereotypes() {
        throw new IllegalStateException("not implemented");
    }

    @Override
    public Class<?> getBeanClass() {
        throw new IllegalStateException("not implemented");
    }

    @Override
    public boolean isAlternative() {
        throw new IllegalStateException("not implemented");
    }

    @Override
    public boolean isNullable() {
        throw new IllegalStateException("not implemented");
    }

    @Override
    public Set<InjectionPoint> getInjectionPoints() {
        throw new IllegalStateException("not implemented");
    }

    @Override
    public T create(CreationalContext<T> creationalContext) {
        throw new IllegalStateException("not implemented");
    }

    @Override
    public void destroy(T instance, CreationalContext<T> creationalContext) {
        throw new IllegalStateException("not implemented");
    }

    public enum Targets {
        TYPE,
        FIELD,
        METHOD
    }


    public BeanNamesContextQualified(Set<Annotation> qualifiers, AnnotatedType<T> target) {
        delegate = target;
        targetType = Targets.TYPE;
        this.qualifiers = qualifiers;
        this.target = target;
    }

    public BeanNamesContextQualified(Set<Annotation> qualifiers, AnnotatedField<T> target) {
        delegate = target.getDeclaringType();
        targetType = Targets.FIELD;
        this.qualifiers = qualifiers;
        this.target = target;
    }

    public BeanNamesContextQualified(Set<Annotation> qualifiers, AnnotatedCallable<T> target) {
        delegate = target.getDeclaringType();
        targetType = Targets.METHOD;
        this.qualifiers = qualifiers;
        this.target = target;
    }
    
    
    
    
}
