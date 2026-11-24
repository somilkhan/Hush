// PLACEHOLDER namespace/applicationId: app.hush.placeholder (see docs/DECISION_LOG.md D-002).
// Rename cost: see docs/architecture/DECISIONS.md ADR-004.

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
    }
}

rootProject.name = "hush"

include(":app")
include(":core:domain")
include(":core:data")
include(":core:player")
include(":design")
