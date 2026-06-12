package com.varpas.sangeet.desktop.diagnostics

import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets
import java.util.concurrent.atomic.AtomicReference

import com.sun.net.httpserver.{HttpExchange, HttpHandler, HttpServer}
import io.circe.Json
import io.circe.parser.parse
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class BugReportClientSpec extends AnyFlatSpec with Matchers:

  /** Spin up a real localhost HTTP server using the JDK's built-in `com.sun.net.httpserver.HttpServer`. Avoids mocking
    * java.net.http internals (awkward) and exercises the real HTTP send path.
    *
    * `responseFactory` lets the test control the server's response per request.
    */
  private def withServer(responseFactory: HttpExchange => (Int, String))(
      test: (String, AtomicReference[String]) => Unit
  ): Unit =
    val server   = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0)
    val lastBody = new AtomicReference[String]("")
    val handler = new HttpHandler:
      def handle(ex: HttpExchange): Unit =
        val bytes = ex.getRequestBody.readAllBytes()
        lastBody.set(new String(bytes, StandardCharsets.UTF_8))
        val (code, body) = responseFactory(ex)
        val outBytes     = body.getBytes(StandardCharsets.UTF_8)
        ex.getResponseHeaders.add("Content-Type", "application/json")
        ex.sendResponseHeaders(code, outBytes.length.toLong)
        ex.getResponseBody.write(outBytes)
        ex.getResponseBody.close()
    server.createContext("/api/v1/bug-reports", handler)
    server.start()
    try
      val baseUrl = s"http://127.0.0.1:${server.getAddress.getPort}/api/v1"
      test(baseUrl, lastBody)
    finally server.stop(0)

  private val examplePayload = BugReportPayload(
    description = "test desktop bug",
    email = Some("user@example.com"),
    eventLog = List(
      Json.obj("type" -> Json.fromString("key"), "code" -> Json.fromString("s"))
    ),
    composition = Some(Json.obj("metadata" -> Json.obj("name" -> Json.fromString("Yaman")))),
    screenshotPngBase64 = Some("iVBORw0KGgo..."),
    metadata = BugReportMetadata("Mac OS X", "14.5", "21.0.5", 1920, 1080, "0.2.0", "2026-06-12T10:00:00Z")
  )

  "BugReportClient.submit" should "POST the payload and return the reportId on 200" in {
    withServer(_ => (200, """{"reportId":"abc-123","status":"received"}""")) { (baseUrl, capturedBody) =>
      val client = new HttpBugReportClient(baseUrl)
      val result = client.submit(examplePayload)
      result shouldBe Right("abc-123")

      // The server saw our payload — verify the shape.
      val sent = parse(capturedBody.get()).getOrElse(fail("server got non-JSON body"))
      sent.hcursor.get[String]("type").toOption shouldBe Some("desktop")
      sent.hcursor.get[String]("description").toOption shouldBe Some("test desktop bug")
      sent.hcursor.get[String]("email").toOption shouldBe Some("user@example.com")
      sent.hcursor.downField("replay").as[List[Json]].toOption.map(_.length) shouldBe Some(1)
      sent.hcursor.downField("composition").as[Json].isRight shouldBe true
      sent.hcursor.get[String]("screenshot").toOption shouldBe Some("iVBORw0KGgo...")
      sent.hcursor.downField("metadata").get[String]("os").toOption shouldBe Some("Mac OS X 14.5")
    }
  }

  it should "return Left when the server returns 503" in {
    withServer(_ => (503, """{"error":"bug_report_storage_failed","message":"sim"}""")) { (baseUrl, _) =>
      val client = new HttpBugReportClient(baseUrl)
      val result = client.submit(examplePayload)
      result.isLeft shouldBe true
      result.left.toOption.get should include("503")
      result.left.toOption.get should include("bug_report_storage_failed")
    }
  }

  it should "return Left when the server returns 200 with malformed body" in {
    withServer(_ => (200, "not json")) { (baseUrl, _) =>
      val client = new HttpBugReportClient(baseUrl)
      val result = client.submit(examplePayload)
      result.isLeft shouldBe true
    }
  }

  it should "return Left when the connection fails" in {
    val client = new HttpBugReportClient("http://127.0.0.1:1/api/v1")
    val result = client.submit(examplePayload)
    result.isLeft shouldBe true
  }

  "BugReportPayload.toJson" should "omit composition and screenshot when absent" in {
    val minimal = examplePayload.copy(composition = None, screenshotPngBase64 = None)
    val json    = minimal.toJson
    json.hcursor.downField("composition").focus shouldBe None
    json.hcursor.downField("screenshot").focus shouldBe None
  }

  it should "serialize email as null when absent" in {
    val noEmail = examplePayload.copy(email = None)
    noEmail.toJson.hcursor.get[Option[String]]("email").toOption.flatten shouldBe None
  }
