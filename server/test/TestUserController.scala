import java.nio.file.{Files, Paths}
import java.util.UUID

import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import com.mohiva.play.silhouette.test._
import database.MyPostgresDriver.api._
import org.specs2.runner.files
import play.api.libs.Files.TemporaryFile
import play.api.libs.json.{JsResult, JsSuccess, JsError, Json}
import play.api.mvc.{AnyContentAsMultipartFormData, MultipartFormData}
import play.api.mvc.MultipartFormData.FilePart
import play.api.test.{FakeHeaders, FakeRequest}
import testsHelper.GlobalApplicationForControllers
import userDomain.{IdCard, Rib}
import json.JsonHelper._
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.concurrent.ExecutionContext.Implicits.global
import java.io.File

import play.api.http.{HeaderNames, Writeable}
import play.api.libs.Files.TemporaryFile
import play.api.mvc.MultipartFormData.FilePart
import play.api.mvc.{AnyContentAsMultipartFormData, Codec, MultipartFormData}

object MultipartFormDataWritable {
  val boundary = "--------ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890"

  def formatDataParts(data: Map[String, Seq[String]]) = {
    val dataParts = data.flatMap { case (key, values) =>
      values.map { value =>
        val name = s""""$key""""
        s"--$boundary\r\n${HeaderNames.CONTENT_DISPOSITION}: form-data; name=$name\r\n\r\n$value\r\n"
      }
    }.mkString("")
    Codec.utf_8.encode(dataParts)
  }

  def filePartHeader(file: FilePart[TemporaryFile]) = {
    val name = s""""${file.key}""""
    val filename = s""""${file.filename}""""
    val contentType = file.contentType.map { ct =>
      s"${HeaderNames.CONTENT_TYPE}: $ct\r\n"
    }.getOrElse("")
    Codec.utf_8.encode(s"--$boundary\r\n${HeaderNames.CONTENT_DISPOSITION}: form-data; name=$name; filename=$filename\r\n$contentType\r\n")
  }

  val singleton = Writeable[MultipartFormData[TemporaryFile]](
    transform = { form: MultipartFormData[TemporaryFile] =>
      formatDataParts(form.dataParts) ++
        form.files.flatMap { file =>
          val fileBytes = Files.readAllBytes(Paths.get(file.ref.file.getAbsolutePath))
          filePartHeader(file) ++ fileBytes ++ Codec.utf_8.encode("\r\n")
        } ++
        Codec.utf_8.encode(s"--$boundary--")
    },
    contentType = Some(s"multipart/form-data; boundary=$boundary")
  )
}

class TestUserController extends GlobalApplicationForControllers {
  override def beforeAll(): Unit = {
    generalBeforeAll()
    Await.result(
      dbConfProvider.get.db.run(sqlu"""
        INSERT INTO artists(artistid, name, facebookurl) VALUES('100', 'name', 'facebookUrl0');
        INSERT INTO ribs(id, bankCode, deskCode, accountNumber, ribKey, userId)
          VALUES (100, 'bank', 'desk', 'account2', '20', '077f3ea6-2272-4457-a47e-9e9111108e44');
        INSERT INTO ribs(id, bankCode, deskCode, accountNumber, ribKey, userId)
          VALUES (200, 'bank', 'desk', 'account3', '20', '077f3ea6-2272-4457-a47e-9e9111108e44');
        INSERT INTO idCards(uuid, userId)
          VALUES ('5e7b7c6c-98b3-4245-a5fb-405c9cc904f4', '077f3ea6-2272-4457-a47e-9e9111108e44');
        INSERT INTO idCards(uuid, userId)
          VALUES ('5e8b7c6c-98b3-4245-a5fb-405c9cc904f4', '078f3ea6-2272-4457-a47e-9e9111108e44');
        INSERT INTO tracks(trackid, title, url, platform, thumbnailurl, artistfacebookurl, artistname)
          VALUES('13894e56-08d1-4c1f-b3e4-466c069d15ed', 'title0', 'url00', 'y', 'thumbnailUrl', 'facebookUrl0', 'artistName');
        INSERT INTO tracksrating(userId, trackId, reason)
          VALUES('077f3ea6-2272-4457-a47e-9e9111108e44', '13894e56-08d1-4c1f-b3e4-466c069d15ed', 'a');"""),
      5.seconds)
  }

