import java.util.concurrent.TimeUnit

import org.fluentlenium.core.FluentPage
import org.fluentlenium.core.domain.FluentWebElement
import org.junit.runner._
import org.openqa.selenium.support.FindBy
import org.specs2.concurrent.ExecutionEnv
import org.specs2.main.CommandLine
import org.specs2.mutable._
import org.specs2.runner._
import play.api.test.WithBrowser
import play.api.libs.json._

import scala.collection.JavaConversions._

/**
 * add your integration spec here.
 * An integration test will fire up a whole play application in a real (or headless) browser
 */
@RunWith(classOf[JUnitRunner])
class IntegrationSpec extends Specification with MultiBrowser with EnvAwareDriver with Drivers {
  import FluentExtensions._

  "Application" should {

    browsers { d =>
      "allow P1 to create a game" in (new WithBrowserAndSauceLabsUpdater(d("allow P1 to create a game")) {

        browser.goTo("/")

        browser.pageSource must contain("Awale")

        browser.click("#click")

        browser.pageSource must contain("To invite")

        browser.await().atMost(30, TimeUnit.SECONDS).until("#invitation").areDisplayed()
        browser.findFirst("#invitation").isDisplayed must equalTo(true)
        browser.findFirst("#game").isDisplayed must equalTo(false)
      })
    }

    browsers { d =>
      "allow P1 to create a game, P2 to join" in (new WithBrowserAndSauceLabsUpdater(d("allow P1 to create a game, P2 to join")) {

        val firstTab = browser.getDriver.getWindowHandle
        val page = browser.createPage(classOf[AwaleSinglePage])

        browser.goTo(page)

        browser.goToInNewTab(page.joinUrl, "P2")

        browser.findFirst("#invitation").isDisplayed must equalTo(false)
        browser.await().atMost(30, TimeUnit.SECONDS).until("#game").areDisplayed() // tempo for chrome on sauce labs

        browser.switchTo(firstTab)

        browser.findFirst("#invitation").isDisplayed must equalTo(false)
        browser.findFirst("#game").isDisplayed must equalTo(true)
      })
    }

    browsers { d =>
      "allow P1 to create a game, P2 to join, P1 to disconnect, P2 to be notified" in (new WithBrowserAndSauceLabsUpdater(d("allow P1 to create a game, P2 to join, P1 to disconnect, P2 to be notified")) {

        val firstTab = browser.getDriver.getWindowHandle
        val page = browser.createPage(classOf[AwaleSinglePage])

        browser.goTo(page)

        browser.goToInNewTab(page.joinUrl, "P2")

        // wait for game begining before closing tabs
        browser.await().atMost(30, TimeUnit.SECONDS).until("#game").areDisplayed() // tempo for chrome on sauce labs

        browser.switchTo(firstTab)
        browser.getDriver.close()
        browser.getDriver.switchTo().window("P2")

        browser.await().atMost(30, TimeUnit.SECONDS).until("#disconnected").areDisplayed() // tempo for chrome on sauce labs
      })
    }


    browsers { d =>
      "allow P1 to create a game, P2 to join, P1 to play the first move" in (new WithBrowserAndSauceLabsUpdater(d("allow P1 to create a game, P2 to join, P1 to play the first move")) {

        val firstTab = browser.getDriver.getWindowHandle
        val page = browser.createPage(classOf[AwaleSinglePage])

        browser.goTo(page)

        browser.goToInNewTab(page.joinUrl, "P2")

        browser.switchTo(firstTab)

        browser.await().atMost(30, TimeUnit.SECONDS).until("#invitation").areNotDisplayed() // tempo for chrome on sauce labs
        browser.findFirst("#game").isDisplayed must equalTo(true)

        browser.await().atMost(30, TimeUnit.SECONDS).until("#active").areDisplayed() // tempo for chrome on sauce labs
        browser.find(".col").get(6).click()
        browser.await().atMost(30, TimeUnit.SECONDS).until("#passive").areDisplayed()
        browser.find(".col").getTexts.toList must equalTo(Seq("4","4","4","4","4","4","0","5","5","5","5","4"))
      })
    }

    browsers { d =>
      "allow a player to capture" in (new WithBrowserAndSauceLabsUpdater(d("allow a player to capture")) {

        val firstTab = browser.getDriver.getWindowHandle
        val page = browser.createPage(classOf[AwaleSinglePage])

        browser.goTo(page)

        browser.goToInNewTab(page.joinUrl, "P2")

        browser.switchTo(firstTab)

        browser.await().atMost(30, TimeUnit.SECONDS).until("#invitation").areNotDisplayed() // tempo for chrome on sauce labs
        browser.findFirst("#game").isDisplayed must equalTo(true)

        // Play 3 (9)
        browser.await().atMost(30, TimeUnit.SECONDS).until("#active").areDisplayed() // tempo for chrome on sauce labs
        browser.find(".col").get(9).click()
        browser.await().atMost(30, TimeUnit.SECONDS).until("#passive").areDisplayed()
        //browser.find(".col").getTexts.toList must equalTo(Seq("4","4","4","4","4","4","0","5","5","5","5","4"))
        browser.switchTo("P2")

        // Play 11 (11)
        browser.await().atMost(30, TimeUnit.SECONDS).until("#active").areDisplayed() // tempo for chrome on sauce labs
        browser.find(".col").get(11).click()
        browser.await().atMost(30, TimeUnit.SECONDS).until("#passive").areDisplayed()
        //browser.find(".col").getTexts.toList must equalTo(Seq("4","4","4","4","4","4","0","5","5","5","5","4"))
        browser.switchTo(firstTab)

        // Play 5 (11)
        browser.await().atMost(30, TimeUnit.SECONDS).until("#active").areDisplayed() // tempo for chrome on sauce labs
        browser.find(".col").get(11).click()
        browser.await().atMost(30, TimeUnit.SECONDS).until("#passive").areDisplayed()
        //browser.find(".col").getTexts.toList must equalTo(Seq("4","4","4","4","4","4","0","5","5","5","5","4"))
        browser.switchTo("P2")

        // Play 7 (10)
        browser.await().atMost(30, TimeUnit.SECONDS).until("#active").areDisplayed() // tempo for chrome on sauce labs
        browser.find(".col").get(10).click()
        browser.await().atMost(30, TimeUnit.SECONDS).until("#passive").areDisplayed()
        browser.find(".col").getTexts.toList must equalTo(Seq("0","5","0","6","6","6","6","6","5","5","0","1"))
        browser.find("#score1").getText must equalTo("2")
        browser.find("#score0").getText must equalTo("0")
      })
    }

    browsers { d =>
      "display the number of connected players" in (new WithBrowserAndSauceLabsUpdater(d("display the number of connected players")) {

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
      })
    }

    browsers { d =>
      "should prevent a third player to join a game"  in (new WithBrowserAndSauceLabsUpdater(d("should prevent a third player to join a game")) {
        val firstTab = browser.getDriver.getWindowHandle
        val page = browser.createPage(classOf[AwaleSinglePage])

        browser.goTo(page)

        val joinUrl = page.joinUrl

        browser.goToInNewTab(joinUrl, "P2")

        browser.goToInNewTab(joinUrl, "P3")

        browser.await().atMost(30, TimeUnit.SECONDS).until("#error").areDisplayed()
      })



    }


  }
}

class AwaleSinglePage extends FluentPage {
  var click: FluentWebElement = null

  @FindBy(css = "#join-url")
  var joinUrlElt: FluentWebElement = null


  def joinUrl : String = {
    click.click
    await().atMost(30, TimeUnit.SECONDS).until("#invitation").areDisplayed()
    joinUrlElt.getValue
  }

  def checkNbPlayers(nbPlayers: String) = await().atMost(30, TimeUnit.SECONDS).until("#nb-players").hasText(nbPlayers)

  override def getUrl = "/"
}

