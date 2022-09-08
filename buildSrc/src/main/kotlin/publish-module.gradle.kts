import com.android.build.gradle.BaseExtension
import groovy.util.Node

plugins {
    `maven-publish`
    signing
    id("org.jetbrains.dokka")
}

tasks {
    register("javadocJar", Jar::class) {
        dependsOn(named("dokkaHtml"))
        archiveClassifier.set("javadoc")
        from("$buildDir/dokka/html")
    }

    register("sourcesJar", Jar::class) {
        archiveClassifier.set("sources")
        from(extensions.findByType<BaseExtension>()?.sourceSets?.getByName("main")?.java?.srcDirs)
    }
}

afterEvaluate {
    publishing {
        publications {
            register<MavenPublication>("mavenAndroid") {
                afterEvaluate { artifact(tasks.getByName("bundleReleaseAar")) }
                artifact(tasks.getByName("javadocJar"))
                artifact(tasks.getByName("sourcesJar"))

                artifactId = requireNotNull(extra.get(KEY_PUBLISH_ARTIFACT_ID)).toString()
                version = requireNotNull(extra.get(KEY_PUBLISH_VERSION)).toString()

                pom {
                    name.set("WalletConnect ${requireNotNull(extra.get(KEY_SDK_NAME))}")
                    description.set("Sign SDK for WalletConnect.")
                    url.set("https://github.com/WalletConnect/WalletConnectKotlinV2")

                    licenses {
                        license {
                            name.set("The Apache License, Version 2.0")
                            url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                        }
                        license {
                            name.set("SQLCipher Community Edition")
                            url.set("https://www.zetetic.net/sqlcipher/license/")
                        }
                    }

                    developers {
                        developer {
                            id.set("KotlinSDKTeam")
                            name.set("WalletConnect Kotlin")
                            email.set("Kotlin@WalletConnect.com")
                        }
                    }

                    scm {
                        connection.set("scm:git:git://github.com/WalletConnect/WalletConnectKotlinV2.git")
                        developerConnection.set("scm:git:ssh://github.com/WalletConnect/WalletConnectKotlinV2.git")
                        url.set("https://github.com/WalletConnect/WalletConnectKotlinV2")
                    }

                    withXml {
                        fun Node.addDependency(dependency: Dependency, scope: String) {
                            appendNode("dependency").apply {
                                appendNode("groupId", dependency.group)
                                appendNode("artifactId", dependency.name)
                                appendNode("version", dependency.version)
                                appendNode("scope", scope)
                            }
                        }

                        asNode().appendNode("dependencies").let { dependencies ->
                            // List all "api" dependencies as "compile" dependencies
                            configurations.named("api").get().allDependencies.forEach {
                                dependencies.addDependency(it, "compile")
                            }
                            // List all "implementation" dependencies as "runtime" dependencies
                            configurations.named("implementation").get().allDependencies.forEach {
                                dependencies.addDependency(it, "runtime")
                            }
                        }
                    }
                }
            }
        }
    }
}

signing {
    useInMemoryPgpKeys(
        System.getenv("SIGNING_KEY_ID"),
        System.getenv("SIGNING_KEY"),
        System.getenv("SIGNING_PASSWORD")
    )
    sign(publishing.publications)
}