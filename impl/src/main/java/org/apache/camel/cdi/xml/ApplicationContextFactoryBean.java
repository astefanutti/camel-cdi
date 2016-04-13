/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.cdi.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement(name = "beans", namespace = "http://www.springframework.org/schema/beans")
@XmlAccessorType(XmlAccessType.FIELD)
public class ApplicationContextFactoryBean {

    @XmlElement(name = "camelContext")
    private List<CamelContextFactoryBean> contexts = new ArrayList<>();

    @XmlElement(name = "errorHandler")
    private List<ErrorHandlerDefinition> errorHandlers = new ArrayList<>();

    @XmlElement(name = "import")
    private List<CamelImportDefinition> imports = new ArrayList<>();

    @XmlElement(name = "restContext")
    private List<CamelRestContextDefinition> restContexts = new ArrayList<>();

    @XmlElement(name = "routeContext")
    private List<CamelRouteContextDefinition> routeContexts = new ArrayList<>();

    public List<CamelContextFactoryBean> getContexts() {
        return contexts;
    }

    public void setContexts(List<CamelContextFactoryBean> contexts) {
        this.contexts = contexts;
    }

    public List<ErrorHandlerDefinition> getErrorHandlers() {
        return errorHandlers;
    }

    public void setErrorHandlers(List<ErrorHandlerDefinition> errorHandlers) {
        this.errorHandlers = errorHandlers;
    }

    public List<CamelImportDefinition> getImports() {
        return imports;
    }

    public void setImports(List<CamelImportDefinition> imports) {
        this.imports = imports;
    }

    public List<CamelRestContextDefinition> getRestContexts() {
        return restContexts;
    }

    public void setRestContexts(List<CamelRestContextDefinition> restContexts) {
        this.restContexts = restContexts;
    }

    public List<CamelRouteContextDefinition> getRouteContexts() {
        return routeContexts;
    }

    public void setRouteContexts(List<CamelRouteContextDefinition> routeContexts) {
        this.routeContexts = routeContexts;
    }
}
