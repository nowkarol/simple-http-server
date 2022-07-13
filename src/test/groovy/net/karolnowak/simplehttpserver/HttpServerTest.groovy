package net.karolnowak.simplehttpserver

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Subject

import java.nio.file.Files

import static java.util.concurrent.TimeUnit.MILLISECONDS

class HttpServerTest extends Specification {
    @Subject
    @Shared
    HttpServer server

    @Subject
    @Shared
    HttpServer serverWithBinaryContent

    OkHttpClient client = new OkHttpClient.Builder().callTimeout(100, MILLISECONDS)
            .readTimeout(100, MILLISECONDS)
            .build()

    def "setupSpec"() {
        byte[] textFileBytes = HttpServerTest.class.getResource("/file").bytes
        server = new HttpServer(textFileBytes, 80)
        server.start()

        byte[] imageBytes = HttpServerTest.class.getResource("/wiki_logo.png").bytes
        serverWithBinaryContent = new HttpServer(imageBytes, 8080)
        serverWithBinaryContent.start()
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
            with(result)  {
                header("Content-Length") == "199002"
                body().bytes().size() == 199002
            }
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
