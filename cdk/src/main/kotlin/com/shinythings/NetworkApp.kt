package com.shinythings

import dev.stratospheric.cdk.Network
import software.amazon.awscdk.App
import software.amazon.awscdk.CfnOutput
import software.amazon.awscdk.CfnOutputProps
import software.amazon.awscdk.Stack
import software.amazon.awscdk.StackProps

fun main() {
    val app = App()

    val accountId = app.node["accountId"]
    val region = app.node["region"]
    val environmentName = app.node["environmentName"]

    // optional parameter
    val sslCertificateArn = app.node.tryGetContext("sslCertificateArn") as? String

    val awsEnvironment = makeEnv(accountId, region)

    val networkStack = Stack(
        app,
        "NetworkStack",
        StackProps.builder()
            .stackName("$environmentName-Network")
            .env(awsEnvironment)
            .build(),
    )

    val network = Network(
        networkStack,
        "Network",
        awsEnvironment,
        environmentName,
        Network.NetworkInputParameters().apply {
            sslCertificateArn?.let { withSslCertificateArn(sslCertificateArn) }
        },
    )

    CfnOutput(
        networkStack,
        "loadBalancerDnsName",
        CfnOutputProps.builder()
            .exportName("loadBalancerDnsName")
            .value(network.outputParameters.loadBalancerDnsName)
            .build()
    )

    app.synth()
}
