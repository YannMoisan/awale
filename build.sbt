import sbt.Keys._
import sbt.Tests

name := """awale"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala).settings(jasmineSettings : _*)  //this adds jasmine settings from the sbt-jasmine plugin into the project
  .settings(
    // Add your own project settings here
    // jasmine configuration, overridden as we don't follow the default project structure sbt-jasmine expects
    appJsDir <+= baseDirectory / "app/assets",
    appJsLibDir <+= baseDirectory / "public/javascripts/lib",
    jasmineTestDir <+= baseDirectory / "test/assets/",
    jasmineConfFile <+= baseDirectory / "test/assets/test.dependencies.js",
    // link jasmine to the standard 'sbt test' action. Now when running 'test' jasmine tests will be run, and if they pass
    // then other Play tests will be executed.
    (test in Test) <<= (test in Test) dependsOn (jasmine)
  )

scalaVersion := "2.11.7"

libraryDependencies += ws

libraryDependencies += "org.webjars" % "jasmine" % "2.2.0"

libraryDependencies += "org.seleniumhq.selenium" % "selenium-java" % "2.48.2" // % test

libraryDependencies += "org.reactivemongo" %% "reactivemongo" % "0.11.7"

libraryDependencies += specs2 % Test

resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"
