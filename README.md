# Nextome SDK Whitelabel app
![Nextome Android Sdk Image](artwork/cover.png)


Official integration docs are available [here](https://docs.nextome.dev/).

## Set up your environment
To use Nextome SDK with your licence:
 1. Provide your Artifactory credentials in `build.gradle` to download the SDK:
```groovy
    maven {
        url "https://nextome.jfrog.io/artifactory/nextome-libs-release-local"
        credentials {
            username = "your_username"
            password = "your_password"
        }
    }
```

 2. Add your SDK `secret` and `developerKey` in `NextomeSettings.kt`:
```kotlin
data class NextomeSettings(
    val secret: String = "secret_here",
    val developerKey: String = "developer_key_here",
    ...
```

A working example of this project is available on [Google Play here](https://play.google.com/store/apps/details?id=com.nextome.test). 