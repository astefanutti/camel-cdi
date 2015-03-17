package org.apache.camel.cdi;

import javax.enterprise.util.AnnotationLiteral;


@ToVeto
class HiddenLiteral extends AnnotationLiteral<Hidden> implements Hidden {
    
    static final Hidden INSTANCE = new HiddenLiteral();
}
