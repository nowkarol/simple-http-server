import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import spock.lang.Specification

class HttpServerTest extends Specification {
    OkHttpClient client = new OkHttpClient()

    def "should serve file"() {
        given:
            new HttpServer("file").start()
        when:
            Response result = client.newCall(new Request.Builder()
                    .get().url("http://localhost").build()).execute()
        then:
            result.body().string() == """this is file content
                                        |second line""".stripMargin('|')
            // server doesn't add CR
    }
}
