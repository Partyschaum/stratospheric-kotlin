package com.shinythings

import dev.stratospheric.cdk.DockerRepository
import software.amazon.awscdk.App
import software.amazon.awscdk.Stack
import software.amazon.awscdk.StackProps

fun main() {
    val app = App()

    val accountId = app.node["accountId"]
    val region = app.node["region"]
    val applicationName = app.node["applicationName"]

    val awsEnvironment = makeEnv(accountId, region)

    val dockerRepositoryStack = Stack(
        app,
        "DockerRepositoryStack",
        StackProps.builder()
            .stackName("$applicationName-DockerRepository")
            .env(awsEnvironment)
            .build(),
    )

    DockerRepository(
        dockerRepositoryStack,
        "DockerRepository",
        awsEnvironment,
        DockerRepository.DockerRepositoryInputParameters(
            applicationName,
            accountId,
            10,
            false
        ),
    )

    app.synth()
}
