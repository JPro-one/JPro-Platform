package one.jpro.routing

import one.jpro.routing.LinkUtil
import org.junit.jupiter.api.Test

class TestRequest {
  @Test
  def fromString(): Unit = {
    assert(Request.fromString("http://localhost:8081/d").queryParameters == Map())
    assert(Request.fromString("http://localhost:8081/d?").queryParameters == Map())
    assert(Request.fromString("http://localhost:8081/d?x=1&y=2").queryParameters == Map("x" -> "1", "y" -> "2"))
    assert(Request.fromString("http://localhost:8081/d?name=aa%26bb").queryParameters == Map("name" -> "aa%26bb"))

  }
}
