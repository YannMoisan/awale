Awale
=====

[![Build Status](https://travis-ci.org/YannMoisan/awale-server.svg?branch=master)](https://travis-ci.org/YannMoisan/awale-server)

This file will be packaged with your application, when using `activator dist`.

## How to run

* Run the server with activator run
* Open a browser on localhost:9000

## How to deploy

prerequisite : java 8 on the server
activator dist
scp
unzip
./bin/awale-server

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
* block a third player to join the game
* log : https://www.playframework.com/documentation/2.3.x/ScalaLogging
* don't expose all methods of the prototype
* extends jasmine with custom check for the whole board
* manage end of game with equality
* save game in a backend
* when capturing, animate score modification point by point
* sauce labs
* choose a licence
* fix a bug on this.capturing…

compatibility
Array.prototype.findIndex : FF25, Chrome 45

## Design decision
* unobtrusive javascript
* State of the game is managed client side to reuse existing code

## Travis REX
* Configuration of github is made automatically by travis

## GUI testing REX
* HTMLUnit do not support javascript
* Firefox Driver fails with Play 2.3 -> upgrade selenium
* Travis fails to run test -> configure xvfb
Firefox console output:
Error: no display specified
=> works but 14 min instead of 2 min
* Running on sauce labs (FirefoxDriver locally and RemoteWebDriver remotely)
[error]    IllegalArgumentException: : No enum constant org.openqa.selenium.Platform.Windows 7  (Platform.java:30)
https://code.google.com/p/selenium/issues/detail?id=8083
* Update selenium

## Links :
* http://docs.travis-ci.com/user/gui-and-headless-browsers/
* https://docs.saucelabs.com/tutorials/java/
* https://saucelabs.com/platforms/
* https://docs.saucelabs.com/ci-integrations/travis-ci
