package com.shinythings

import software.amazon.awscdk.App
import software.amazon.awscdk.Stack
import software.amazon.awscdk.StackProps

fun main() {
    val app = App()

    val accountId = app.node["accountId"]
    val region = app.node["region"]

    val awsEnvironment = makeEnv(accountId, region)

    Stack(
        app,
        "Bootstrap",
        StackProps.builder()
            .env(awsEnvironment)
            .build(),
    )

    app.synth()
}

