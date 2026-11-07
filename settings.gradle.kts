pluginManagement {
    repositories {
        // آینهٔ علی‌بابا — اولویت با آن است چون Maven Central گاهی ۴۲۹ می‌دهد
        maven { url = uri("https://maven.aliyun.com/repository/central") }
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
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
        // آینهٔ علی‌بابا — اولویت با آن است چون Maven Central گاهی ۴۲۹ می‌دهد
        maven { url = uri("https://maven.aliyun.com/repository/central") }
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        google()
        // آینهٔ مستقیم Maven Central — repo.maven.apache.org گاهی محدودیت نرخ (429) دارد
        maven { url = uri("https://repo1.maven.org/maven2") }
        mavenCentral()
    }
}

rootProject.name = "HamrahDermanEtiyad"
include(":app")
