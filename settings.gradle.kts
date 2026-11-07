pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        // آینهٔ مستقیم Maven Central — repo.maven.apache.org گاهی محدودیت نرخ (429) دارد
        maven { url = uri("https://repo1.maven.org/maven2") }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        // آینهٔ مستقیم Maven Central — repo.maven.apache.org گاهی محدودیت نرخ (429) دارد
        maven { url = uri("https://repo1.maven.org/maven2") }
        mavenCentral()
    }
}

rootProject.name = "HamrahDermanEtiyad"
include(":app")
