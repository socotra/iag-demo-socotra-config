rootProject.name = "ec-config-template"

pluginManagement {
    repositories {
        mavenCentral()
        maven("https://maven.pkg.github.com/socotra/config-sdk-template") {
            credentials {
                username = ""
                password = ""
            }
        }
    }
}
