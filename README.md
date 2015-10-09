Awale
=====

Travis : [![Build Status](https://travis-ci.org/YannMoisan/awale-server.svg?branch=master)](https://travis-ci.org/YannMoisan/awale-server)

Sauce Labs : [![Sauce Test Status](https://saucelabs.com/buildstatus/yamo93)](https://saucelabs.com/u/yamo93)

It's a free online awale game

It is written in Scala 2.11, and relies on Play 2.3. It uses MongoDB 3.0 to store games. HTTP requests and websocket connections are proxied by nginx. Client-side is written with raw javascript.

Use [github issues](https://github.com/YannMoisan/awale-server/issues) for bug reports and feature requests.

Staging
-------
After each push, tests are executed on [Travis CI](https://travis-ci.org/YannMoisan/awale-server), then functional tests on SauceLabs. If successfull, it's deployed on a staging env on [heroku](http://awale-server.herokuapp.com/).

