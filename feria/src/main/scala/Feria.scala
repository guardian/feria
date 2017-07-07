package feria

import cats.syntax.either._
import org.joda.time.{DateTime, DateTimeZone}
import org.openqa.selenium._
import org.openqa.selenium.firefox._
import org.openqa.selenium.firefox.internal._
import org.openqa.selenium.remote.DesiredCapabilities
import scopt._

import scala.sys.process._
import scala.util._

case class Config(profiles: Seq[String] = Nil, access: String = "dev")
case class AWSCommand(command: String) {
  def run: Unit = {
    val exitCode = command.!
    println(s"Executed command. Command = [${command}], exit code = [${exitCode}]")
  }
}

object Feria {
  private def driverFor(os: String, arch: String): Option[FirefoxDriver] = {
    val path = (os, arch) match {
      case ("Mac OS X", "x86_64") => Some("./bin/geckodriver-osx")
      case ("Linux", "amd64") => Some("./bin/geckodriver-linux")
      case _ => None
    }

    path.map(binary => {
      System.setProperty("webdriver.gecko.driver", binary)

      // Use default profile so we are logged in to Google
      val capabilities = new FirefoxOptions()
        .setProfile(new ProfilesIni().getProfile("default"))
        .addTo(DesiredCapabilities.firefox)

      new FirefoxDriver(capabilities)
    })
  }

  private def commandsFor(profile: String, driver: FirefoxDriver, config: Config): Either[String, List[AWSCommand]] = {
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

        Right(commands.map(AWSCommand).toList)
      case _ =>
        Left("Couldn't find the textarea I was looking for. Has your Google login expired?")
    }
  }

  def getAWSCommands(config: Config): Either[String, List[AWSCommand]] = {
    val os = System.getProperty("os.name")
    val arch = System.getProperty("os.arch")

    driverFor(os, arch)
      .toRight(s"Couldn't find a geckodriver binary for $os $arch.")
      .flatMap { driver =>
        import cats.syntax.traverse._
        import cats.instances.list._
        import cats.instances.either._

        config.profiles.toList.traverseU(commandsFor(_, driver, config)).map(_.flatten)
      }
  }
}

object Main extends App {
  val validAccessValues = Set("dev", "cloudformation", "s3-all", "s3-read", "lambda", "kinesis-read", "sqs-consumer")
  val validAccessValuesString = validAccessValues.mkString("[", ", ", "]")
  val optionParser = new OptionParser[Config]("feria") {
    opt[String]("access") action { (a, cfg) => cfg.copy(access = a)
  } text s"The type of access you need. One of $validAccessValuesString. Default = dev"

  arg[String]("profiles").required.unbounded() action { (p, cfg) =>
    cfg.copy(profiles = cfg.profiles :+ p)
  } text "The AWS profile ID(s), e.g. capi frontend"

    checkConfig { c =>
      if (!validAccessValues(c.access))
        failure(s"Invalid access type. Expected one of $validAccessValuesString")
      else
        success
    }
  }

  optionParser.parse(args, Config())
    .toRight(s"could not create config from args: ${args.mkString(",")}")
    .flatMap(Feria.getAWSCommands)
    .fold(
      err => Console.err.println(err),
      cmds => cmds.foreach(_.run)
    )
}