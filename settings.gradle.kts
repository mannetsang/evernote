// Maven Central rate-limits shared CI runners (HTTP 429), which fails a build
// at random. Google's mirror of Central is checked first; anything it lacks
// still comes from Central itself.
val centralMirror = "https://maven-central.storage-download.googleapis.com/maven2/"

pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        maven(centralMirror) { name = "MavenCentralMirror" }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        maven(centralMirror) { name = "MavenCentralMirror" }
        mavenCentral()
    }
}

rootProject.name = "Evernote"
include(":app")
