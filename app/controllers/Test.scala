package controllers

import java.io.{IOException, FileNotFoundException}
import java.math.MathContext
import java.text.Normalizer
import java.util.Date
import play.api.libs.iteratee.{Iteratee, Enumerator}
import play.api.libs.ws.WS
import play.api.libs.ws.Response
import play.api.mvc._
import play.libs.F.Promise
import scala.concurrent.Future
import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits._
import models.Event
import scala.io.Source
import play.api.mvc.Results._
import scala.util.{Success, Failure}
import services.Utilities._

object Test extends Controller{

  /*def test1 = WebSocket.using[String] { request =>
    // Log events to the console
    val in = Iteratee.foreach[String](println).map { _ =>
      println("Disconnecteddddd")
    }

    // Send a single 'Hello!' message
    val out = Enumerator("Hello!")

    (in, out)
  }*/

  def mock(serviceName: String) = {
    val start = System.currentTimeMillis()
    def getLatency(r: Any): Long = System.currentTimeMillis() - start
    val token = play.Play.application.configuration.getString("facebook.token")
    val soundCloudClientId = play.Play.application.configuration.getString("soundCloud.clientId")
    val echonestApiKey = play.Play.application.configuration.getString("echonest.apiKey")
    val youtubeKey = play.Play.application.configuration.getString("youtube.key")
    serviceName match {
      case "a" =>
        WS.url("http://api.soundcloud.com/users/rone-music?client_id=" +
          soundCloudClientId).get().map { response => Json.toJson("yo")}
      case "b" =>
        WS.url("https://graph.facebook.com/v2.2/search?q=" + "iam"
          + "&type=page&fields=name,cover%7Bsource%7D,id,category,link,website&access_token=" + token).get()
          .map { response => Json.toJson("sacoche!!!")}
      case _ => WS.url("https://graph.facebook.com/v2.2/search?q=" + "iam"
        + "&type=page&fields=name,cover%7Bsource%7D,id,category,link,website&access_token=" + token).get()
        .map { response => Json.toJson("???!!!")}
    }
  }


