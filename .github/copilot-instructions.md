# AInewsMakerFetcher - GitHub Copilot Instructions

AInewsMakerFetcher is an Android mobile application written in Kotlin that fetches and displays AI news reports from the GitHub API. The app presents reports from the `spymag/AInewsMaker` repository in a filterable list view and provides detailed markdown rendering of individual reports.

Always reference these instructions first and fallback to search or bash commands only when you encounter unexpected information that does not match the info here.

## Working Effectively

### Prerequisites and Setup
- **CRITICAL**: Internet connectivity is REQUIRED for building and running this application
- Android SDK must be properly configured (API level 35-36)
- Java 11+ is required (confirmed working with Java 17)
- Ensure `ANDROID_HOME` and `ANDROID_SDK_ROOT` environment variables are set
- Network access to GitHub API (`api.github.com`) required for app functionality

### Bootstrap and Build Commands
- **NEVER CANCEL**: Initial Gradle setup takes 3-5 minutes downloading dependencies. Set timeout to 10+ minutes.
- Clean build: `./gradlew clean build` -- takes 5-10 minutes on first run. NEVER CANCEL. Set timeout to 15+ minutes.
- Debug build: `./gradlew assembleDebug` -- takes 2-5 minutes. NEVER CANCEL. Set timeout to 10+ minutes.
- Release build: `./gradlew assembleRelease` -- takes 3-7 minutes. NEVER CANCEL. Set timeout to 15+ minutes.

### Testing Commands
- Unit tests: `./gradlew test` -- takes 30-60 seconds. Set timeout to 5+ minutes.
- **NEVER CANCEL**: Instrumentation tests: `./gradlew connectedAndroidTest` -- takes 3-8 minutes. Set timeout to 15+ minutes.
- Test with build: `./gradlew clean test` -- combines clean and test, takes 3-6 minutes total.

### Common Gradle Tasks
- List all tasks: `./gradlew tasks`
- Clean workspace: `./gradlew clean`
- Check code style: `./gradlew check` (includes lint and test)
- Build and install debug APK: `./gradlew installDebug` (requires connected device/emulator)

## Known Issues and Limitations

### Build Failures
- **Network Required**: Build fails with "Plugin not found" errors in environments without internet access due to dependency resolution
- **AGP Version**: The Android Gradle Plugin version in `gradle/libs.versions.toml` may need adjustment based on repository availability
- **Firewall**: Corporate firewalls may block access to `dl.google.com` and `maven.google.com` repositories
- **DNS Blocking**: Some environments block DNS resolution for Google and GitHub domains

### Environment-Specific Issues
- Build succeeds on systems with internet connectivity to Google Maven and Maven Central repositories
- Cannot build in isolated environments or containers without external network access
- Android emulator/device required for instrumentation tests and app validation
- App requires access to GitHub API (`https://api.github.com/repos/spymag/AInewsMaker/contents/reports`)

## Validation Scenarios

### Network Connectivity Validation
Before attempting to build or run the app, ALWAYS verify network connectivity:

```bash
# Test basic internet connectivity
ping -c 1 google.com

# Test GitHub API access
curl -I "https://api.github.com/repos/spymag/AInewsMaker/contents/reports"

# Test Google Maven repository access
curl -I "https://dl.google.com/dl/android/maven2/"

# Test Maven Central access
curl -I "https://repo.maven.apache.org/maven2/"
```

If any of these tests fail, building and running the app will not work. Document connectivity issues in your analysis.

### Manual Testing Requirements
After making code changes, ALWAYS perform these validation steps:

1. **Network Validation**: Verify connectivity using commands above
2. **Build Validation**: Ensure `./gradlew assembleDebug` completes successfully
3. **Unit Test Validation**: Run `./gradlew test` and verify all tests pass
4. **App Functionality Test**: Install APK and verify:
   - App launches without crashes
   - Reports list loads from GitHub API (requires internet)
   - Date filtering works (From Date, To Date, Clear Filter buttons)
   - Tapping a report opens detail view with rendered markdown
   - Refresh action in menu updates the report list

### Critical User Flows to Test
- **Startup Flow**: Launch app → reports load from GitHub API → scroll through list
- **Filter Flow**: Tap From Date → select date → tap To Date → select date → verify filtered results
- **Detail Flow**: Tap any report → verify markdown content renders correctly → back button returns to list
- **Refresh Flow**: Menu → Refresh → verify updated reports appear
- **Network Error Handling**: Test behavior when GitHub API is unreachable (should handle gracefully)

### Expected App Behavior
- **Data Source**: Fetches `.md` files from `spymag/AInewsMaker` repository via GitHub API
- **Date Parsing**: Extracts dates from filenames using regex pattern `(\d{4}-\d{2}-\d{2})`
- **Markdown Rendering**: Uses CommonMark library to render markdown content as HTML
- **Sorting**: Reports displayed in descending order by date (newest first)
- **Filtering**: Date range filtering using LocalDate comparisons

