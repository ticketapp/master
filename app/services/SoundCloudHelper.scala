package services

import play.api.libs.json._
import play.api.libs.ws.WSResponse

trait SoundCloudHelper {
  def removeUselessInSoundCloudWebsite(website: String): String = website match {
    case soundCloudWebsite if soundCloudWebsite contains "soundcloud" =>
      if (soundCloudWebsite.count(_ == '/') > 1)
        soundCloudWebsite.take(soundCloudWebsite.lastIndexOf('/'))
      else
        soundCloudWebsite
    case _ => website
  }

  def readSoundCloudWebsites(soundCloudResponse: WSResponse): Seq[String] = {
    val readSoundCloudUrls: Reads[Seq[String]] = Reads.seq((__ \ "url").read[String])
    soundCloudResponse.json
      .asOpt[Seq[String]](readSoundCloudUrls)
      .getOrElse(Seq.empty)
  }
}
