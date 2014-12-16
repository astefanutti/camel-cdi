# Camel CDI - Quickstarts

Welcome to the Camel CDI quickstarts which demonstrates how to use the Camel CDI annotations (@ContextName, @Uri, ...) within your projects 
& how to run or deploy them into the different containers.

The quickstarts are categorized accordingly to the use case & annotations being used:

* [@ContextName annotation](#contextname-annotation)
* [Default CamelContext](#default-camelcontext)

Here are the instructions to follow to run or deploy the project using one of the different container :

* [Modular - OSGI Platform](#modular---osgi-platform)
* [Java SE](#java-se)
* [Web Container (Tomcat, Jetty)](#web-container)
* [Java EE (Wildfly)](#java-ee-wildfly)

## ContextName annotation

blabla blabla

## Default CamelContext

blabla blabla

## Java SE

The different examples can be launched using the Main BootStrap class part of each example directly or with the maven exec:java plugin

```
mvn clean compile -Pstandalone exec:java
```

Alternatively, you can also use the Camel Maven plugin able to start a Weld2 container with the DeltaSpike BootStrap Manager

```
mvn clean compile -Pstandalone camel:run
```


## Web Container

### Eclipse Jetty

To play with the different camel CDI quickstarts and run or deploy them into a Jetty Web Container, we have designed the maven web-jetty module
to include the required dependencies, web resources needed and configure the maven plugins that you will use to launch the container locally or generates the
WAR that next you will be able to deploy into the container.

TODO : Explain what is required/needed to use/play with Weld CDI & Jetty

#### Run a quickstart locally

* Open a terminal and move to the maven module `web-jetty`
* Launch this maven command using the `jetty:run` plugin goal and pass as parameter the groupId & artifactId
  of the quickstart that you would like to use

```
mvn clean jetty:run -Dquickstart.groupId=io.astefanutti.camel.cdi -Dquickstart.artifactId=defaultcamelcontext
```

#### Deploy a WAR into the Web Container

* Prerequisite : Download and unzip/untar the Jetty server (version recommended >= 9.x) - [Jetty Server 9.2](http://download.eclipse.org/jetty/9.2.6.v20141205/dist/jetty-distribution-9.2.6.v20141205.tar.gz)

* The WAR can be generated and deployed in a Jetty server using this command

```
mvn clean install -Dquickstart.groupId=io.astefanutti.camel.cdi -Dquickstart.artifactId=defaultcamelcontext
```
* Open terminal and move to the directory where you have installed/decompressed jetty
* Copy the WAR file to the webapp directory of jetty

```
cp ${HOME_QUICKARSTART_CDI}/web-jetty/target/web-jetty-1.1-SNAPSHOT.war webapps/
```

* Start the Jetty server with the modules `deploy, jndi & servlet`

```
java -jar start.jar --module=deploy,jndi,servlet

```
* Check the result into the log of the server. This snapshot is an example generated with the quickstart +defaultcamelcontext+

    2014-12-16 07:13:40.075:INFO:oejs.Server:main: jetty-9.3.0.M1
    2014-12-16 07:13:41,726 [main] INFO org.jboss.weld.environment.servletWeldServlet: WELD-ENV-001008: Initialize Weld using ServletContainerInitializer
    2014-12-16 07:13:41,734 [main] INFO org.jboss.weld.Version: WELD-000900: 2.2.6 (Final)
    2014-12-16 07:13:41,852 [main] WARN org.jboss.weld.environment.servletWeldServlet: WELD-ENV-001004: Found both WEB-INF/beans.xml and WEB-INF/classes/META-INF/beans.xml. It's not portable to use both locations at the same time. Weld is going to use file:/Users/chmoulli/Repos/Github/new-camel-cdi/quickstarts/web-jetty/src/main/webapp/WEB-INF/beans.xml.
    2014-12-16 07:13:41,900 [main] INFO org.jboss.weld.Bootstrap: WELD-000101: Transactional services not available. Injection of @Inject UserTransaction not available. Transactional observers will be invoked synchronously.
    2014-12-16 07:13:41,965 [main] WARN org.jboss.weld.Interceptor: WELD-001700: Interceptor annotation class javax.ejb.PostActivate not found, interception based on it is not enabled
    2014-12-16 07:13:41,965 [main] WARN org.jboss.weld.Interceptor: WELD-001700: Interceptor annotation class javax.ejb.PrePassivate not found, interception based on it is not enabled
    2014-12-16 07:13:42,162 [main] INFO org.jboss.weld.environment.servletJetty: WELD-ENV-001200: Jetty 7.2+ detected, CDI injection will be available in Servlets and Filters. Injection into Listeners should work on Jetty 9.1.1 and newer.
    2014-12-16 07:13:43,052 [main] INFO org.apache.camel.impl.converter.DefaultTypeConverter: Loaded 178 type converters
    Camel CDI :: Example 2 will be started
    2014-12-16 07:13:43,096 [main] INFO org.apache.camel.cdi.CdiCamelContext: Apache Camel 2.14.0 (CamelContext: camel-cdi) is starting
    2014-12-16 07:13:43,096 [main] INFO org.apache.camel.management.ManagedManagementStrategy: JMX is enabled
    2014-12-16 07:13:43,305 [main] INFO org.apache.camel.cdi.CdiCamelContext: AllowUseOriginalMessage is enabled. If access to the original message is not needed, then its recommended to turn this option off as it may improve performance.
    2014-12-16 07:13:43,305 [main] INFO org.apache.camel.cdi.CdiCamelContext: StreamCaching is not in use. If using streams then its recommended to enable stream caching. See more details at http://camel.apache.org/stream-caching.html
    2014-12-16 07:13:43,346 [main] INFO org.apache.camel.cdi.CdiCamelContext: Route: timerToDirect started and consuming from: Endpoint[timer://start]
    2014-12-16 07:13:43,347 [main] INFO org.apache.camel.cdi.CdiCamelContext: Route: directToBean started and consuming from: Endpoint[direct://continue]
    2014-12-16 07:13:43,349 [main] INFO org.apache.camel.cdi.CdiCamelContext: Total 2 routes, of which 2 is started.
    2014-12-16 07:13:43,349 [main] INFO org.apache.camel.cdi.CdiCamelContext: Apache Camel 2.14.0 (CamelContext: camel-cdi) started in 0.253 seconds
    2014-12-16 07:13:43,455 [main] INFO org.jboss.weld.environment.servletWeldServlet: WELD-ENV-001006: org.jboss.weld.environment.servlet.EnhancedListener used for ServletContext notifications
    2014-12-16 07:13:43,455 [main] INFO org.jboss.weld.environment.servletWeldServlet: WELD-ENV-001009: org.jboss.weld.environment.servlet.Listener used for ServletRequest and HttpSession notifications
    2014-12-16 07:13:43.482:INFO:oejsh.ContextHandler:main: Started o.e.j.m.p.JettyWebAppContext@2d0cadbc{/,file:///Users/chmoulli/Repos/Github/new-camel-cdi/quickstarts/web-jetty/src/main/webapp/,AVAILABLE}{file:///Users/chmoulli/Repos/Github/new-camel-cdi/quickstarts/web-jetty/src/main/webapp/}
    2014-12-16 07:13:43.501:INFO:oejs.ServerConnector:main: Started ServerConnector@755fe083{HTTP/1.1,[http/1.1]}{0.0.0.0:8080}
    2014-12-16 07:13:43.502:INFO:oejs.Server:main: Started @6161ms
    [INFO] Started Jetty Server
    2014-12-16 07:13:44,358 [Camel (camel-cdi) thread #0 - timer://start] INFO timerToDirect: Message received : Camel CDI Example 2 for the Context : simple
    2014-12-16 07:13:44,360 [Camel (camel-cdi) thread #0 - timer://start] INFO timerToDirect: Message received : Camel CDI Example 2 for the header : message from file
    2014-12-16 07:13:44,362 [Camel (camel-cdi) thread #0 - timer://start] INFO directToBean: >> Response : Hello CDI user for example 2.
    2014-12-16 07:13:45,351 [Camel (camel-cdi) thread #0 - timer://start] INFO timerToDirect: Message received : Camel CDI Example 2 for the Context : simple
    2014-12-16 07:13:45,351 [Camel (camel-cdi) thread #0 - timer://start] INFO timerToDirect: Message received : Camel CDI Example 2 for the header : message from file
    2014-12-16 07:13:45,352 [Camel (camel-cdi) thread #0 - timer://start] INFO directToBean: >> Response : Hello CDI user for example 2.
    2014-12-16 07:13:46,351 [Camel (camel-cdi) thread #0 - timer://start] INFO timerToDirect: Message received : Camel CDI Example 2 for the Context : simple
    2014-12-16 07:13:46,352 [Camel (camel-cdi) thread #0 - timer://start] INFO timerToDirect: Message received : Camel CDI Example 2 for the header : message from file
    2014-12-16 07:13:46,352 [Camel (camel-cdi) thread #0 - timer://start] INFO directToBean: >> Response : Hello CDI user for example 2.
    2014-12-16 07:13:47,352 [Camel (camel-cdi) thread #0 - timer://start] INFO timerToDirect: Message received : Camel CDI Example 2 for the Context : simple
    2014-12-16 07:13:47,353 [Camel (camel-cdi) thread #0 - timer://start] INFO timerToDirect: Message received : Camel CDI Example 2 for the header : message from file
    2014-12-16 07:13:47,354 [Camel (camel-cdi) thread #0 - timer://start] INFO directToBean: >> Response : Hello CDI user for example 2.
    2014-12-16 07:13:48,353 [Camel (camel-cdi) thread #0 - timer://start] INFO timerToDirect: Message received : Camel CDI Example 2 for the Context : simple
    2014-12-16 07:13:48,353 [Camel (camel-cdi) thread #0 - timer://start] INFO timerToDirect: Message received : Camel CDI Example 2 for the header : message from file
    2014-12-16 07:13:48,354 [Camel (camel-cdi) thread #0 - timer://start] INFO directToBean: >> Response : Hello CDI user for example 2.
    2014-12-16 07:13:49,353 [Camel (camel-cdi) thread #0 - timer://start] INFO timerToDirect: Message received : Camel CDI Example 2 for the Context : simple
    2014-12-16 07:13:49,353 [Camel (camel-cdi) thread #0 - timer://start] INFO timerToDirect: Message received : Camel CDI Example 2 for the header : message from file
    2014-12-16 07:13:49,354 [Camel (camel-cdi) thread #0 - timer://start] INFO directToBean: >> Response : Hello CDI user for example 2.
    ^C2014-12-16 07:13:50.151:INFO:oejs.ServerConnector:Thread-1: Stopped ServerConnector@755fe083{HTTP/1.1,[http/1.1]}{0.0.0.0:8080}
    2014-12-16 07:13:50,352 [Camel (camel-cdi) thread #0 - timer://start] INFO timerToDirect: Message received : Camel CDI Example 2 for the Context : simple
    2014-12-16 07:13:50,353 [Camel (camel-cdi) thread #0 - timer://start] INFO timerToDirect: Message received : Camel CDI Example 2 for the header : message from file
    2014-12-16 07:13:50,353 [Camel (camel-cdi) thread #0 - timer://start] INFO directToBean: >> Response : Hello CDI user for example 2.


### Apache Tomcat

* Command to be used to run locally Tomcat with Weld2

```
mvn war:inplace -Ptomcat tomcat7:run
```


## Java EE Wildfly

* Command to be used to run locally Wildfly with Weld2

```
mvn -Pwar wildfly:run
```

## Modular - OSGI Platform

### Installation

* Download and install Apache Karaf 3.x - http://karaf.apache.org/index/community/download.html#Karaf3.0.2
* open a terminal on your machine (Unix/Windows), move to the bin directory and launch Apache Karaf

    ```
    ./karaf or karaf.bat
    ```
    
* Install the Weld Container using the feature command. When this command is excuted the bundles defined within the features XML file
  will be downloaded and deployed into the Karaf container.
    
    ```
    feature:repo-add mvn:org.ops4j.pax.cdi/pax-cdi-features/0.9.0/xml/features
    feature:install pax-cdi-1.2-weld
    ```
    
* REMARK : As PAx-CDI Weld (= version 0.9) will deploy a older version of Weld, we have to do a manipulation to install the 
  version of Weld-2.2.6.Final that we have qualified for that release. So, remove the deployed bundle of weld and install the version 2.2.6.Final
  
  * Retrieve the bundle number of weld deployed 
    ```
    list | grep -i weld
    115 | Active |  80 | 2.2.4.Final | Weld OSGi Bundle
    ```
  * Remove it
    ```
    uninstall 115
    ```
  * Install Weld 2.2.6.Final
    ```
    install -s mvn:org.jboss.weld/weld-osgi-bundle/2.2.6.Final
    ```

* Control the list of the bundles deployed. They should contain OPS4J Pax CDI & Weld and 
  the bundles required like JBoss Logging, Guava, CDI APIs, JSR330, EL, ...
 
    ```
    list
    ID | State  | Lvl | Version     | Name
    ---------------------------------------------------------------------------
    67 | Active |  80 | 1.2         | javax.interceptor API
    68 | Active |  80 | 1.2.0       | CDI APIs
    69 | Active |  80 | 1.0         | Apache Geronimo JSR-330 Spec API
    70 | Active |  80 | 3.0.0       | Expression Language 3.0 API
    71 | Active |  80 | 1.7.1       | OPS4J Pax Swissbox :: Tracker
    72 | Active |  80 | 1.7.1       | OPS4J Pax Swissbox :: Lifecycle
    73 | Active |  80 | 1.7.1       | OPS4J Pax Swissbox :: Extender
    74 | Active |  80 | 1.7.1       | OPS4J Pax Swissbox :: OSGi Core
    75 | Active |  80 | 1.4.0       | OPS4J Base - Service Provider Access
    76 | Active |  80 | 1.4.0       | OPS4J Base - Lang
    77 | Active |  80 | 3.18.0      | Apache XBean OSGI Bundle Utilities
    78 | Active |  80 | 0.8.0       | OPS4J Pax CDI Bean Bundle API
    79 | Active |  80 | 0.8.0       | OPS4J Pax CDI Service Provider Interface
    80 | Active |  80 | 0.8.0       | OPS4J Pax CDI Portable Extension for OSGi
    81 | Active |  80 | 0.8.0       | OPS4J Pax CDI Extender for Bean Bundles
    83 | Active |  80 | 3.1.3.GA    | JBoss Logging 3
    84 | Active |  80 | 13.0.1      | Guava: Google Core Libraries for Java
    85 | Active |  80 | 0.8.0       | OPS4J Pax CDI Weld Adapter
    86 | Active |  80 | 2.2.6.Final | Weld OSGi Bundle
    ```
    
* Now, we can install the repository containing the camel features
    ```
    feature:repo-add camel 2.14.0
    ```
    
* And next, the features of camel that we need to run the quickstarts (= core camel libraries)
    ```
    feature:install camel
    ```
    
* The following step will consist in installing the new camel-cdi component
    ```    
    install -s mvn:io.astefanutti.camel.cdi/camel-cdi
    ```    
* Check
    
    ```
     99 | Active |  50 | 2.14.0         | camel-core
    100 | Active |  50 | 2.14.0         | camel-karaf-commands
    115 | Active |  50 | 1.1.1          | geronimo-jta_1.1_spec
    116 | Active |  50 | 2.14.0         | camel-spring
    117 | Active |  50 | 2.14.0         | camel-blueprint
    119 | Active |  80 | 1.2            | javax.interceptor API
    120 | Active |  80 | 1.2.0          | CDI APIs
    121 | Active |  80 | 3.0.0          | Expression Language 3.0 API
    122 | Active |  80 | 1.1.0.SNAPSHOT | camel-cdi
    ```    
* Install one of the quickstarts

  * Simple case : @ContextName("simple") and HelloBean

  ```        
  install -s mvn:io.astefanutti.camel.cdi/simplecontextname/1.1-SNAPSHOT 
  ```       

  * Simple case : Default CamelContextName created by the CDI extension and HelloBean

  ```
  install -s mvn:io.astefanutti.camel.cdi/defaultcamelcontext/1.1-SNAPSHOT      
  ```