## Code Structure and Navigation

### Key Source Files
- `app/src/main/java/com/spymag/ainewsmakerfetcher/MainActivity.kt` - Main list activity with date filtering
- `app/src/main/java/com/spymag/ainewsmakerfetcher/ReportActivity.kt` - Detail view for individual reports
- `app/src/main/java/com/spymag/ainewsmakerfetcher/Report.kt` - Data class representing a report
- `app/src/main/java/com/spymag/ainewsmakerfetcher/ReportAdapter.kt` - ListView adapter for report list

### Important Implementation Details
- **Network Calls**: Uses `HttpURLConnection` in background threads for GitHub API calls
- **UI Threading**: Proper `runOnUiThread` usage for UI updates from background threads
- **Error Handling**: Catches exceptions in network operations but may fail silently
- **Date Handling**: Uses `LocalDate` for date operations and `Calendar` for date picker
- **Window Insets**: Handles edge-to-edge display with proper inset handling

### Configuration Files
- `app/build.gradle.kts` - Module-level build configuration
- `build.gradle.kts` - Project-level build configuration  
- `gradle/libs.versions.toml` - Dependency version management
- `app/src/main/AndroidManifest.xml` - App manifest with INTERNET permission

### Layout Files
- `app/src/main/res/layout/activity_main.xml` - Main list layout with filter buttons
- `app/src/main/res/layout/activity_report.xml` - Report detail layout
- `app/src/main/res/layout/item_report.xml` - List item layout for reports
- `app/src/main/res/menu/main_menu.xml` - Menu with refresh action

### Test Files
- `app/src/test/java/com/spymag/ainewsmakerfetcher/ExampleUnitTest.kt` - Unit tests
- `app/src/androidTest/java/com/spymag/ainewsmakerfetcher/ExampleInstrumentedTest.kt` - Instrumentation tests

## Development Workflow

### Making Changes
1. ALWAYS run `./gradlew clean build` after making significant changes
2. Run unit tests with `./gradlew test` before committing
3. Test app functionality manually on device/emulator
4. Follow Kotlin coding conventions as specified in `AGENTS.md`
5. Verify network-dependent features work with live GitHub API

### Code Style and Quality
- Use 4-space indentation (no tabs)
- Follow Kotlin official coding conventions
- Keep functions small and focused
- Add KDoc comments for public APIs
- Handle network operations in background threads
- Use proper UI thread management for view updates

## Common Tasks Reference

### Repository Structure
```
AInewsMakerFetcher/
├── .github/
│   └── copilot-instructions.md
├── app/
│   ├── build.gradle.kts
│   ├── src/
│   │   ├── main/java/com/spymag/ainewsmakerfetcher/
│   │   ├── test/java/com/spymag/ainewsmakerfetcher/
│   │   └── androidTest/java/com/spymag/ainewsmakerfetcher/
├── build.gradle.kts
├── gradle/libs.versions.toml
├── gradlew / gradlew.bat
└── AGENTS.md
```

### Key Dependencies
- AndroidX Core KTX (1.17.0)
- AndroidX AppCompat (1.6.1)
- Material Design Components (1.10.0)
- CommonMark (0.21.0) - Markdown parsing
- JUnit (4.13.2) - Testing
- Espresso (3.7.0) - UI Testing

### Expected Build Times
- **NEVER CANCEL**: Clean build: 5-10 minutes (first time), 2-5 minutes (subsequent)
- Incremental build: 30 seconds - 2 minutes
- Unit tests: 30-60 seconds
- **NEVER CANCEL**: Instrumentation tests: 3-8 minutes
- **NEVER CANCEL**: Initial dependency download: 3-5 minutes

### API Endpoints Used
- `https://api.github.com/repos/spymag/AInewsMaker/contents/reports` - Fetches report list
- Individual report files accessed via `download_url` from GitHub API response

## Troubleshooting

### Common Build Issues
- "Plugin not found" → Check internet connectivity and repository access
- "SDK not found" → Verify ANDROID_HOME is set correctly
- Version conflicts → Check `gradle/libs.versions.toml` for compatible versions
- Network timeout → Increase Gradle timeout settings or check firewall rules

### Runtime Issues  
- Empty report list → Check internet connectivity for GitHub API access
- Crash on report tap → Verify URL format and markdown parsing dependencies
- UI layout issues → Check for device compatibility and theme conflicts
- Network errors → Verify GitHub API accessibility (`curl "https://api.github.com/repos/spymag/AInewsMaker/contents/reports"`)

### Testing Environment Issues
- Cannot run app functionality tests without internet connectivity
- Emulator required for full validation - cannot test on headless systems
- Some CI environments may block external API calls