/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.cdi.sample.metrics;

import com.codahale.metrics.MetricRegistry;
import io.astefanutti.metrics.cdi.MetricsConfiguration;
import org.apache.camel.component.metrics.MetricsComponent;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.Produces;
import javax.inject.Named;
import javax.inject.Singleton;

class MetricsCdiConfig {

    @Produces
    @Singleton
    @Named(MetricsComponent.METRIC_REGISTRY_NAME)
    // TODO: change to @ApplicationScoped when WELD-2083 is fixed, and remove when Camel Metrics component looks up for the Metrics registry by type only
    MetricRegistry registry = new MetricRegistry();

    static void configure(@Observes MetricsConfiguration config) {
        config.useAbsoluteName(true);
    }
}
