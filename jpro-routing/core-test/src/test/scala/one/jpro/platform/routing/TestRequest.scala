package one.jpro.platform.routing

import org.junit.jupiter.api.Test

class TestRequest {

  @Test
  def domainTest(): Unit = {
    assert(Request.fromString("http://localhost:8081/d").getDomain() == "localhost")
    assert(Request.fromString("http://127.0.0.1:8081/d").getDomain() == "127.0.0.1")
  }

  @Test
  def addressTest(): Unit = {
    assert(Request.fromString("http://localhost:8081/d").getProtocol() == "http")
    assert(Request.fromString("http://localhost:8081/d").getDomain() == "localhost")
    assert(Request.fromString("http://localhost:8081/d").getPort() == 8081)
    assert(Request.fromString("http://localhost:8081/d").getPath() == "/d")
    assert(Request.fromString("http://localhost:8081/d").getDirectory() == "/")
  }

  @Test
  def fromString(): Unit = {
    assert(Request.fromString("http://localhost:8081/d").getQueryParametersScala() == Map())
    assert(Request.fromString("http://localhost:8081/d?").getQueryParametersScala() == Map())
    assert(Request.fromString("http://localhost:8081/d?x=1&y=2").getQueryParametersScala() == Map("x" -> "1", "y" -> "2"))
    assert(Request.fromString("http://localhost:8081/d?name=aa%26bb").getQueryParametersScala() == Map("name" -> "aa%26bb"))
  }
}
