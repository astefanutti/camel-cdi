# Camel CDI - Quickstarts

Welcome to the Camel CDI quickstarts which demonstrates how to use the Camel CDI annotations (@ContextName, @Uri, ...) within your projects 
& how to run or deploy them into the different containers.

The quickstarts are categorized accordingly to the use case & annotations being used:

* [@ContextName annotation](#contextname-annotation)
* [Default CamelContext](#default-camelcontext)

Here are the instructions to follow to run or deploy the project using one of the different container :

* [Modular - OSGI Platform](#modular---osgi-platform)
* Java SE
* Web Container (Tomcat, Jetty)
* Java EE (Wildfly)

## ContextName annotation

blabla blabla

## Default CamelContext

blabla blabla

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
** Retrieve the bundle number of weld deployed 
```
list | grep -i weld
115 | Active |  80 | 2.2.4.Final | Weld OSGi Bundle
```
** Remove it
```
uninstall 115
```
** Install Weld 2.2.6.Final
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

** Simple case : @ContextName("simple") and HelloBean

  ```        
  install -s mvn:io.astefanutti.camel.cdi/simplecontextname/1.1-SNAPSHOT 
  ```       

** Simple case : Default CamelContextName created by the CDI extension and HelloBean

  ```
  install -s mvn:io.astefanutti.camel.cdi/defaultcamelcontext/1.1-SNAPSHOT      
  ```
