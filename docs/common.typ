#let soft(value) = {
  let previous = ""
  let result = ""
  for c in value.clusters() {
    if previous != "" and ((lower(previous) == previous and upper(previous) != previous and upper(c) == c and lower(c) != c) or previous == "<" or previous == ">") { result += "\u{200b}" }
    result += c
    previous = c
  }
  text(result)
}
