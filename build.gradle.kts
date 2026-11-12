import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinJvm
import com.vanniktech.maven.publish.SourcesJar
import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion

plugins {
    id("org.jetbrains.kotlin.jvm") version "2.2.21"
    id("org.jetbrains.kotlin.plugin.spring") version "2.2.21"
    id("java-library")
    id("com.vanniktech.maven.publish") version "0.37.0"
    id("org.jlleitschuh.gradle.ktlint") version "14.0.1"
}

group = "io.github.themoment-team"
version = "2.0.1"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

repositories {
    mavenCentral()
}

val kotlinVersion = getKotlinPluginVersion()
val springBootVersion = "4.0.9"
val springdocVersion = "3.0.3"

dependencies {
    // Spring Boot BOM: 이 프로젝트의 컴파일/테스트 버전 정렬에만 사용 (소비자에게 버전 constraint를 전파하지 않음)
    compileOnly(platform("org.springframework.boot:spring-boot-dependencies:$springBootVersion"))
    testImplementation(platform("org.springframework.boot:spring-boot-dependencies:$springBootVersion"))

    // Kotlin: 발행 메타데이터에 버전이 빠지지 않도록 플러그인 버전으로 고정
    api("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")
    api("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")

    // Spring Boot: 소비자 프로젝트의 Spring Boot 버전을 그대로 사용
    compileOnly("org.springframework.boot:spring-boot-starter-web")
    compileOnly("org.springframework.boot:spring-boot-autoconfigure")
    compileOnly("com.fasterxml.jackson.core:jackson-annotations")
    compileOnly("org.slf4j:slf4j-api")

    // Swagger/OpenAPI (springdoc 3.0.x = Spring Boot 4.0.x)
    api("org.springdoc:springdoc-openapi-starter-webmvc-ui:$springdocVersion")

    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-web")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

// Maven Central 배포 설정. CI에서는 ORG_GRADLE_PROJECT_* 환경 변수로 인증 정보와 서명 키를 전달합니다.
mavenPublishing {
    // Kotlin 소스는 javadoc 도구로 문서화되지 않으므로 Central 요구사항을 충족하는 빈 javadoc jar를 발행합니다.
    configure(KotlinJvm(javadocJar = JavadocJar.Empty(), sourcesJar = SourcesJar.Sources()))
    publishToMavenCentral()
    // 서명 키에 실제 값이 있을 때만 서명합니다. 키가 없어도 publishToMavenLocal로 로컬 검증을 할 수 있고,
    // 서명 없이 Central에 올리면 Portal 검증에서 거부되므로 잘못 배포될 위험은 없습니다.
    // GitHub Secrets가 누락되면 ORG_GRADLE_PROJECT_* 값이 빈 문자열로 주입되므로 isPresent만으로 판단하지 않습니다.
    val signingKey = providers.gradleProperty("signingInMemoryKey").orNull
    if (!signingKey.isNullOrBlank()) {
        signAllPublications()
    }
    coordinates(group.toString(), "the-sdk", version.toString())

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

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.release.set(17)
}

tasks.withType<Javadoc> {
    options.encoding = "UTF-8"
}
