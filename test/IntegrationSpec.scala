import java.net.URL
import java.util.concurrent.TimeUnit

import akka.actor.Props
import controllers.{SupervisorActor, MyController}
import org.fluentlenium.core.Fluent
import org.openqa.selenium.WebDriver
import org.openqa.selenium.remote.{DesiredCapabilities, RemoteWebDriver}
import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._
import play.api.libs.concurrent.Akka
import play.api.{GlobalSettings, Application}

import play.api.test._
import play.api.test.Helpers._

import scala.collection.JavaConversions._

/**
 * add your integration spec here.
 * An integration test will fire up a whole play application in a real (or headless) browser
 */
@RunWith(classOf[JUnitRunner])
class IntegrationSpec extends Specification with EnvAwareDriver {
  import FluentExtensions._

  "Application" should {

    "allow one user to create a game" in ((s: String) => new WithBrowser(driver(s)) {

      browser.goTo("http://localhost:" + port)

      browser.pageSource must contain("Awale")

      browser.click("#click")

      browser.pageSource must contain("To invite")

      browser.await().atMost(5, TimeUnit.SECONDS).until("#invitation").areDisplayed()
      browser.findFirst("#invitation").isDisplayed must equalTo(true)
      browser.findFirst("#game").isDisplayed must equalTo(false)
    })

    "allow one user to create a game, and another user to join" in ((s: String) => new WithBrowser(driver(s)) {

      browser.goTo("http://localhost:" + port)

      browser.pageSource must contain("Awale")

      browser.click("#click")

      browser.pageSource must contain("To invite")

      browser.await().atMost(5, TimeUnit.SECONDS).until("#invitation").areDisplayed()

      browser.findFirst("#invitation").isDisplayed must equalTo(true)
      browser.findFirst("#game").isDisplayed must equalTo(false)

      val joinUrl = browser.$("#join-url").getValue

      browser.goToInNewTab(joinUrl)

      browser.findFirst("#invitation").isDisplayed must equalTo(false)
      browser.findFirst("#game").isDisplayed must equalTo(true)

      browser.firstTab()

      browser.findFirst("#invitation").isDisplayed must equalTo(false)
      browser.findFirst("#game").isDisplayed must equalTo(true)
    })

    "allow one user to create a game, and another user to join, and the first one to play the first move" in ((s: String) => new WithBrowser(driver(s)) {

      browser.goTo("http://localhost:" + port)

      browser.pageSource must contain("Awale")

      browser.click("#click")

      browser.pageSource must contain("To invite")

      browser.await().atMost(5, TimeUnit.SECONDS).until("#invitation").areDisplayed()
      browser.findFirst("#invitation").isDisplayed must equalTo(true)
      browser.findFirst("#game").isDisplayed must equalTo(false)

      val joinUrl = browser.$("#join-url").getValue

      browser.goToInNewTab(joinUrl)

      browser.findFirst("#invitation").isDisplayed must equalTo(false)
      browser.findFirst("#game").isDisplayed must equalTo(true)

      browser.firstTab()

      browser.findFirst("#invitation").isDisplayed must equalTo(false)
      browser.findFirst("#game").isDisplayed must equalTo(true)

      browser.find(".col").get(6).click()
      browser.await().atMost(5, TimeUnit.SECONDS).until("#passive").areDisplayed()
      browser.find(".col").getTexts.toList must equalTo(Seq("4","4","4","4","4","4","0","5","5","5","5","4"))
    })


  }
}

object FluentExtensions {

  implicit class EnhancedFluentAdapter(f: Fluent) {
    def goToInNewTab(url: String): Fluent = {
      val tabs = f.getDriver.getWindowHandles()
      f.executeScript(s"window.open('${url}', '_blank');")
      val tabs2 = f.getDriver.getWindowHandles()
      tabs2.removeAll(tabs)
      f.getDriver.switchTo().window(tabs2.iterator.next)
      f
    }
    def firstTab() : Fluent = {
      val currentTab = f.getDriver.getWindowHandle
      val tabs = f.getDriver.getWindowHandles()
      tabs.remove(currentTab)
      f.getDriver.switchTo().window(tabs.iterator().next())
      f
    }
  }
}

trait EnvAwareDriver {
  def driver(name: String): WebDriver = {
    WebDriverFactory(FIREFOX)
//    if (System.getenv("CI") != "true") {
//      WebDriverFactory(FIREFOX)
//    } else {
//      val caps = DesiredCapabilities.firefox()
//      caps.setCapability("platform", "Windows 7")
//      caps.setCapability("version", "38.0")
//      caps.setCapability("tunnelIdentifier", System.getenv("TRAVIS_JOB_NUMBER"))
//      caps.setCapability("build", System.getenv("TRAVIS_BUILD_NUMBER"))
//      caps.setCapability("name", name)
//      new RemoteWebDriver(new URL("http://yamo93:c1783a7f-802a-41b5-af11-6c6d1841851e@ondemand.saucelabs.com:80/wd/hub"), caps)
//    }
  }
}