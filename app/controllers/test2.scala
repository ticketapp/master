package controllers

import java.io.{IOException, FileNotFoundException}
import java.util.Date
import play.api.data._
import play.api.data.Forms._
import play.api.libs.ws.WS
import play.api.libs.ws.Response
import play.api.mvc._
import scala.concurrent.Future
import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits._
import models.{Place, Event}
import scala.io.Source
import play.api.mvc.Results._
import scala.util.{Failure, Success, Try}
import play.api.libs.functional.syntax._

object Test2 extends Controller {

  val json = Json.parse("""[{
"created_at":"Sat, 11 Feb 2012 06:38:28 +0000",
"entities":{
"hashtags":[
{
"text": "Shubhdin",
"indices": [
9,
18
]
}
],
"urls": [],
"user_mentions": [
{
"screen_name": "SAMdLaw",
"name": "Sabyasachi Mohapatra",
"id": 104420398,
"id_str": "104420398",
"indices": [
0,
8
]
}
]
},
"from_user": "nilayshah80",
"from_user_id": 213599118,
"from_user_id_str": "213599118",
"from_user_name": "Nilay Shah",
"geo": {
"coordinates": [
18.6003,
73.825
],
"type": "Point"
},
"id": 168222351106899968,
"id_str": "168222351106899968",
"iso_language_code": "in",
"metadata": {
"result_type": "recent"
},
"profile_image_url": "http://a2.twimg.com/profile_images/1528184590/IMG_0465_normal.JPG",
"profile_image_url_https": "https://si0.twimg.com/profile_images/1528184590/IMG_0465_normal.JPG",
"source": "&lt;a href=&quot;http://twabbit.wordpress.com/&quot; rel=&quot;nofollow&quot;&gt;twabbit&lt;/a&gt;",
"text": "@SAMdLaw #Shubhdin mitra",
"to_user": "SAMdLaw",
"to_user_id": 104420398,
"to_user_id_str": "104420398",
"to_user_name": "Sabyasachi Mohapatra",
"in_reply_to_status_id": 168219865197461505,
"in_reply_to_status_id_str": "168219865197461505"
},
{
"created_at": "Sun, 12 Feb 2012 01:54:07 +0000",
"entities": {
"hashtags": [
{
"text": "IWIllAlwaysLoveYou",
"indices": [
88,
107
]
}
],
"urls": [],
"user_mentions": [],
"media": [
{
"id": 168513175187238912,
"id_str": "168513175187238912",
"indices": [
108,
128
],
"media_url": "http://p.twimg.com/Alat1wsCMAAh-wE.jpg",
"media_url_https": "https://p.twimg.com/Alat1wsCMAAh-wE.jpg",
"url": "http://t.co/dRc4dXH3",
"display_url": "pic.twitter.com/dRc4dXH3",
"expanded_url": "http://twitter.com/RIPWhitneyH/status/168513175183044608/photo/1",
"type": "photo",
"sizes": {
"orig": {
"w": 395,
"h": 594,
"resize": "fit"
},
"large": {
"w": 395,
"h": 594,
"resize": "fit"
},
"thumb": {
"w": 150,
"h": 150,
"resize": "crop"
},
"small": {
"w": 340,
"h": 511,
"resize": "fit"
},
"medium": {
"w": 395,
"h": 594,
"resize": "fit"
}
}
}
]
},
"from_user": "RIPWhitneyH",
"from_user_id": 19319043,
"from_user_id_str": "19319043",
"from_user_name": "RIP Whitney Houston",
"geo": null,
"id": 168513175183044608,
"id_str": "168513175183044608",
"iso_language_code": "en",
"metadata": {
"recent_retweets": 8,
"result_type": "popular"
},
"profile_image_url": "http://a2.twimg.com/profile_images/1820957590/images__13__normal.jpg",
"profile_image_url_https": "https://si0.twimg.com/profile_images/1820957590/images__13__normal.jpg",
"source": "&lt;a href=&quot;http://twitter.com/&quot;&gt;web&lt;/a&gt;",
"text": "R-T if you think that the Grammy's should organize an \"R.I.P. Whitney Houston\" tribute. #IWIllAlwaysLoveYou http://t.co/dRc4dXH3",
"to_user": null,
"to_user_id": null,
"to_user_id_str": null,
"to_user_name": null
}]""")


  val readMediaUrls: Reads[Seq[String]] = {
    Reads.seq((__ \ "media_url").read[String])
  }

  val readOptionalEntities: Reads[Option[Seq[String]]] = {
    (__ \ "entities" \ "media").readNullable(readMediaUrls)
  }

  // 2: Taking only one url from these

  val readOnlyOneUrl: Reads[Option[String]] =
    readOptionalEntities.map { case maybeUrls =>
      maybeUrls.toList.flatten.headOption
    }

  // 3: Read the tweet Id

  val readId: Reads[String] = (__ \ "id_str").read[String]

  // 4: Read a list of combining id with media url

  val readIdAndUrl: Reads[(String, Option[String])] = {
    readId.and(readOnlyOneUrl).apply((id, maybeUrl) => (id, maybeUrl))
  }

  val readAnArray: Reads[Seq[(String, Option[String])]] = {
    Reads.seq(readIdAndUrl)
  }

  // 5: Collecting only tweets with media

  val collectOnlyTweetsWithUrl: Reads[Seq[(String, String)]] = {
    readAnArray.map { tweets =>
      tweets.collect{ case (id, Some(url)) => (id, url) }
    }
  }

  // 6: Read tweets

  case class Tweet(id: String, image: String)
  case class Category(id: String)

  val readTweets: Reads[Seq[Tweet]] = {
    collectOnlyTweetsWithUrl.map { tweets =>
      tweets.map{ case (id, url) => Tweet(id, url) }
    }
  }

  // 7. Use the Reads classes to actually parse some JSON

  val token = play.Play.application.configuration.getString("facebook.token")

  def test2(pattern: String) = Action {
    val js = WS.url("https://graph.facebook.com/v2.2/search?q=" + pattern
      + "&limit=30&type=page&fields=name,cover,id,category,likes,link,website&access_token=" + token).get.map {
      response => println(response.json)
    }

    val value = Json.parse("""[{"name":"Azerbaïdjan","id":"109765775708835","category":"Musician/band","likes":80106,"link":"https://www.facebook.com/pages/Azerba%C3%AFdjan/109765775708835"}]""")

    //1: read the category and the id
    val readCategory: Reads[String] = (__ \ "category").read[String]

    //2: read the id
    val readId: Reads[String] = (__ \ "id").read[String]

    /*val readOptionalCover: Reads[Option[Seq[String]]] = {
      (__ \ "cover").readNullable(readMediaUrls)
    }*/

    val readIdAndCategory: Reads[(String, String)] =
      readId.and(readCategory).apply((id: String, category: String) => (id, category))

    val readAnArray: Reads[Seq[(String, String)]] = Reads.seq(readIdAndCategory)

    val collectOnlyMusicians: Reads[Seq[(String)]] = {
      readAnArray.map { pages =>
        pages.collect{ case (id, "Musician/band") => id }
      }
    }

    val readArtists: Reads[Seq[Category]] = {
      collectOnlyMusicians.map { artists =>
        artists.map{ case id => Category(id) }
      }
    }

    val artists = value.as[Seq[Category]](readArtists)

    val tweets = json.as[Seq[Tweet]](readTweets)
    Ok("Got tweets: " + artists)
  }

/*
  def test2 = Action {
    Ok("Okay\n")
  }*/
}

