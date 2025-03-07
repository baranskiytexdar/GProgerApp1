pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // Добавляем Maven репозиторий для ksoap2
        maven { url = uri("https://repo.maven.apache.org/maven2/") }
        maven { url = uri("https://jitpack.io") } // Добавляем JitPack для дополнительных библиотек
    }
}

rootProject.name = "GProgerApp1"
include(":app")
