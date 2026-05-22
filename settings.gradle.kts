pluginManagement {
	includeBuild("build-logic")
	includeBuild("anvilcord-gradle-plugin")
}

rootProject.name = "AnvilCord"

include(
	"anvilcord-core",
	"anvilcord-discord",
	"anvilcord-example-plugin",
	"anvilcord-starter-consumer",
	"anvilcord-starter"
)
