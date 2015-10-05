import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._

import play.api.test._
import play.api.test.Helpers._

/**
 * add your integration spec here.
 * An integration test will fire up a whole play application in a real (or headless) browser
 */
@RunWith(classOf[JUnitRunner])
class IntegrationSpec extends Specification {

  "Application" should {

    "work from within a browser" in new WithBrowser(WebDriverFactory(FIREFOX)) {

      browser.goTo("http://localhost:" + port)

      browser.pageSource must contain("Awale")

      browser.click("#click")

      browser.pageSource must contain("To invite")

      println(browser.$("#join-url").getValue)

      //[error] Caused by com.gargoylesoftware.htmlunit.ScriptException: TypeError: Cannot find function addEventListener in object [object HTMLDocument]. (http://localhost:19001/assets/awale-ws.js#134)
    }
  }
}
