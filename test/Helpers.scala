import java.net.URL
import java.util.concurrent.TimeUnit

import org.fluentlenium.core.Fluent
import org.openqa.selenium.WebDriver
import org.openqa.selenium.remote.{DesiredCapabilities, RemoteWebDriver, SessionId}
import org.specs2.execute.{AsResult, Result}
import org.specs2.mutable.Specification
import org.specs2.specification.core.Fragment
import play.api.libs.json.Json
import play.api.libs.ws.{WS, WSAuthScheme, WSRequestHolder}
import play.api.test.Helpers._
import play.api.test.{FakeApplication, Helpers, WebDriverFactory, WithBrowser}

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

/**
 * Created by yamo on 20/10/15.
 */
object FluentExtensions {

  implicit class EnhancedFluentAdapter(f: Fluent) {

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

trait MultiBrowser {
  self : Specification =>

  def drivers : Seq[String => WebDriver]

  def browsers(u: (String => WebDriver) => Fragment) = Fragment.foreach(drivers) { driver => u(driver) }
}

trait EnvAwareDriver {
  def localDrivers: Seq[String => WebDriver]
  def remoteDrivers: Seq[String => WebDriver]

  def drivers: Seq[String => WebDriver] = if (System.getenv("CI") != "true") localDrivers else remoteDrivers
}

trait Drivers {
  def localDrivers: Seq[String => WebDriver] = List(_ => WebDriverFactory(FIREFOX))
  def remoteDrivers: Seq[String => WebDriver] = {
    import DesiredCapabilities._
    import SauceLabs._
    List((firefox(), "40.0"), (firefox(), "38.0"), (firefox(), "39.0")).map { case (caps, version) =>
      (name: String) =>
        caps.setCapability("platform", "Windows 7")
        caps.setCapability("version", version)
        caps.setSauceLabs(name)
        new RemoteWebDriver(new URL("http://yamo93:c1783a7f-802a-41b5-af11-6c6d1841851e@ondemand.saucelabs.com:80/wd/hub"), caps)

    }
  }
}

object SauceLabs {
  implicit class SauceLabsCapabilities(caps: DesiredCapabilities) {
    def setSauceLabs(name: String) = {
      caps.setCapability("tunnelIdentifier", System.getenv("TRAVIS_JOB_NUMBER"))
      caps.setCapability("build", System.getenv("TRAVIS_BUILD_NUMBER"))
      caps.setCapability("name", name)
    }
  }
}

abstract class WithBrowserAndSauceLabsUpdater[WEBDRIVER <: WebDriver](
                                                                       webDriver: WebDriver = WebDriverFactory(Helpers.HTMLUNIT),
                                                                       app: FakeApplication = FakeApplication(),
                                                                       port: Int = Helpers.testServerPort) extends WithBrowser(webDriver, app, port) {

  // call synchronously the Sauce Labs REST API
  def updateJob(sessionId: SessionId, passed: Boolean) = {
    val holder: WSRequestHolder = WS.url(s"https://saucelabs.com/rest/v1/yamo93/jobs/${sessionId}")
    val data = Json.obj("passed" -> passed)
    val f = holder.withAuth("yamo93", "c1783a7f-802a-41b5-af11-6c6d1841851e", WSAuthScheme.BASIC).put(data).map(t => {println(t.body)})
    Await.result (f, Duration(5, TimeUnit.SECONDS))
  }

  def getSessionId() : Option[SessionId] = webDriver match {
    case remote : RemoteWebDriver => Some(remote.getSessionId)
    case _ => None
  }

  override def around[T: AsResult](t: => T): Result = {
    var maybeResult : Option[Result] = None
    val maybeSessionId = getSessionId()  // call before browser.quit() in super.around
    try {
      maybeResult = Some(super.around(t))
      maybeResult.get
    }
    finally {
      maybeSessionId.foreach { updateJob(_, maybeResult.map(_.isSuccess).getOrElse(false)) }
    }
  }
}
