# Releasing

## Prerequisites

- GPG key uploaded to `keyserver.ubuntu.com` and `keys.openpgp.org`
- Maven Central credentials configured in `~/.gradle/gradle.properties`:

```properties
mavenCentralUsername=<token username>
mavenCentralPassword=<token password>
signing.keyId=<short 8-char key ID>
signing.secretKeyRingFile=/path/to/.gnupg/secring.gpg
signing.password=<key passphrase>
```

## Publishing

1. Update `version` in `build.gradle.kts`
2. Run `./gradlew publishToMavenCentral`
3. Go to https://central.sonatype.com/publishing/deployments and click "Publish"
4. Artifacts appear on Maven Central within 10-30 minutes
