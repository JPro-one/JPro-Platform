package one.jpro.platform.routing

import org.junit.jupiter.api.Test

class TestRequest {
  @Test
  def fromString(): Unit = {
    assert(Request.fromString("http://localhost:8081/d").getQueryParametersScala() == Map())
    assert(Request.fromString("http://localhost:8081/d?").getQueryParametersScala() == Map())
    assert(Request.fromString("http://localhost:8081/d?x=1&y=2").getQueryParametersScala() == Map("x" -> "1", "y" -> "2"))
    assert(Request.fromString("http://localhost:8081/d?name=aa%26bb").getQueryParametersScala() == Map("name" -> "aa%26bb"))
  }
}
