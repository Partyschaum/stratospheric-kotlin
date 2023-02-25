package com.shinythings

import dev.stratospheric.cdk.ApplicationEnvironment
import dev.stratospheric.cdk.Network
import dev.stratospheric.cdk.Service
import software.amazon.awscdk.App
import software.amazon.awscdk.Stack
import software.amazon.awscdk.StackProps

fun main() {
    val app = App()

    val accountId = app.node["accountId"]
    val region = app.node["region"]
    val applicationName = app.node["applicationName"]
    val environmentName = app.node["environmentName"]
    val springProfile = app.node["springProfile"]
    val dockerImageTag = app.node["dockerImageTag"]
    val dockerImageRepositoryName = app.node["dockerImageRepositoryName"]

    val awsEnvironment = makeEnv(accountId, region)

    val applicationEnvironment = ApplicationEnvironment(applicationName, environmentName)

    val serviceStack = Stack(
        app,
        "ServiceStack",
        StackProps.builder()
            .stackName(applicationEnvironment.prefix("Service"))
            .env(awsEnvironment)
            .build()
    )

    val networkOutputParameters =
        Network.getOutputParametersFromParameterStore(serviceStack, applicationEnvironment.environmentName)

    val dockerImageSource = Service.DockerImageSource(dockerImageRepositoryName, dockerImageTag)
    val environmentVariables = mapOf("SPRING_PROFILES_ACTIVE" to springProfile)
    val serviceInputParameters = Service.ServiceInputParameters(dockerImageSource, environmentVariables)
        .withHealthCheckIntervalSeconds(30)

    Service(
        serviceStack,
        "Service",
        awsEnvironment,
        applicationEnvironment,
        serviceInputParameters,
        networkOutputParameters,
    )

    app.synth()
}
