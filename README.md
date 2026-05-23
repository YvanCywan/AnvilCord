# AnvilCord

AnvilCord is an experimental Java framework for building Discord bot applications that can host runtime-discoverable plugins. It combines Spring Boot auto-configuration, Discord4J gateway integration, a small core plugin API, and a Gradle plugin that makes it easier for host applications to depend on the framework.

The project is intended to become a foundation for Discord bots where:

- the host application owns deployment and configuration;
- feature modules can be packaged as separate plugin jars;
- plugins are discovered at runtime with Java `ServiceLoader`;
- plugin packages can contribute Spring-managed components and annotation-free slash commands;
- blocking command or event code can run on virtual threads while the framework handles Discord gateway wiring.

## Repository contents

This is a multi-module Gradle build.

| Module | Purpose |
| --- | --- |
| `anvilcord-core` | Core public API: `@AnvilCordPluginHost`, plugin contracts, plugin context, framework events, and the virtual event bus. |
| `anvilcord-discord` | Discord-specific integration built on Discord4J, including gateway lifecycle support, bot configuration properties, Discord events, and the `SlashCommand` contract/orchestrator. |
| `anvilcord-starter` | Spring Boot starter/runtime application. It auto-configures the event bus, Discord gateway bridge, plugin discovery, command discovery, and default framework beans. |
| `anvilcord-gradle-plugin` | Gradle plugin published as `io.github.yvancywan.anvilcord`; it configures host applications with AnvilCord dependencies and the starter main class. |
| `anvilcord-example-plugin` | Example runtime-only plugin jar that demonstrates `ServiceLoader` registration, event listeners, custom events, and a slash command. |
| `anvilcord-starter-consumer` | Non-published sample host application used to verify the starter and runtime plugin integration. |
| `build-logic` | Shared Gradle convention plugins for Java, dependency management, publishing, tests, and toolchain settings. |

## Prerequisites

- macOS, Linux, or Windows with a shell capable of running the included Gradle wrapper.
- A JDK that can satisfy the build's Java toolchain configuration. The current convention plugin requests Java 25.
- A Discord bot token when running against Discord.
- A Discord application ID when you want the framework to sync slash commands for a specific application.

No system Gradle installation is required; use `./gradlew` from the repository root.

## Build and test

From the repository root:

```sh
./gradlew build
```

Run all tests:

```sh
./gradlew test
```

Build a single module:

```sh
./gradlew :anvilcord-core:build
./gradlew :anvilcord-discord:build
./gradlew :anvilcord-starter:build
```

Publish the framework artifacts to your local Maven repository for testing from another project:

```sh
./gradlew publishToMavenLocal
```

Publish release artifacts to Maven Central through Sonatype Central Portal:

```sh
./gradlew publishToMavenCentral -Pversion=0.2.2
```

That uploads a deployment for manual release in Central Portal. To upload and automatically release after Central validates the deployment, use:

```sh
./gradlew publishAndReleaseToMavenCentral -Pversion=0.2.2
```

Maven Central publishing requires Central Portal user-token credentials and a GPG signing key. Provide them as Gradle properties:

```properties
mavenCentralUsername=<central-portal-token-username>
mavenCentralPassword=<central-portal-token-password>
signingInMemoryKey=<ascii-armored-private-gpg-key>
signingInMemoryKeyPassword=<gpg-key-password>
```

In CI, expose those Gradle properties before Gradle starts by using Gradle's `ORG_GRADLE_PROJECT_` environment variable convention:

```sh
export ORG_GRADLE_PROJECT_mavenCentralUsername="<central-portal-token-username>"
export ORG_GRADLE_PROJECT_mavenCentralPassword="<central-portal-token-password>"
export ORG_GRADLE_PROJECT_signingInMemoryKey="$(gpg --export-secret-keys --armor <key-id>)"
export ORG_GRADLE_PROJECT_signingInMemoryKeyPassword="<gpg-key-password>"
```

For GitHub Actions, keep secrets named however you prefer, but map them to the `ORG_GRADLE_PROJECT_*` names on the publish step:

```yaml
env:
  ORG_GRADLE_PROJECT_mavenCentralUsername: ${{ secrets.MAVEN_CENTRAL_USERNAME }}
  ORG_GRADLE_PROJECT_mavenCentralPassword: ${{ secrets.MAVEN_CENTRAL_PASSWORD }}
  ORG_GRADLE_PROJECT_signingInMemoryKey: ${{ secrets.SIGNING_KEY }}
  ORG_GRADLE_PROJECT_signingInMemoryKeyPassword: ${{ secrets.SIGNING_PASSWORD }}
```

Snapshots use versions ending in `-SNAPSHOT` and are published with the same `publishToMavenCentral` task.

Override the project version for a local build or publish:

```sh
./gradlew build -Pversion=0.1.0-SNAPSHOT
```

## Release versioning

GitHub Actions creates the next release tag from Conventional Commits with `mathieudutour/github-tag-action` in `.github/workflows/build-and-publish.yml`.

- `feat:` commits bump the minor version.
- Other commit types bump the patch version.
- Commits with `!` in the header, or `BREAKING CHANGE:` / `BREAKING-CHANGE:` in the body, bump the major version.

