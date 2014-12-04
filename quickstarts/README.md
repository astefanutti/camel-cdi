# Camel CDI - QuickStarts

* [Link to Header 2](#header-2)
* [Link to Header 3](#header-3)

## Header 2

blabla blabla

## Header 3

blabla blabla



# Karaf Instructions

## Installation

* Download and install Apache Karaf 3.x - http://karaf.apache.org/index/community/download.html#Karaf3.0.2
* Move to the bin directory, open a terminal and launch Apache Karaf

    ```
    ./karaf
    ```
    
* Install the Weld Container using the feature command
    
    ```
    feature:install pax-cdi-1.2-weld
    ```
    
* ! Weld-2.2.6.Final should be installed    
    
* Check the bundles deployed   
 
    ```
    list
    
    ID | State  | Lvl | Version     | Name
    -----------------------------------------------------------------------------------------------------------
    67 | Active |  80 | 1.0         | Apache Geronimo Java Contexts and Dependency Injection (JSR-299) Spec API
    68 | Active |  80 | 1.0         | Apache Geronimo JSR-330 Spec API
    69 | Active |  80 | 1.0         | Interceptor 1.1
    70 | Active |  80 | 1.0.3       | Apache Geronimo Expression Language Spec 2.2
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
    82 | Active |  80 | 1.2.0.Beta1 | Weld OSGi Bundle
    83 | Active |  80 | 0.8.0       | OPS4J Pax CDI Weld Adapter
    84 | Active |  80 | 2.1.1.Final | Weld OSGi Bundle
    ```
    
* Install camel repo
    ```
    feature:repo-add camel 2.14.0
    ```
    
* Install camel features (= core camel libraries)
    ```
    feature:install camel
    ```
    
* Add features of the camel cdi project
    ```    
    feature:repo-add mvn:io.astefanutti.camel.cdi/features/1.1-SNAPSHOT/xml/features    
    feature:install camel-cdi
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
* Install Camel CDI examples

Simple case : @ContextName("simple") and HelloBean

  ```        
  bundle:install mvn:io.astefanutti.camel.cdi/contextname-hellobean/1.1-SNAPSHOT 
  ```       

Simple case : Default CamelContextName created by the CDI extension and HelloBean

  ```
  bundle:install mvn:io.astefanutti.camel.cdi/defaultcamelcontext-hellobean/1.1-SNAPSHOT      
  ```
