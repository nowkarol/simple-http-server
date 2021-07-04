import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Subject


import static java.util.concurrent.TimeUnit.MILLISECONDS

class HttpServerTest extends Specification {
    @Subject
    @Shared
    HttpServer server = new HttpServer("file")

    OkHttpClient client = new OkHttpClient.Builder().callTimeout(100, MILLISECONDS)
            .readTimeout(100, MILLISECONDS)
            .build()

    def "setupSpec"() {
        server.start()
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

    def "should return 405 and Allow header when request uses method other than GET"() {
        when:
            Response result = client.newCall(new Request.Builder()
                    .delete().url("http://localhost").build()).execute()
        then:
            result.code() == 405
            result.header("Accept") == "GET"
            result.body().string().isEmpty()
    }
}
