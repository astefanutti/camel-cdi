package org.apache.camel.cdi.example1;


import org.jboss.weld.environment.se.StartMain;

import java.util.concurrent.CountDownLatch;

public class Main {

    public static void main(String[] args) throws Exception {
        (new StartMain(args)).go();
        new CountDownLatch(1).await();
    }
}
