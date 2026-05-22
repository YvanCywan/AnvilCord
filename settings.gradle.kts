pluginManagement {
	includeBuild("build-logic")
}

rootProject.name = "AnvilCord"

include(
	"anvilcord-core",
	"anvilcord-discord",
	"anvilcord-starter-consumer",
	"anvilcord-starter"
)
