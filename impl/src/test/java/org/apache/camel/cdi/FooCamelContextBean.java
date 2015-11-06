package org.apache.camel.cdi;

import org.apache.camel.impl.DefaultCamelContext;

import javax.enterprise.context.ApplicationScoped;

@ContextName("foo")
@ApplicationScoped
public class FooCamelContextBean extends DefaultCamelContext {
}
