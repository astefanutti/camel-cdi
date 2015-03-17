package org.apache.camel.cdi;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Annotation to veto an {@code AnnotatedType} for CDI 1.1+ backward compatibility
 */
@Target(value = { TYPE })
@Retention(value = RUNTIME)
@Documented
@interface ToVeto {
}
