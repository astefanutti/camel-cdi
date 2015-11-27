package org.apache.camel.cdi.sample.xml;


import org.apache.camel.CamelContext;
import org.apache.camel.model.ModelHelper;
import org.apache.camel.model.RoutesDefinition;

import javax.enterprise.inject.Produces;
import java.io.InputStream;

public class Routes {

    @Produces
    RoutesDefinition routes(CamelContext context) throws Exception {
        try (InputStream routes = getClass().getResourceAsStream("/routes.xml")) {
            return ModelHelper.createModelFromXml(context, routes, RoutesDefinition.class);
        }
    }
}
