 package one.jpro.routing

import one.jpro.routing.LinkUtil
import org.junit.jupiter.api.Test

class TestLinkUtil {

  @Test
  def testValidLink1(): Unit = {
    assert(LinkUtil.isValidLink("http://localhost:8081/d"))
  }

  @Test
  def testValidLink2(): Unit = {
    assert(!LinkUtil.isValidLink("http://localhost:8081/d a"))
    assert(LinkUtil.isValidLink("http://localhost:8081/d%20a"))
  }

  @Test
  def testValidLink3(): Unit = {
    assert(LinkUtil.isValidLink("tel:+923020506910"))
    assert(LinkUtil.isValidLink("mailto:aaa@domain.com"))
  }

  @Test
  def testValidLink4(): Unit = {
    assert(LinkUtil.isValidLink("http://localhost:8070/info?name=Abid%26Aqib"))

  }

}
