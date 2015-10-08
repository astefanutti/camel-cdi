package org.apache.camel.cdi.se.bean;

import org.apache.camel.cdi.CdiCamelContext;
import org.apache.camel.cdi.ContextName;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;

@ApplicationScoped
@Named("second")
@ContextName("second")
public class SecondNamedCamelContextBean extends CdiCamelContext {
}
