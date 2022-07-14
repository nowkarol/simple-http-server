package net.karolnowak.simplehttpserver

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Subject

import java.nio.file.Path

import static java.util.concurrent.TimeUnit.MILLISECONDS
import static net.karolnowak.simplehttpserver.ContentKt.contentFromPath

class HttpServerTest extends Specification {
    @Subject
    @Shared
    HttpServer serverWithTextFile

    @Subject
    @Shared
    HttpServer serverWithBinaryContent

    @Subject
    @Shared
    HttpServer serverWithDirectory

    OkHttpClient client = new OkHttpClient.Builder().callTimeout(100, MILLISECONDS)
            .readTimeout(100, MILLISECONDS)
            .build()

    def "setupSpec"() {
        Path textFilePath = Path.of(HttpServerTest.class.getResource("/file").toURI())
        serverWithTextFile = new HttpServer(contentFromPath(textFilePath), 80)
        serverWithTextFile.start()

        Path imagePath = Path.of(HttpServerTest.class.getResource("/wiki_logo.png").toURI())
        serverWithBinaryContent = new HttpServer(contentFromPath(imagePath), 8080)
        serverWithBinaryContent.start()

        Path directoryPath = Path.of(HttpServerTest.class.getResource("/wiki_logo.png").toURI()).parent
        serverWithDirectory = new HttpServer(contentFromPath(directoryPath), 8081)
        serverWithDirectory.start()
    }

    def "should serve file"() {
        when:
            Response result = client.newCall(new Request.Builder()
                    .get().url("http://localhost").build()).execute()
        then:
            result.body().string() == """this is file content
                                        |second line""".stripMargin('|')
            // server doesn't add CR
    }

    def "should serve file multiple times"() {
        when:
            10.times {
                client.newCall(new Request.Builder()
                        .get().url("http://localhost").build()).execute()
            }
        then:
            noExceptionThrown()
    }

    def "should serve binary file"() {
        when:
            Response result = client.newCall(new Request.Builder()
                .get().url("http://localhost:8080").build()).execute()
        then:

            with(result) {
                header("Content-Length") == "199002"
                body().bytes().size() == 199002
            }
    }

    def "should serve directory listing"() {
        when:
            Response result = client.newCall(new Request.Builder()
                .get().url("http://localhost:8081").build()).execute()
        then:
        result.body().string() == "file\r\nwiki_logo.png"
        // but it adds CR LF between positions in listing
    }

    def "should return 405 and Allow header when request uses method other than GET"() {
        when:
            Response result = client.newCall(new Request.Builder()
                    .delete().url("http://localhost").build()).execute()
        then:
            with (result) {
                code == 405
                header("Accept") == "GET"
                body().string().isEmpty()
            }
    }
}
