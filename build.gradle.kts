plugins {
    id("org.jetbrains.kotlin.jvm") version "2.2.21"
    id("org.jetbrains.kotlin.plugin.spring") version "2.2.21"
    id("java-library")
    id("maven-publish")
    id("org.jlleitschuh.gradle.ktlint") version "14.0.1"
}

group = "team.themoment"
version = "2.0"

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

val springBootVersion = "4.0.9"
val springdocVersion = "3.0.3"

dependencies {
    // Spring Boot BOM: 전이 의존성을 패치된 4.0.x로 정렬 (Gradle은 더 높은 버전을 선택하므로 소비자 버전을 낮추지 않음)
    api(platform("org.springframework.boot:spring-boot-dependencies:$springBootVersion"))

    // Kotlin
    api("org.jetbrains.kotlin:kotlin-reflect")
    api("org.jetbrains.kotlin:kotlin-stdlib")

    // Spring Boot: 소비자 프로젝트의 Spring Boot 버전을 그대로 사용
    compileOnly("org.springframework.boot:spring-boot-starter-web")
    compileOnly("org.springframework.boot:spring-boot-autoconfigure")
    compileOnly("com.fasterxml.jackson.core:jackson-annotations")
    compileOnly("org.slf4j:slf4j-api")

    // Swagger/OpenAPI (springdoc 3.0.x = Spring Boot 4.0.x)
    api("org.springdoc:springdoc-openapi-starter-webmvc-ui:$springdocVersion")

    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-web")
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