Pushes to `main` create and push the next `v*.*.*` tag, then publish the exact release version without the leading `v`.

The workflow passes the generated version to every Gradle invocation with `-Pversion=...`. The build also accepts `-Pverison=...`, `-PanvilCordVersion=...`, and `ANVILCORD_VERSION` as compatibility aliases.

## Running the sample host

`anvilcord-starter-consumer` is the sample host application. It applies the AnvilCord Gradle plugin and places `anvilcord-example-plugin` on the runtime classpath.

Set Discord credentials with environment variables:

```sh
export BOT_CORE_TOKEN="your-discord-bot-token"
export BOT_CORE_APPLICATION_ID="your-discord-application-id"
```

Then run the consumer application:

```sh
./gradlew :anvilcord-starter-consumer:run
```

The sample configuration maps those environment variables to Spring properties:

```yaml
bot:
  core:
    token: ${BOT_CORE_TOKEN:}
    application-id: ${BOT_CORE_APPLICATION_ID:}
```

If the token is blank, the application can still bind configuration for local development, but the Discord gateway cannot connect until `bot.core.token` is configured.

## Creating a host application

A host application marks its Spring Boot entry point with `@AnvilCordPluginHost`:

```java
package com.example.bot;

import io.github.yvancywan.anvilcord.core.AnvilCordPluginHost;

@AnvilCordPluginHost
public class BotApplication {
}
```

When using the AnvilCord Gradle plugin, the host receives the core API at compile time and the starter runtime on the runtime/test classpath:

```kotlin
plugins {
    id("io.github.yvancywan.anvilcord") version "0.0.1-SNAPSHOT"
}

anvilCord {
    version.set("0.0.1-SNAPSHOT")
}
```

For local development inside this repository, included builds and project dependencies are used automatically. For external projects, publish locally first with `./gradlew publishToMavenLocal` or consume a published AnvilCord version when available.

## Creating a runtime plugin

An AnvilCord plugin implements `AnvilCordPlugin`:

```java
package com.example.plugin;

import io.github.yvancywan.anvilcord.core.plugin.AnvilCordPlugin;
import io.github.yvancywan.anvilcord.core.plugin.AnvilCordPluginContext;

public final class ExamplePlugin implements AnvilCordPlugin {
    @Override
    public String id() {
        return "example-plugin";
    }

    @Override
    public void initialize(AnvilCordPluginContext context) {
        // Register listeners or publish framework/plugin events here.
    }
}
```

Register the implementation for Java `ServiceLoader` by adding this resource to the plugin jar:

```text
META-INF/services/io.github.yvancywan.anvilcord.core.plugin.AnvilCordPlugin
```

The resource contains the implementation class name:

```text
com.example.plugin.ExamplePlugin
```

Add the plugin jar to the host runtime classpath, for example:

```kotlin
dependencies {
    runtimeOnly("com.example:my-anvilcord-plugin:1.0.0")
}
```

By default, AnvilCord scans the plugin implementation package and subpackages. Override `scanBasePackages()` in your plugin if your Spring components or command classes live elsewhere.

## Creating slash commands

Commands implement `io.github.yvancywan.anvilcord.discord.command.SlashCommand` and return Discord application command metadata:

```java
package com.example.plugin;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.discordjson.json.ApplicationCommandRequest;
import io.github.yvancywan.anvilcord.discord.command.SlashCommand;

public final class HelloCommand implements SlashCommand {
    @Override
    public ApplicationCommandRequest commandRequest() {
        return ApplicationCommandRequest.builder()
                .name("hello")
                .description("Replies with a greeting.")
                .build();
    }

    @Override
    public void execute(ChatInputInteractionEvent event) {
        event.reply("Hello from AnvilCord!").block();
    }
}
```

The framework discovers command implementations from host and plugin packages and executes each command on a virtual thread.

## Reacting to Discord events and requesting bot actions

`anvilcord-discord` publishes stable Discord gateway payload records such as `DiscordGatewayEvents.MessageCreated`, `MemberJoined`, `ChannelUpdated`, and `InteractionReceived` to the shared event bus. Plugins can also publish `DiscordBotActions` records such as `SendChannelMessage` to request bot side effects.

See [`anvilcord-discord/README.md`](anvilcord-discord/README.md) for the complete event and action payload reference.

## Native image and container support

The starter module applies Spring Boot and GraalVM Native Build Tools. Depending on your local GraalVM and Docker setup, useful tasks include:

```sh
./gradlew :anvilcord-starter:bootBuildImage
./gradlew :anvilcord-starter:nativeCompile
./gradlew :anvilcord-starter:nativeTest
```

See `HELP.md` for the Spring Initializr-generated reference links and additional notes about GraalVM native image support.

## Development notes

- Keep framework API types in `anvilcord-core` when they need to be visible to host applications or plugins.
- Keep Discord4J-specific types in `anvilcord-discord` unless they are intentionally part of the core abstraction.
- Keep Spring Boot runtime wiring in `anvilcord-starter`.
- Use `anvilcord-example-plugin` and `anvilcord-starter-consumer` to verify runtime-only plugin discovery and starter integration.
- Run `./gradlew test` before committing changes.
