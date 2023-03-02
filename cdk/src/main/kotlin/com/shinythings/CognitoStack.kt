package com.shinythings

import dev.stratospheric.cdk.ApplicationEnvironment
import software.amazon.awscdk.Duration
import software.amazon.awscdk.Environment
import software.amazon.awscdk.Stack
import software.amazon.awscdk.StackProps
import software.amazon.awscdk.customresources.AwsCustomResource
import software.amazon.awscdk.customresources.AwsCustomResourcePolicy
import software.amazon.awscdk.customresources.AwsSdkCall
import software.amazon.awscdk.customresources.PhysicalResourceId
import software.amazon.awscdk.customresources.SdkCallsPolicyOptions
import software.amazon.awscdk.services.cognito.AccountRecovery
import software.amazon.awscdk.services.cognito.AutoVerifiedAttrs
import software.amazon.awscdk.services.cognito.CognitoDomainOptions
import software.amazon.awscdk.services.cognito.Mfa
import software.amazon.awscdk.services.cognito.OAuthFlows
import software.amazon.awscdk.services.cognito.OAuthScope
import software.amazon.awscdk.services.cognito.OAuthSettings
import software.amazon.awscdk.services.cognito.PasswordPolicy
import software.amazon.awscdk.services.cognito.SignInAliases
import software.amazon.awscdk.services.cognito.StandardAttribute
import software.amazon.awscdk.services.cognito.StandardAttributes
import software.amazon.awscdk.services.cognito.UserPool
import software.amazon.awscdk.services.cognito.UserPoolClient
import software.amazon.awscdk.services.cognito.UserPoolClientIdentityProvider
import software.amazon.awscdk.services.cognito.UserPoolDomain
import software.amazon.awscdk.services.ssm.StringParameter
import software.constructs.Construct

