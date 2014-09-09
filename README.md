# CDI extension for Camel

[![Build Status][Travis badge]][Travis build] [![Coverage Status][Coveralls badge]][Coveralls build] [![Dependency Status][VersionEye badge]][VersionEye build]

[Travis badge]: https://travis-ci.org/astefanutti/camel-cdi.svg
[Travis build]: https://travis-ci.org/astefanutti/camel-cdi
[Coveralls badge]: https://img.shields.io/coveralls/astefanutti/camel-cdi.svg
[Coveralls build]: https://coveralls.io/r/astefanutti/camel-cdi
[VersionEye badge]: https://www.versioneye.com/user/projects/53fca400e09da310ea0006c4/badge.svg
[VersionEye build]: https://www.versioneye.com/user/projects/53fca400e09da310ea0006c4

[CDI][] portable extension for Apache [Camel][] compliant with [JSR 346: Contexts and Dependency Injection for Java<sup>TM</sup> EE 1.2][JSR 346 1.2].

[CDI]: http://www.cdi-spec.org/
[Camel]: http://camel.apache.org/
[JSR 299]: https://jcp.org/en/jsr/detail?id=299
[JSR 346]: https://jcp.org/en/jsr/detail?id=346
[JSR 346 1.1]: https://jcp.org/aboutJava/communityprocess/final/jsr346/index.html
[JSR 346 1.2]: https://jcp.org/aboutJava/communityprocess/mrel/jsr346/index.html
[CDI 1.1]: http://docs.jboss.org/cdi/spec/1.1/cdi-spec.html
[CDI 1.2]: http://docs.jboss.org/cdi/spec/1.2/cdi-spec.html

## Why Refactoring Camel CDI?

Since version `2.10` of Camel, the [Camel CDI][] component supports the integration of Camel in CDI enabled environments. However, some experiments and _battlefield_ tests prove it troublesome to use because of the following concerns:
+ It relies on older [CDI 1.0][JSR 299] version of the specification and makes incorrect usages of [container lifecycle events][] w.r.t. [Assignability of type variables, raw and parameterized types]. As a consequence, it does not work properly with newer implementation versions like Weld 2.x and containers like Wildfly 8.x as reported in [CAMEL-7760][] among other issues.
+ It relies on Apache [DeltaSpike][] and its `BeanManagerProvider` class to retrieve the `BeanManager` instance during the CDI container initialisation. That may not be suitable in complex container configurations, for example, in multi CDI container per JVM context, as reported in [CAMEL-6338][] and that causes [CAMEL-6095][] and [CAMEL-6937][].
+ It relies on _DeltaSpike_ and its [configuration mechanism][DeltaSpike Configuration Mechanism] to source configuration locations for the [Properties component][]. While this is suitable for most use cases, it relies on the `ServiceLoader` mechanism to support custom [configuration sources][ConfigSource] that may not be suitable in more complex container configurations and relates to [CAMEL-5986].
+ Besides, while _DeltaSpike_ is a valuable addition to the CDI ecosystem, Camel CDI having a direct dependency on it is questionable from a design standpoint as opposed to relying on standard Camel mechanism for producing the Camel Properties component and delegating, as a plugable strategy, the configuration sourcing concern and implementation choice to the application itself or eventually using the [Java EE Configuration JSR] when available.
+ It declares a `CamelContext` CDI bean that's automatically instantiated and started with a `@PostConstruct` lifecycle callback called before the CDI container is completely initialized. That prevents, among other side effects, proper advising of Camel routes as documented in [Camel AdviceWith][].
+ It uses the `@ContextName` annotation to bind routes to the `CamelContext` instance specified by name as an attempt to provide multi-context support. However, that is an incomplete feature from the CDI programming model standpoint as discussed in [CAMEL-5566][] and that causes [CAMEL-5742][].

The objective of this project is to alleviate all these concerns, provide additional features, and have that improved version of the Camel CDI component contributed back in the official codeline.

