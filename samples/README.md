# Camel CDI Samples

Welcome to the Camel CDI samples which demonstrate how to use CDI with your Camel projects and how to deploy / run them into different containers in a portable way.

## Java SE

Each sample can be launched individually using the [`Main`](../impl/src/main/java/org/apache/camel/cdi/Main.java) class that is provided by Camel CDI (directly from the IDE or the `java` command) or using the Camel, Exec or HawtIO Maven plugins.

The CDI implementation used to execute the samples can be explicitly defined, e.g.:

- To use Weld SE as CDI implementation (the default):

    ```
    $ mvn hawtio:camel -Pstandalone,weld
    ```

- Or to use OpenWebBeans as CDI implementation:

    ```
    $ mvn hawtio:camel -Pstandalone,owb
    ```

#### Camel Maven Plugin

To run a sample using the [Camel Maven plugin](http://camel.apache.org/camel-maven-plugin.html):

- Compile the sample:

    ```
    $ mvn clean compile
    ```

- Run the Camel Maven plugin:

    ```
    $ mvn camel:run
    ```

#### Exec Maven Plugin

To run a sample using the [Exec Maven plugin](http://www.mojohaus.org/exec-maven-plugin/):

- Compile the sample:

    ```
    $ mvn clean compile
    ```

- Run the Exec Maven plugin:

    ```
    $ mvn exec:exec
    ```

#### HawtIO Maven Plugin

To run a sample using the [HawtIO Maven plugin](http://hawt.io/maven/):

- Compile the sample:

    ```
    $ mvn clean compile
    ```

- Run the HawtIO Maven plugin:

    ```
    $ mvn hawtio:camel
    ```