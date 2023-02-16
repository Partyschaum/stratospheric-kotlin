package com.shinythings

import software.amazon.awscdk.Environment
import software.constructs.Node

operator fun Node.get(key: String): String {
    return try {
        tryGetContext(key)
            .let { it as String }
    } catch (pie: ClassCastException) {
        throw IllegalArgumentException("The key '$key' is no string.", pie)
    } catch (pie: NullPointerException) {
        throw IllegalArgumentException("The key '$key' does not exist.", pie)
    }
}

fun makeEnv(account: String, region: String): Environment =
    Environment.builder()
        .account(account)
        .region(region)
        .build()
