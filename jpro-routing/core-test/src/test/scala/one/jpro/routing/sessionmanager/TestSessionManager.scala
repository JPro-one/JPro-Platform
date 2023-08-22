package one.jpro.routing.sessionmanager

import one.jpro.routing.sessionmanager.SessionManager
import org.junit.jupiter.api.Test

class TestSessionManager {
  @Test
  def someTests(): Unit = {
      assert(SessionManager.mergeURLs("http://a/b", "/c") == "http://a/c")
      assert(SessionManager.mergeURLs("http://a.com/b", "/") == "http://a.com/")
      assert(SessionManager.mergeURLs("http://a.com/b/c", "./d") == "http://a.com/b/d")
      assert(SessionManager.mergeURLs("http://a.com/b/c", "../d") == "http://a.com/d")
      assert(SessionManager.mergeURLs("http://a/b", "/") == "http://a/")
  }
}
