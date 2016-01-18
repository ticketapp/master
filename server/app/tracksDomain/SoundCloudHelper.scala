package tracksDomain

import play.api.libs.json._
import play.api.libs.ws.WSResponse

import scala.collection.immutable.Seq


trait SoundCloudHelper {
  def removeUselessInSoundCloudWebsite(website: String): String = website match {
    case soundCloudWebsite if soundCloudWebsite contains "soundcloud" =>
      if (soundCloudWebsite.count(_ == '/') > 1)
        soundCloudWebsite.take(soundCloudWebsite.lastIndexOf('/'))
      else
        soundCloudWebsite
    case _ => website
  }

  def readSoundCloudWebsites(soundCloudResponse: WSResponse): Vector[String] = {
    val readSoundCloudUrls = Reads.seq((__ \ "url").read[String])
    soundCloudResponse.json
      .asOpt[scala.Seq[String]](readSoundCloudUrls)
      .getOrElse(Seq.empty)
      .toVector
  }
}
