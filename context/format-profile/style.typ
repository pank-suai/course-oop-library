#let profile-style(body) = {
  set text(font: "Times New Roman", size: 14pt)
  show table: set text(size: 14pt)
  show heading.where(level: 1): it => { pagebreak(weak: true); it }
  body
}
