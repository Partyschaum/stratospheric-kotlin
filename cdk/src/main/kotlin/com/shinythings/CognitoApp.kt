package com.shinythings

import dev.stratospheric.cdk.ApplicationEnvironment
import software.amazon.awscdk.App

fun main() {
    val app = App()

    val environmentName = app.node["environmentNam"]
    val applicationName = app.node["applicationName"]
    val accountId = app.node["accountId"]
    val region = app.node["region"]
    val applicationUrl = app.node["applicationUrl"]
    val loginPageDomainPrefix = app.node["loginPageDomainPrefix"]

    val awsEnvironment = makeEnv(accountId, region)

    val applicationEnvironment = ApplicationEnvironment(
        applicationName,
        environmentName,
    )

    CognitoStack(
        scope = app,
        id = "cognito",
        awsEnvironment = awsEnvironment,
        applicationEnvironment = applicationEnvironment,
        inputParameters = CognitoStack.CognitoInputParameters(
            applicationName = applicationName,
            applicationUrl = applicationUrl,
            loginPageDomainPrefix = loginPageDomainPrefix,
        )
    )

    app.synth()
}
