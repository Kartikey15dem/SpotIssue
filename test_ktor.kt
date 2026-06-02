import io.ktor.client.request.forms.*
import io.ktor.http.*

fun main() {
    val multipartData = formData {
        append("files", byteArrayOf(1,2,3), Headers.build {
            append(HttpHeaders.ContentType, "application/octet-stream")
            append(HttpHeaders.ContentDisposition, "filename=\"test.jpg\"")
        })
    }
    multipartData.forEach { part ->
        println(part.headers)
    }
}
