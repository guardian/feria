package feria

import org.openqa.selenium._
import org.openqa.selenium.firefox._
import org.openqa.selenium.firefox.internal._

import scopt._

import scala.collection.JavaConverters._
import scala.util._
import scala.sys.process._

case class Config(profile: String = "", access: String = "dev")

object Feria extends App {

  val validAccessValues = Set("dev", "admin", "cloudformation")
  val validAccessValuesString = validAccessValues.mkString("[", ", ", "]")
  val optionParser = new OptionParser[Config]("feria") {
    opt[String]("access") action { (a, cfg) => cfg.copy(access = a) 
    } text s"The type of access you need. One of $validAccessValuesString. Default = dev"

    arg[String]("profile") action { (p, cfg) => cfg.copy(profile = p) 
    } text "The AWS profile ID, e.g. capi"

    checkConfig { c =>
      if (!validAccessValues(c.access))
        failure(s"Invalid access type. Expected one of $validAccessValuesString")
      else
        success
    }
  }

  optionParser.parse(args, Config()) map { config => run(config) }

  def run(config: Config): Unit = {
    val driver = {
      // Use default profile so we are logged in to Google
      val firefoxProfile = new ProfilesIni().getProfile("default")
      new FirefoxDriver(firefoxProfile)
    }

    try {
      val permissionId = s"${config.profile}-${config.access}"  
      driver.get(s"https://janus.gutools.co.uk/credentials?permissionId=$permissionId")

      // What luck! There's only one textarea on the page, and it happens to be the element we want
      Try(driver.findElement(By.tagName("textarea"))) match {
        case Success(textarea) =>
          val commands = textarea.getAttribute("value").split("\n")
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

}