[Camel CDI]: http://camel.apache.org/cdi.html
[DeltaSpike]: https://deltaspike.apache.org/
[DeltaSpike Configuration Mechanism]: https://deltaspike.apache.org/configuration.html
[ConfigSource]: https://deltaspike.apache.org/configuration.html#custom-config-sources
[Camel AdviceWith]: http://camel.apache.org/advicewith.html
[Properties component]: http://camel.apache.org/properties
[CAMEL-5566]: https://issues.apache.org/jira/browse/CAMEL-5566
[CAMEL-5742]: https://issues.apache.org/jira/browse/CAMEL-5742
[CAMEL-5986]: https://issues.apache.org/jira/browse/CAMEL-5986
[CAMEL-6338]: https://issues.apache.org/jira/browse/CAMEL-6338
[CAMEL-6095]: https://issues.apache.org/jira/browse/CAMEL-6095
[CAMEL-6937]: https://issues.apache.org/jira/browse/CAMEL-6937
[CAMEL-7760]: https://issues.apache.org/jira/browse/CAMEL-7760
[container lifecycle events]: http://docs.jboss.org/cdi/spec/1.2/cdi-spec.html#init_events
[Assignability of type variables, raw and parameterized types]: http://docs.jboss.org/cdi/spec/1.2/cdi-spec.html#observers_assignability
[Java EE Configuration JSR]: http://javaeeconfig.blogspot.ch/

## How is Camel CDI improved?

### Bug Fixes

This project fixes the following issues currently opened in the Camel CDI backlog:
- [CAMEL-5742]: The `@ContextName` should only refer to a `CamelContext` and not create a new `CamelContext` on the fly
- [CAMEL-5986]: Property placeholders do not work for CDI injection
- [CAMEL-6338]: Camel CDI shouldn't use DeltaSpike bean manager provider in the `CamelExtension`
- [CAMEL-6937]: `BeanManager` cannot be retrieved when Camel CDI is deployed in Karaf
- [CAMEL-7760]: WELD-001409: Ambiguous dependencies for type `CdiCamelContext`

### Improved Features

###### Multiple Camel Contexts

The official Camel CDI declares the `ContextName` annotation that can be used to declare multiple `CamelContext` instances. However that annotation is not declared as a CDI qualifier and does not fit nicely in the CDI programming model as discussed in [CAMEL-5566][]. That improved version alleviates that concerns so that the `ContextName` annotation can be used as a proper CDI qualifier, e.g.:

```java
@ApplicationScoped
@ContextName("first")
public class FirstCamelContext extends CdiCamelContext {

    @Inject
    private FirstCamelContext(BeanManager beanManager) {
        super(beanManager);
    }
}
```

And then:

```java
@Inject
@ContextName("first")
CamelContext firstCamelContext;
```

###### Camel Beans Integration

The [bean integration][] support with multiple Camel contexts has been improved so that beans and components can be declared per Camel context.

Each time a bean is looked up by type, a bean of that type annotated with the `@ContextName` qualifier is first looked up. If such bean exists, a contextual reference of that bean is retrieved, else the lookup falls back to a bean with the `@Default` qualifier, e.g.:

```java
@ContextName("first")
class FirstCamelContextRoute extends RouteBuilder {

    @Override
    public void configure() {
        // Lookup CDI bean with qualifier @ContextName("first") then @Default if any
        from("...").bean(Bean.class);
    }
}
```

For Camel components that are looked up by name, that approach cannot be used unfortunately as bean names declared with the `@Named` qualifier must be unique as documented in [Ambiguous EL names][]. To still support the ability to define Camel beans and components per Camel context, the bean name is prefixed with the context name for the lookup, e.g.:

```java
@ContextName("first")
class FirstCamelContextRoute extends RouteBuilder {

    @Override
    public void configure() {
        // Lookup CDI bean with qualifier @Named("first:beanName") then @Named("beanName") if any
        from("...").beanRef("beanName");
    }
}
```

