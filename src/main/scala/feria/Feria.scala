package feria

import org.joda.time.{DateTime, DateTimeZone}
import org.openqa.selenium._
import org.openqa.selenium.firefox._
import org.openqa.selenium.firefox.internal._
import org.openqa.selenium.remote.DesiredCapabilities
import scopt._

import scala.sys.process._
import scala.util._

case class Config(profiles: Seq[String] = Nil, access: String = "dev")

object Feria extends App {
  val os = System.getProperty("os.name")
  val arch = System.getProperty("os.arch")

  val geckodriverPath = (os, arch) match {
    case ("Mac OS X", "x86_64") => Some("./bin/geckodriver-osx")
    case ("Linux", "amd64") => Some("./bin/geckodriver-linux")
    case _ => None
  }

  val validAccessValues = Set("dev", "cloudformation", "s3-all", "s3-read", "lambda", "kinesis-read", "sqs-consumer")
  val validAccessValuesString = validAccessValues.mkString("[", ", ", "]")
  val optionParser = new OptionParser[Config]("feria") {
    opt[String]("access") action { (a, cfg) => cfg.copy(access = a) 
    } text s"The type of access you need. One of $validAccessValuesString. Default = dev"

    arg[String]("profiles").required.unbounded() action { (p, cfg) => cfg.copy(profiles = cfg.profiles :+ p)
    } text "The AWS profile ID(s), e.g. capi frontend"

    checkConfig { c =>
      if (!validAccessValues(c.access))
        failure(s"Invalid access type. Expected one of $validAccessValuesString")
      else
        success
    }
  }

  optionParser.parse(args, Config()) map { config => run(config) }

  def run(config: Config): Unit = {
    val maybeDriver = geckodriverPath.map(binary => {
      System.setProperty("webdriver.gecko.driver", binary)

      // Use default profile so we are logged in to Google
      val capabilities = new FirefoxOptions()
        .setProfile(new ProfilesIni().getProfile("default"))
        .addTo(DesiredCapabilities.firefox)

      new FirefoxDriver(capabilities)
    })

    for(profile <- config.profiles; driver <- maybeDriver) {
      try {
        val permissionId = s"${profile}-${config.access}"
        val timeZoneOffset = DateTimeZone.getDefault.getOffset(DateTime.now) / 3600000
        driver.get(s"https://janus.gutools.co.uk/credentials?permissionId=$permissionId&tzOffset=$timeZoneOffset")
        // If you are signed in to multiple accounts you will be redirected to an 'Account Chooser' page
        if (driver.getCurrentUrl.contains("AccountChooser"))
          driver.findElementByXPath("//button[contains(@value,'guardian.co.uk')]").click()

        // What luck! There's only one textarea on the page, and it happens to be the element we want
        Try(driver.findElement(By.tagName("textarea"))) match {
          case Success(textarea) =>
            val commands = textarea.getAttribute("value").split("\n")
              .map(_.replaceAllLiterally("""&& \""", "").trim)
            for (cmd <- commands) {
              val exitCode = cmd.!
              println(s"Executed command. Command = [$cmd], exit code = [$exitCode]")
            }
          case _ =>
            Console.err.println("Couldn't find the textarea I was looking for. Has your Google login expired?")
        }
      } finally {
        driver.quit()
      }
    }

    if (maybeDriver.isEmpty)
      Console.err.println(s"Couldn't find a geckodriver binary for $os $arch.")
  }

}
