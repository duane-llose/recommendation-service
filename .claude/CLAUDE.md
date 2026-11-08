# CLAUDE.md

## Project Overview
`recommendation-service` is a stateless aggregator. Calls movie-service and user-service to build personalized home page recommendations. Part of the Netflux distributed system.

## Root Package
`com.vinsguru.netflux.recommendation`

## Upstream Dependencies
- `movie-service`
  - Base URL configured via `app.movie-service.base-url`
- `user-service`
  - Base URL configured via `app.user-service.base-url`

## Active Tech Stack
- **Backend:** Java 25, Spring Boot 4.1.0, Maven
- **Testing:** JUnit 5, MockRestServiceServer (mocking upstream HTTP calls)

## Core Operational Commands

### Development & Build Lifecycle
- Build project: `./mvnw clean compile`
- Package production artifact: `./mvnw clean package`
- Run locally: `./mvnw spring-boot:run`

### Testing Lifecycle
- Run all tests: `./mvnw test`
- Run a single test class: `./mvnw test -Dtest=ClassName`