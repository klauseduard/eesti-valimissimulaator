import java.io.File

// JVM-i testid käivitatakse projekti kaustas (vt build.gradle.kts)
actual fun loeProjektiFail(tee: String): String = File(tee).readText()
