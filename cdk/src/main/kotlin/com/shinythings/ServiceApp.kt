package com.shinythings

import dev.stratospheric.cdk.ApplicationEnvironment
import dev.stratospheric.cdk.Network
import dev.stratospheric.cdk.Service
import software.amazon.awscdk.App
import software.amazon.awscdk.Stack
import software.amazon.awscdk.StackProps
import software.amazon.awscdk.services.iam.Effect
import software.amazon.awscdk.services.iam.PolicyStatement

fun main() {
    val app = App()

    val accountId = app.node["accountId"]
    val region = app.node["region"]
    val applicationName = app.node["applicationName"]
    val environmentName = app.node["environmentName"]
    val springProfile = app.node["springProfile"]
    val dockerImageTag = app.node["dockerImageTag"]
    val dockerRepositoryName = app.node["dockerRepositoryName"]

    val awsEnvironment = makeEnv(accountId, region)

    val applicationEnvironment = ApplicationEnvironment(applicationName, environmentName)

    // This stack is just a container for the parameters below, because they need a Stack as a scope.
    // We're making this parameters stack unique with each deployment by adding a timestamp, because updating an existing
    // parameters stack will fail because the parameters may be used by an old service stack.
    // This means that each update will generate a new parameters stack that needs to be cleaned up manually!
    val timestamp = System.currentTimeMillis()
    val parametersStack = Stack(
        app,
        "ServiceParametersStack-$timestamp",
        StackProps.builder()
            .stackName(applicationEnvironment.prefix("Service-Parameters-$timestamp"))
            .env(awsEnvironment)
            .build()

    )

    val cognitoOutputParameters = CognitoStack.getOutputParametersFromParameterStore(
        scope = parametersStack,
        applicationEnvironment = applicationEnvironment,
    )

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

    val dockerImageSource = Service.DockerImageSource(dockerRepositoryName, dockerImageTag)

    val environmentVariables = mapOf(
        "SPRING_PROFILES_ACTIVE" to springProfile,
        "COGNITO_CLIENT_ID" to cognitoOutputParameters.userPoolClientId,
        "COGNITO_CLIENT_SECRET" to cognitoOutputParameters.userPoolClientSecret,
        "COGNITO_USER_POOL_ID" to cognitoOutputParameters.userPoolId,
        "COGNITO_LOGOUT_URL" to cognitoOutputParameters.logoutUrl,
        "COGNITO_PROVIDER_URL" to cognitoOutputParameters.providerUrl,
    )

    val serviceInputParameters = Service.ServiceInputParameters(dockerImageSource, environmentVariables)
        .withHealthCheckIntervalSeconds(30)
        .withTaskRolePolicyStatements(
            listOf(
                PolicyStatement.Builder.create()
                    .sid("AllowCreatingUsers")
                    .effect(Effect.ALLOW)
                    .resources(
                        listOf(
                            "arn:aws:cognito-idp:$region:$accountId:userpool/${cognitoOutputParameters.userPoolId}",
                        )
                    )
                    .actions(
                        listOf(
                            "cognito-idp:AdminCreateUser",
                        )
                    )
                    .build()
            )
        )

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
