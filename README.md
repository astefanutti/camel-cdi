# CDI Extension for Camel

[![Build Status][Travis badge]][Travis build] [![Coverage Status][Coveralls badge]][Coveralls build] [![Dependency Status][VersionEye badge]][VersionEye build] [![Maven Central][Maven Central badge]][Maven Central build]

[Travis badge]: https://travis-ci.org/astefanutti/camel-cdi.svg?branch=master
[Travis build]: https://travis-ci.org/astefanutti/camel-cdi
[Coveralls badge]: https://coveralls.io/repos/astefanutti/camel-cdi/badge.svg
[Coveralls build]: https://coveralls.io/github/astefanutti/camel-cdi
[VersionEye badge]: https://www.versioneye.com/user/projects/53fca400e09da310ea0006c4/badge.svg
[VersionEye build]: https://www.versioneye.com/user/projects/53fca400e09da310ea0006c4
[Maven Central badge]: http://img.shields.io/maven-central/v/io.astefanutti.camel.cdi/camel-cdi.svg
[Maven Central build]: http://repo1.maven.org/maven2/io/astefanutti/camel/cdi/camel-cdi/1.2.0/

[CDI][] portable extension for Apache [Camel][] compliant with [JSR 346: Contexts and Dependency Injection for Java<sup>TM</sup> EE 1.2][JSR 346 1.2].

[CDI]: http://www.cdi-spec.org/
[Camel]: http://camel.apache.org/
[JSR 299]: https://jcp.org/en/jsr/detail?id=299
[JSR 346]: https://jcp.org/en/jsr/detail?id=346
[JSR 346 1.1]: https://jcp.org/aboutJava/communityprocess/final/jsr346/index.html
[JSR 346 1.2]: https://jcp.org/aboutJava/communityprocess/mrel/jsr346/index.html
[CDI 1.1]: http://docs.jboss.org/cdi/spec/1.1/cdi-spec.html
[CDI 1.2]: http://docs.jboss.org/cdi/spec/1.2/cdi-spec.html

## About

