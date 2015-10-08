package org.apache.camel.cdi.se.bean;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.cdi.ContextName;

@ContextName("first")
public class FirstNamedCamelContextRoute extends RouteBuilder {

    @Override
    public void configure() {
        from("direct:in").transform(body().prepend("first-")).to("direct:out");
    }
}
