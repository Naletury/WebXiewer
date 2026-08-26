![WebXiewer](./icon.svg)

# Overview

due to [x for android](https://play.google.com/store/apps/details?id=com.twitter.android) enforcing enhanced [play integrity checks](https://developer.android.com/google/play/integrity/verdicts#optional-device-labels) and stricter vpn/proxy detection, many devices fail the `MEETS_STRONG_INTEGRITY` verification (often due to missing gapps, unlocked bootloader, or custom rom) and cannot sign in or sign up.

WebXiewer bypasses these restrictions by using the mobile web interface, while providing an experience as close as possible to the native app.

>[!WARNING]
> this app does **NOT** block ads or provide any vpn services.

# Features

- [x] full-featured X web interface
  - [ ] sign up
- [ ] multiple account support
- [x] custom User-Agent settings

# Versioning

WebXiewer uses a structured versioning scheme to view the full rules:

you can run `scripts/version.sh` without any arguments to see the basic rules:

```bash
bash scripts/version.sh
```

it will output:

```
Usage: ./v <tag>
  - <tag>: tag name
    - must start with v
    - format: vM.mTn
      - M: Major version code
        value range expression: {x | x ∈ ℕ}
      - m: minor version code
        value range expression: {x | x ∈ [0, 10), x ∈ ℕ}
      - T: Type name (r|a|b|p)
        - r: release
          *when selecting this type, there is no need to fill in the value of n
        - a: alpha version (early beta version)
        - b: beta version
        - p: patch or fix
      - n: Number of sub-version
        *when selecting the type of release, there is no need to fill in the value of this
        value range expression: {x | x ∈ [0, 100), x ∈ ℕ}
```

## Version Code and Version Name mapping

the version code is derived from the tag, with a decrement rule for alpha/beta versions:

|  Tag   | Version Name | Version Code |  Type   | Note                            |
| :----: | :----------: | :----------: | :-----: | :------------------------------ |
| v1.0r  | 1.0-release  |    10000     | release | no decrement                    | 
| v2.2a1 |  2.2-alpha1  |    21101     |  alpha  | minor 2 → code minor 1          |   
| v2.0a1 |  2.0-alpha1  |    19101     |  alpha  | minor 0 → code major 1, minor 9 |     
| v1.5b2 |  1.5-beta2   |    14202     |  beta   | minor 5 → code minor 4          |     
| v2.2p1 |  2.2-patch1  |    22001     |  patch  | no decrement                    |

## Distinguish between release and patch

for release versions, the number of sub-version should always be 0, while it cannot be 0 for patch version. this is to make sure that the version code of patch versions is always higher than release.

## Decrement rule

for alpha and beta versions, the version code uses one minor version lower than the tag name. this ensures that alpha/beta builds are correctly ordered below their corresponding release builds.

  - v2.2a1 → version code uses 2.1 (minor decremented by 1)
  - v2.0a1 → version code uses 1.9 (major decremented by 1, minor wraps to 9)
  - v0.0a1 is invalid (cannot decrement further)

# Building and publishing

## Fork and clone

```bash
git clone https://github.com/yourName/WebXiewer
cd WebXiewer
```

## Development workflow

```bash
# switch to dev branch
git switch dev

# make your changes
git add -A
git commit -m "feat: your description"
git push
```

## Test

```bash
# merge to test branch
git switch test
git merge dev
git push
```

## Trigger a manual build:

1. go to `Actions → Manual Build`
2. select branch: `test`
3. fill in version info
4. click Run workflow

## Release

```bash
# merge to main
git switch main
git merge test

# generate version info
bash scripts/version.sh v2.2a1

# publish
bash scripts/publish.sh
```

the release workflow will automatically:

  - build the release apk
  - generate release notes
  - create a github release with the apk attached

## Build signed apk via actions

if you need to build the apk signed with your own keystore, you need to complete the following configurations:

### Generate a keystore

```bash
keytool -genkey -v -keystore release.keystore -alias your_alias -keyalg RSA -keysize 2048 -validity 10000
```

place the generated `release.keystore` file in the root of the project.

### Add repository secrets

go to your repository `Settings → Secrets and variables → Actions` and add the following secrets:

|    Secret Name     | Description                    |
| :----------------: | :----------------------------- |
|  KEYSTORE\_BASE64  | base64 encoded keystore file   |
| KEYSTORE\_PASSWORD | keystore password              |
|     KEY\_ALIAS     | key alias used in the keystore |
|   KEY\_PASSWORD    | key password                   |

### Encode your keystore to base64

```bash
base64 -i release.keystore -o release.keystore.b64
```

copy the content of `release.keystore.b64` and paste it as the value of `KEYSTORE_BASE64`.

### Verify

once configured, the release workflow will automatically sign the apk using your keystore. the signed apk will be attached to the github release.

## Force publish

if you need to overwrite an existing tag:

```bash
bash scripts/publish.sh -f
```
this will delete local and remote git tag with the same name (if have) and re-create a same tag.

# GitHub actions

|      workflow       | trigger(s)                             | output                                                                 |
| :-----------------: | :------------------------------------- | :--------------------------------------------------------------------- |
| **Android Release** | push tag `v*` (use ci publish scripts) | signed/unsigned release apk + release notes attached to github release |
|  **Manual Build**   | manual                                 | debug apk (artifact, 7-day retention)                                  |

## Android Release

triggered automatically when pushing a tag matching `v*`. this workflow:

  1. builds the release apk (unsigned if secrets are not configured)
  2. generates release notes from commit history
  3. creates a github release with the apk attached

## Manual Build

triggered manually from the actions tab. this workflow:

  1. builds a debug apk
  2. uploads it as an artifact (available for 7 days)

# License

[GNU GENERAL PUBLIC LICENSE Version 3](./LICENSE)