Version `1.2.0` of this component [has been merged](https://github.com/apache/camel/commit/0421c24dfcf992f3296ed746469771e3800200e3) into Apache Camel as part of [CAMEL-9201](https://issues.apache.org/jira/browse/CAMEL-9201). It now serves as an upstream project to explore possible evolution of the Camel CDI integration using the latest versions of the underlying technologies.

Hereafter is the original project statement:
> Since version `2.10` of Camel, the [Camel CDI][] component supports the integration of Camel in CDI enabled environments. However, some experiments and _battlefield_ tests prove it troublesome to use because of the following concerns:
> + It relies on older [CDI 1.0][JSR 299] version of the specification and makes incorrect usages of [container lifecycle events][] w.r.t. [Assignability of type variables, raw and parameterized types]. As a consequence, it does not work properly with newer implementation versions like Weld 2.x and containers like WildFly 8.x as reported in [CAMEL-7760][] among other issues.
> + It relies on Apache [DeltaSpike][] and its `BeanManagerProvider` class to retrieve the `BeanManager` instance during the CDI container initialisation. That may not be suitable in complex container configurations, for example, in multiple CDI containers per JVM context, as reported in [CAMEL-6338][] and that causes [CAMEL-6095][] and [CAMEL-6937][].
> + It relies on DeltaSpike and its [configuration mechanism][DeltaSpike Configuration Mechanism] to source configuration locations for the [Properties component][]. While this is suitable for most use cases, it relies on the `ServiceLoader` mechanism to support custom [configuration sources][ConfigSource] that may not be suitable in more complex container configurations and relates to [CAMEL-5986][].
> + Besides, while DeltaSpike is a valuable addition to the CDI ecosystem, Camel CDI having a direct dependency on it is questionable from a design standpoint as opposed to relying on standard Camel mechanism for producing the Camel Properties component and delegating, as a plugable strategy, the configuration sourcing concern and implementation choice to the application itself or eventually using the [Java EE Configuration JSR][] when available.
> + It declares a `CamelContext` CDI bean that's automatically instantiated and started with a `@PostConstruct` lifecycle callback called before the CDI container is completely initialized. That prevents, among other side effects like [CAMEL-9336][], proper configuration of the Camel context as reported in [CAMEL-8325][] and advising of Camel routes as documented in [Camel AdviceWith][].
> + It uses the `@ContextName` annotation to bind routes to the `CamelContext` instance specified by name as an attempt to provide support for multiple Camel contexts per application. However, that is an incomplete feature from the CDI programming model standpoint as discussed in [CAMEL-5566][] and that causes [CAMEL-5742][].

> The objective of this project is to alleviate all these concerns, provide additional features, and have that improved version of the Camel CDI component contributed back into the official codeline.

[Camel CDI]: http://camel.apache.org/cdi.html
[DeltaSpike]: https://deltaspike.apache.org/
[DeltaSpike Configuration Mechanism]: https://deltaspike.apache.org/configuration.html
[ConfigSource]: https://deltaspike.apache.org/configuration.html#custom-config-sources
[Camel AdviceWith]: http://camel.apache.org/advicewith.html
[Properties component]: http://camel.apache.org/properties
[CAMEL-5566]: https://issues.apache.org/jira/browse/CAMEL-5566
[CAMEL-5742]: https://issues.apache.org/jira/browse/CAMEL-5742
[CAMEL-5986]: https://issues.apache.org/jira/browse/CAMEL-5986
[CAMEL-6095]: https://issues.apache.org/jira/browse/CAMEL-6095
[CAMEL-6336]: https://issues.apache.org/jira/browse/CAMEL-6336
[CAMEL-6338]: https://issues.apache.org/jira/browse/CAMEL-6338
[CAMEL-6937]: https://issues.apache.org/jira/browse/CAMEL-6937
[CAMEL-7760]: https://issues.apache.org/jira/browse/CAMEL-7760
[CAMEL-8325]: https://issues.apache.org/jira/browse/CAMEL-8325
[CAMEL-9336]: https://issues.apache.org/jira/browse/CAMEL-9336
[container lifecycle events]: http://docs.jboss.org/cdi/spec/1.2/cdi-spec.html#init_events
[Assignability of type variables, raw and parameterized types]: http://docs.jboss.org/cdi/spec/1.2/cdi-spec.html#observers_assignability
[Java EE Configuration JSR]: http://javaeeconfig.blogspot.ch/

## Getting Started

#### Using Maven

Add the `camel-cdi` library as a dependency:

```xml
<dependency>
    <groupId>io.astefanutti.camel.cdi</groupId>
    <artifactId>camel-cdi</artifactId>
    <version>1.2.0</version>
</dependency>
```

#### Required Dependencies

Besides depending on Camel (`camel-core` and `camel-core-osgi` optionally), Camel CDI requires a CDI enabled environment running in Java 8 or greater.

#### Supported Containers

This version of Camel CDI is currently successfully tested with the following containers:

| Container             | Version        | Environment                        |
| --------------------- | -------------- | ---------------------------------- |
| [Weld][]              | `2.4.0.Final`  | Java SE 8 / [CDI 1.2][JSR 346 1.2] |
| [OpenWebBeans][]      | `1.7.0`        | Java SE 8 / [CDI 1.2][JSR 346 1.2] |
| [WildFly 8][WildFly]  | `8.2.1.Final`  | [Java EE 7][]                      |
| [WildFly 9][WildFly]  | `9.0.2.Final`  | [Java EE 7][]                      |
| [WildFly 10][WildFly] | `10.0.0.Final` | [Java EE 7][]                      |
| [WildFly Camel][]     | `4.2.1`        | [Java EE 7][]                      |
| [Karaf][]<br/>[PAX CDI Weld][] | `4.0.4`<br/>`1.0.0.RC1` | [OSGi 6][] |

WildFly 8.1 requires to be patched with Weld 2.2+ as documented in [Weld 2.2 on WildFly][].

[Karaf]: https://karaf.apache.org
[OpenWebBeans]: http://openwebbeans.apache.org/
[PAX CDI Weld]: https://ops4j1.jira.com/wiki/display/PAXCDI/Pax+CDI
[Weld]: http://weld.cdi-spec.org/
[WildFly]: http://www.wildfly.org/
[WildFly Camel]: https://github.com/wildfly-extras/wildfly-camel

[Java EE 7]: https://jcp.org/en/jsr/detail?id=342
[OSGi 6]: https://www.osgi.org/osgi-release-6-javadoc/

[Weld 2.2 on WildFly]: http://weld.cdi-spec.org/news/2014/04/15/weld-220-final/

## Usage

#### CDI Event Camel Endpoint

The CDI event endpoint bridges the [CDI events][] facility with the Camel routes so that CDI events can be seamlessly observed / consumed (respectively produced / fired) from Camel consumers (respectively by Camel producers).

The `CdiEventEndpoint<T>` bean can be used to observe / consume CDI events whose _event type_ is `T`, for example:

```java
@Inject
CdiEventEndpoint<String> cdiEventEndpoint;

from(cdiEventEndpoint).log("CDI event received: ${body}");
```

This is equivalent to writing:

```java
@Inject
@Uri("direct:event")
ProducerTemplate producer;

void observeCdiEvents(@Observes String event) {
    producer.sendBody(event);
}

from("direct:event").log("CDI event received: ${body}");
```

Conversely, the `CdiEventEndpoint<T>` bean can be used to produce / fire CDI events whose _event type_ is `T`, for example:

```java
@Inject
CdiEventEndpoint<String> cdiEventEndpoint;

from("direct:event").to(cdiEventEndpoint).log("CDI event sent: ${body}");
```

This is equivalent to writing:

```java
@Inject
Event<String> event;

from("direct:event").process(new Processor() {
    @Override
    public void process(Exchange exchange) {
        event.fire(exchange.getBody(String.class));
    }
}).log("CDI event sent: ${body}");
```

Or using a Java 8 lambda expression:
```java
@Inject
Event<String> event;

from("direct:event")
    .process(exchange -> event.fire(exchange.getIn().getBody(String.class)))
    .log("CDI event sent: ${body}");
```

The type variable `T`, respectively the qualifiers, of a particular `CdiEventEndpoint<T>` injection point are automatically translated into the parameterized _event type_, respectively into the _event qualifiers_, e.g.:

```java
@Inject
@FooQualifier
CdiEventEndpoint<List<String>> cdiEventEndpoint;

from("direct:event").to(cdiEventEndpoint);

void observeCdiEvents(@Observes @FooQualifier List<String> event) {
    logger.info("CDI event: {}", event);
}
```

When multiple Camel contexts exist in the CDI container, the `@ContextName` qualifier can be used to qualify the `CdiEventEndpoint<T>` injection points, e.g.:

```java
@Inject
@ContextName("foo")
CdiEventEndpoint<List<String>> cdiEventEndpoint;
// Only observes / consumes events having the @ContextName("foo") qualifier
from(cdiEventEndpoint).log("Camel context 'foo' > CDI event received: ${body}");
// Produces / fires events with the @ContextName("foo") qualifier
from("...").to(cdiEventEndpoint);

void observeCdiEvents(@Observes @ContextName("foo") List<String> event) {
    logger.info("Camel context 'foo' > CDI event: {}", event);
}
```

Note that the CDI event Camel endpoint dynamically adds an [observer method][] for each unique combination of _event type_ and _event qualifiers_ and solely relies on the container typesafe [observer resolution][], which leads to an implementation as efficient as possible.

Besides, as the impedance between the _typesafe_ nature of CDI and the _dynamic_ nature of the [Camel component][] model is quite high, it is not possible to create an instance of the CDI event Camel endpoint via [URIs][]. Indeed, the URI format for the CDI event component is:

```
cdi-event://PayloadType<T1,...,Tn>[?qualifiers=QualifierType1[,...[,QualifierTypeN]...]]
```

With the authority `PayloadType` (respectively the `QualifierType`) being the URI escaped fully qualified name of the payload (respectively qualifier) raw type followed by the type parameters section delimited by angle brackets for payload parameterized type. Which leads to _unfriendly_ URIs, e.g.:

```
cdi-event://org.apache.camel.cdi.se.pojo.EventPayload%3Cjava.lang.Integer%3E?qualifiers=org.apache.camel.cdi.se.qualifier.FooQualifier%2Corg.apache.camel.cdi.se.qualifier.BarQualifier
```

But more fundamentally, that would prevent efficient binding between the endpoint instances and the observer methods as the CDI container doesn't have any ways of discovering the Camel context model during the deployment phase.

[CDI events]: http://docs.jboss.org/cdi/spec/1.2/cdi-spec.html#events
[observer method]: http://docs.jboss.org/cdi/spec/1.2/cdi-spec.html#observer_methods
[observer resolution]: http://docs.jboss.org/cdi/spec/1.2/cdi-spec.html#observer_resolution
[Camel component]: http://camel.apache.org/component.html
[URIs]: http://camel.apache.org/uris.html

#### Camel Events to CDI Events

Camel provides a set of [management events][] that can be subscribed to for listening to Camel context, service, route and exchange events. This version of Camel CDI seamlessly translates these Camel events into CDI events that can be observed using CDI [observer methods][], e.g.:

```java
void onContextStarting(@Observes CamelContextStartingEvent event) {
    // Called before the default Camel context is about to start
}

```

When multiple Camel contexts exist in the CDI container, the `@ContextName` qualifier can be used to refine the observer method resolution to a particular Camel context as specified in [observer resolution][], e.g.:

```java
void onRouteStarted(@Observes @ContextName("first") RouteStartedEvent event) {
    // Called after the route (event.getRoute()) for the
    // Camel context ("first") has started
}

```

Similarly, the `@Default` qualifier can be used to observe Camel events for the _default_ Camel context if multiples contexts exist, e.g.:

```java
void onExchangeCompleted(@Observes @Default ExchangeCompletedEvent event) {
    // Called after the exchange (event.getExchange()) processing has completed
}

```

In that example, if no qualifier is specified, the `@Any` qualifier is implicitly assumed, so that corresponding events for all the Camel contexts deployed get received.

Note that the support for Camel events translation into CDI events is only activated if observer methods listening for Camel events are detected in the deployment, and that per Camel context.

[management events]: http://camel.apache.org/maven/current/camel-core/apidocs/org/apache/camel/management/event/package-summary.html
[observer methods]: http://docs.jboss.org/cdi/spec/1.2/cdi-spec.html#observer_methods

#### Type Converter Beans

CDI beans annotated with the `@Converter` annotation are automatically registered into all the deployed Camel contexts, e.g.:

```java
@Converter
public class TypeConverter {

    @Converter
    public Output convert(Input input) {
        //...
    }
}
```
Note that CDI injection is supported within the type converters.

#### Multiple Camel Contexts

The `@ContextName` qualifier can be used to declared multiple Camel contexts, e.g.:

```java
@ApplicationScoped
@ContextName("foo")
class FooCamelContext extends DefaultCamelContext {

}

@ApplicationScoped
@ContextName("bar")
class BarCamelContext extends DefaultCamelContext {

}
```

And then use that same qualifier to declare [injected fields][], e.g.:

```java
@Inject
@ContextName("foo")
CamelContext fooCamelContext;

@Inject
@ContextName("bar")
CamelContext fooCamelContext;
```

Note that Camel CDI provides the `@ContextName` qualifier for convenience though any [CDI qualifiers][] can be used to declare the Camel context beans and the injection points.

[CDI qualifiers]: http://docs.jboss.org/cdi/spec/1.2/cdi-spec.html#qualifiers
[injected fields]: http://docs.jboss.org/cdi/spec/1.2/cdi-spec.html#injected_fields

#### Configuration Properties

Camel CDI relies on _standard_ Camel abstractions and CDI mechanisms. The configuration sourcing concern is delegated to the application so that it can provide any `PropertiesComponent` bean that's tailored for its need, e.g.:

```java
@Produces
@ApplicationScoped
@Named("properties")
PropertiesComponent propertiesComponent() {
    Properties properties = new Properties();
    properties.put("property", "value");
    PropertiesComponent component = new PropertiesComponent();
    component.setInitialProperties(properties);
    component.setLocation("classpath:placeholder.properties");
    return component;
}

```

#### Camel Context Customization

Any `CamelContext` class can be used to declare a custom Camel context bean that uses the `@PostConstruct` and `@PreDestroy` lifecycle callbacks, e.g.:

```java
@ApplicationScoped
class CustomCamelContext extends DefaultCamelContext {

    @PostConstruct
    void customize() {
        // Sets the Camel context name
        setName("custom");
        // Adds properties location
        getComponent("properties", PropertiesComponent.class)
            .setLocation("classpath:placeholder.properties");
    }

    @PreDestroy
    void cleanUp() {
        // ...
    }
}
```

[Producer][producer method] and [disposer][disposer method] methods can be used as well to customize the Camel context bean, e.g.:

```java
class CamelContextFactory {

    @Produces
    @ApplicationScoped
    CamelContext customize() {
        DefaultCamelContext context = new DefaultCamelContext();
        context.setName("custom");
        return context;
    }

    void cleanUp(@Disposes CamelContext context) {
        // ...
    }
}
```

Similarly, [producer fields][producer field] can be used, e.g.:

```java
@Produces
@ApplicationScoped
CamelContext context = new CustomCamelContext();

class CustomCamelContext extends DefaultCamelContext {

    CustomCamelContext() {
        setName("custom");
    }
}
```

This pattern can be used to avoid having the Camel context started automatically at deployment time by calling the `setAutoStartup` method, e.g.:

```java
@ApplicationScoped
class ManualStartupCamelContext extends DefaultCamelContext {

    @PostConstruct
    void manual() {
        setAutoStartup(false);
    }
}

```

[producer method]: http://docs.jboss.org/cdi/spec/1.2/cdi-spec.html#producer_method
[disposer method]: http://docs.jboss.org/cdi/spec/1.2/cdi-spec.html#disposer_method
[producer field]: http://docs.jboss.org/cdi/spec/1.2/cdi-spec.html#producer_field

#### Camel Route Builders

Camel CDI detects any beans of type `RouteBuilder` and automatically adds the declared routes to the corresponding Camel context at deployment time, e.g.:

```java
@ContextName("foo")
class FooCamelContextRoute extends RouteBuilder {

    @Override
    public void configure() {
        from("direct:inbound")
            .setHeader("context").constant("foo")
            .to("mock:outbound");
    }
}
```

#### Camel CDI Beans

Camel CDI declares some producer method beans that can be used to inject Camel objects of types `Endpoint`, `MockEndpoint`, `ProducerTemplate` and `TypeConverter`, e.g.:

```java
@Inject
@Uri("direct:inbound")
ProducerTemplate producerTemplate;

@Inject
MockEndpoint outbound; // URI defaults to the member name, i.e. mock:outbound

@Inject
@Uri("direct:inbound")
Endpoint endpoint;

@Inject
@ContextName("foo")
TypeConverter converter;
```

#### Camel Annotations Support

Camel comes with a set of [annotations][Camel annotations] that are supported by Camel CDI for both standard CDI injection and Camel [bean integration][], e.g.:

```java
@PropertyInject("property")
String property;

@Produce(uri = "mock:outbound")
ProducerTemplate producer;

// Equivalent to:
// @Inject @Uri("direct:inbound")
// Endpoint endpoint;
@EndpointInject(uri = "direct:inbound")
Endpoint endpoint;

// Equivalent to:
// @Inject @ContextName("foo") @Uri("direct:inbound")
// Endpoint contextEndpoint;
@EndpointInject(uri = "direct:inbound", context = "foo")
Endpoint contextEndpoint;

// Equivalent to:
// @Inject MyBean bean;
@BeanInject
MyBean bean;

@Consume(uri = "seda:inbound")
void consume(@Body String body) {
    //...
}
```

[Camel annotations]: http://camel.apache.org/bean-integration.html#BeanIntegration-Annotations
[bean integration]: http://camel.apache.org/bean-integration.html

#### Black Box Camel Contexts

The [context component][] enables the creation of Camel components out of Camel contexts and the mapping of local endpoints within these components from other Camel contexts based on the identifiers used to register these  _black box_ Camel contexts in the Camel registry.

For example, given the two Camel contexts declared as CDI beans:

```java
@ApplicationScoped
@Named("blackbox")
@ContextName("foo")
class FooCamelContext extends DefaultCamelContext {

}
```

```java
@ApplicationScoped
@ContextName("bar")
class BarCamelContext extends DefaultCamelContext {

}
```

With the `foo` Camel context being registered into the Camel registry as `blackbox` by annotating it with the `@Named("blackbox")` qualifier, and the following route being added to it:

```java
@ContextName("foo")
FooRouteBuilder extends RouteBuilder {

    @Override
    public void configure() {
        from("direct:in")/*...*/.to("direct:out");
    }
}
```

It is possible to refer to the local endpoints of `foo` from the `bar` Camel context route:

```java
@ContextName("bar")
BarRouteBuilder extends RouteBuilder {

    @Override
    public void configure() {
        from("...").to("blackbox:in");
        //...
        from("blackbox:out").to("...");
    }
}
```

[context component]: http://camel.apache.org/context.html

## License

Copyright © 2014-2016, Antonin Stefanutti

Published under Apache Software License 2.0, see LICENSE