:warning: Unfortunately, namespaces are not supported for EL variables as opposed to EL functions which leads to invalid EL names that cannot be used in EL expressions.

[bean integration]: http://camel.apache.org/bean-integration.html
[Ambiguous EL names]: [http://docs.jboss.org/cdi/spec/1.2/cdi-spec.html#ambig_names]

###### Camel Annotations Support

Camel comes with a set of [annotations][Camel annotations] that are supported by Camel CDI for both standard CDI injection and Camel [bean integration][], e.g.:

```java
@PropertyInject("property")
String property;
```

Unfortunately, the official version of Camel CDI does not support the `context` attribute declared on these annotations as part of the multi Camel contexts support. That improved version provides that support, e.g.:

```java
@EndpointInject(uri = "direct:inbound", context = "first")
Endpoint firstContextInboundEndpoint;

@EndpointInject(uri = "direct:inbound", context = "second")
Endpoint secondContextInboundEndpoint;
```

[Camel annotations]: http://camel.apache.org/bean-integration.html#BeanIntegration-Annotations

###### Camel Context Customization

```java
@ApplicationScoped
class CustomCamelContext extends CdiCamelContext {

    @Inject
    CustomCamelContext(BeanManager beanManager) {
        super(beanManager);
        // Set the Camel context name
        setName("custom");
        // Add properties location
        PropertiesComponent properties = getComponent("properties", PropertiesComponent.class)
        properties.setLocation("classpath:placeholder.properties");
    }

    // Bind the Camel context lifecycle to that of the bean
    @PostConstruct
    public void start() {
        super.start();
    }

    @PreDestroy
    public void stop() {
        super.stop();
    }
}
```

### New Features

###### Type Converter Beans

CDI beans annotated with the `@Converter` annotation are automatically registered in the corresponding Camel context, e.g.:

```java
@Converter
public class TypeConverter {

    @Converter
    public Output convert(Input input) {
        ...
    }
}
```
Note that CDI injection is supported within the type converters.

### Existing features

###### Camel Route Builders

```java
@ContextName("first")
class FirstCamelContextRoute extends RouteBuilder {

    @Override
    public void configure() {
        from("direct:inbound").setHeader("context").constant("first").to("mock:outbound");
    }
}
```

###### Camel CDI Beans

Camel CDI declares some producer method beans that can be used to inject Camel objects, e.g.:

```java
@Inject
@Uri("direct:inbound")
ProducerTemplate producerTemplate;

@Inject
@Uri("mock:outbound")
MockEndpoint mockEndpoint;
```

## What are the Supported Containers?

This improved version of _Camel CDI_ is currently successfully tested with the following containers:

| Container        | Version       | Specification          | Arquillian Container Adapter           |
| ---------------- | ------------- | ---------------------- | -------------------------------------- |
| [Weld SE][]      | `2.2.4.Final` | [CDI 1.2][JSR 346 1.2] | `arquillian-weld-se-embedded-1.1`      |
| [OpenWebBeans][] | `2.0.0`       | [CDI 1.1][JSR 346 1.1] | `owb-arquillian-standalone`            |
| [Weld EE][]      | `2.2.4.Final` | [CDI 1.2][JSR 346 1.2] | `arquillian-weld-ee-embedded-1.1`      |
| [WildFly][]      | `8.1.0.Final` | [Java EE 7][]          | `wildfly-arquillian-container-managed` |

[Weld SE]: http://weld.cdi-spec.org/
[Weld EE]: http://weld.cdi-spec.org/
[WildFly]: http://www.wildfly.org/
[OpenWebBeans]: http://openwebbeans.apache.org/
[Java EE 7]: https://jcp.org/en/jsr/detail?id=342

## License

Copyright © 2014, Antonin Stefanutti

Published under Apache Software License 2.0, see LICENSE
