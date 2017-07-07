lazy val feria = project.settings(
  scalaVersion := "2.11.7",
  version := "0.2.5",
  libraryDependencies ++= Seq(
    "org.seleniumhq.selenium" % "selenium-java" % "3.4.0",
    "com.github.scopt" %% "scopt" % "3.3.0",
    "joda-time" % "joda-time" % "2.9.3",
    "org.joda" % "joda-convert" % "1.8",
    "org.typelevel" %% "cats" % "0.9.0"
  )
)

run in Compile <<= (run in Compile in feria)