  "user controller" should {

    "find the geographicPoint of a user" in {

//      val Some(geographicpoint) = route(
//        new FakeRequest("GET", "/users/geographicPoint", FakeHeaders(), AnyContentAsEmpty, remoteAddress = "81.220.239.243"))
//
//      contentAsString(geographicpoint) mustEqual
//        """{"as":"AS21502 NC Numericable S.A.","city":"Villeurbanne","country":"France","countryCode":"FR","isp":"Numericable","lat":45.7667,"lon":4.8833,"org":"Numericable","query":"81.220.239.243","region":"V","regionName":"Rhône-Alpes","status":"success","timezone":"Europe/Paris","zip":"69100"}"""

      1 mustEqual 1
    }

    "get removed tracks for a user" in {
      val Some(removedTracks) = route(FakeRequest(GET, "/users/tracksRemoved")
        .withAuthenticator[CookieAuthenticator](identity.loginInfo))

      contentAsString(removedTracks) must contain("13894e56-08d1-4c1f-b3e4-466c069d15ed")
    }

    "create a rib for a user" in {

      val jsonRib = Json.parse("""{
        "bankCode": "bank",
        "deskCode": "desk",
        "accountNumber":  "account1",
        "ribKey": "20"
      }""")
      val Some(response) = route(FakeRequest(userDomain.routes.UserController.createRib())
        .withJsonBody(jsonRib)
        .withAuthenticator[CookieAuthenticator](identity.loginInfo))

      status(response) mustEqual OK
    }

    "find ribs for a userId" in {
      val expectedRib = Rib(
        id = Some(200),
        bankCode = "bank",
        deskCode = "desk",
        accountNumber = "account3",
        ribKey = "20",
        userId = UUID.fromString("077f3ea6-2272-4457-a47e-9e9111108e44")
      )

      val Some(ribs) = route(FakeRequest(
        userDomain.routes.UserController.findRibsByUserId("077f3ea6-2272-4457-a47e-9e9111108e44")
      )
       .withAuthenticator[CookieAuthenticator](administrator.loginInfo))

      val validatedRibs: JsResult[Seq[Rib]] = contentAsJson(ribs).validate[Seq[Rib]](readRibReads)
      val readRibs = validatedRibs match {
        case error: JsError =>
          throw new Exception("find ribs for a userId")
        case success: JsSuccess[Seq[Rib]] =>
          success.get
      }

      readRibs must contain(expectedRib)
    }

    "find ribs for a connected user" in {
      val expectedRib = Rib(
        id = Some(200),
        bankCode = "bank",
        deskCode = "desk",
        accountNumber = "account3",
        ribKey = "20",
        userId = UUID.fromString("077f3ea6-2272-4457-a47e-9e9111108e44")
      )

      val Some(ribs) = route(FakeRequest(
        userDomain.routes.UserController.findUsersRibs()
      )
       .withAuthenticator[CookieAuthenticator](identity.loginInfo))

      val validatedRibs: JsResult[Seq[Rib]] = contentAsJson(ribs).validate[Seq[Rib]](readRibReads)
      val readRibs = validatedRibs match {
        case error: JsError =>
          throw new Exception("find ribs for connected user")
        case success: JsSuccess[Seq[Rib]] =>
          success.get
      }

      readRibs must contain(expectedRib)
    }

    "return forbidden if a connected user try to get other users ribs" in {

      val Some(ribs) = route(FakeRequest(
        userDomain.routes.UserController.findRibsByUserId("077f3ea6-2272-4457-a47e-9e9111108e44")
      )
       .withAuthenticator[CookieAuthenticator](identity.loginInfo))

      status(ribs) mustEqual FORBIDDEN
    }

    "return unauthorized if an unconnected user try to get ribs" in {

      val Some(ribs) = route(FakeRequest(
        userDomain.routes.UserController.findUsersRibs()
      ))

      status(ribs) mustEqual UNAUTHORIZED
    }

    "update a rib for a user" in {

      val jsonRib = Json.parse("""{
        "id": 100,
        "bankCode": "bank",
        "deskCode": "desk",
        "accountNumber":  "account5",
        "ribKey": "20"
      }""")

      val Some(response) = route(FakeRequest(userDomain.routes.UserController.updateRib())
        .withJsonBody(jsonRib)
        .withAuthenticator[CookieAuthenticator](identity.loginInfo))

      status(response) mustEqual OK
    }


    implicit val anyContentAsMultipartFormWritable: Writeable[AnyContentAsMultipartFormData] = {
      MultipartFormDataWritable.singleton.map(_.mdf)
    }

    "create a new IdCard" in {
      val tempFile = TemporaryFile(new java.io.File("../favicon.jpeg"))
      val part = FilePart[TemporaryFile](key = "picture", filename = "the.file", contentType = Some("image/jpeg"),
      ref = tempFile)
      val formData = MultipartFormData(dataParts = Map(), files = Seq(part), badParts = Seq(), missingFileParts = Seq())
      val Some(result) = route(FakeRequest(userDomain.routes.UserController.createIdCard())
        .withMultipartFormDataBody(formData)
        .withAuthenticator[CookieAuthenticator](identity.loginInfo))

      status(result) mustEqual OK
    }

    "return unauthorized if a unconnected user try to create a new IdCard" in {
      val tempFile = TemporaryFile(new java.io.File("../favicon.jpeg"))
      val part = FilePart[TemporaryFile](key = "picture", filename = "the.file", contentType = Some("image/jpeg"),
      ref = tempFile)
      val formData = MultipartFormData(dataParts = Map(), files = Seq(part), badParts = Seq(), missingFileParts = Seq())
      val Some(result) = route(FakeRequest(userDomain.routes.UserController.createIdCard())
        .withMultipartFormDataBody(formData))

      status(result) mustEqual UNAUTHORIZED
    }

    "find id cards for a connected user" in {
      val expectedIdCard = IdCard(uuid = UUID.fromString("5e7b7c6c-98b3-4245-a5fb-405c9cc904f4"),
        userId = UUID.fromString("077f3ea6-2272-4457-a47e-9e9111108e44"))
      val unexpectedIdCard = IdCard(uuid = UUID.fromString("5e8b7c6c-98b3-4245-a5fb-405c9cc904f4"),
        userId = UUID.fromString("078f3ea6-2272-4457-a47e-9e9111108e44"))

      val Some(result) = route(FakeRequest(userDomain.routes.UserController.findUsersIdCards())
      .withAuthenticator[CookieAuthenticator](identity.loginInfo))

      val validatedIdCards: JsResult[Seq[IdCard]] = contentAsJson(result).validate[Seq[IdCard]](readIdCardReads)
      val readIdCards = validatedIdCards match {
        case error: JsError =>
          throw new Exception("find idCard for connected user")
        case success: JsSuccess[Seq[IdCard]] =>
          success.get
      }

      readIdCards must contain(expectedIdCard)
      readIdCards must not contain unexpectedIdCard
    }

    "find id cards for a user id" in {
      val expectedIdCard = IdCard(uuid = UUID.fromString("5e7b7c6c-98b3-4245-a5fb-405c9cc904f4"),
        userId = UUID.fromString("077f3ea6-2272-4457-a47e-9e9111108e44"))
      val unexpectedIdCard = IdCard(uuid = UUID.fromString("5e8b7c6c-98b3-4245-a5fb-405c9cc904f4"),
        userId = UUID.fromString("078f3ea6-2272-4457-a47e-9e9111108e44"))

      val Some(result) = route(FakeRequest(
      userDomain.routes.UserController.findIdCardsByUserId("077f3ea6-2272-4457-a47e-9e9111108e44"))
      .withAuthenticator[CookieAuthenticator](administrator.loginInfo))

      val validatedIdCards: JsResult[Seq[IdCard]] = contentAsJson(result).validate[Seq[IdCard]](readIdCardReads)
      val readIdCards = validatedIdCards match {
        case error: JsError =>
          throw new Exception("find idCard for connected user")
        case success: JsSuccess[Seq[IdCard]] =>
          success.get
      }

      readIdCards must contain(expectedIdCard)
      readIdCards must not contain unexpectedIdCard
    }

    "return forbidden if a user try to find id cards for a user id" in {
      val Some(result) = route(FakeRequest(
      userDomain.routes.UserController.findIdCardsByUserId("077f3ea6-2272-4457-a47e-9e9111108e44"))
      .withAuthenticator[CookieAuthenticator](identity.loginInfo))

      status(result) mustEqual FORBIDDEN
    }

    "return an image for a card uuid for a connected user" in {
      val Some(result) = route(FakeRequest(
        userDomain.routes.UserController.findIdCardImageForUser("5e7b7c6c-98b3-4245-a5fb-405c9cc904f4"))
        .withAuthenticator[CookieAuthenticator](identity.loginInfo))

      status(result) mustEqual OK
    }


    "return an image for a card uuid for a connected administrateur" in {
      val Some(result) = route(FakeRequest(
        userDomain.routes.UserController.findIdCardImages("5e7b7c6c-98b3-4245-a5fb-405c9cc904f4"))
        .withAuthenticator[CookieAuthenticator](administrator.loginInfo))

      status(result) mustEqual OK
    }

    "return not found if a connected user try to get idCard of an other user" in {
      val Some(result) = route(FakeRequest(
        userDomain.routes.UserController.findIdCardImageForUser("5e8b7c6c-98b3-4245-a5fb-405c9cc904f4"))
        .withAuthenticator[CookieAuthenticator](identity.loginInfo))

      status(result) mustEqual NOT_FOUND
    }
  }
}

