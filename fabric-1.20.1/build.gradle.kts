plugins {
    id("fabric-loom") version "1.17.20"
    id("java")
}

base.archivesName = "${property("mod_archives_name")}-fabric"
group = property("mod_maven_group") as String
version = property("mod_version") as String

repositories {
    maven { url = uri("https://maven.terraformersmc.com/") }
}

loom {
    accessWidenerPath = file("src/main/resources/${property("mod_access_widener_name")}")
}

dependencies {
    minecraft("com.mojang:minecraft:${property("minecraft_version")}")
    mappings(loom.officialMojangMappings())
    modImplementation("net.fabricmc:fabric-loader:${property("fabric_loader_version")}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${property("fabric_api_version")}")
    modCompileOnly("com.terraformersmc:modmenu:${property("modmenu_version")}")
}

java {
    withSourcesJar()
    toolchain.languageVersion = JavaLanguageVersion.of(17)
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release = 17
}

tasks.withType<Jar> {
    from(rootProject.file("../COPYING"))
    from(rootProject.file("../COPYING.LESSER"))
    from(rootProject.file("../licenses")) { into("licenses") }
}

tasks.named<ProcessResources>("processResources") {
    val placeholders = mapOf(
        "mod_license" to project.findProperty("mod_license"),
        "mod_version" to project.version,
        "mod_id" to project.findProperty("mod_id"),
        "mod_name" to project.findProperty("mod_name"),
        "mod_homepage_url" to project.findProperty("mod_homepage_url"),
        "mod_description" to project.findProperty("mod_description"),
        "mod_mixin_config" to project.findProperty("mod_mixin_config"),
        "mod_issues_url" to project.findProperty("mod_issues_url"),
        "mod_fabric_sources_url" to project.findProperty("mod_fabric_sources_url"),
        "fabric_loader_version" to project.findProperty("fabric_loader_version"),
        "fabric_api_version" to project.findProperty("fabric_api_version"),
        "fabric_api_dependency_version" to project.findProperty("fabric_api_dependency_version"),
        "fabric_minecraft_version_range" to project.findProperty("fabric_minecraft_version_range"),
        "modmenu_version" to project.findProperty("modmenu_version"),
    )
    inputs.properties(placeholders)
    filesMatching("fabric.mod.json") { expand(placeholders) }
}
