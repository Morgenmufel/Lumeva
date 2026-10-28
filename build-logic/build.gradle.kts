plugins { `kotlin-dsl` }
repositories { gradlePluginPortal(); mavenCentral() }
dependencies {
    implementation("org.springframework.boot:spring-boot-gradle-plugin:4.1.1")
    implementation("org.apache.avro:avro-compiler:1.12.2")
}