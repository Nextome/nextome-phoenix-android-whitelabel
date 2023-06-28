pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()

        maven {
            name = "artifactory"
            url = uri("https://packages.nextome.dev/artifactory/nextome-libs-prod/")
            // TODO: Add your Nextome Repository credentials here
            credentials {
                username = "username"
                password = "password"
            }
        }
    }
}

rootProject.name = "Phoenix Whitelabel App"
include(":app")
 