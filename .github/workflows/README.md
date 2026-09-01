# CI проекта

Workflow `ci.yml` проверяет только Desktop JVM-вариант:

- `./gradlew test` — запускает тесты `core:jvmTest` и `app:jvmTest`;
- `./gradlew build` — собирает модули и выполняет проверки;
- `./gradlew :docs:compileTypst` — компилирует пояснительную записку.

Workflow выполняется на Temurin Java 17 и загружает JUnit-отчёты как artifact.

Web/WASM-сборка и публикация GitHub Pages удалены, поскольку методичка требует настольное JVM-приложение.
