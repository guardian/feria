scalaVersion := "2.11.8"

version := "0.2.3"

libraryDependencies ++= Seq(
  "org.seleniumhq.selenium" % "selenium-java" % "2.53.1",
  "com.github.scopt" %% "scopt" % "3.5.0",
  "joda-time" % "joda-time" % "2.9.4",
  "org.joda" % "joda-convert" % "1.8.1"
)
