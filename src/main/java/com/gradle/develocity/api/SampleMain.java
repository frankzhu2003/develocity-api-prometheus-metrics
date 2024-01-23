package com.gradle.develocity.api;

import com.gradle.develocity.api.builds.BuildsApiSample;
import com.gradle.develocity.api.tests.TestsApiSample;
import io.prometheus.client.exporter.HTTPServer;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.HelpCommand;

import java.io.IOException;

@Command(
    name = "develocity-api-samples",
    description = "A program that demonstrates using the Develocity API to extract build and tests data",
    synopsisHeading = "%n@|bold Usage:|@ ",
    optionListHeading = "%n@|bold Options:|@%n",
    commandListHeading = "%n@|bold Commands:|@%n",
    parameterListHeading = "%n@|bold Parameters:|@%n",
    descriptionHeading = "%n",
    synopsisSubcommandLabel = "COMMAND",
    usageHelpAutoWidth = true,
    usageHelpWidth = 120,
    subcommands = {BuildsApiSample.class, TestsApiSample.class, HelpCommand.class}
)
public final class SampleMain {

    public static void main(final String[] args) throws Exception, IOException {

        //TODO fzhu code
        // Prometheus metrics point
        HTTPServer server = new HTTPServer(8081);
        System.out.println("******** local metrics server started ***********");

        //noinspection InstantiationOfUtilityClass
        System.exit(new CommandLine(new SampleMain()).execute(args));
    }

}
