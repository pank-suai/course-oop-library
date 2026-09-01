import de.infolektuell.gradle.typst.tasks.TypstCompileTask

plugins{
    alias(libs.plugins.typst)
}


typst {
    version = "v0.14.2"
}

tasks.withType<TypstCompileTask>().configureEach {
    useSystemFonts = true
}

val main by typst.sourceSets.registering {
    root.set(layout.projectDirectory)
    documents.add("../index")
}