  def test1 = Action {
    //val yo = Enumerator.generateM[JsValue](Future { Option(Json.toJson("yo\n")) })
    val yo = Enumerator(Seq("a", "b", "c", "d"))
    //val tcho = Enumerator("tcho")

    //val yoTcho = yo.andThen(tcho)

    val a = Enumerator.flatten(mock("a").map { str => Enumerator(Json.toJson(Map("champ" -> str))) })
    val b = Enumerator.flatten(mock("b").map { str => Enumerator(Json.toJson(Map("champ" -> str))) })


    val body = Enumerator.interleave(b, a)

    //Ok.chunked(body)
    Ok(Json.toJson(Json.parse("""[{"name":"Asco","foreign_ids":[{"catalog":"facebook","foreign_id":"facebook:artist:311868940661"}],"urls":{"mb_url":"http://musicbrainz.org/artist/0da14f65-7415-4c3e-8233-6f194908c9bc.html","myspace_url":"http://www.myspace.com/weaponsound","wikipedia_url":"http://en.wikipedia.org/wiki/Asco","lastfm_url":"http://www.last.fm/music/Asco"},"images":[{"url":"http://userserve-ak.last.fm/serve/500/5144525/ASCO+therealstreetkids.jpg","tags":[],"width":500,"height":371,"aspect_ratio":1.3477088948787062,"verified":false,"license":{"type":"cc-by-sa","attribution":"sergiomanxera","url":"www.last.fm/user/sergiomanxera"}},{"url":"http://a1.ec-images.myspacecdn.com/profile01/140/c17a8ebc63804eccb50ce22c00e16e34/p.jpg","tags":[],"width":170,"height":256,"aspect_ratio":0.6640625,"verified":false,"license":{"type":"all-rights-reserved","attribution":"myspace","url":"http://a1.ec-images.myspacecdn.com/profile01/140/c17a8ebc63804eccb50ce22c00e16e34/p.jpg"}},{"url":"http://userserve-ak.last.fm/serve/500/78084130/ASCO+gbdfddffd.jpg","tags":[],"width":500,"height":375,"aspect_ratio":1.3333333333333333,"verified":false,"license":{"type":"cc-by-sa","attribution":"hoyparahoy","url":"www.last.fm/user/hoyparahoy"}}],"id":"AR482G311C8A42C4C0"},{"name":"A.S.C.O","foreign_ids":[{"catalog":"facebook","foreign_id":"facebook:artist:174132699276436"}],"urls":{"mb_url":"http://musicbrainz.org/artist/e1eb1cba-8eb4-4541-8bb2-d1a40141ce85.html","lastfm_url":"http://www.last.fm/music/A.S.C.O","official_url":"http://www.ascostreetpunk.tk","myspace_url":"http://www.myspace.com/ascostreetpunkanarias"},"images":[{"url":"http://userserve-ak.last.fm/serve/500/27723227/ASCO+l_6a125d36f75a4bc0bd66a6556e93.jpg","tags":[],"width":500,"height":454,"aspect_ratio":1.1013215859030836,"verified":false,"license":{"type":"cc-by-sa","attribution":"redstar73","url":"www.last.fm/user/redstar73"}},{"url":"http://a2.ec-images.myspacecdn.com/profile01/127/d6db894c8c11402a8007332f64b30147/p.jpg","tags":[],"width":170,"height":133,"aspect_ratio":1.2781954887218046,"verified":false,"license":{"type":"all-rights-reserved","attribution":"myspace","url":"http://a2.ec-images.myspacecdn.com/profile01/127/d6db894c8c11402a8007332f64b30147/p.jpg"}}],"id":"ARHLKQT11C8A416170"},{"name":"Ascoil Sun","urls":{"lastfm_url":"http://www.last.fm/music/Ascoil+Sun"},"images":[{"url":"http://userserve-ak.last.fm/serve/500/73377282/Ascoil+Sun+402119_10150472016451432_68864.jpg","tags":[],"width":500,"height":500,"aspect_ratio":1.0,"verified":false,"license":{"type":"cc-by-sa","attribution":"kirstu","url":"www.last.fm/user/kirstu"}},{"url":"http://userserve-ak.last.fm/serve/500/51518975/Ascoil+Sun+00++++Pinnacle+Of+Co.png","tags":[],"width":500,"height":500,"aspect_ratio":1.0,"verified":false,"license":{"type":"cc-by-sa","attribution":"Ascoil Sun","url":"www.last.fm/music/Ascoil+Sun"}},{"url":"http://userserve-ak.last.fm/serve/500/73377316/Ascoil+Sun+402119_10150472016451432_68864.jpg","tags":[],"width":500,"height":500,"aspect_ratio":1.0,"verified":false,"license":{"type":"cc-by-sa","attribution":"Ascoil Sun","url":"www.last.fm/music/Ascoil+Sun"}}],"id":"ARFRQTJ135799520F2"},{"name":"Ascorbite","urls":{"lastfm_url":"http://www.last.fm/music/Ascorbite"},"images":[],"id":"ARNGWZP1471E2E194F"},{"name":"Ascoltare","foreign_ids":[{"catalog":"facebook","foreign_id":"facebook:artist:150470441683556"}],"urls":{"mb_url":"http://musicbrainz.org/artist/43c50d76-cbf9-4ce1-8558-aad466995468.html","lastfm_url":"http://www.last.fm/music/Ascoltare","official_url":"http://ascoltare.co.uk/","myspace_url":"http://www.myspace.com/ascoltare"},"images":[{"url":"http://userserve-ak.last.fm/serve/_/38951985/Ascoltare+Wire+Tapper+12.jpg","tags":[],"width":160,"height":160,"aspect_ratio":1.0,"verified":false,"license":{"type":"cc-by-sa","attribution":"Sneakingtrain","url":"www.last.fm/user/Sneakingtrain"}},{"url":"http://a3.ec-images.myspacecdn.com/profile01/136/fd1c7117a75547bca3550683d66444f8/p.gif","tags":[],"width":170,"height":46,"aspect_ratio":3.6956521739130435,"verified":false,"license":{"type":"all-rights-reserved","attribution":"myspace","url":"http://a3.ec-images.myspacecdn.com/profile01/136/fd1c7117a75547bca3550683d66444f8/p.gif"}}],"id":"AR48CNL1187B992C06"},{"name":"Ascona","foreign_ids":[{"catalog":"facebook","foreign_id":"facebook:artist:193775967062"}],"urls":{"lastfm_url":"http://www.last.fm/music/Ascona","official_url":"http://www.thisisascona.com","wikipedia_url":"http://en.wikipedia.org/wiki/Ascona","myspace_url":"http://www.myspace.com/asconamusic"},"images":[{"url":"http://userserve-ak.last.fm/serve/500/17451315/Ascona.jpg","tags":[],"verified":false,"license":{"type":"unknown","attribution":"n/a","url":"http://www.last.fm/music/Ascona/+images"}},{"url":"http://a4.ec-images.myspacecdn.com/profile01/141/66d7e367fc724f3691eae2ea68dd1649/p.jpg","tags":[],"width":170,"height":235,"aspect_ratio":0.723404255319149,"verified":false,"license":{"type":"all-rights-reserved","attribution":"myspace","url":"http://a4.ec-images.myspacecdn.com/profile01/141/66d7e367fc724f3691eae2ea68dd1649/p.jpg"}},{"url":"http://upload.wikimedia.org/wikipedia/commons/2/29/Lago.maggiore.jpg","tags":[],"width":640,"height":480,"aspect_ratio":1.3333333333333333,"verified":false,"license":{"type":"cc-by-sa","attribution":"Crux","url":"http://upload.wikimedia.org/wikipedia/commons/2/29/Lago.maggiore.jpg"}},{"url":"http://upload.wikimedia.org/wikipedia/commons/b/bd/Lago_Maggiore-Mappa.png","tags":[],"width":590,"height":941,"aspect_ratio":0.6269925611052072,"verified":false,"license":{"type":"cc-by-sa","attribution":"Ian Spackman","url":"http://upload.wikimedia.org/wikipedia/commons/b/bd/Lago_Maggiore-Mappa.png"}},{"url":"http://upload.wikimedia.org/wikipedia/commons/a/ae/Ascona2008_img_6658.jpg","tags":[],"width":2048,"height":1536,"aspect_ratio":1.3333333333333333,"verified":false,"license":{"type":"cc-by-sa","attribution":"BotMultichill","url":"http://upload.wikimedia.org/wikipedia/commons/a/ae/Ascona2008_img_6658.jpg"}},{"url":"http://upload.wikimedia.org/wikipedia/commons/f/fb/Ascona_Albergo_Hotel.jpg","tags":[],"width":1600,"height":1200,"aspect_ratio":1.3333333333333333,"verified":false,"license":{"type":"cc-by-sa","attribution":"Flickr upload bot","url":"http://upload.wikimedia.org/wikipedia/commons/f/fb/Ascona_Albergo_Hotel.jpg"}},{"url":"http://upload.wikimedia.org/wikipedia/commons/4/40/Kirche-in-ascona.jpg","tags":[],"width":600,"height":800,"aspect_ratio":0.75,"verified":false,"license":{"type":"cc-by-sa","attribution":"Idéfix","url":"http://upload.wikimedia.org/wikipedia/commons/4/40/Kirche-in-ascona.jpg"}},{"url":"http://upload.wikimedia.org/wikipedia/commons/7/77/Hotel_monte_verit%C3%A0.jpg","tags":[],"width":2816,"height":2112,"aspect_ratio":1.3333333333333333,"verified":false,"license":{"type":"cc-by-sa","attribution":"LittleJoe","url":"http://upload.wikimedia.org/wikipedia/commons/7/77/Hotel_monte_verit%C3%A0.jpg"}},{"url":"http://upload.wikimedia.org/wikipedia/commons/9/99/Tessin_05_2006_226.jpg","tags":[],"width":1280,"height":960,"aspect_ratio":1.3333333333333333,"verified":false,"license":{"type":"cc-by","attribution":"Flickr upload bot","url":"http://upload.wikimedia.org/wikipedia/commons/9/99/Tessin_05_2006_226.jpg"}},{"url":"http://upload.wikimedia.org/wikipedia/commons/c/ce/Ascona.lago.jpg","tags":[],"width":900,"height":356,"aspect_ratio":2.5280898876404496,"verified":false,"license":{"type":"cc-by-sa","attribution":"File Upload Bot (Magnus Manske)","url":"http://upload.wikimedia.org/wikipedia/commons/c/ce/Ascona.lago.jpg"}},{"url":"http://upload.wikimedia.org/wikipedia/commons/e/e9/Panorama_ascona.jpg","tags":[],"width":6564,"height":1401,"aspect_ratio":4.685224839400428,"verified":false,"license":{"type":"cc-by-sa","attribution":"BotMultichill","url":"http://upload.wikimedia.org/wikipedia/commons/e/e9/Panorama_ascona.jpg"}},{"url":"http://upload.wikimedia.org/wikipedia/en/4/45/Ascona-saeed.jpg","tags":[],"width":640,"height":480,"aspect_ratio":1.3333333333333333,"verified":false,"license":{"type":"cc-by-sa","attribution":"Wikipedic  (talk | contribs)","url":"http://upload.wikimedia.org/wikipedia/en/4/45/Ascona-saeed.jpg"}},{"url":"http://upload.wikimedia.org/wikipedia/commons/f/fc/Tessin_Mai_2007_042.jpg","tags":[],"width":1600,"height":1200,"aspect_ratio":1.3333333333333333,"verified":false,"license":{"type":"cc-by","attribution":"Tobyc75","url":"http://upload.wikimedia.org/wikipedia/commons/f/fc/Tessin_Mai_2007_042.jpg"}},{"url":"http://upload.wikimedia.org/wikipedia/commons/e/ee/13_Ascona%2C_Ticino.jpg","tags":[],"width":515,"height":355,"aspect_ratio":1.4507042253521127,"verified":false,"license":{"type":"cc-by-sa","attribution":"Ras67","url":"http://upload.wikimedia.org/wikipedia/commons/e/ee/13_Ascona%2C_Ticino.jpg"}},{"url":"http://upload.wikimedia.org/wikipedia/commons/5/53/Karte_Gemeinde_Ascona_2013.png","tags":[],"width":1476,"height":1307,"aspect_ratio":1.1293037490436113,"verified":false,"license":{"type":"cc-by-sa","attribution":"Tschubby","url":"http://upload.wikimedia.org/wikipedia/commons/5/53/Karte_Gemeinde_Ascona_2013.png"}}],"id":"ARELOKF124207805D6"},{"name":"Ascon Bates","foreign_ids":[{"catalog":"facebook","foreign_id":"facebook:artist:153999024654394"}],"urls":{"myspace_url":"http://www.myspace.com/asconbates","lastfm_url":"http://www.last.fm/music/Ascon+Bates"},"images":[{"url":"http://a1.ec-images.myspacecdn.com/profile01/126/8954e865220a429f96c311db6f3803b7/p.jpg","tags":[],"width":170,"height":127,"aspect_ratio":1.3385826771653544,"verified":false,"license":{"type":"all-rights-reserved","attribution":"myspace","url":"http://a1.ec-images.myspacecdn.com/profile01/126/8954e865220a429f96c311db6f3803b7/p.jpg"}}],"id":"ARDDJUP12B3B35514F"},{"name":"Ascoy","foreign_ids":[{"catalog":"facebook","foreign_id":"facebook:artist:112612448750979"}],"urls":{"official_url":"http://www.celam.org/publicaciones/ficha.php?id=253","lastfm_url":"http://www.last.fm/music/Ascoy"},"images":[],"id":"ARUTYDA12F1299C3A8"},{"name":"Ascona 400","urls":{"lastfm_url":"http://www.last.fm/music/Ascona+400"},"images":[],"id":"ARVFYQE13F29CD6F82"},{"name":"Ascottpres","urls":{"lastfm_url":"http://www.last.fm/music/Ascottpres"},"images":[],"id":"AREDUHF133B3F12553"},{"name":"Ascolta !","urls":{"lastfm_url":"http://www.last.fm/music/Ascolta+%21"},"images":[],"id":"ARKSIWC139111DE82B"},{"name":"Ascolto","foreign_ids":[{"catalog":"facebook","foreign_id":"facebook:artist:113796025298974"}],"urls":{"mb_url":"http://musicbrainz.org/artist/671a6dde-ba7a-4ba5-882f-973ca13880c7.html","lastfm_url":"http://www.last.fm/music/Ascolto"},"images":[],"id":"ARYH68E1187B9B5D74"},{"name":"Ascott & Garry Todd","urls":{"lastfm_url":"http://www.last.fm/music/+noredirect/Ascott+%26+Garry+Todd"},"images":[],"id":"ARCQVPD138B9889476"},{"name":"Ascolta la tua radio. N. 2","urls":{"lastfm_url":"http://www.last.fm/music/Ascolta+la+tua+radio.+N.+2"},"images":[],"id":"ARCYEFS1385DE93CA0"},{"name":"Ascoy & Limeñita","urls":{},"images":[],"id":"ARGDFXJ136F6F0C0A2"}]""")))
  }
}
