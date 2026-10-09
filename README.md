# Библиотечная информационная система

Курсовой проект по ООП: Панков В. Д., М412. Руководитель — Е. О. Шумова.
Настольное приложение на Kotlin и Compose Desktop. Данные сохраняются локально в SQLite через Room.

## Запуск на другом компьютере

Нужны **JDK 17**, Git и интернет для первого скачивания зависимостей. Gradle отдельно устанавливать не нужно: wrapper включён в репозиторий. Запуск требует графического рабочего стола; это не консольная программа.

### macOS / Linux

```sh
git clone https://github.com/pank-suai/course-oop-library.git
cd course-oop-library
java -version
./gradlew :app:run --no-daemon
```

Если запускаешь из скачанных файлов, а не из `git clone`, перед запуском выполни `chmod +x gradlew`.

### Windows (PowerShell)

```powershell
git clone https://github.com/pank-suai/course-oop-library.git
cd course-oop-library
java -version
.\gradlew.bat :app:run --no-daemon
```

`java -version` должен показывать установленный JDK 17; `JAVA_HOME` должен указывать на него. Если команда `java` не найдена, установи JDK и открой терминал заново. При первом запуске Gradle скачивает зависимости, поэтому запуск может занять несколько минут.

Приложение не заполняет базу автоматически. Для демонстрации нажми «Тестовые книги», затем «Тестовые читатели», либо открой «Архитектура» → «Добавить демоданные». **Не нажимай «Очистить данные», если нужны существующие записи.**

Личная база не хранится в репозитории и не переносится вместе с `git clone`. На другом компьютере будет своя база:
- macOS: `~/Library/Application Support/CourseOopLibrary/library.db`;
- Linux: `~/.local/share/course-oop-library/library.db`;
- Windows: `%LOCALAPPDATA%/CourseOopLibrary/library.db`.

## Возможности

Четыре русские вкладки: «Читатели», «Издания», «Выдачи», «Архитектура». Регистрация и поиск, книги и периодика, свободный остаток и филиал, выдача с проверкой лимита, возврат и история. Защищено удаление связанных записей; выдача и возврат выполняются транзакционно.

Учебные кнопки добавляют вымышленных читателей и книги без очистки существующих записей. Повторное добавление не создаёт дубликаты. Обложки известных учебных ISBN, шрифт Inter и векторные значки встроены; в работе приложения сеть не нужна.

## Структура

- `app/` — окно, формы, состояния и UI-тесты;
- `core/` — объекты предметной области, правила, Room/SQLite и тесты;
- `gradlew`, `gradlew.bat`, `gradle/` — Gradle wrapper;
- `docs/` — актуальная записка, исходники Typst, UML PlantUML и снимки интерфейса;
- `context/bibliography-profile/` — профиль оформления записки;
- `presentation/` — презентация ГУАП и исходники;
- `prep/` — памятка для защиты;
- `artifacts/` — результаты проверок; `artifacts/history/` относится к прежней версии и не служит одобрением текущей.

## Документы

- [Пояснительная записка](docs/report.pdf): 100 страниц без титула и задания, 16 источников; полные листинги 26 производственных файлов, без А.1/А.2, с увеличенным интервалом.
- [Презентация](presentation/course-oop-library.pdf): 15 слайдов.
- [Памятка](prep/defense-notes.pdf): 3 страницы; не часть сдаваемой записки.

Титул и подписанное задание добавляются отдельно. Подписи, оценка и дата согласования за преподавателя не заполнялись. В личный кабинет работа не отправлялась.

## Тесты и сборка

```sh
./gradlew clean test build --no-daemon
./gradlew :docs:compileTypst --no-daemon
./gradlew :app:createDistributable --no-daemon
```

В Windows замени `./gradlew` на `.\gradlew.bat`. Установочные пакеты создаются на соответствующей ОС (`:app:packageDmg`, `:app:packageMsi`, `:app:packageDeb`). Готовые пакеты для всех ОС не заявляются: подтверждённые результаты находятся в `artifacts/publication-verification.json`.

При установленном Typst документы можно собрать из корня:

```sh
typst compile --root . docs/index.typ docs/report.pdf
typst compile --root . presentation/main.typ presentation/course-oop-library.pdf
typst compile --root . prep/index.typ prep/defense-notes.pdf
```

Для приложения Typst и шрифты записки не нужны. Тестовый набор содержит 67 проверок: 17 core и 50 app. Тесты используют отдельные временные базы. Нативный запуск ранее проверен на macOS; Windows/Linux не выдаются за вручную испытанные ОС.

## Архитектура и ограничения

`Compose UI → ViewModel / StateFlow → LibraryService → репозитории → Room → SQLite`.
Reader и Librarian реализуют Person, Book и Periodical — Publication. LibraryBranch и Loan также реализуют LibraryEntity. Loan связывает читателей и издания отношением «многие ко многим».

Применены Repository, MVVM, Strategy, Factory, Observer, State и Facade. Это локальная учебная система: без сетевого доступа, бронирования, штрафов, отдельных форм управления филиалами и сотрудниками. Экземпляры учитываются количеством, а не индивидуальными инвентарными номерами.
