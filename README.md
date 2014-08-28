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
[JSR 346]: https://jcp.org/en/jsr/detail?id=346
[JSR 346 1.1]: https://jcp.org/aboutJava/communityprocess/final/jsr346/index.html
[JSR 346 1.2]: https://jcp.org/aboutJava/communityprocess/mrel/jsr346/index.html
[CDI 1.1]: http://docs.jboss.org/cdi/spec/1.1/cdi-spec.html
[CDI 1.2]: http://docs.jboss.org/cdi/spec/1.2/cdi-spec.html

## About

Since version `2.10` of Camel, the [Camel CDI][] component supports the integration of Camel in CDI enabled environments. However, some experiments and _battlefield_ tests prove it troublesome to use because of the following concerns:
+ It relies on Apache [DeltaSpike][] and its `BeanManagerProvider` class to retrieve the `BeanManager` instance during the CDI container initialisation. That may not be suitable in complex container scenarios, for example, in multi CDI container per JVM context, as reported in [CAMEL-6338][] and that causes [CAMEL-6095][] and [CAMEL-6937][].
+ It relies on _DeltaSpike_ and its [configuration mechanism][DeltaSpike Configuration Mechanism] to source configuration locations for the [Properties component][]. While this is suitable for most use cases, it relies on the `ServiceLoader` mechanism to support custom [configuration sources][ConfigSource] that may not be suitable in more complex container scenarios and relates to [CAMEL-5986].
+ Besides, while _DeltaSpike_ is a valuable addition to the CDI ecosystem, having a direct dependency on it is questionable from a design standpoint as opposed to relying on standard CDI mechanism for producing the Camel configuration properties and delegating the configuration sourcing concern and implementation choice to the application itself.
+ It declares a `CamelContext` CDI bean that's automatically instantiated and started with a `@PostConstruct` lifecycle callback called before the CDI container is completely initialized. That prevents, among other side effects, proper advising of Camel routes as documented in [Camel AdviceWith][].
+ It uses the `@ContextName` annotation to bind routes to the `CamelContext` instance specified by name as an attempt to provide multi-context support. Though that is an uncompleted feature as discussed in [CAMEL-5566][] and [CAMEL-5742][] which reduces significantly the code understandability.

The objective of this project is to alleviate all these concerns, provide additional features, and have that improved version of the CDI Camel component contributed back in the official codeline.

[Camel CDI]: http://camel.apache.org/cdi.html
[DeltaSpike]: https://deltaspike.apache.org/
[DeltaSpike Configuration Mechanism]: https://deltaspike.apache.org/configuration.html
[ConfigSource]: https://deltaspike.apache.org/configuration.html#custom-config-sources
[Camel AdviceWith]: http://camel.apache.org/advicewith.html
[Properties component]: http://camel.apache.org/properties
[CAMEL-5566]: CAMEL-5566
[CAMEL-5742]: CAMEL-5742
[CAMEL-5986]: https://issues.apache.org/jira/browse/CAMEL-5986
[CAMEL-6338]: https://issues.apache.org/jira/browse/CAMEL-6338
[CAMEL-6095]: https://issues.apache.org/jira/browse/CAMEL-6095
[CAMEL-6937]: https://issues.apache.org/jira/browse/CAMEL-6937

## Getting Started

#### Supported Containers

_Camel CDI_ is currently successfully tested with the following containers:

| Container        | Version       | Specification          | Arquillian Container Adapter      |
| ---------------- | ------------- | ---------------------- | --------------------------------- |
| [Weld SE][]      | `2.2.4.Final` | [CDI 1.2][JSR 346 1.2] | `arquillian-weld-se-embedded-1.1` |
| [OpenWebBeans][] | `2.0.0`       | [CDI 1.1][JSR 346 1.1] | `owb-arquillian-standalone`       |

[Weld SE]: http://weld.cdi-spec.org/
[OpenWebBeans]: http://openwebbeans.apache.org/

## License

Copyright © 2013-2014, Antonin Stefanutti

Published under Apache Software License 2.0, see LICENSE
