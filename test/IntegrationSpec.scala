import java.net.URL

import org.openqa.selenium.WebDriver
import org.openqa.selenium.remote.{DesiredCapabilities, RemoteWebDriver}
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
class IntegrationSpec extends Specification with EnvAwareDriver {

  "Application" should {

    //"work from within a browser" in new WithBrowser(WebDriverFactory(FIREFOX)) {
      "work from within a browser" in new WithBrowser(driver()) {

      browser.goTo("http://localhost:" + port)

      browser.pageSource must contain("Awale")

      browser.click("#click")

      browser.pageSource must contain("To invite")

      println(browser.$("#join-url").getValue)

      //[error] Caused by com.gargoylesoftware.htmlunit.ScriptException: TypeError: Cannot find function addEventListener in object [object HTMLDocument]. (http://localhost:19001/assets/awale-ws.js#134)
    }
  }
}

trait EnvAwareDriver {
  def driver(): WebDriver = {
    if (System.getenv("CI") != "true") {
      WebDriverFactory(FIREFOX)
    } else {
      val caps = DesiredCapabilities.firefox()
      caps.setCapability("platform", "Windows 7")
      caps.setCapability("version", "38.0")
      caps.setCapability("tunnelIdentifier", System.getenv("TRAVIS_JOB_NUMBER"))
      new RemoteWebDriver(new URL("http://yamo93:c1783a7f-802a-41b5-af11-6c6d1841851e@ondemand.saucelabs.com:80/wd/hub"), caps)
    }
  }
}