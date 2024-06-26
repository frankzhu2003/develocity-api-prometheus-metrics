package com.gradle.develocity.api.builds;

import com.gradle.develocity.api.ProjectMetrics;
import com.gradle.enterprise.api.GradleEnterpriseApi;
import com.gradle.enterprise.api.client.ApiException;
import com.gradle.enterprise.api.model.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

final class BuildCacheBuildProcessor implements BuildProcessor {

    private static final Set<GradleBuildCachePerformanceTaskExecutionEntry.AvoidanceOutcomeEnum> GRADLE_CACHE_HIT_TYPES = new HashSet<>();
    private static final Set<MavenBuildCachePerformanceGoalExecutionEntry.AvoidanceOutcomeEnum> MAVEN_CACHE_HIT_TYPES = new HashSet<>();

    static {
        GRADLE_CACHE_HIT_TYPES.add(GradleBuildCachePerformanceTaskExecutionEntry.AvoidanceOutcomeEnum.AVOIDED_FROM_LOCAL_CACHE);
        GRADLE_CACHE_HIT_TYPES.add(GradleBuildCachePerformanceTaskExecutionEntry.AvoidanceOutcomeEnum.AVOIDED_FROM_REMOTE_CACHE);
        MAVEN_CACHE_HIT_TYPES.add(MavenBuildCachePerformanceGoalExecutionEntry.AvoidanceOutcomeEnum.AVOIDED_FROM_LOCAL_CACHE);
        MAVEN_CACHE_HIT_TYPES.add(MavenBuildCachePerformanceGoalExecutionEntry.AvoidanceOutcomeEnum.AVOIDED_FROM_REMOTE_CACHE);
    }

    private final GradleEnterpriseApi api;
    private final String projectName;

    BuildCacheBuildProcessor(GradleEnterpriseApi api, String projectName) {
        this.api = api;
        this.projectName = projectName;
    }

    @Override
    public void process(Build build) {
        try {
            switch (build.getBuildToolType()) {
                case "gradle":
                    processGradleBuild(build);
                    break;
                case "maven":
                    processMavenBuild(build);
                    break;
                default:
                    System.out.println("Unsupported build tool type received - " + build.getBuildToolType());
            }
        } catch (ApiException e) {
            reportError(build, e);
        }
    }

    private void processMavenBuild(Build build) throws ApiException {
        MavenAttributes attributes = api.getMavenAttributes(build.getId(), new BuildModelQuery());

        String  ciTag = "local";
        for (String tag : attributes.getTags())  {
            if (tag.equalsIgnoreCase("ci")) {
                ciTag = "ci";
                break;
            }
        }

        if (projectName == null || projectName.equals(attributes.getTopLevelProjectName())) {
            MavenBuildCachePerformance model = api.getMavenBuildCachePerformance(build.getId(), new BuildModelQuery());
            reportBuild(
                build,
                computeCacheHitPercentage(model),
                computeAvoidanceSavingsRatioPercentage(model),
                attributes.getTopLevelProjectName(),
                attributes.getBuildDuration(),
                attributes.getEnvironment().getUsername()
            );

            String project = "null-value";
            if (attributes.getTopLevelProjectName() != null){
                project = attributes.getTopLevelProjectName();
            }

            //TODO fzhu code
            postMetrics(
                    build,
                    getMavenLocalCache(model),
                    getMavenRemoteCache(model),
                    project,
                    attributes.getBuildDuration(),
                    attributes.getBuildOptions().getMaxNumberOfThreads() > 0,
                    ciTag,
                    "maven",
                    computeCacheHitPercentage(model),
                    computeAvoidanceSavingsRatioPercentage(model)
            );
        }
    }

    private void processGradleBuild(Build build) throws ApiException {
        GradleAttributes attributes = api.getGradleAttributes(build.getId(), new BuildModelQuery());

        String  ciTag = "local";
        for (String tag : attributes.getTags())  {
            if (tag.equalsIgnoreCase("ci")) {
                ciTag = "ci";
                break;
            }
        }

        if (projectName == null || projectName.equals(attributes.getRootProjectName())) {
            GradleBuildCachePerformance model = api.getGradleBuildCachePerformance(build.getId(), new BuildModelQuery());
            reportBuild(
                build,
                computeCacheHitPercentage(model),
                computeAvoidanceSavingsRatioPercentage(model),
                attributes.getRootProjectName(),
                attributes.getBuildDuration(),
                attributes.getEnvironment().getUsername()
            );

            //TODO fzhu code
            String project = "null-value";
            if (attributes.getRootProjectName() != null){
                project = attributes.getRootProjectName();
            }

            postMetrics(
                    build,
                    getGradleLocalCache(model),
                    getGradleRemoteCache(model),
                    project,
                    attributes.getBuildDuration(),
                    attributes.getBuildOptions().getParallelProjectExecutionEnabled(),
                    ciTag,
                    "gradle",
                    computeCacheHitPercentage(model),
                    computeAvoidanceSavingsRatioPercentage(model)
            );
        }
    }

    private void reportBuild(Build build, BigDecimal cacheHitPercentage, BigDecimal avoidanceSavingsRatioPercentage, String rootProjectName, Long buildDuration, String username) {
        System.out.printf("Build Scan | %s | Project: %s | 🗓  %s | ⏱  %s ms\t| 👤 %s%n - \tCache hit percentage: %s%%%n - \tAvoidance savings ratio: %s%%%n%n",
            buildScanUrl(build),
            rootProjectName,
            Instant.ofEpochMilli(build.getAvailableAt()).toString(),
            buildDuration,
            username,
            cacheHitPercentage,
            avoidanceSavingsRatioPercentage
        );
    }

