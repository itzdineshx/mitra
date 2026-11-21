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

rootProject.name = "mitra"

// One app. `app` is what gets installed; everything in modules/ is a part of that same app.
include(":app")

// Parts of the app, all in modules/. Short names: ./gradlew :trust:test
val modules =
    listOf(
        "engine-api", // pure Kotlin: screen types + ExecutionEngine interface
        "trust", // pure Kotlin: TrustEngine (R1–R14) + TrustedExecutor
        "web", // pure Kotlin: domain allow-list, HTTPS, look-alike checks
        "plugins", // pure Kotlin: signed JSON recipe loader
        "model", // on-device LLM (LiteRT-LM + Gemma 4)
        "voice", // speech in / out
        "agents", // orchestrator + agents
        "data", // local encrypted storage
        "ui", // shared Compose theme and components
        "engine-access", // Accessibility Service: taps, types, reads the screen
        "engine-guide", // fallback: speaks steps, the user taps
    )
modules.forEach { name ->
    include(":$name")
    project(":$name").projectDir = file("modules/$name")
}
