scalaVersion := "2.11.7"

version := "0.2.4"

libraryDependencies ++= Seq(
  "org.seleniumhq.selenium" % "selenium-java" % "3.0.1",
  "com.github.scopt" %% "scopt" % "3.3.0",
  "joda-time" % "joda-time" % "2.9.3",
  "org.joda" % "joda-convert" % "1.8"
)
