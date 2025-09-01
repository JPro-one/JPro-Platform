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


  @Test
  def matchesSoft_pathOnly(): Unit = {
    val r = Request.fromString("http://localhost:8081/d?a=1&b=2")
    assert(Request.matchesSoft(r, "/d"))
    assert(Request.matchesSoft(r, "/d?x=9")) // query ignored
  }

  @Test
  def matchesSoft_fullUrl(): Unit = {
    val r = Request.fromString("https://example.com:443/path/sub?x=1")
    assert(Request.matchesSoft(r, "https://example.com:443/path/sub?y=2")) // query ignored
  }

  @Test
  def matchesSoft_failures(): Unit = {
    assert(!Request.matchesSoft(Request.fromString("http://localhost:8081/d"), "http://otherhost:8081/d"))
    assert(!Request.matchesSoft(Request.fromString("http://localhost:8081/d"), "http://localhost:9090/d"))
    assert(!Request.matchesSoft(Request.fromString("http://localhost:8081/d"), "/x"))
    assert(!Request.matchesSoft(Request.fromString("http://localhost:8081/d?z=1"), "http://localhost:8081/x?y=2"))
  }

  @Test
  def matchesSoft_pathOnly_ignoresDomainAndQuery(): Unit = {
    val r = Request.fromString("http://foo.example:8080/d?p=1&x=2")
    assert(Request.matchesSoft(r, "/d"))
    assert(Request.matchesSoft(r, "/d?ignored=ok"))
  }

  @Test
  def matchesSoft_queryOrderAndExtrasIgnored(): Unit = {
    val r = Request.fromString("http://localhost:8081/path?a=1&b=2&c=3")
    assert(Request.matchesSoft(r, "/path?b=9&a=8")) // order, values irrelevant
    assert(Request.matchesSoft(r, "http://localhost:8081/path?z=zzz")) // full URL, query ignored
  }

  @Test
  def matchesSoft_schemeAndHostCaseInsensitive(): Unit = {
    val r = Request.fromString("HTTP://ExAmPlE.com:80/Case")
    assert(Request.matchesSoft(r, "http://example.COM:80/Case"))
    assert(Request.matchesSoft(r, "HtTp://EXAMPLE.com/Case")) // port omitted in URL → wildcard
  }

  @Test
  def matchesSoft_trailingSlashIsStrict(): Unit = {
    val r1 = Request.fromString("http://localhost:8081/dir")
    val r2 = Request.fromString("http://localhost:8081/dir/")
    assert(!Request.matchesSoft(r1, "/dir/"))
    assert(!Request.matchesSoft(r2, "/dir"))
  }

  @Test
  def matchesSoft_fullUrl_portMustMatchWhenSpecified(): Unit = {
    val rNoPort = Request.fromString("http://example.com/path") // request port == -1
    val r8080   = Request.fromString("http://example.com:8080/path")

    assert(Request.matchesSoft(rNoPort, "http://example.com/path")) // both omit port
    assert(!Request.matchesSoft(rNoPort, "http://example.com:80/path")) // URL specifies port → must match, but request has -1
    assert(Request.matchesSoft(r8080, "http://example.com:8080/path")) // explicit port matches
    assert(!Request.matchesSoft(r8080, "http://example.com:9090/path")) // explicit port mismatch
  }

  @Test
  def matchesSoft_fullUrl_domainMismatch(): Unit = {
    val r = Request.fromString("http://localhost:8081/d")
    assert(!Request.matchesSoft(r, "http://otherhost:8081/d"))
  }

  @Test
  def matchesSoft_ipv6Host(): Unit = {
    val r = Request.fromString("http://[::1]:8080/d")
    assert(Request.matchesSoft(r, "http://[::1]:8080/d"))
    assert(!Request.matchesSoft(r, "http://[::1]:9090/d"))
  }

  @Test
  def matchesSoft_encodedPath_consistent(): Unit = {
    // both sides use getPath(), so %20 is treated consistently
    val r = Request.fromString("http://localhost:8081/a%20b")
    assert(Request.matchesSoft(r, "/a%20b"))
    assert(!Request.matchesSoft(r, "/a b")) // literal space ≠ %20 in URI
  }

  @Test
  def matchesSoft_invalidUrlString_returnsFalse(): Unit = {
    val r = Request.fromString("http://localhost:8081/d")
    assert(!Request.matchesSoft(r, "http://exa mple.com/illegal")) // parse failure → false
    assert(!Request.matchesSoft(r, "not a url at all"))
  }

  @Test
  def matchesSoft_fullUrl_schemeMustMatch(): Unit = {
    val rHttp = Request.fromString("http://example.com/x")
    val rHttps = Request.fromString("https://example.com/x")
    assert(Request.matchesSoft(rHttp, "http://example.com/x"))
    assert(Request.matchesSoft(rHttps, "https://example.com/x"))
    assert(!Request.matchesSoft(rHttp, "https://example.com/x"))
  }
}
