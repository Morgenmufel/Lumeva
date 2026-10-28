plugins { id("lumeva.java-service") }

dependencies {
    implementation ("org.springframework.boot:spring-boot-starter-webmvc")
    implementation ("org.springframework.boot:spring-boot-starter-validation")

    implementation ("org.springframework.boot:spring-boot-starter-security")
    implementation ("org.springframework.boot:spring-boot-starter-security-oauth2-resource-server")

    implementation ("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation ("org.postgresql:postgresql")
    implementation ("org.springframework.boot:spring-boot-starter-liquibase")

    implementation ("org.springframework.boot:spring-boot-starter-cache")
    implementation ("com.github.ben-manes.caffeine:caffeine")
    implementation ("org.springframework.boot:spring-boot-starter-data-redis")
    implementation ("org.springframework.boot:spring-boot-starter-kafka")
    implementation (project(":libs:event-contracts"))
    // kafka-clients берём из BOM Spring Boot, а не тот, что тянет Confluent
    implementation ("io.confluent:kafka-avro-serializer:8.1.6") {
        exclude(group = "org.apache.kafka")
    }

    implementation ("org.springframework.boot:spring-boot-starter-actuator")
    implementation ("org.springframework.boot:spring-boot-starter-data-mongodb")
    compileOnly ("org.projectlombok:lombok")
    annotationProcessor ("org.projectlombok:lombok")

    implementation ("org.mapstruct:mapstruct:1.6.2")
    annotationProcessor ("org.mapstruct:mapstruct-processor:1.6.2")
    annotationProcessor ("org.projectlombok:lombok-mapstruct-binding:0.2.0")

    testImplementation ("org.springframework.boot:spring-boot-starter-test")
    testImplementation ("org.springframework.boot:spring-boot-starter-security-test")
    testImplementation ("org.springframework.kafka:spring-kafka-test")
    testImplementation ("org.springframework.boot:spring-boot-testcontainers")
    testImplementation ("org.awaitility:awaitility")
    testImplementation("org.testcontainers:testcontainers-junit-jupiter")
    testImplementation("org.testcontainers:testcontainers-postgresql")
    testImplementation("org.testcontainers:testcontainers-kafka")
    testImplementation("org.testcontainers:testcontainers-mongodb")

}
