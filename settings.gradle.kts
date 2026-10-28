pluginManagement {
    includeBuild("build-logic")

}

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        mavenCentral()
        // Сериализаторы Schema Registry публикуются только здесь.
        // content ограничивает репозиторий группой io.confluent: подменить через него другую библиотеку нельзя.
        maven("https://packages.confluent.io/maven/") {
            content { includeGroupAndSubgroups("io.confluent") }
        }
    }
}
rootProject.name = "lumeva"
include(":services:profile-service")
include(":libs:event-contracts")
