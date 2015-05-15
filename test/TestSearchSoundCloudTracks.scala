import scala.collection.mutable
import org.scalatestplus.play._
import play.api.libs.json.{JsValue, Json}

class TestSearchSoundCloudTracks extends PlaySpec {

  "A Stack" must {
    "pop values in last-in-first-out order" in {
      val stack = new mutable.Stack[Int]
      stack.push(1)
      stack.push(2)
      stack.pop() mustBe 2
      stack.pop() mustBe 1
    }

    "throw NoSuchElementException if an empty stack is popped" in {
      val emptyStack = new mutable.Stack[Int]
      a [NoSuchElementException] must be thrownBy {
        emptyStack.pop()
      }
    }
  }
}


//val json: JsValue = Json.parse(
//"""{"kind":"youtube#searchListResponse","etag":"\"9iWEWaGPvvCMMVNTPHF9GiusHJA/-emRO9O4bi4aZjfLEAFP9IPRtKU\"","nextPageToken":"CAUQAA","pageInfo":{"totalResults":119,"resultsPerPage":5},"items":[{"kind":"youtube#searchResult","etag":"\"9iWEWaGPvvCMMVNTPHF9GiusHJA/7gpuGRBiIksChmCDg0KUO3QCPLc\"","id":{"kind":"youtube#video","videoId":"uSrEH17Z8Ww"},"snippet":{"publishedAt":"2008-11-15T12:53:41.000Z","channelId":"UCh0H23k4B9WjJ9eAopmoQeQ","title":"Manu Chao - Trapped By Love - Le Rendez Vous","description":"From \"Próxima Estación: Esperanza\"","thumbnails":{"default":{"url":"https://i.ytimg.com/vi/uSrEH17Z8Ww/default.jpg"},"medium":{"url":"https://i.ytimg.com/vi/uSrEH17Z8Ww/mqdefault.jpg"},"high":{"url":"https://i.ytimg.com/vi/uSrEH17Z8Ww/hqdefault.jpg"}},"channelTitle":"mzapa","liveBroadcastContent":"none"}},{"kind":"youtube#searchResult","etag":"\"9iWEWaGPvvCMMVNTPHF9GiusHJA/TW7opfrmg3f3AkAtwmefBEHEjUM\"","id":{"kind":"youtube#video","videoId":"om3Meryuzq0"},"snippet":{"publishedAt":"2013-02-19T14:44:39.000Z","channelId":"UCzGpxRavGnm6yym4Pt7B2sg","title":"Manu Chao - Le Rendez-Vous","description":"Manu Chao - Le Rendez-Vous, from the album \"Próxima Estación: Esperanza\" Click here http://po.st/ManuChaoYt and subscribe to Manu Chao's official channel ...","thumbnails":{"default":{"url":"https://i.ytimg.com/vi/om3Meryuzq0/default.jpg"},"medium":{"url":"https://i.ytimg.com/vi/om3Meryuzq0/mqdefault.jpg"},"high":{"url":"https://i.ytimg.com/vi/om3Meryuzq0/hqdefault.jpg"}},"channelTitle":"manuchao","liveBroadcastContent":"none"}},{"kind":"youtube#searchResult","etag":"\"9iWEWaGPvvCMMVNTPHF9GiusHJA/A1SgWJSrGhQbmbqUuKUPlji-g3Q\"","id":{"kind":"youtube#video","videoId":"z1glKwXXai4"},"snippet":{"publishedAt":"2012-09-16T18:51:41.000Z","channelId":"UCHqvT_Dno6q1q0tPPhuEJRg","title":"Manu Chao - Le rendez-vous","description":"Dale un me gusta! Y suscribiste por favor para que me den más animo de seguir aportando así :), canción interpretada por el grupo manu chao, en su disco: \"Pr.","thumbnails":{"default":{"url":"https://i.ytimg.com/vi/z1glKwXXai4/default.jpg"},"medium":{"url":"https://i.ytimg.com/vi/z1glKwXXai4/mqdefault.jpg"},"high":{"url":"https://i.ytimg.com/vi/z1glKwXXai4/hqdefault.jpg"}},"channelTitle":"","liveBroadcastContent":"none"}},{"kind":"youtube#searchResult","etag":"\"9iWEWaGPvvCMMVNTPHF9GiusHJA/Cu-eh9uhkaLg3PRoIg_wey2Wq4M\"","id":{"kind":"youtube#video","videoId":"cDF2kWb3ddo"},"snippet":{"publishedAt":"2014-07-21T03:44:08.000Z","channelId":"UCwB-dWM3wwuvrezjx4aHAag","title":"Manu Chao - Trapped By Love / Le Rendez Vous","description":"Manu Chao - Trapped By Love / Le Rendez Vous Fotos de Manu Chao e de quando estive com ele! Uma honra! Em Recife-PE - Brasil, 2011. \"A Mano Negra é ...","thumbnails":{"default":{"url":"https://i.ytimg.com/vi/cDF2kWb3ddo/default.jpg"},"medium":{"url":"https://i.ytimg.com/vi/cDF2kWb3ddo/mqdefault.jpg"},"high":{"url":"https://i.ytimg.com/vi/cDF2kWb3ddo/hqdefault.jpg"}},"channelTitle":"","liveBroadcastContent":"none"}},{"kind":"youtube#searchResult","etag":"\"9iWEWaGPvvCMMVNTPHF9GiusHJA/WFxPS9Xa9HLISwX4S0DQv0R4iQY\"","id":{"kind":"youtube#video","videoId":"KaxARajCrfs"},"snippet":{"publishedAt":"2015-02-14T20:32:26.000Z","channelId":"UCZqaVXoIyjCcC6DBG2R_v1g","title":"Trapped by Love - Le rendez vous - Manu Chao Subtitulada","description":"Todo lo mostrado tiene sus respectivos dueños y sus respectivos derechos reservados. El video presente no tiene intención de apropiarse de ellos. Un día de p.","thumbnails":{"default":{"url":"https://i.ytimg.com/vi/KaxARajCrfs/default.jpg"},"medium":{"url":"https://i.ytimg.com/vi/KaxARajCrfs/mqdefault.jpg"},"high":{"url":"https://i.ytimg.com/vi/KaxARajCrfs/hqdefault.jpg"}},"channelTitle":"VespidaeLamar","liveBroadcastContent":"none"}}]}""")

/*val artist = Artist(
  artistId = 5l,
  facebookId = Some("facebook id"),
  name = "name",
  description = Some("description"),
  facebookUrl = "facebook url",
  websites = Set("website"),
  images = Set.empty,
  genres = Set.empty,
  tracks = Seq.empty
)*/
//val result = SearchSoundCloudTracks.readSoundCloudTracks(json, artist)