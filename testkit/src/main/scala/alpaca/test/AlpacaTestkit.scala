package alpaca.test

import scala.io.Source
import java.io.{ PrintWriter, File }
import scala.sys.process._
import com.typesafe.scalalogging.slf4j.Logging

object AlpacaTestkit extends Logging {

  def main(args: Array[String]): Unit = {
    def lines(file: String) = Source.fromFile(file).getLines()

    args.foreach { arg =>
      val file = arg
      val test = extractTestName(lines(file))
      val script = extractScript(lines(file))
      val expected = extractExpectedOutput(lines(file))

      val f = File.createTempFile("test", "alpaca")
      f.deleteOnExit()

      val pw = new PrintWriter(f)
      try {
        pw.println(script)
      } finally {
        if (pw != null) {
          pw.close()
        }
      }

      val command = new File("./bin/alpaca").getCanonicalPath
      val p = Process(List(command, "-q", f.getCanonicalPath))
      val out = p.lines.mkString("\n")

      if (out.trim == expected.trim) {
        logger.info(s"Test $test passed")
      } else {
        logger.error(s"$test failed")
        logger.error(s"$expected expected, but $out given")
      }
    }

  }

  def extractTestName(lines: Iterator[String]): String = {
    lines.dropWhile(_.trim != "--TEST--").drop(1).takeWhile(_.trim != "--FILE--").mkString("\n")
  }

  def extractScript(lines: Iterator[String]): String = {
    lines.dropWhile(_.trim != "--FILE--").drop(1).takeWhile(_.trim != "--EXPECT--").mkString("\n")
  }

  def extractExpectedOutput(lines: Iterator[String]): String = {
    lines.dropWhile(_.trim != "--EXPECT--").drop(1).mkString("\n").trim
  }

}
