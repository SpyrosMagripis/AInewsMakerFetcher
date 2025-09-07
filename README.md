# AInewsMakerFetcher

An Android application that fetches and displays AI news reports and daily action items from GitHub repositories. The app provides an elegant interface for browsing markdown content with GitHub-flavored rendering, date filtering, and AI-powered summaries.

## Features

### 📱 **Dual-Tab Interface**
- **AI News Tab**: Browse reports from the `spymag/AInewsMaker` repository
- **Actions Tab**: View daily action items from `SpyrosMagripis/FilesServer`

### 📊 **Advanced Markdown Rendering**
- GitHub-flavored markdown support with tables, strikethrough, and task lists
- Custom CSS styling that matches GitHub's appearance  
- WebView-based rendering with zoom controls
- Dark mode support that adapts to system theme
- Syntax highlighting placeholders for code blocks

### 🔍 **Smart Filtering & Navigation**
- Date range filtering with intuitive date pickers
- Chronological sorting (newest first)
- Pull-to-refresh functionality
- Seamless navigation between list and detail views

### 🤖 **AI-Powered Summaries**
- OpenAI integration for generating summaries of recent reports
- Text-to-speech functionality for audio summaries
- Configurable summary timeframes
- Background processing with progress indicators

### 🎨 **Modern UI/UX**
- Material Design components
- Edge-to-edge display with proper inset handling
- Responsive layout optimized for mobile devices
- Smooth tab navigation with ViewPager2

## Requirements

### System Requirements
- **Android API Level**: 35-36 (Android 14+)
- **Java**: 11 or higher
- **Android SDK**: API level 35-36 configured
- **Internet Connection**: Required for fetching reports and summaries

### Development Requirements
- Android Studio with Kotlin support
- Gradle 8.13+
- Android Gradle Plugin 8.13.0

## Installation & Setup

### 1. Clone the Repository
```bash
git clone https://github.com/SpyrosMagripis/AInewsMakerFetcher.git
cd AInewsMakerFetcher
```

### 2. Configure API Keys (Optional)
Create a `local.properties` file in the project root:

```properties
# Optional: For AI summary generation
OPENAI_API_KEY=your_openai_api_key_here

# Optional: For private repository access
repoPat=your_github_personal_access_token
```

### 3. Build the Application

⚠️ **Important**: Internet connectivity is required for building due to dependency resolution.

```bash
# Clean and build (first time may take 5-10 minutes)
./gradlew clean build

# Debug build
./gradlew assembleDebug

# Release build  
./gradlew assembleRelease
```

### 4. Install on Device
```bash
# Install debug APK to connected device/emulator
./gradlew installDebug
```

## Configuration

### GitHub Personal Access Token
To access private repositories or increase API rate limits, configure a GitHub PAT in `local.properties`:

```properties
repoPat=ghp_your_token_here
```

The token needs `repo` scope for private repository access.

### OpenAI API Integration
For AI summary generation, add your OpenAI API key:

```properties
OPENAI_API_KEY=sk-your_key_here
```

Without this key, the summary feature will display "OpenAI key not configured."

## Usage

### Basic Navigation
1. **Launch** the app to see the AI News tab by default
2. **Switch tabs** using the bottom tab bar to view Actions
3. **Tap any report** to view detailed markdown content
4. **Use date filters** to narrow down reports by timeframe
5. **Pull down** to refresh the content

### Filtering Reports
- **From Date**: Set the earliest date for reports
- **To Date**: Set the latest date for reports  
- **Clear**: Remove all date filters

### AI Summaries
1. Select "Last 3 days" from the summary dropdown
2. Wait for the AI to generate a summary
3. Tap "Listen to summary" for text-to-speech playback

### Markdown Features
The app supports GitHub-flavored markdown including:
- Headers with proper hierarchy
- **Bold** and *italic* text
- Code blocks with language detection
- Tables with proper formatting
- ~~Strikethrough~~ text
- Task lists with checkboxes
- Automatic link detection

## Architecture

### Core Components
- **MainActivity**: Tab-based navigation controller
- **ReportsFragment**: AI news list and filtering
- **ActionsFragment**: Daily actions display
- **ReportActivity**: Detailed markdown viewer
- **SummaryAudioService**: Text-to-speech background service

### Data Sources
- `https://api.github.com/repos/spymag/AInewsMaker/contents/reports`
- `https://api.github.com/repos/SpyrosMagripis/FilesServer/contents/ActionsForToday`

### Key Libraries
- **CommonMark** (0.21.0): Markdown parsing and rendering
- **AndroidX**: Core Android components
- **Material Design**: UI components
- **ViewPager2**: Tab navigation

## Development

### Building & Testing
```bash
# Run unit tests
./gradlew test

# Run instrumentation tests (requires device/emulator)
./gradlew connectedAndroidTest

# Check code style and run all tests
./gradlew check
```

### Code Style
- Follow [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)
- Use 4-space indentation (no tabs)
- Add KDoc comments for public APIs
- Keep functions small and focused

## Troubleshooting

### Build Issues

**"Plugin not found" errors:**
- Ensure internet connectivity for dependency resolution
- Check access to `dl.google.com` and `maven.google.com`
- Verify firewall settings

**Network connectivity test:**
```bash
# Test basic connectivity
ping google.com

# Test GitHub API
curl -I "https://api.github.com/repos/spymag/AInewsMaker/contents/reports"

# Test Google Maven
curl -I "https://dl.google.com/dl/android/maven2/"
```

**SDK issues:**
- Verify `ANDROID_HOME` and `ANDROID_SDK_ROOT` environment variables
- Ensure Android SDK API level 35-36 is installed

### Runtime Issues

**Empty reports list:**
- Check internet connection
- Verify GitHub API accessibility
- Confirm repository exists and is public

**Summary generation fails:**
- Verify OpenAI API key in `local.properties`
- Check API key permissions and credits
- Ensure network connectivity to OpenAI API

**Private repository access:**
- Configure GitHub PAT with appropriate permissions
- Verify token is correctly set in `local.properties`

## Contributing

1. Read the `AGENTS.md` file for contribution guidelines
2. Fork the repository and create a feature branch
3. Follow the code style guidelines
4. Run tests before submitting: `./gradlew test`
5. Include test results in pull request descriptions
6. Reference relevant issues in your PR

## License

This project is available under the terms specified in the repository. Please check the LICENSE file for details.

## Support

For issues and feature requests, please use the GitHub Issues tab. When reporting issues, include:
- Android version and device information
- Steps to reproduce the issue
- Relevant error messages or logs
- Network connectivity test results (if applicable)

---

**Note**: This application requires internet connectivity to function properly. Ensure stable network access to GitHub API endpoints for optimal experience.