This file will be packaged with your application, when using `activator dist`.

## How to run

* Run the server with activator run
* Open a browser on localhost:9000

## How to deploy

prerequisite : java 8 on the server
activator dist
scp
unzip
./bin/awale

activator test : js and scala
activator jasmine : js test

## Todo

* resist to browser reload
* manage server crash
* testing actor : http://doc.akka.io/docs/akka/2.3.14/scala/testing.html
* dockerize
* use jshint, babel
* mobile
* display time elapsed by player
* log : https://www.playframework.com/documentation/2.3.x/ScalaLogging
* don't expose all methods of the prototype
* extends jasmine with custom check for the whole board
* manage end of game with equality (draw)
* save game in a backend
* when capturing, animate score modification point by point
* sauce labs
* manage URL with pushState
* DNS awale.org
* heroku
* preprod / prod
* get javascript console from sauce labs test
* SSL and WSS
* headers
* cookies to identify returning visitor
* session play ?
* disable animation and store pref is localstorage
* make the sytem work if mongo not present
* use cache with travis
* add a DNS for staging env

compatibility
Array.prototype.findIndex : FF25, Chrome 45

## Design decision
* unobtrusive javascript
* State of the game is managed client side to reuse existing code
* use raw text for websocket communication (simple)

## Travis REX
* Configuration of github is made automatically by travis

## GUI testing REX
* HTMLUnit do not support javascript
      //[error] Caused by com.gargoylesoftware.htmlunit.ScriptException: TypeError: Cannot find function addEventListener in object [object HTMLDocument]. (http://localhost:19001/assets/awale-ws.js#134)

* Firefox Driver fails with Play 2.3 -> upgrade selenium
* Travis fails to run test -> configure xvfb
Firefox console output:
Error: no display specified
=> works but 14 min instead of 2 min
* Running on sauce labs (FirefoxDriver locally and RemoteWebDriver remotely)
[error]    IllegalArgumentException: : No enum constant org.openqa.selenium.Platform.Windows 7  (Platform.java:30)
https://code.google.com/p/selenium/issues/detail?id=8083
* Update selenium
object MyController extends Controller {
  lazy val supervisor = Akka.system.actorOf(Props[SupervisorActor])
}
object Global extends GlobalSettings {

  var supervisor : ActorRef = null;

  override def onStart(app: Application) {
    supervisor = Akka.system.actorOf(Props[SupervisorActor])

  }
}

* compilation issue
[error] /home/travis/build/YannMoisan/awale/test/IntegrationSpec.scala:32: not found: value TimeUnit

[error]       browser.await().atMost(5, TimeUnit.SECONDS).until("#invitation").areDisplayed()
* "#0" works in the browser but not in the test
http://www.w3.org/TR/html5/dom.html#the-id-attribute
* use browser tabs instead of 2 drivers

* travis setup heroku

* automatically pass the example name to sauce labs

* configure nginx for websocket proxy
location /wsapp/ {
    proxy_pass http://wsbackend;
    proxy_http_version 1.1;
    proxy_set_header Upgrade $http_upgrade;
    proxy_set_header Connection "upgrade";
}

* websocket disconnection on heroku ?
* https://devcenter.heroku.com/articles/websockets#application-architecture
* https://devcenter.heroku.com/articles/websocket-security

    no_ssl_bump_domains: all

reactive mongo 0.11.7 with play 2.3

## Links :
* http://docs.travis-ci.com/user/gui-and-headless-browsers/
* https://docs.saucelabs.com/tutorials/java/
* https://saucelabs.com/platforms/
* https://docs.saucelabs.com/ci-integrations/travis-ci
* http://ics-software-engineering.github.io/play-example-fluentlenium/
* http://neemzy.org/articles/deploy-to-your-own-server-through-ssh-with-travis-ci
* https://guides.github.com/pdfs/githubflow-online.pdf
* https://panopticlick.eff.org/
