import java.net.URL

import org.fluentlenium.adapter.IsolatedTest
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
      "allow one user to create a game, and another to join the game" in new WithBrowser(driver()) {

      browser.goTo("http://localhost:" + port)

      browser.pageSource must contain("Awale")

      browser.click("#click")

      browser.pageSource must contain("To invite")

      browser.findFirst("#invitation").isDisplayed must equalTo(true)
      browser.findFirst("#game").isDisplayed must equalTo(false)

      val joinUrl = browser.$("#join-url").getValue

//      val browser2 = new IsolatedTest()
//
//      browser2.goTo(joinUrl)
//
//      browser.findFirst("#game").isDisplayed must equalTo(true)
//      browser.findFirst("#active").isDisplayed must equalTo(true)
//      browser.findFirst("#passive").isDisplayed must equalTo(false)
//
//      browser2.findFirst("#game").isDisplayed must equalTo(true)
//      browser2.findFirst("#active").isDisplayed must equalTo(false)
//      browser2.findFirst("#passive").isDisplayed must equalTo(true)


//        browser.click("#0")
//        browser.findFirst("#1").getText must equalTo(5)
//
//        browser.quit()
//        browser2.quit()
    }

//    "allow player 1 to play the first turn" in new WithBrowser(driver()) {
//
//      browser.goTo("http://localhost:" + port)
//
//      browser.pageSource must contain("Awale")
//
//      browser.click("#click")
//
//      browser.pageSource must contain("To invite")
//      browser.takeScreenShot()
//
//      browser.findFirst("#invitation").isDisplayed must equalTo(true)
//      browser.findFirst("#game").isDisplayed must equalTo(false)
//
//      val joinUrl = browser.$("#join-url").getValue
//
//      val browser2 = new IsolatedTest()
//
//      browser2.goTo(joinUrl)
//
//      browser.findFirst("#game").isDisplayed must equalTo(true)
//      browser.findFirst("#active").isDisplayed must equalTo(true)
//      browser.findFirst("#passive").isDisplayed must equalTo(false)
//
//      browser2.findFirst("#game").isDisplayed must equalTo(true)
//      browser2.findFirst("#active").isDisplayed must equalTo(false)
//      browser2.findFirst("#passive").isDisplayed must equalTo(true)
//
//      browser.click("#0")
//      browser.findFirst("#1").getText must equalTo(5)
//
//      browser.quit()
//      browser2.quit()
//
//    }
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
      caps.setCapability("build", System.getenv("TRAVIS_BUILD_NUMBER"))
      new RemoteWebDriver(new URL("http://yamo93:c1783a7f-802a-41b5-af11-6c6d1841851e@ondemand.saucelabs.com:80/wd/hub"), caps)
    }
  }
}