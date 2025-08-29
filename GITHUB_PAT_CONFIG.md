# GitHub PAT Configuration

## Question: What property do I need in local.properties for the API key?

**Answer: `GITHUB_PAT`**

## Configuration Steps

1. Create a `local.properties` file in the root directory of the project (same level as `build.gradle.kts`)

2. Add your GitHub Personal Access Token with the property name `GITHUB_PAT`:

```properties
GITHUB_PAT=your_github_personal_access_token_here
```

## Why This Property Name?

The Android app code references `BuildConfig.GITHUB_PAT` in multiple places:

- `ActionsFragment.kt` (line ~60): `val githubPat = BuildConfig.GITHUB_PAT`
- `ReportActivity.kt` (line ~44): `val githubPat = BuildConfig.GITHUB_PAT`

The build configuration in `app/build.gradle.kts` reads this property from `local.properties` and makes it available to the code via BuildConfig:

```kotlin
val githubPat = localProps.getProperty("GITHUB_PAT", "")
buildConfigField("String", "GITHUB_PAT", "\"$githubPat\"")
```

## Complete local.properties Example

```properties
# GitHub Personal Access Token for accessing private repositories
GITHUB_PAT=ghp_your_actual_token_here

# OpenAI API Key (if needed for other features)
OPENAI_API_KEY=your_openai_key_here
```

## Getting a GitHub Personal Access Token

1. Go to GitHub → Settings → Developer settings → Personal access tokens → Tokens (classic)
2. Generate a new token with appropriate repository access permissions
3. Copy the token and paste it as the value for `GITHUB_PAT` in your `local.properties` file

## Security Note

The `local.properties` file is already included in `.gitignore` to prevent accidentally committing your API keys to version control.