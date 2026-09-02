#let init(body) = {
  set text(
    font: "Times New Roman",
    size: 14pt,
    lang: "ru",
    region: "ru",
    hyphenate: true,
  )
  set page(
    paper: "a4",
    margin: (top: 20mm, bottom: 20mm, left: 20mm, right: 10mm),
    numbering: "1",
  )
  set par(justify: true, leading: 1.2em, first-line-indent: 1.25cm)
  set heading(numbering: none)
  show heading: it => block(width: 100%, above: 1em, below: 0.6em)[
    #align(left)[#text(weight: "bold", hyphenate: false)[#it.body]]
  ]
  show table: set text(hyphenate: true)
  show table: set par(justify: false, leading: 0.3em, first-line-indent: 0em)
  show table.cell.where(y: 0): set text(weight: "bold")
  show figure.where(kind: image): set figure(supplement: "Рисунок")
  show figure.where(kind: table): set figure(supplement: "Таблица")
  show figure.where(kind: table): set figure.caption(position: top)
  show figure: it => {
    v(0.6em)
    it
    v(0.6em)
  }
  show raw: set text(font: "JetBrains Mono", size: 6.15pt, lang: "en")
  body
}

#let chapter(title) = {
  pagebreak()
  heading(level: 1, numbering: none)[#title]
}

#let structural(title) = {
  pagebreak()
  align(center)[#text(weight: "bold")[#title]]
  v(0.8em)
}
