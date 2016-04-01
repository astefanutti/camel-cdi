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
package org.apache.camel.cdi.osgi;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import org.apache.camel.CamelContext;
import org.apache.camel.ServiceStatus;
import org.apache.camel.component.metrics.MetricsComponent;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

import javax.inject.Inject;

import static org.apache.camel.cdi.osgi.CommonPaxExamOptions.CAMEL_CDI;
import static org.apache.camel.cdi.osgi.CommonPaxExamOptions.CAMEL_METRICS;
import static org.apache.camel.cdi.osgi.CommonPaxExamOptions.KARAF;
import static org.apache.camel.cdi.osgi.CommonPaxExamOptions.METRICS_CDI;
import static org.apache.camel.cdi.osgi.CommonPaxExamOptions.PAX_CDI_IMPL;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.when;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.debugConfiguration;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class MetricsPaxExamTest {

    @Configuration
    public Option[] config() {
        return options(
            KARAF.option(),
            PAX_CDI_IMPL.option(),
            CAMEL_CDI.option(),
            // Metrics sample
            METRICS_CDI.option(),
            CAMEL_METRICS.option(),
            mavenBundle()
                .groupId("io.astefanutti.camel.cdi")
                .artifactId("camel-cdi-sample-metrics")
                .versionAsInProject(),
            when(false)
                .useOptions(
                    debugConfiguration("5005", true))
        );
    }

    @Inject
    private CamelContext context;

    @Test
    public void test() throws Exception {
        assertThat("Context name is incorrect!", context.getName(), equalTo("camel-cdi-metrics"));

        assertThat("Route status is incorrect!", context.getRouteStatus("unreliable-service"), equalTo(ServiceStatus.Started));

        // Wait a while so that the timer can kick in
        Thread.sleep(10000L);

        // We need to retrieve the metrics registry before we stop the Camel context otherwise the component is removed
        MetricRegistry registry = context.getComponent("metrics", MetricsComponent.class).getMetricRegistry();

        // And stop the Camel context so that inflight exchanges get completed
        context.stop();

        Meter generated = registry.meter("generated");
        Meter attempt = registry.meter("attempt");
        Meter success = registry.meter("success");
        Meter redelivery = registry.meter("redelivery");
        Meter error = registry.meter("error");
        Gauge ratio = registry.getGauges().get("success-ratio");

        assertThat("Meter counts are not consistent!", attempt.getCount() - redelivery.getCount() - success.getCount() - error.getCount(), equalTo(0L));

        assertThat("Success rate gauge value is incorrect!", ratio.getValue(), equalTo(success.getOneMinuteRate() / generated.getOneMinuteRate()));
    }
}
