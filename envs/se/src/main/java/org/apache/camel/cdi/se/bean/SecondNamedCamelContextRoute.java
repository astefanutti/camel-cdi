package org.apache.camel.cdi.se.bean;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.cdi.ContextName;

@ContextName("second")
public class SecondNamedCamelContextRoute extends RouteBuilder {

    @Override
    public void configure() {
        from("direct:in").transform(body().prepend("second-")).to("direct:out");
    }
}
