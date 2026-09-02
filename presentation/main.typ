#import "lib/guap-template.typ": *

#show: presentation.with(
  title: [Объектно-ориентированная библиотечная система],
  subtitle: [Курсовой проект · Room 3 · SQLite · Compose Desktop],
  author: ("Панков Василий Дмитриевич", ),
)

#let card(heading, body, fill: guap-blue.lighten(92%), stroke: guap-blue) = {
  block(width: 100%, fill: fill, radius: 8pt, inset: 10pt, stroke: (paint: stroke, thickness: 1.2pt))[
    #text(size: 30pt, weight: "bold", fill: guap-dark-blue)[#heading]\
    #text(size: 28pt, fill: black)[#body]
  ]
}

#let wide-card(heading, body, fill: guap-blue.lighten(92%), stroke: guap-blue) = {
  block(width: 100%, fill: fill, radius: 8pt, inset: 10pt, stroke: (paint: stroke, thickness: 1.2pt))[
    #text(size: 30pt, weight: "bold", fill: guap-dark-blue)[#heading]\
    #text(size: 28pt, fill: black)[#body]
  ]
}

#let compact-card(heading, body, fill: guap-blue.lighten(92%), stroke: guap-blue) = {
  block(width: 100%, fill: fill, radius: 8pt, inset: 8pt, stroke: (paint: stroke, thickness: 1.2pt))[
    #align(left + horizon)[
      #text(size: 28pt, weight: "bold", fill: guap-dark-blue)[#heading: ]
      #text(size: 28pt, fill: black)[#body]
    ]
  ]
}

#let requirement-line(label, fill: guap-blue.lighten(94%), stroke: guap-blue) = {
  block(width: 100%, fill: fill, radius: 8pt, inset: 8pt, stroke: (paint: stroke, thickness: 1.2pt))[
    #align(left + horizon)[#text(size: 30pt, fill: guap-dark-blue, weight: "bold")[#label]]
  ]
}

#let diagram(path, height: 5.15in) = align(center + horizon)[
  #image(path, width: 100%, height: height, fit: "contain")
]

#slide(title: "Проблема и цель")[
  #grid(
    columns: (1fr, 1fr),
    gutter: 18pt,
    card([Проблема], [Единая модель читателей, фонда и выдач.], fill: guap-red.lighten(92%), stroke: guap-red),
    card([Цель], [Надёжное Desktop-приложение с Room и связью M:N.]),
  )
  #v(18pt)
  #wide-card([Фокус], [ООП · Room 3 · транзакции · реактивный пользовательский интерфейс], fill: guap-magenta.lighten(93%), stroke: guap-magenta)
]

#slide(title: "Требования и ограничения")[
  #grid(
    columns: (1fr, 1fr),
    gutter: 12pt,
    requirement-line([1. LibraryEntity]),
    requirement-line([2. Reader ↔ Publication], fill: guap-cyan.lighten(94%), stroke: guap-cyan),
    requirement-line([3. Room 3 + SQLite], fill: guap-magenta.lighten(94%), stroke: guap-magenta),
    requirement-line([4. Desktop JVM], fill: guap-blue.lighten(95%), stroke: guap-blue),
    requirement-line([5. Семь паттернов], fill: guap-red.lighten(94%), stroke: guap-red),
  )
]

#slide(title: "Доменная модель и иерархия")[
  #diagram("diagrams/domain-overview.svg", height: 4.5in)
]

#slide(title: "Реляционная модель Room")[
  #diagram("diagrams/room-overview.svg", height: 3.0in)
  #v(10pt)
  #compact-card([Гарантии], [M:N через loans · внешние ключи RESTRICT · транзакционные операции])
]

#slide(title: "Архитектура приложения")[
  #grid(
    columns: (1fr, 1fr, 1fr),
    gutter: 12pt,
    compact-card([Compose UI], [события и отображение]),
    compact-card([ViewModel], [StateFlow и валидация], fill: guap-magenta.lighten(94%), stroke: guap-magenta),
    compact-card([LibraryService], [сценарии Facade], fill: guap-cyan.lighten(94%), stroke: guap-cyan),
    compact-card([Repository], [доменная граница]),
    compact-card([Room DAO], [транзакции и Flow], fill: guap-magenta.lighten(94%), stroke: guap-magenta),
    compact-card([SQLite], [постоянные данные], fill: guap-cyan.lighten(94%), stroke: guap-cyan),
  )
]

#slide(title: "Паттерны: роли в системе")[
  #grid(
    columns: (1fr, 1fr),
    gutter: 14pt,
    compact-card([Repository], [граница хранения]),
    compact-card([MVVM], [состояние экрана], fill: guap-magenta.lighten(94%), stroke: guap-magenta),
    compact-card([Strategy], [правила выдачи], fill: guap-cyan.lighten(94%), stroke: guap-cyan),
    compact-card([Factory], [валидные объекты], fill: guap-cyan.lighten(95%), stroke: guap-cyan),
    compact-card([Observer], [Flow → StateFlow]),
    compact-card([State], [жизненный цикл Loan], fill: guap-red.lighten(94%), stroke: guap-red),
  )
  #v(12pt)
  #compact-card([Facade], [RoomLibraryService], fill: guap-magenta.lighten(94%), stroke: guap-magenta)
]

