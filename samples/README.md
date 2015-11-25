# Camel CDI Samples

Welcome to the Camel CDI samples which demonstrate how to use CDI with your Camel projects and how to deploy / run them into different containers in a portable way.

## Java SE

Each sample can be individually launched using the `Main` class that is provided by Camel CDI (directly from the IDE or the `java` command) or using the Exec Maven plugin:

```
$ mvn clean compile exec:exec
```

Or to explicitly use Weld SE as CDI implementation (the default):

```
$ mvn clean compile exec:exec -Pstandalone,weld
```

Or to use OpenWebBeans as CDI implementation:

```
$ mvn clean compile exec:exec -Pstandalone,owb
```

Alternatively, you can also use the Camel Maven plugin that is able to start a CDI container with DeltaSpike container control:

```
$ mvn clean compile camel:run
```

Or to explicitly use Weld SE as CDI implementation (the default):

```
$ mvn clean compile camel:run -Pstandalone,weld
```

Or to use OpenWebBeans as CDI implementation:

```
$ mvn clean compile camel:run -Pstandalone,owb
```