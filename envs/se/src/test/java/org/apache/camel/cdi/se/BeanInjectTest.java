/**
 * Copyright (C) 2014 Antonin Stefanutti (antonin.stefanutti@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.cdi.se;

import org.apache.camel.cdi.CdiCamelExtension;
import org.apache.camel.cdi.CdiPropertiesComponent;
import org.apache.camel.cdi.se.bean.BeanInjectBean;
import org.apache.camel.cdi.se.bean.PropertyInjectBean;
import org.apache.camel.component.properties.PropertiesComponent;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.Properties;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

@RunWith(Arquillian.class)
public class BeanInjectTest {

    @Deployment
    public static Archive<?> deployment() {
        return ShrinkWrap.create(JavaArchive.class)
            // Camel CDI
            .addPackage(CdiCamelExtension.class.getPackage())
            // Test classes
            .addClass(BeanInjectBean.class)
            .addClass(PropertyInjectBean.class)
            // Bean archive deployment descriptor
            .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @Produces
    @ApplicationScoped
    @Named("properties")
    private static PropertiesComponent configuration() {
        Properties configuration = new Properties();
        configuration.put("property", "value");
        return new CdiPropertiesComponent(configuration);
    }

    @Inject
    private BeanInjectBean bean;

    @Test
    public void propertyInjectField() {
        assertThat(bean.getInjectBeanField(), is(notNullValue()));
        assertThat(bean.getInjectBeanField().getProperty(), is(equalTo("value")));
    }

    @Test
    public void propertyInjectMethod() {
        assertThat(bean.getInjectBeanMethod(), is(notNullValue()));
        assertThat(bean.getInjectBeanMethod().getProperty(), is(equalTo("value")));
    }
}
