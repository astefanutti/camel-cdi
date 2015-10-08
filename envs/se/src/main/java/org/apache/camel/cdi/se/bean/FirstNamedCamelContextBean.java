package org.apache.camel.cdi.se.bean;

import org.apache.camel.cdi.CdiCamelContext;
import org.apache.camel.cdi.ContextName;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;

@ApplicationScoped
@Named("first")
@ContextName("first")
public class FirstNamedCamelContextBean extends CdiCamelContext {
}
