package com.shinythings

import software.amazon.awscdk.App
import software.amazon.awscdk.Stack
import software.amazon.awscdk.StackProps
import software.amazon.awscdk.services.iam.AccessKey
import software.amazon.awscdk.services.iam.AccessKeyProps
import software.amazon.awscdk.services.iam.Group
import software.amazon.awscdk.services.iam.GroupProps
import software.amazon.awscdk.services.iam.ManagedPolicy
import software.amazon.awscdk.services.iam.User
import software.amazon.awscdk.services.iam.UserProps
import software.amazon.awscdk.services.ssm.StringParameter

fun main() {
    val gitHubActionsUserName = "github"

    val gitHubActionsGroupName1 = "GitHubActions1"
    val gitHubActionsGroupName2 = "GitHubActions2"

    val app = App()

    val accountId = app.node["accountId"]
    val region = app.node["region"]

    val awsEnvironment = makeEnv(accountId, region)

    val gitHubActionsStack = Stack(
        app,
        "GitHubActionsStack",
        StackProps.builder()
            .stackName("GitHubActionsStack")
            .description("This stack creates the 'gitHub' user needed by the CI to trigger CloudFormation deployments")
            .env(awsEnvironment)
            .build(),
    )

    val managedGroupPolicies1 = listOf(
        "AWSCertificateManagerFullAccess",
        "AWSCloudFormationFullAccess",
        "AWSKeyManagementServicePowerUser",
        "AWSLambda_FullAccess",
        "AmazonCognitoPowerUser",
        "AmazonEC2ContainerRegistryFullAccess",
        "AmazonEC2FullAccess",
        "AmazonECS_FullAccess",
        "AmazonMQApiFullAccess",
        "AmazonRDSFullAccess",
    ).map { ManagedPolicy.fromAwsManagedPolicyName(it) }

    val managedGroupPolicies2 = listOf(
        "AmazonS3FullAccess",
        "AmazonSSMFullAccess",
        "IAMFullAccess",
        "job-function/SystemAdministrator",
    ).map { ManagedPolicy.fromAwsManagedPolicyName(it) }

    val gitHubActionsGroup1 = Group(
        gitHubActionsStack,
        "GitHubActionsGroup1",
        GroupProps.builder()
            .groupName(gitHubActionsGroupName1)
            .managedPolicies(managedGroupPolicies1)
            .build()
    )

    val gitHubActionsGroup2 = Group(
        gitHubActionsStack,
        "GitHubActionsGroup2",
        GroupProps.builder()
            .groupName(gitHubActionsGroupName2)
            .managedPolicies(managedGroupPolicies2)
            .build()
    )

    val gitHubActionsUser = User(
        gitHubActionsStack,
        "GitHubActionsUser",
        UserProps.builder()
            .userName(gitHubActionsUserName)
            .groups(listOf(gitHubActionsGroup1, gitHubActionsGroup2))
            .build()
    )

    val gitHubActionsUserAccessKey = AccessKey(
        gitHubActionsStack,
        "GitHubActionsUserAccessKey",
        AccessKeyProps.builder()
            .user(gitHubActionsUser)
            .build()
    )

    StringParameter.Builder.create(gitHubActionsStack, "GitHubUserName")
        .parameterName("GitHubUserName")
        .stringValue(gitHubActionsUser.userName)
        .build()

    StringParameter.Builder.create(gitHubActionsStack, "GitHubUserAccessKey")
        .parameterName("GitHubUserAccessKey")
        .stringValue(gitHubActionsUserAccessKey.accessKeyId)
        .build()

    StringParameter.Builder.create(gitHubActionsStack, "GitHubUserSecretAccessKey")
        .parameterName("GitHubUserSecretAccessKey")
        .stringValue(gitHubActionsUserAccessKey.secretAccessKey.unsafeUnwrap())
        .build()

    app.synth()
}
