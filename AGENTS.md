# AGENTS Instructions

This file contains guidelines for contributing to the **AInewsMakerFetcher** project. Its scope covers the entire repository.

## Code Style
- Use **Kotlin** for production code and adhere to the official [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html).
- Indent using 4 spaces. Avoid tabs.
- Name source files and classes using `PascalCase`.
- Keep functions small and focused; add KDoc comments for public APIs when necessary.

## Git Practices
- Avoid committing build artifacts, local properties, or IDE-specific files.
- Use clear, descriptive commit messages. Each commit should represent a single logical change.

## Testing
- After making changes, run `./gradlew test` to ensure the project builds and tests pass.
- Include the test command and its result in pull request descriptions.

## Pull Requests
- Reference relevant issues where applicable.
- Summarize your changes and note any follow-up work or limitations.
- Ensure your PR description mentions that you have read and followed this `AGENTS.md` file.

