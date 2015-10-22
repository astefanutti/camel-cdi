package org.apache.camel.cdi;

import javax.enterprise.context.ApplicationScoped;

@ContextName("foo")
@ApplicationScoped
public class FooCamelContextBean extends CdiCamelContext {
}
