package com.gradle.develocity.api;

import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;

public class ProjectMetrics {

    static public Counter bDurationMetric = Counter.build()
            .name("build_duration_counter")
            .help("Duration of the build")
            .labelNames("project","local_cache","remote_cache")
            .register();
    static public Counter bDNumberMetric = Counter.build()
            .name("build_duration_number_counter")
            .help("number of the build")
            .labelNames("project","local_cache","remote_cache")
            .register();
    static public Gauge buildDurationMetric = Gauge.build()
            .name("build_duration")
            .help("Duration of the build")
            .labelNames("project","local_cache","remote_cache")
            .register();
}
