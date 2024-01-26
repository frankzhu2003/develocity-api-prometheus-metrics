package com.gradle.develocity.api;

import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;
import io.prometheus.client.Histogram;

public class ProjectMetrics {

    static public Counter bDurationMetric = Counter.build()
            .name("build_duration_counter")
            .help("Duration of the build")
            .labelNames("project","local_cache","remote_cache", "parallel", "run_env", "build_tool")
            .register();
    static public Counter bDNumberMetric = Counter.build()
            .name("build_duration_number_counter")
            .help("number of the build")
            .labelNames("project","local_cache","remote_cache", "parallel", "run_env", "build_tool")
            .register();
    static public Gauge buildDurationMetric = Gauge.build()
            .name("build_duration")
            .help("Duration of the build")
            .labelNames("project","local_cache","remote_cache", "parallel", "run_env", "build_tool")
            .register();

    static public Histogram buildCacheMetric = Histogram.build()
            .name("cache_hit_ratio")
            .help("Cache hit percentage")
            .labelNames("project","local_cache","remote_cache", "parallel", "run_env", "build_tool")
            .buckets(10, 20, 30, 40, 50, 60, 70, 80, 90)
            .register();

    static public Histogram buildCacheAvoidanceMetric = Histogram.build()
            .name("avoidance_saving_ratio")
            .help("Avoidance savings ratio")
            .labelNames("project","local_cache","remote_cache", "parallel", "run_env", "build_tool")
            .buckets(10, 20, 30, 40, 50, 60, 70, 80, 90)
            .register();
}
