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


#block(breakable: false)[
#set par(justify: true, leading: 1.2em, first-line-indent: (amount: 1.25cm, all: true), hanging-indent: 0pt)
#h(1.25cm)#text("1. ")Объектно-ориентированное программирование: методические указания к выполнению курсового проекта / сост. Е. О. Шумова. — СПб.: ГУАП, 2026. — 16 с.]

#block(breakable: false)[
#set par(justify: true, leading: 1.2em, first-line-indent: (amount: 1.25cm, all: true), hanging-indent: 0pt)
#h(1.25cm)#text("2. ")Паттерны объектно-ориентированного проектирования / Э. Гамма, Р. Хелм, Р. Джонсон, Д. Влиссидес. — СПб.: Питер, 2026. — 448 с. — ISBN 978-5-4461-1595-2.]

#block(breakable: false)[
#set par(justify: true, leading: 1.2em, first-line-indent: (amount: 1.25cm, all: true), hanging-indent: 0pt)
#h(1.25cm)#text("3. ")Classes [Электронный ресурс] \/\/ Kotlin Documentation. URL: #source-url(3) (дата обращения: 07.10.2026).]

#block(breakable: false)[
#set par(justify: true, leading: 1.2em, first-line-indent: (amount: 1.25cm, all: true), hanging-indent: 0pt)
#h(1.25cm)#text("4. ")Interfaces [Электронный ресурс] \/\/ Kotlin Documentation. URL: #source-url(4) (дата обращения: 07.10.2026).]

#block(breakable: false)[
#set par(justify: true, leading: 1.2em, first-line-indent: (amount: 1.25cm, all: true), hanging-indent: 0pt)
#h(1.25cm)#text("5. ")Inheritance [Электронный ресурс] \/\/ Kotlin Documentation. URL: #source-url(5) (дата обращения: 07.10.2026).]

#block(breakable: false)[
#set par(justify: true, leading: 1.2em, first-line-indent: (amount: 1.25cm, all: true), hanging-indent: 0pt)
#h(1.25cm)#text("6. ")Class Diagram syntax and features [Электронный ресурс] \/\/ PlantUML Documentation. URL: #source-url(6) (дата обращения: 07.10.2026).]

#block(breakable: false)[
#set par(justify: true, leading: 1.2em, first-line-indent: (amount: 1.25cm, all: true), hanging-indent: 0pt)
#h(1.25cm)#text("7. ")Flows [Электронный ресурс] \/\/ Kotlin Documentation. URL: #source-url(7) (дата обращения: 07.10.2026).]

#block(breakable: false)[
#set par(justify: true, leading: 1.2em, first-line-indent: (amount: 1.25cm, all: true), hanging-indent: 0pt)
#h(1.25cm)#text("8. ")Compose Multiplatform [Электронный ресурс] \/\/ JetBrains. URL: #source-url(8) (дата обращения: 07.10.2026).]

#block(breakable: false)[
#set par(justify: true, leading: 1.2em, first-line-indent: (amount: 1.25cm, all: true), hanging-indent: 0pt)
#h(1.25cm)#text("9. ")StateFlow [Электронный ресурс] \/\/ kotlinx.coroutines API. URL: #source-url(9) (дата обращения: 07.10.2026).]

#block(breakable: false)[
#set par(justify: true, leading: 1.2em, first-line-indent: (amount: 1.25cm, all: true), hanging-indent: 0pt)
#h(1.25cm)#text("10. ")Hieatt E., Mee R. Repository [Электронный ресурс] \/\/ Martin Fowler. — 2003. URL: #source-url(10) (дата обращения: 07.10.2026).]

#block(breakable: false)[
#set par(justify: true, leading: 1.2em, first-line-indent: (amount: 1.25cm, all: true), hanging-indent: 0pt)
#h(1.25cm)#text("11. ")Model-View-ViewModel [Электронный ресурс] \/\/ Microsoft Learn. URL: #source-url(11) (дата обращения: 07.10.2026).]

#block(breakable: false)[
#set par(justify: true, leading: 1.2em, first-line-indent: (amount: 1.25cm, all: true), hanging-indent: 0pt)
#h(1.25cm)#text("12. ")Set up Room database for KMP [Электронный ресурс] \/\/ Android Developers. URL: #source-url(12) (дата обращения: 07.10.2026).]

#block(breakable: false)[
#set par(justify: true, leading: 1.2em, first-line-indent: (amount: 1.25cm, all: true), hanging-indent: 0pt)
#h(1.25cm)#text("13. ")Gradle Wrapper [Электронный ресурс] \/\/ Gradle User Manual. URL: #source-url(13) (дата обращения: 07.10.2026).]

#block(breakable: false)[
#set par(justify: true, leading: 1.2em, first-line-indent: (amount: 1.25cm, all: true), hanging-indent: 0pt)
#h(1.25cm)#text("14. ")SQLite Foreign Key Support [Электронный ресурс] \/\/ SQLite Documentation. URL: #source-url(14) (дата обращения: 07.10.2026).]

#block(breakable: false)[
#set par(justify: true, leading: 1.2em, first-line-indent: (amount: 1.25cm, all: true), hanging-indent: 0pt)
#h(1.25cm)#text("15. ")Transaction [Электронный ресурс] \/\/ SQLite Documentation. URL: #source-url(15) (дата обращения: 07.10.2026).]

#block(breakable: false)[
#set par(justify: true, leading: 1.2em, first-line-indent: (amount: 1.25cm, all: true), hanging-indent: 0pt)
#h(1.25cm)#text("16. ")kotlin-test [Электронный ресурс] \/\/ Kotlin Core API. URL: #source-url(16) (дата обращения: 07.10.2026).]
