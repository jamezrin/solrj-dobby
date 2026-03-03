# Releasing

Releases are automated via two GitHub Actions workflows.

## Required secrets

Configure these in the repository settings under **Settings → Secrets and variables → Actions**:

| Secret | How to obtain |
|--------|--------------|
| `MAVEN_CENTRAL_USERNAME` | Portal token username from [central.sonatype.com](https://central.sonatype.com) |
| `MAVEN_CENTRAL_PASSWORD` | Portal token password from [central.sonatype.com](https://central.sonatype.com) |
| `GPG_PRIVATE_KEY` | `gpg --export-secret-keys --armor KEY_ID \| base64 -w0` |
| `GPG_PASSWORD` | Your GPG key passphrase |

The GPG key must be uploaded to a public keyserver before publishing:

```bash
gpg --keyserver keyserver.ubuntu.com --send-keys KEY_ID
gpg --keyserver keys.openpgp.org --send-keys KEY_ID
```

## Release process

### 1. Trigger the Release workflow

Go to **Actions → Release → Run workflow** and select the bump type:

- `patch` - bug fixes (1.0.1 → 1.0.2)
- `minor` - new features, backward-compatible (1.0.1 → 1.1.0)
- `major` - breaking changes (1.0.1 → 2.0.0)

The workflow will:
1. Compute the new version
2. Update `build.gradle.kts` and `README.md`
3. Commit and push to `main`
4. Create and push the tag `vX.Y.Z`
5. Create a **draft** GitHub Release with auto-generated release notes

### 2. Review and publish the draft release

Go to **Releases**, review the draft and the generated changelog, edit the notes if needed, then click **Publish release**.

### 3. Maven Central publishes automatically

Publishing the release triggers the **Publish** workflow, which:
1. Runs the full test suite (unit + integration)
2. Signs and publishes artifacts to Maven Central

Artifacts appear on Maven Central within 10-30 minutes after the workflow completes.
