/*
 * Copyright 2013 Toshiyuki Takahashi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package alpaca

import org.openqa.selenium.WebDriver
import org.openqa.selenium.firefox.{ FirefoxProfile, FirefoxDriver }
import java.io.File
import com.typesafe.scalalogging.slf4j._

object Alpaca extends Logging {

  case class Config(profile: Option[File], script: File, args: List[String], info: Boolean, debug: Boolean, maximize: Boolean, quit: Boolean)

  def main(args: Array[String]): Unit = {

    def createDriver(profileDir: Option[File]): WebDriver = {
      val profile = profileDir.map {
        d => new FirefoxProfile(d)
      }.getOrElse(new FirefoxProfile())
      profile.setPreference("intl.accept_languages", "no,ja-jp,en")
      new FirefoxDriver(profile)
    }

    def setUpLogger(config: Config) {
      import org.slf4j.LoggerFactory
      import ch.qos.logback.classic.Level
      import ch.qos.logback.classic.Logger
      val root = LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME).asInstanceOf[Logger]
      if (config.debug) {
        root.setLevel(Level.DEBUG)
      } else if (config.info) {
        root.setLevel(Level.INFO)
      } else {
        root.setLevel(Level.ERROR)
      }
    }

    val parser = new scopt.OptionParser[Config]("alpaca") {
      head("alpaca", "0.1.1")
      opt[File]('p', "profile") text "firefox profile" optional () action {
        (x, c) => c.copy(profile = Some(x))
      }
      opt[Unit]('v', "verbose") text "verbose mode" optional () action {
        (x, c) => c.copy(info = true)
      }
      opt[Unit]('d', "debug") text "debug mode" optional () action {
        (x, c) => c.copy(debug = true)
      }
      opt[Unit]("maximize") text "maximize the window" optional () action {
        (x, c) => c.copy(maximize = true)
      }
      opt[Unit]('q', "quit") text "close the window automatically" optional () action {
        (x, c) => c.copy(quit = true)
      }
      arg[File]("<file>") required () action {
        (x, c) => c.copy(script = x)
      } text "scripts"
      arg[String]("<arg1> <arg2> <arg3>...") unbounded () optional () action {
        (x, c) => c.copy(args = c.args :+ x)
      } text "command line arguments"
    }

    val defaultConfig = Config(
      profile = None,
      script = null,
      args = Nil,
      info = false,
      debug = false,
      maximize = false,
      quit = false
    )

    parser.parse(args, defaultConfig).foreach {
      config =>
        setUpLogger(config)

        config.profile match {
          case Some(p) => logger.info(s"Profile: $p")
          case None => logger.info(s"Profile: default")
        }

        val expressions: List[AST] = Interpreter.loadFile(config.script)
        logger.info(config.toString)
        logger.info("Opening browser...")
        val driver = createDriver(config.profile)

        if (config.maximize) {
          driver.manage().window().maximize()
        }
        val environment = new GlobalEnvironment(driver, config.script)
        environment.setVariable('args, Value(ListType, config.args), reassignment = false)
        try {
          Interpreter.evaluateStatements(expressions)(environment)
        } catch {
          case e: UnexpectedException => {
            Console.err.println("Maybe it's a bug.")
            e.printStackTrace()
            driver.close()
            sys.exit(1)
          }
          case e: Throwable => {
            println(e.getMessage)
            if (config.debug) {
              e.printStackTrace()
            }
            driver.close()
            sys.exit(1)
          }
        }

        if (config.quit) {
          driver.close()
          driver.quit()
        }
    }
  }
}
