# Contributing to Simple Death Bans

Thank you for your interest in contributing to Simple Death Bans.

## Getting Started

### Prerequisites
- Java 21 or higher
- Git
- An IDE with Gradle support (IntelliJ IDEA recommended)

### Setup
1. Fork the repository
2. Clone your fork: `git clone https://github.com/YOUR_USERNAME/SimpleDeathBans.git`
3. Import as a Gradle project in your IDE
4. Run `./gradlew genSources` to generate Minecraft sources
5. Run `./gradlew build` to verify setup

## Development

### Building
```bash
./gradlew build
```

### Testing
1. Run `./gradlew runClient` for client testing
2. Run `./gradlew runServer` for server testing

### Code Style
- Follow existing code conventions
- Use meaningful variable and method names
- Add comments for complex logic
- Keep methods focused and concise

## Submitting Changes

### Pull Requests
1. Create a feature branch from `main`
2. Make your changes with clear, atomic commits
3. Test your changes thoroughly
4. Update documentation if applicable
5. Submit a pull request with a clear description

### Commit Messages
- Use present tense ("Add feature" not "Added feature")
- Keep the first line under 72 characters
- Reference issues when applicable

### Pull Request Guidelines
- One feature or fix per pull request
- Include relevant issue numbers
- Ensure the build passes
- Update CHANGELOG.md for notable changes

## Reporting Issues

### Bug Reports
Include:
- Minecraft version
- Mod version
- Fabric Loader version
- Steps to reproduce
- Expected vs actual behavior
- Crash logs if applicable

### Feature Requests
- Describe the feature clearly
- Explain the use case
- Consider implementation complexity

## Code of Conduct

- Be respectful and constructive
- Focus on the code, not the person
- Help others learn and improve

## License

By contributing, you agree that your contributions will be licensed under the MIT License.

## Questions

For questions, open an issue or reach out through the project's GitHub Discussions.
