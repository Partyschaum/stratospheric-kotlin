import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "3.0.2"
    id("io.spring.dependency-management") version "1.1.0"
    id("com.google.cloud.tools.jib") version "3.3.1"
    kotlin("jvm") version "1.8.10"
    kotlin("plugin.spring") version "1.8.10"
}

group = "de.shinythings"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_17

repositories {
    mavenCentral()
}

val awsSpringVersion = "2.4.4"
val webjarsLocatorCoreVersion = "0.52"
val bootstrapVersion = "5.2.3"
val popperVersion = "2.9.3"
val jQueryVersion = "3.6.3"
val fontAwesomeVersion = "6.2.1"

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")

    implementation(platform("io.awspring.cloud:spring-cloud-aws-dependencies:$awsSpringVersion"))
    implementation("io.awspring.cloud:spring-cloud-starter-aws")

    implementation("nz.net.ultraq.thymeleaf:thymeleaf-layout-dialect")

    implementation("org.webjars:webjars-locator-core:$webjarsLocatorCoreVersion")
    implementation("org.webjars:bootstrap:$bootstrapVersion")
    implementation("org.webjars:popper.js:$popperVersion")
    implementation("org.webjars:jquery:$jQueryVersion")
    implementation("org.webjars:font-awesome:$fontAwesomeVersion")

    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "17"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

val awsAccountId = providers.gradleProperty("awsAccountId").get()
val awsRegionId = providers.gradleProperty("awsRegionId").get()
val imageTag = providers.gradleProperty("imageTag").get().ifBlank { null }

jib {
    from {
        image = "eclipse-temurin:17-jre"
    }
    to {
        image = "$awsAccountId.dkr.ecr.$awsRegionId.amazonaws.com/todo-app"
        tags = setOfNotNull("latest", imageTag)
    }
}
