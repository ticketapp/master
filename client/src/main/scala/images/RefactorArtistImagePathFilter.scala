package images
import com.greencatsoft.angularjs.{Filter, injectable}

@injectable("refactorArtistImagePathFilter")
class RefactorArtistImagePathFilter extends Filter[String] {

  override def filter(imagePath: String): String = imagePath.dropRight("""\x\x""".length)
}
