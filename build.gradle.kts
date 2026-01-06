plugins {
    `java-library`
    `maven-publish`
}

group = "team.themoment"
version = "1.0"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
    sourceCompatibility = JavaVersion.VERSION_13
    targetCompatibility = JavaVersion.VERSION_13
    withSourcesJar()
    withJavadocJar()
}

repositories {
    mavenCentral()
}

dependencies {
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
    options.release.set(13)
}

tasks.withType<Javadoc> {
    options.encoding = "UTF-8"
}
