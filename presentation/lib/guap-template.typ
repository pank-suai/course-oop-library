#import "@preview/tiaoma:0.2.0": qrcode
#import "@preview/polylux:0.4.0" as polylux
#import polylux: uncover, only

// Цвета ГУАП
#let guap-blue = rgb("#005aaa") 
#let guap-dark-blue = rgb("#1b3f95") 
#let guap-cyan = rgb("#00a9e0") 
#let guap-magenta = rgb("#c6168d") 
#let guap-red = rgb("#e30613") 

// Размеры и отступы
#let margin-x = 1.25cm
#let margin-top = 100pt // Отступ сверху для контента
#let margin-bottom = 40pt

// Плейсхолдер для значка
#let guap-icon-placeholder(size: 50pt) = {
  box(width: size, height: size, fill: gray.lighten(80%), radius: 0pt)[
    #align(center + horizon)[
      #text(fill: gray, size: size * 0.2)[Знак]
    ]
  ]
}


// Логотип для слайдов (маленький)
#let guap-logo-small() = {
  image("images/guap-logo.png", width: 80pt)
}

#let presentation(
  title: [],
  subtitle: [],
  author: ("Автор", ),
  body
) = {
  set document(title: title, author: author)
  set page(
    paper: "presentation-16-9", 
    margin: 0pt,
  )
  // The project review requires every visible text run to be at least 28 pt.
  set text(font: "Arial", size: 28pt, lang: "ru")

  set list(marker: [#text(fill: guap-red)[•]])
  set figure.caption(separator: [. ], position: top)
  show figure.caption: set text(size: 28pt, weight: "bold", fill: guap-dark-blue)
  show figure.caption: set align(left)

  show table.cell.where(y: 0): set text(fill: white, weight: "bold")
  set table(
    // Границы: цвет guap-blue, тонкие линии
    stroke: (paint: guap-blue, thickness: 0.75pt),
    
    // Заливка: синяя для первой строки (row 0), прозрачная для остальных
    fill: (col, row) => if row == 0 { guap-blue } else { none },
    
    // Внутренние отступы для комфортного чтения
    inset: 10pt,
    
    // Выравнивание по умолчанию (можно переопределить при вызове)
    align: horizon + left,
    
    // Передача всех остальных аргументов (columns, content)
  )

  show figure.where(kind: image): set figure(supplement: "Рисунок")

  // --- Титульный слайд ---
  page(background: {
      image("images/title-bg.png", fit: "cover", width: 100%, height: 100%)
    
  })[

    // Заголовок
    #place(top + left, dx: margin-x, dy: 38%)[
      #block(width: 85%)[
        #set text(fill: guap-blue, weight: "medium", size: 46pt)
        #set par(leading: 0.35em)
        #title
      ]
    ]

    // Подзаголовок
    #place(top + left, dx: margin-x, dy: 66%)[
      #block(width: 80%)[
        #set text(fill: black, size: 30pt, weight: "regular")
        #set par(leading: 0.4em)
        #subtitle
      ]
    ]

    // Год
    #place(bottom + right, dx: -margin-x, dy: -margin-x)[
      #text(fill: guap-red, size: 30pt, weight: "medium", [#datetime.today().year()])
    ]
  ]

  // --- Настройки для обычных слайдов ---
  set page(
    margin: (top: margin-top, bottom: margin-bottom, left: margin-x, right: margin-x),
    background: {
       rect(width: 100%, height: 100%, fill: white)
    },
    // Хедер с логотипом
    header: context {
      place(top + right, dx: 0pt, dy: 30pt)[ // 20pt от верха страницы
         #guap-logo-small()
      ]
    },
    // Футер с номером страницы
    footer: context {
      align(right)[
        #text(size: 28pt, fill: rgb("002755"))[
           #counter(page).display()
        ]
      ]
    }
  )
  show emph: set text(fill: guap-blue)
  
  body
}

// --- Функции слайдов ---

// Базовый слайд
#let slide(title: auto, content) = {
  polylux.slide[
    // Заголовок
    #if title != auto [
       #place(top + left, dx: 0pt, dy: -65pt)[
          #block(width: 100%)[
             #text(fill: guap-blue, size: 34pt, weight: "bold", title)
             #v(-12pt)
             #line(length: 100%, stroke: (paint: guap-blue, thickness: 1.5pt))
          ]
       ]
    ]
    
    // Контент
    #set text(size: 28pt, fill: black, weight: "regular")
    #set par(leading: 0.5em, justify: false)
    #content
  ]
}

// Слайд с двумя колонками
#let slide-cols(title: auto, left-col, right-col, fraction: (1fr, 1fr)) = {
  slide(title: title)[
    #grid(
      columns: fraction,
      gutter: 30pt,
      left-col,
      right-col
    )
  ]
}

// Слайд "Текст и рисунок"
#let slide-text-image(title: auto, text-content, image-content, caption: none, kind: image) = {
  slide(title: title)[
    #grid(
      columns: (1.1fr, 1fr), // Тексту чуть больше места
      gutter: 30pt,
      [
        #text-content
      ],
      [
        #figure(
        block(width: 100%, height: 100%)[ // Фиксированная высота для примера
           #layout(size => {
              block(
                 width: 100%, 
                 height: auto, // высота по контенту или фиксированная
                 clip: true,
                 radius: (top-left: 60pt, rest: 0pt), // Скругление как на макете
                 image-content
              )
           })
        ],
          caption: caption,
          kind: image
        )
      ]
    )
  ]
}


#let final-slide(
  contacts: [Контактные данные], 
  qr-code: none // Можно указать любые данные для QR-кода
) = {
  polylux.slide[
    #set page(
      header: none, 
      footer: none, 
      margin: 0pt,
      background: {
           image("images/last-bg.png", width: 100%, height: 100%, fit: "cover")
      }
    )

    // Контейнер для Логотипа, URL и QR-кода (Верхний правый угол)
    #place(top + right, dx: -margin-x, dy: 65pt)[
      #if qr-code != none {
          qrcode(qr-code, options: (
             scale: 2.0, 
             output-options: (
              barcode-dotty-mode: true
             ),             
             dot-size: 1.0, 
             fg-color: guap-blue, 
             bg-color: none
           ))
        }
    ]

    // Текст "Контактные данные" (Справа, ближе к середине/низу)
    #place(right + horizon, dx: -margin-x, dy: 10%)[
       #text(size: 32pt, fill: black)[
         #contacts
       ]
    ]
  ]
}


