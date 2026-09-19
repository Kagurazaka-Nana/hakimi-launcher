# AGENTS.md

## Project overview
- `hakimi-launcher` is a minimal Minecraft launcher built with JDK 25.
- Backend core (models / rules / download / launch logic) remains Java under `src/main/java`.
- Frontend is being built as a Kotlin Compose Multiplatform Material desktop UI under `src/main/kotlin` — currently a frontend-only scaffold, with `LauncherBackend` as the reserved interface for wiring the Java backend later.
- Core packages:
  - `com.minecraft.launcher.model.manifest`: Mojang version manifest models.
  - `com.minecraft.launcher.model.version`: per-version metadata models for downloaded client JSON.
  - `com.minecraft.launcher.model.rule`: Mojang rule evaluation for OS/features.
  - `com.minecraft.launcher.util`: JSON, platform, and Windows version helpers.
  - `com.minecraft.launcher.backend`: `LauncherBackend` interface + `StubLauncherBackend` placeholder implementation.
  - `com.minecraft.launcher.ui`: Compose Multiplatform Material desktop UI scaffold.
- Runtime downloads currently write into `temp/` and `launcherTest/.minecraft/`; treat these as generated local data, not source.

## Branch and PR workflow
- Treat `dev` as the development base branch. `dev` is not considered ahead; start development directly from `dev`.
- For the Kotlin UI work, use the branch `dev-kotlin` created from `dev`.
- Before opening a PR, run the relevant focused tests plus the full Gradle check locally when the environment supports it.
- PRs should be opened only after tests pass and the CI workflow has completed successfully.

## Build and test commands
- Requires JDK 25 (local path: `C:\Users\ColaPig\.jdks\graalvm-jdk-25`) and Gradle.
- Use the Gradle wrapper from this workspace: `./gradlew.bat test`, `./gradlew.bat build`, `./gradlew.bat run`.
- Focused test: `./gradlew.bat test --tests com.minecraft.launcher.model.rule.RuleEvaluatorTest`, `... --tests ...GameDeserializerTest`, or `... --tests ...JvmDeserializerTest`.
- CI uses `./gradlew test` with JaCoCo XML at `build/reports/jacoco/test/jacocoTestReport.xml`.
- Run the UI with `./gradlew.bat run`.
- Backend JNA native access on Windows needs `--enable-native-access=ALL-UNNAMED`; the packaged launcher starts with that flag.

## Coding and model conventions
- The project targets Java 25 / Kotlin; Java 21+ APIs such as `List#getFirst()` are acceptable.
- Jackson is the JSON binding layer. Many model classes use immutable final fields with `@JsonProperty` constructors or Lombok `@Builder` + `@Jacksonized`.
- Keep `lombok.config` aligned with the Jackson major version; it pins Lombok `@Jacksonized` to Jackson 2.
- Prefer `@JsonIgnoreProperties(ignoreUnknown = true)` on Mojang JSON model classes so upstream metadata additions do not break parsing.
- `GameDeserializer` exists because `arguments.game[]` is heterogeneous: entries may be raw strings or objects whose `value` is either a string or a string array. Preserve this behavior when editing argument models.

## Rule and platform gotchas
- `RuleEvaluator` implements Mojang-style last-matching-rule-wins semantics. Empty or null rule lists default to allowed unless a different fallback is passed.
- Mojang `os.version` values are regex patterns such as `^10\.`; matching intentionally uses `Pattern.matcher(...).find()` rather than full-string matching.
- Unit tests for rules should inject a deterministic `RuleContext` directly. Avoid `RuleContext.fromSystem()` in tests because it depends on the host OS.
- `RuleContext.fromSystem()` caches the system context and uses `OSVersionUtil` for exact Windows versions.
- `OSVersionUtil` uses JNA to call `ntdll!RtlGetVersion` on Windows and falls back to `System.getProperty("os.version")`; do not remove the native-access run note unless this implementation changes.

## Documentation to check
- Read `README.md` for the user-facing run instructions and project intent, but verify package paths against source because the README may lag behind code layout.
- Check `.github/workflows/ci.yml` before changing build, Java version, coverage, or Sonar behavior.

## Docs submodule (mandatory reading)
- `docs/` is a submodule pointing at the private repo `Kagurazaka-Nana/hakimi-launcher-docs`. It holds the authoritative design/rules notes for this project (rule evaluator, argument generation, unit-test style, review guidelines, current progress).
- **Read the relevant `docs/*.md` before changing behavior in an area it covers** (e.g. `Rule.md` before touching `model/rule`). After a PR that changes that behavior, update the corresponding `docs/` notes.
- The docs are a separate repo and must be committed/pushed inside `docs/` (the submodule) on their own; the parent repo only records the submodule pointer.
- The rule documentation (`docs/Rule.md` + `docs/Evaluator.md`) is the merged/authoritative source; the repo-root `RULES.md` is a short pointer to it.
