# CI проекта

Workflow `ci.yml` работает на Ubuntu с Temurin JDK 17. `xvfb-run` создаёт виртуальный дисплей для настоящих Compose UI-тестов.

- `./gradlew clean test build` — тесты core/app и сборка Desktop JVM;
- `./gradlew :docs:compileTypst` — компиляция записки;
- JUnit-отчёты сохраняются при любом результате.

Это Linux CI, не доказательство ручного запуска интерфейса на Windows или macOS. Web/WASM и GitHub Pages не используются.
