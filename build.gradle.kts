plugins {
    id("org.jetbrains.kotlin.jvm") version "2.1.10"
    id("org.jetbrains.kotlin.plugin.spring") version "2.1.10"
    id("java-library")
    id("maven-publish")
}

group = "team.themoment"
version = "1.0-RC3"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
    withSourcesJar()
    withJavadocJar()
}

repositories {
    mavenCentral()
}

dependencies {
    // Kotlin
    api("org.jetbrains.kotlin:kotlin-reflect")
    api("org.jetbrains.kotlin:kotlin-stdlib")

    // Spring Boot
    api("org.springframework.boot:spring-boot-starter-web:3.4.1")
    api("org.springframework.boot:spring-boot-autoconfigure:3.4.1")

    // Swagger/OpenAPI
    api("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.15")

    // Jackson
    api("com.fasterxml.jackson.module:jackson-module-kotlin:2.18.2")

    // Logging
    api("org.slf4j:slf4j-api:2.0.16")

    // Testing
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = project.group.toString()
            artifactId = "the-sdk"
            version = project.version.toString()

            from(components["java"])

            pom {
                name = "The SDK"
                description = "Modular SDK for reusable Spring Boot components"
                url = "https://github.com/themoment-team/the-sdk"

                licenses {
                    license {
                        name = "MIT License"
                        url = "https://opensource.org/licenses/MIT"
                    }
                }

                developers {
                    developer {
                        id = "themoment"
                        name = "themoment-team"
                        email = "official.themoment.team@gmail.com"
                    }
                }

                scm {
                    connection = "scm:git:git://github.com/themoment-team/the-sdk.git"
                    developerConnection = "scm:git:ssh://github.com:themoment-team/the-sdk.git"
                    url = "https://github.com/themoment-team/the-sdk"
                }
            }
        }
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.release.set(17)
}

tasks.withType<Javadoc> {
    options.encoding = "UTF-8"
}
