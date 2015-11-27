package org.apache.camel.cdi.sample.xml;

import javax.inject.Named;
import java.util.Date;

@Named
public class MyBean {

    public String someMethodName() {
        return new Date().toString();
    }
}
