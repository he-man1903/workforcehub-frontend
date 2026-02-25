plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    id("java")
}

dependencies {
    // Web + Security
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")

    // PostgreSQL
    runtimeOnly("org.postgresql:postgresql")

    // Redis — refresh token storage
    implementation("org.springframework.boot:spring-boot-starter-data-redis")

    // JWT (internal token generation)
    implementation("io.jsonwebtoken:jjwt-api:0.12.5")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.5")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.5")

    // Google ID token verification
    implementation("com.google.api-client:google-api-client:2.4.0")
    implementation("com.google.auth:google-auth-library-oauth2-http:1.23.0")

    // Rate limiting (bucket4j)
    implementation("com.bucket4j:bucket4j-core:8.10.1")
    implementation("com.bucket4j:bucket4j-redis:8.10.1")

    // Metrics
    implementation("io.micrometer:micrometer-registry-prometheus")

    // Logging
    implementation("net.logstash.logback:logstash-logback-encoder:7.4")

    // OpenAPI / Swagger (annotations + models + UI)
    // 2.6.x is compatible with Spring Boot 3.3.x
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.6.0")
}
