package org.apache.camel.cdi;

import javax.enterprise.util.AnnotationLiteral;

/**
 *
 */
@ToVeto
public class HiddenLiteral extends AnnotationLiteral<Hidden> implements Hidden {
    
    public static final Hidden INSTANCE = new HiddenLiteral();
}