    //TODO fzhu code
    private void postMetrics(Build build, Boolean localCache, Boolean remoteCache, String rootProjectName, Long buildDuration, Boolean parallel, String ciTag, String buildTool, BigDecimal cacheRate, BigDecimal avoidanceRate) {
//        System.out.println("************** post metrics *****************");
//        System.out.println("************** build scan "+ buildScanUrl(build));

        ProjectMetrics.buildDurationMetric.labels(rootProjectName, localCache?"Yes":"No", remoteCache?"Yes":"No", parallel?"Yes":"No", ciTag, buildTool).set(buildDuration);
        ProjectMetrics.bDurationMetric.labels(rootProjectName, localCache?"Yes":"No", remoteCache?"Yes":"No", parallel?"Yes":"No", ciTag, buildTool).inc(buildDuration);
        ProjectMetrics.bDNumberMetric.labels(rootProjectName, localCache?"Yes":"No", remoteCache?"Yes":"No", parallel?"Yes":"No", ciTag, buildTool).inc(1);
        ProjectMetrics.buildCacheMetric.labels(rootProjectName, localCache?"Yes":"No", remoteCache?"Yes":"No", parallel?"Yes":"No", ciTag, buildTool).observe(cacheRate.toBigInteger().doubleValue());
        ProjectMetrics.buildCacheAvoidanceMetric.labels(rootProjectName, localCache?"Yes":"No", remoteCache?"Yes":"No", parallel?"Yes":"No", ciTag, buildTool).observe(avoidanceRate.toBigInteger().doubleValue());

    }

    //TODO fzhu code
    private static boolean getGradleLocalCache(GradleBuildCachePerformance model) {

        try {
            assert model.getBuildCaches() != null;
            return model.getBuildCaches().getLocal().getIsEnabled();
        }catch (Exception e){
            return false;
        }
    }

    //TODO fzhu code
    private static boolean getGradleRemoteCache(GradleBuildCachePerformance model) {

        try {
            assert model.getBuildCaches() != null;
            return model.getBuildCaches().getRemote().getIsEnabled();
        }catch (Exception e){
            return false;
        }
    }

    //TODO fzhu code
    private static boolean getMavenLocalCache(MavenBuildCachePerformance model) {

        try {
            assert model.getBuildCaches().getLocal().getIsEnabled() != null;
            return model.getBuildCaches().getLocal().getIsEnabled();
        }catch (Exception e){
            return false;
        }
    }

    //TODO fzhu code
    private static boolean getMavenRemoteCache(MavenBuildCachePerformance model) {

        try {
            assert model.getBuildCaches().getLocal().getIsEnabled() != null;
            return model.getBuildCaches().getRemote().getIsEnabled();
        }catch (Exception e){
            return false;
        }
    }

    private void reportError(Build build, ApiException e) {
        System.err.printf("API Error %s for Build Scan ID %s%n%s%n", e.getCode(), build.getId(), e.getResponseBody());
        ApiProblemParser.maybeParse(e, api.getApiClient().getObjectMapper())
            .ifPresent(apiProblem -> {
                // Types of API problems can be checked as following
                if (apiProblem.getType().equals("urn:gradle:enterprise:api:problems:build-deleted")) {
                    // Handle the case when the Build Scan is deleted.
                    System.err.println(apiProblem.getDetail());
                }
            });
    }

    private URI buildScanUrl(Build build) {
        return URI.create(api.getApiClient().getBasePath() + "/s/" + build.getId());
    }

    private static BigDecimal computeAvoidanceSavingsRatioPercentage(GradleBuildCachePerformance gradleBuildCachePerformanceModel) {
        return toPercentage(gradleBuildCachePerformanceModel.getAvoidanceSavingsSummary().getRatio());
    }

    private static BigDecimal computeAvoidanceSavingsRatioPercentage(MavenBuildCachePerformance mavenBuildCachePerformanceModel) {
        return toPercentage(mavenBuildCachePerformanceModel.getAvoidanceSavingsSummary().getRatio());
    }

    private static BigDecimal computeCacheHitPercentage(GradleBuildCachePerformance model) {
        int numTasks = model.getTaskExecution().size();
        long numAvoidedTasks = model.getTaskExecution().stream()
            .filter(task -> GRADLE_CACHE_HIT_TYPES.contains(task.getAvoidanceOutcome()))
            .count();

        return toPercentage(numTasks, numAvoidedTasks);
    }

    private static BigDecimal computeCacheHitPercentage(MavenBuildCachePerformance model) {
        int numGoals = model.getGoalExecution().size();
        long numAvoidedGoals = model.getGoalExecution().stream()
            .filter(goal -> MAVEN_CACHE_HIT_TYPES.contains(goal.getAvoidanceOutcome()))
            .count();

        return toPercentage(numGoals, numAvoidedGoals);
    }

    private static BigDecimal toPercentage(Double ratio) {
        return toPercentage(BigDecimal.valueOf(ratio));
    }

    private static BigDecimal toPercentage(BigDecimal ratio) {
        return ratio.multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal toPercentage(long total, long portion) {
        if (total == 0) {
            return BigDecimal.ZERO;
        } else {
            return toPercentage(BigDecimal.valueOf(portion).divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP));
        }
    }

}
