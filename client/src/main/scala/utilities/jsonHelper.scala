package utilities

import upickle.Js

import scala.scalajs.js.Date
import java.util

trait jsonHelper {

  implicit val dateTimeWriter = upickle.default.Writer[util.Date] {
    case date => Js.Str(date.toString)
  }

  implicit val dateTimeReader = upickle.default.Reader[util.Date] {
    case Js.Str(str) => new util.Date(new Date(str).getTime().toLong)
    case number => new util.Date(number.value.toString.toLong)
  }

  implicit val optionalDateTimeReader = upickle.default.Reader[Option[util.Date]] {
    case Js.Str(str) => Option(new util.Date(new Date(str).getTime().toLong))
    case Js.Num(number) => Option(new util.Date(number.toLong))
    case _ => None
  }

  implicit val optionalIntReader = upickle.default.Reader[Option[Int]] {
    case Js.Num(number) => Option(number.toInt)
    case _ => None
  }

  implicit val optionalLongReader = upickle.default.Reader[Option[Long]] {
    case Js.Num(number) => Option(number.toLong)
    case _ => None
  }

  implicit val optionalStringReader = upickle.default.Reader[Option[String]] {
    case Js.Str(str) => Option(str)
    case _ => None
  }
}
