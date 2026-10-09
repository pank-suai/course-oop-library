#let source-url(n) = {
  let urls = (
    none,
    "https://www.piter.com/collection/A20146/product/patterny-obektno-orientirovannogo-proektirovaniya",
    "https://kotlinlang.org/docs/classes.html",
    "https://kotlinlang.org/docs/interfaces.html",
    "https://kotlinlang.org/docs/inheritance.html",
    "https://plantuml.com/class-diagram",
    "https://kotlinlang.org/docs/coroutines-flow.html",
    "https://github.com/JetBrains/compose-multiplatform",
    "https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/-state-flow",
    "https://martinfowler.com/eaaCatalog/repository.html",
    "https://learn.microsoft.com/en-us/dotnet/architecture/maui/mvvm",
    "https://developer.android.com/kotlin/multiplatform/room",
    "https://docs.gradle.org/current/userguide/gradle_wrapper.html",
    "https://www.sqlite.org/foreignkeys.html",
    "https://www.sqlite.org/lang_transaction.html",
    "https://kotlinlang.org/api/core/kotlin-test",
  )
  let u = urls.at(n - 1)
  if u == none { [] } else {
    let visible = u.clusters().map(c => if ("/", ".", "-", "_").contains(c) { c + "\u{200b}" } else { c }).join()
    link(u, text(hyphenate: false, visible))
  }
}

#{
  set par(first-line-indent: 0pt, hanging-indent: 0pt)
  show enum: set par(first-line-indent: 0pt, hanging-indent: 0pt)
  enum(numbering: "1.", indent: 1.25cm, body-indent: 0.7em, tight: false,
    block(breakable: false)[Объектно-ориентированное программирование: методические указания к выполнению курсового проекта / сост. Е. О. Шумова. — СПб.: ГУАП, 2026. — 16 с.],
    block(breakable: false)[Паттерны объектно-ориентированного проектирования / Э. Гамма, Р. Хелм, Р. Джонсон, Д. Влиссидес. — СПб.: Питер, 2026. — 448 с. — ISBN 978-5-4461-1595-2.],
    block(breakable: false)[Classes [Электронный ресурс] \/\/ Kotlin Documentation. URL: #source-url(3) (дата обращения: 07.10.2026).],
    block(breakable: false)[Interfaces [Электронный ресурс] \/\/ Kotlin Documentation. URL: #source-url(4) (дата обращения: 07.10.2026).],
    block(breakable: false)[Inheritance [Электронный ресурс] \/\/ Kotlin Documentation. URL: #source-url(5) (дата обращения: 07.10.2026).],
    block(breakable: false)[Class Diagram syntax and features [Электронный ресурс] \/\/ PlantUML Documentation. URL: #source-url(6) (дата обращения: 07.10.2026).],
    block(breakable: false)[Flows [Электронный ресурс] \/\/ Kotlin Documentation. URL: #source-url(7) (дата обращения: 07.10.2026).],
    block(breakable: false)[Compose Multiplatform [Электронный ресурс] \/\/ JetBrains. URL: #source-url(8) (дата обращения: 07.10.2026).],
    block(breakable: false)[StateFlow [Электронный ресурс] \/\/ kotlinx.coroutines API. URL: #source-url(9) (дата обращения: 07.10.2026).],
    block(breakable: false)[Hieatt E., Mee R. Repository [Электронный ресурс] \/\/ Martin Fowler. — 2003. URL: #source-url(10) (дата обращения: 07.10.2026).],
    block(breakable: false)[Model-View-ViewModel [Электронный ресурс] \/\/ Microsoft Learn. URL: #source-url(11) (дата обращения: 07.10.2026).],
    block(breakable: false)[Set up Room database for KMP [Электронный ресурс] \/\/ Android Developers. URL: #source-url(12) (дата обращения: 07.10.2026).],
    block(breakable: false)[Gradle Wrapper [Электронный ресурс] \/\/ Gradle User Manual. URL: #source-url(13) (дата обращения: 07.10.2026).],
    block(breakable: false)[SQLite Foreign Key Support [Электронный ресурс] \/\/ SQLite Documentation. URL: #source-url(14) (дата обращения: 07.10.2026).],
    block(breakable: false)[Transaction [Электронный ресурс] \/\/ SQLite Documentation. URL: #source-url(15) (дата обращения: 07.10.2026).],
    block(breakable: false)[kotlin-test [Электронный ресурс] \/\/ Kotlin Core API. URL: #source-url(16) (дата обращения: 07.10.2026).],
  )
}
