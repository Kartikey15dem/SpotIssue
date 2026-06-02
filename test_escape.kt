fun String.escapeIfNeeded(): String = when {
    this.contains('"') -> this.replace("\"", "\\\"")
    else -> this
}
fun main() {
    val key = "files\"; filename=\"test.jpg"
    println("form-data; name=\"${key.escapeIfNeeded()}\"")
}