class CognitoStack(
    scope: Construct,
    id: String,
    private val awsEnvironment: Environment,
    private val applicationEnvironment: ApplicationEnvironment,
    inputParameters: CognitoInputParameters,
) : Stack(
    scope,
    id,
    StackProps.builder()
        .stackName(applicationEnvironment.prefix("Cognito"))
        .env(awsEnvironment)
        .build()
) {

    val outputParameters: CognitoOutputParameters

    private val logoutUrl =
        "https://${inputParameters.loginPageDomainPrefix}.auth.${awsEnvironment.region}.amazoncognito.com/logout"

    init {
        val userPool = createUserPool(applicationName = inputParameters.applicationName)

        val userPoolClient = createUserPoolClient(
            userPool = userPool,
            applicationName = inputParameters.applicationName,
            applicationUrl = inputParameters.applicationUrl,
        )

        createUserPoolDomain(
            userPool = userPool,
            loginPageDomainPrefix = inputParameters.loginPageDomainPrefix,
        )

        outputParameters = writeOutputParametersToParameterStore(
            userPool = userPool,
            userPoolClient = userPoolClient,
        )
    }

    private fun createUserPool(applicationName: String) =
        UserPool.Builder.create(this, "userPool")
            .userPoolName("$applicationName-user-pool")
            .selfSignUpEnabled(false)
            .standardAttributes(
                StandardAttributes.builder()
                    .email(
                        StandardAttribute.builder()
                            .required(true)
                            .mutable(false)
                            .build()
                    )
                    .build()
            )
            .signInAliases(
                SignInAliases.builder()
                    .username(true)
                    .email(true)
                    .build()
            )
            .signInCaseSensitive(true)
            .autoVerify(
                AutoVerifiedAttrs.builder()
                    .email(true)
                    .build()
            )
            .mfa(Mfa.OFF)
            .accountRecovery(AccountRecovery.EMAIL_ONLY)
            .passwordPolicy(
                PasswordPolicy.builder()
                    .requireLowercase(true)
                    .requireDigits(true)
                    .requireSymbols(true)
                    .requireUppercase(true)
                    .minLength(12)
                    .tempPasswordValidity(Duration.days(7))
                    .build()
            )
            .build()

    private fun createUserPoolClient(userPool: UserPool, applicationName: String, applicationUrl: String) =
        UserPoolClient.Builder.create(this, "userPoolClient")
            .userPoolClientName("$applicationName-client")
            .generateSecret(true)
            .userPool(userPool)
            .oAuth(
                OAuthSettings.builder()
                    .callbackUrls(
                        listOf(
                            "$applicationUrl/login/oauth2/code/cognito",
                            "http://localhost:8080/login/oauth2/code/cognito",
                        )
                    )
                    .logoutUrls(
                        listOf(
                            applicationUrl,
                            "http://localhost:8080",
                        )
                    )
                    .flows(
                        OAuthFlows.builder()
                            .authorizationCodeGrant(true)
                            .build()
                    )
                    .scopes(
                        listOf(
                            OAuthScope.EMAIL,
                            OAuthScope.OPENID,
                            OAuthScope.PROFILE
                        )
                    )
                    .build()
            )
            .supportedIdentityProviders(
                listOf(UserPoolClientIdentityProvider.COGNITO)
            )
            .build()

    private fun createUserPoolDomain(userPool: UserPool, loginPageDomainPrefix: String) =
        UserPoolDomain.Builder.create(this, "userPoolDomain")
            .userPool(userPool)
            .cognitoDomain(
                CognitoDomainOptions.builder()
                    .domainPrefix(loginPageDomainPrefix)
                    .build()
            )

    private fun writeOutputParametersToParameterStore(
        userPool: UserPool,
        userPoolClient: UserPoolClient
    ): CognitoOutputParameters {
        StringParameter.Builder.create(this, "userPoolId")
            .parameterName(createParameterName(applicationEnvironment, ParameterName.USER_POOL_ID))
            .stringValue(userPool.userPoolId)
            .build()

        StringParameter.Builder.create(this, "userPoolClientId")
            .parameterName(createParameterName(applicationEnvironment, ParameterName.USER_POOL_CLIENT_ID))
            .stringValue(userPoolClient.userPoolClientId)
            .build()

        StringParameter.Builder.create(this, "logoutUrl")
            .parameterName(createParameterName(applicationEnvironment, ParameterName.USER_POOL_LOGOUT_URL))
            .stringValue(logoutUrl)
            .build()

        StringParameter.Builder.create(this, "providerUrl")
            .parameterName(createParameterName(applicationEnvironment, ParameterName.USER_POOL_PROVIDER_URL))
            .stringValue(userPool.userPoolProviderUrl)
            .build()

        // CloudFormation does not expose the UserPoolClient secret, so we can't access it directly with
        // CDK. As a workaround, we create a custom resource that calls the AWS API to get the secret, and
        // then store it in the parameter store like the other parameters.
        // Source: https://github.com/aws/aws-cdk/issues/7225
        val fetchUserPoolClientMetadata = AwsSdkCall.builder()
            .region(awsEnvironment.region)
            .service("CognitoIdentityServiceProvider")
            .action("describeUserPoolClient")
            .parameters(
                mapOf(
                    "UserPoolId" to userPool.userPoolId,
                    "ClientId" to userPoolClient.userPoolClientId,
                )
            )
            .physicalResourceId(PhysicalResourceId.of(userPoolClient.userPoolClientId))
            .build()

        val describeUserPoolResource = AwsCustomResource.Builder.create(this, "describeUserPool")
            .resourceType("Custom::DescribeCognitoUserPoolClient")
            .installLatestAwsSdk(false)
            .onCreate(fetchUserPoolClientMetadata)
            .onUpdate(fetchUserPoolClientMetadata)
            .policy(
                AwsCustomResourcePolicy.fromSdkCalls(
                    SdkCallsPolicyOptions.builder()
                        .resources(AwsCustomResourcePolicy.ANY_RESOURCE)
                        .build()
                )
            )
            .build()

        val userPoolClientSecret = describeUserPoolResource.getResponseField("UserPoolClient.ClientSecret")

        StringParameter.Builder.create(this, "userPoolClientSecret")
            .parameterName(createParameterName(applicationEnvironment, ParameterName.USER_POOL_CLIENT_SECRET))
            .stringValue(userPoolClientSecret)
            .build()

        return CognitoOutputParameters(
            userPoolId = userPool.userPoolId,
            userPoolClientId = userPoolClient.userPoolClientId,
            userPoolClientSecret = userPoolClientSecret,
            logoutUrl = logoutUrl,
            providerUrl = userPool.userPoolProviderUrl,
        )
    }

    companion object {
        fun getOutputParametersFromParameterStore(
            scope: Construct,
            applicationEnvironment: ApplicationEnvironment
        ) = CognitoOutputParameters(
            userPoolId = getParameter(scope, ParameterName.USER_POOL_ID, applicationEnvironment),
            userPoolClientId = getParameter(scope, ParameterName.USER_POOL_CLIENT_ID, applicationEnvironment),
            userPoolClientSecret = getParameter(scope, ParameterName.USER_POOL_CLIENT_SECRET, applicationEnvironment),
            logoutUrl = getParameter(scope, ParameterName.USER_POOL_LOGOUT_URL, applicationEnvironment),
            providerUrl = getParameter(scope, ParameterName.USER_POOL_PROVIDER_URL, applicationEnvironment),
        )
        private fun createParameterName(applicationEnvironment: ApplicationEnvironment, parameterName: ParameterName) =
            "${applicationEnvironment.environmentName}-${applicationEnvironment.applicationName}-Cognito-${parameterName.identifier}"

        private fun getParameter(
            scope: Construct,
            parameterName: ParameterName,
            applicationEnvironment: ApplicationEnvironment
        ) = StringParameter.fromStringParameterName(
            scope,
            parameterName.name,
            createParameterName(
                applicationEnvironment = applicationEnvironment,
                parameterName = parameterName,
            )
        ).stringValue
    }

    data class CognitoInputParameters(
        val applicationName: String,
        val applicationUrl: String,
        val loginPageDomainPrefix: String,
    )

    data class CognitoOutputParameters(
        val userPoolId: String,
        val userPoolClientId: String,
        val userPoolClientSecret: String,
        val logoutUrl: String,
        val providerUrl: String,
    )

    enum class ParameterName(val identifier: String) {
        USER_POOL_ID("userPoolId"),
        USER_POOL_CLIENT_ID("userPoolClientId"),
        USER_POOL_CLIENT_SECRET("userPoolClientSecret"),
        USER_POOL_LOGOUT_URL("userPoolLogoutUrl"),
        USER_POOL_PROVIDER_URL("userPoolProviderUrl"),
    }
}
