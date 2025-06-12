plugins {
    java
    id("socotra-ec-config-developer") version "v0.6.5"
}

`socotra-config-developer` {
    apiUrl.set(System.getenv("SOCOTRA_KERNEL_API_URL") ?: "")
    tenantLocator.set(System.getenv("SOCOTRA_KERNEL_TENANT_LOCATOR") ?: "")
    personalAccessToken.set(System.getenv("SOCOTRA_KERNEL_ACCESS_TOKEN") ?: "")
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.8.2")
}
