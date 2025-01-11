# Lunchtime Restaurant Discovery

Lunchtime Restaurant Discovery: An Android App That Demonstrates Best Practices

## Brief Project Description

LunchTime is a modern Android application built with best practices and latest technologies. This
project was created using the default Android Studio New App template (Empty Activity) and
implemented as a Kotlin Jetpack Compose project.

## Build Instruction

## Project Setup

For security best practices, sensitive information such as the Google Places API key is stored in
`gradle.properties` and excluded from GitHub. The file is included in the repository with a
placeholder - please replace it with the actual API key value before you build the project.

## Environment

- Android Studio Ladybug | 2024.2.1 Patch 2
- Gradle Version: 8.9

## Architecture & Design Patterns

- Clean Architecture with clear separation of concerns:
    - Data Layer (Repository Pattern)
    - Domain Layer (Use Cases/Business Logic)
    - Presentation Layer (MVVM with ViewModels)


- Single Activity architecture using Jetpack Compose
- Unidirectional Data Flow using `StateFlow`
- State Management using `sealed` classes for UI states
- State Encapsulation in ViewModels (`MutableStateFlow` field is always private)
- Error Handling with `Result`

## Key Technologies & Libraries

- UI:
    - Jetpack Compose for declarative UI
    - Material Design 3 components
    - Google Maps Compose 
- Networking:
    - [Ktor Client](https://ktor.io/docs/client-create-and-configure.html#configure-client) for HTTP requests 
      - Native coroutines support
      - Lightweight and flexible compared to Retrofit
      - Easy configuration and interceptors
    - [Coil Compose](https://coil-kt.github.io/coil/compose/) for image loading and caching
      - Built specifically for Compose with native integration
      - Memory and disk caching out of the box
      - Coroutines-based image loading
      - Smaller footprint compared to Glide/Picasso
    - Kotlin Serialization for JSON parsing
      - Compile-time type safety
      - Better performance than Gson/Moshi
      - Native Kotlin support with less boilerplate
      - Direct integration with Ktor
- Dependency Injection:
    - Hilt
- Asynchronous Operations:
    - Kotlin Coroutines & Flow
- Data Persistence:
    - DataStore for lightweight favorites storage
 - Location-based services:
    - [FusedLocationProvider](https://developers.google.com/location-context/fused-location-provider)
      - Simplified API compared to LocationManager
      - Battery-efficient
- Testing:
    - Unit tests with JUnit4
    - MockK for mocking
    - Coroutines test utilities
    - Custom test rules for [coroutines testing](https://developer.android.com/kotlin/coroutines/test)

## Screenshots

<table>
  <tr>
    <td>List View</td>
     <td>Map View</td>
     <td>Details View</td>
  </tr>
  <tr>
    <td><img src="Screenshot_1.png" width=270 height=555></td>
    <td><img src="Screenshot_2.png" width=270 height=555></td>
    <td><img src="Screenshot_3.png" width=270 height=555></td>
  </tr>
 </table>

## Documentation

- Google Places API
    - [Nearby-Search](https://developers.google.com/maps/documentation/places/web-service/nearby-search)
    - [Text-Search](https://developers.google.com/maps/documentation/places/web-service/text-search)
    - [Place-Photos](https://developers.google.com/maps/documentation/places/web-service/place-photos)
    - [Place-Details](https://developers.google.com/maps/documentation/places/web-service/place-details)
- Android Map Compose
    - [Documentation and Examples](https://github.com/googlemaps/android-maps-compose)
- Android DI (Dagger and Hilt)
    - [Cheatsheet](https://developer.android.com/training/dependency-injection/hilt-cheatsheet)
- Android Best Practices
    - [ViewModel/Repository/Coroutines](https://developer.android.com/kotlin/coroutines/coroutines-best-practices)
    - [Flow](https://developer.android.com/kotlin/flow)
    - [runCatching](https://dev.to/1noshishi/mastering-runcatching-in-kotlin-how-to-avoid-coroutine-cancellation-issues-5go2)