#slide(title: "Паттерны границ: Repository и Facade")[
  #grid(
    columns: (1fr, 1fr),
    gutter: 18pt,
    card([Repository], [Доменный интерфейс → Room-адаптер → DAO.]),
    card([Facade], [ViewModel → RoomLibraryService → репозитории.], fill: guap-magenta.lighten(94%), stroke: guap-magenta),
  )
]

#slide(title: "Паттерны UI: MVVM и Observer")[
  #grid(
    columns: (1fr, 1fr),
    gutter: 18pt,
    card([MVVM], [ViewModel принимает события, проверяет ввод и формирует UiState.]),
    card([Observer], [Room Flow → Repository → StateFlow → Compose.], fill: guap-cyan.lighten(94%), stroke: guap-cyan),
  )
  #v(18pt)
  #wide-card([Жизненный цикл], [ViewModel реализуют AutoCloseable и отменяют собственные CoroutineScope.], fill: guap-red.lighten(94%), stroke: guap-red)
]

#slide(title: "Паттерны домена: Strategy и Factory")[
  #grid(
    columns: (1fr, 1fr),
    gutter: 18pt,
    card([Strategy], [DefaultLoanPolicy: книга — 21 день, периодика — 7 дней, лимит выдач.]),
    card([Factory], [LibraryEntityFactory создаёт UUID и валидные предметные объекты.], fill: guap-cyan.lighten(94%), stroke: guap-cyan),
  )
  #v(18pt)
  #wide-card([Результат], [Правила можно заменить без изменения UI, Room и сценариев приложения.])
]

#slide(title: "Состояние выдачи: State")[
  #grid(
    columns: (1fr, auto, 1fr, auto, 1fr),
    gutter: 10pt,
    compact-card([ACTIVE], [выдано]),
    align(center + horizon)[#text(size: 40pt, fill: guap-blue)[→]],
    compact-card([OVERDUE], [срок истёк], fill: guap-red.lighten(94%), stroke: guap-red),
    align(center + horizon)[#text(size: 40pt, fill: guap-blue)[→]],
    compact-card([RETURNED], [возвращено], fill: guap-cyan.lighten(94%), stroke: guap-cyan),
  )
  #v(24pt)
  #wide-card([Переходы], [refreshOverdue сохраняет просрочку; returnLoan проверяет время и увеличивает остаток один раз.])
]

#slide(title: "Транзакционные сценарии")[
  #grid(
    columns: (1fr, 1fr, 1fr),
    gutter: 16pt,
    compact-card([Выдача], [ACTIVE + лимит + остаток], fill: guap-blue.lighten(94%), stroke: guap-blue),
    compact-card([Возврат], [RETURNED + остаток ровно +1], fill: guap-cyan.lighten(94%), stroke: guap-cyan),
    compact-card([Данные], [идемпотентная загрузка + очистка], fill: guap-magenta.lighten(94%), stroke: guap-magenta),
  )
  #v(22pt)
  #wide-card([Защита], [внешние ключи RESTRICT, проверка времени и persisted overdue refresh], fill: guap-red.lighten(94%), stroke: guap-red)
]

#slide(title: "Пользовательский интерфейс")[
  #grid(
    columns: (1fr, 1fr),
    gutter: 16pt,
    image("../docs/images/screen_readers_room.png", width: 100%, height: 4.1in, fit: "contain"),
    image("../docs/images/screen_loans_room.png", width: 100%, height: 4.1in, fit: "contain"),
  )
]

#slide(title: "Тесты и контрольный сценарий")[
  #align(center)[#text(size: 34pt, weight: "bold", fill: guap-blue)[35 тестов · 0 ошибок · 0 пропусков]]
  #v(12pt)
  #table(
    columns: (1.2fr, 2.8fr, 0.65fr, 1fr),
    inset: 8pt,
    [Модуль], [Группа], [N], [Итог],
    [core], [Домен], [4], [Да],
    [core], [Persistence], [10], [Да],
    [core], [Application], [3], [Да],
    [app], [ViewModel], [18], [Да],
  )
]

#final-slide(
  contacts: [
    #text(size: 34pt, weight: "bold", fill: guap-blue)[ИТОГ]\
    Room и SQLite сохраняют сущности, связи и выдачи.\
    Repository отделяет домен от Room.\
    Семь паттернов участвуют в production-сценариях.\
    Compose поддерживает поиск, удаление, выдачу и администрирование.\
    #text(size: 32pt, weight: "bold", fill: guap-red)[Спасибо за внимание]
  ],
  qr-code: none,
)
