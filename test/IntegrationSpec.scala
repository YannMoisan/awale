import java.net.URL
import java.util.concurrent.TimeUnit

import org.fluentlenium.core.Fluent
import org.junit.runner._
import org.openqa.selenium.WebDriver
import org.openqa.selenium.remote.{DesiredCapabilities, RemoteWebDriver}
import org.specs2.mutable._
import org.specs2.runner._
import play.api.test.Helpers._
import play.api.test._

import scala.collection.JavaConversions._
import scala.collection.JavaConverters._

/**
 * add your integration spec here.
 * An integration test will fire up a whole play application in a real (or headless) browser
 */
@RunWith(classOf[JUnitRunner])
class IntegrationSpec extends Specification with EnvAwareDriver {
  import FluentExtensions._

  "Application" should {

    examplesBlock {
      for (d <- drivers) {
        "allow P1 to create a game" in ((s: String) => new WithBrowser(d(s)) {

          browser.goTo("/")

          browser.pageSource must contain("Awale")

          browser.click("#click")

          browser.pageSource must contain("To invite")

          browser.await().atMost(5, TimeUnit.SECONDS).until("#invitation").areDisplayed()
          browser.findFirst("#invitation").isDisplayed must equalTo(true)
          browser.findFirst("#game").isDisplayed must equalTo(false)
    })}}

    examplesBlock {
      for (d <- drivers) {
        "allow P1 to create a game, P2 to join" in ((s: String) => new WithBrowser(d(s)) {

          browser.goTo("/")

          browser.pageSource must contain("Awale")

          browser.click("#click")

          browser.pageSource must contain("To invite")

          browser.await().atMost(5, TimeUnit.SECONDS).until("#invitation").areDisplayed()

          browser.findFirst("#invitation").isDisplayed must equalTo(true)
          browser.findFirst("#game").isDisplayed must equalTo(false)

          val joinUrl = browser.$("#join-url").getValue

          val firstTab = browser.getDriver.getWindowHandle

          browser.goToInNewTab(joinUrl, "P2")

          browser.findFirst("#invitation").isDisplayed must equalTo(false)
          browser.findFirst("#game").isDisplayed must equalTo(true)

          browser.switchTo(firstTab)

          browser.findFirst("#invitation").isDisplayed must equalTo(false)
          browser.findFirst("#game").isDisplayed must equalTo(true)
    })}}

    examplesBlock {
      for (d <- drivers) {
        "allow P1 to create a game, P2 to join, P1 to disconnect, P2 to be notified" in ((s: String) => new WithBrowser(d(s)) {

          browser.goTo("/")

          browser.pageSource must contain("Awale")

          browser.click("#click")

          browser.pageSource must contain("To invite")

          browser.await().atMost(5, TimeUnit.SECONDS).until("#invitation").areDisplayed()

          browser.findFirst("#invitation").isDisplayed must equalTo(true)
          browser.findFirst("#game").isDisplayed must equalTo(false)

          val joinUrl = browser.$("#join-url").getValue

          val firstTab = browser.getDriver.getWindowHandle
          browser.goToInNewTab(joinUrl, "P2")

          browser.findFirst("#invitation").isDisplayed must equalTo(false)
          browser.findFirst("#game").isDisplayed must equalTo(true)

          browser.switchTo(firstTab)

          browser.findFirst("#invitation").isDisplayed must equalTo(false)
          browser.findFirst("#game").isDisplayed must equalTo(true)

          browser.getDriver.close()

          // when I close, do I need to switch ?
          // it remains only one window
          val tabs = browser.getDriver.getWindowHandles()
          browser.getDriver.switchTo().window(tabs.iterator().next())

          browser.findFirst("#disconnected").isDisplayed must equalTo(true)
    })}}


    examplesBlock {
      for (d <- drivers) {
        "allow P1 to create a game, P2 to join, P1 to play the first move" in ((s: String) => new WithBrowser(d(s)) {

          browser.goTo("/")

          browser.pageSource must contain("Awale")

          browser.click("#click")

          browser.pageSource must contain("To invite")

          browser.await().atMost(5, TimeUnit.SECONDS).until("#invitation").areDisplayed()
          browser.findFirst("#invitation").isDisplayed must equalTo(true)
          browser.findFirst("#game").isDisplayed must equalTo(false)

          val joinUrl = browser.$("#join-url").getValue

          val firstTab = browser.getDriver.getWindowHandle
          browser.goToInNewTab(joinUrl, "P2")

          browser.findFirst("#invitation").isDisplayed must equalTo(false)
          browser.findFirst("#game").isDisplayed must equalTo(true)

          browser.switchTo(firstTab)

          browser.findFirst("#invitation").isDisplayed must equalTo(false)
          browser.findFirst("#game").isDisplayed must equalTo(true)

          browser.find(".col").get(6).click()
          browser.await().atMost(5, TimeUnit.SECONDS).until("#passive").areDisplayed()
          browser.find(".col").getTexts.toList must equalTo(Seq("4","4","4","4","4","4","0","5","5","5","5","4"))
    })}}

    examplesBlock {
      for (d <- drivers) {
        "display the number of connected players" in ((s: String) => new WithBrowser(d(s)) {

          browser.goTo("/")

          browser.$("#nb-players").getText must equalTo("1")

          val firstTab = browser.getDriver.getWindowHandle
          browser.goToInNewTab("/", "P2")

          browser.$("#nb-players").getText must equalTo("2")

          browser.goToInNewTab("/", "P3")
          browser.$("#nb-players").getText must equalTo("3")

          browser.getDriver.close

          browser.getDriver.switchTo().window("P2")
          browser.$("#nb-players").getText must equalTo("2")

          browser.getDriver.switchTo().window(firstTab)
          browser.$("#nb-players").getText must equalTo("2")


          // browser.getDriver.switchTo().window(firstTab)
          // browser.firstTab(firstTab, browser.getDriver)
        })}}


  }
}

object FluentExtensions {

  implicit class EnhancedFluentAdapter(f: Fluent) {
    val initialHandle = f.getDriver.getWindowHandle

    def goToInNewTab(url: String, windowName: String): Fluent = {
      f.executeScript(s"window.open('${url}', '${windowName}');")
      f.getDriver.switchTo().window(windowName)
      f
    }
    def switchTo(windowName: String) : Fluent = {
      f.getDriver.switchTo().window(windowName)
      f
    }
  }
}

trait EnvAwareDriver {
  import DesiredCapabilities._
  def drivers: Seq[String => WebDriver] = {
    if (System.getenv("CI") != "true") {
      List(_ => WebDriverFactory(FIREFOX))
    } else {
      // do not instantiate RemoteWebDriver early, it creates an HTTP conn behind the scene
       List((firefox(), "40.0"), (firefox(), "38.0"), (firefox(), "39.0")).map { case (caps, version) =>
         (name : String) =>
            caps.setCapability("platform", "Windows 7")
            caps.setCapability("version", version)
            caps.setCapability("tunnelIdentifier", System.getenv("TRAVIS_JOB_NUMBER"))
            caps.setCapability("build", System.getenv("TRAVIS_BUILD_NUMBER"))
            caps.setCapability("name", name)
            new RemoteWebDriver(new URL("http://yamo93:c1783a7f-802a-41b5-af11-6c6d1841851e@ondemand.saucelabs.com:80/wd/hub"), caps)
      }
    }
  }
}