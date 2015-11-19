package org.apache.camel.cdi;

import org.apache.camel.impl.DefaultCamelContext;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;

@Named("default")
@ApplicationScoped
public class DefaultCamelContextBean extends DefaultCamelContext {
}
