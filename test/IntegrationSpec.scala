import java.net.URL
import java.util.concurrent.TimeUnit

import org.fluentlenium.core.domain.FluentWebElement
import org.fluentlenium.core.{Fluent, FluentPage}
import org.junit.runner._
import org.openqa.selenium.WebDriver
import org.openqa.selenium.remote.{DesiredCapabilities, RemoteWebDriver}
import org.openqa.selenium.support.FindBy
import org.specs2.execute.{AsResult, Result}
import org.specs2.mutable._
import org.specs2.runner._
import play.api.libs.ws._
import play.api.test.Helpers._
import play.api.test._
import play.libs.Json
import scala.concurrent.ExecutionContext.Implicits.global

import scala.collection.JavaConversions._

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
        "allow P1 to create a game" in ((s: String) => new WithBrowser2(d(s)) {

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
        "allow P1 to create a game, P2 to join" in ((s: String) => new WithBrowser2(d(s)) {

          val firstTab = browser.getDriver.getWindowHandle
          val page = browser.createPage(classOf[AwaleSinglePage])

          browser.goTo(page)

          browser.goToInNewTab(page.joinUrl, "P2")

          browser.findFirst("#invitation").isDisplayed must equalTo(false)
          browser.findFirst("#game").isDisplayed must equalTo(true)

          browser.switchTo(firstTab)

          browser.findFirst("#invitation").isDisplayed must equalTo(false)
          browser.findFirst("#game").isDisplayed must equalTo(true)
    })}}

    examplesBlock {
      for (d <- drivers) {
        "allow P1 to create a game, P2 to join, P1 to disconnect, P2 to be notified" in ((s: String) => new WithBrowser2(d(s)) {

          val firstTab = browser.getDriver.getWindowHandle
          val page = browser.createPage(classOf[AwaleSinglePage])

          browser.goTo(page)

          browser.goToInNewTab(page.joinUrl, "P2")

          browser.switchTo(firstTab)
          browser.getDriver.close()
          browser.getDriver.switchTo().window("P2")

          browser.findFirst("#disconnected").isDisplayed must equalTo(true)
    })}}


    examplesBlock {
      for (d <- drivers) {
        "allow P1 to create a game, P2 to join, P1 to play the first move" in ((s: String) => new WithBrowser2(d(s)) {

          val firstTab = browser.getDriver.getWindowHandle
          val page = browser.createPage(classOf[AwaleSinglePage])

          browser.goTo(page)

          browser.goToInNewTab(page.joinUrl, "P2")

          browser.switchTo(firstTab)

          browser.findFirst("#invitation").isDisplayed must equalTo(false)
          browser.findFirst("#game").isDisplayed must equalTo(true)

          browser.await().atMost(5, TimeUnit.SECONDS).until("#active").areDisplayed() // tempo for chrome on sauce labs
          browser.find(".col").get(6).click()
          browser.await().atMost(5, TimeUnit.SECONDS).until("#passive").areDisplayed()
          browser.find(".col").getTexts.toList must equalTo(Seq("4","4","4","4","4","4","0","5","5","5","5","4"))
    })}}

    examplesBlock {
      for (d <- drivers) {
        "display the number of connected players" in ((s: String) => new WithBrowser2(d(s)) {

          val firstTab = browser.getDriver.getWindowHandle
          val page = browser.createPage(classOf[AwaleSinglePage])

          page.go()
          page.checkNbPlayers("1")

          browser.goToInNewTab("/", "P2")

          page.checkNbPlayers("2")

          browser.goToInNewTab("/", "P3")
          page.checkNbPlayers("3")

          browser.getDriver.close

          browser.getDriver.switchTo().window("P2")
          page.checkNbPlayers("2")

          browser.getDriver.switchTo().window(firstTab)
          page.checkNbPlayers("2")

          // browser.getDriver.switchTo().window(firstTab)
          // browser.firstTab(firstTab, browser.getDriver)
        })}}


  }
}

class AwaleSinglePage extends FluentPage {
  var click: FluentWebElement = null

  @FindBy(css = "#join-url")
  var joinUrlElt: FluentWebElement = null


  def joinUrl : String = {
    click.click
    await().atMost(1, TimeUnit.SECONDS).until("#invitation").areDisplayed()
    joinUrlElt.getValue
  }

  def checkNbPlayers(nbPlayers: String) = await().atMost(1, TimeUnit.SECONDS).until("#nb-players").hasText(nbPlayers)

  override def getUrl = "/"
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
       List((firefox(), "40.0"), (chrome(), "45.0"), (firefox(), "39.0")).map { case (caps, version) =>
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

abstract class WithBrowser2[WEBDRIVER <: WebDriver](
                                                    webDriver: WebDriver = WebDriverFactory(Helpers.HTMLUNIT),
                                                    app: FakeApplication = FakeApplication(),
                                                    port: Int = Helpers.testServerPort) extends WithBrowser(webDriver, app, port) {

  override def around[T: AsResult](t: => T): Result = {
    try {
      val r = Helpers.running(TestServer(port, app))(AsResult.effectively(t))
      if (webDriver.isInstanceOf[RemoteWebDriver]) {
        val sessionId = webDriver.asInstanceOf[RemoteWebDriver].getSessionId
        println(s"sessionId:${sessionId}")
        val holder: WSRequestHolder = WS.url(s"https://saucelabs.com/rest/v1/yamo93/jobs/${sessionId}")
        //val data = Json.obj("passed" -> r.isSuccess)
        val data = Json.parse(s"""{"passed": ${r.isSuccess}}""")
        val f = holder.withAuth("yamo93", "c1783a7f-802a-41b5-af11-6c6d1841851e", WSAuthScheme.BASIC).post(data.asText())
        f.onComplete(t => println(s"Job update result : ${t.map(r => r.body)}"))
      }
      r
    } finally {
      browser.quit()
    }
  }
}

