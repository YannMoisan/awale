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
    (test in Test) <<= (test in Test) dependsOn (jasmine),
    parallelExecution in (Test) := false,
    testOptions in Test += Tests.Argument("sequential")
  )

scalaVersion := "2.11.1"

libraryDependencies ++= Seq(
  jdbc,
  anorm,
  cache,
  ws
)

libraryDependencies += "org.webjars" % "jasmine" % "2.2.0"

libraryDependencies += "org.seleniumhq.selenium" % "selenium-java" % "2.46.0" // % test

libraryDependencies += "org.reactivemongo" %% "reactivemongo" % "0.10.5.0.akka23"
