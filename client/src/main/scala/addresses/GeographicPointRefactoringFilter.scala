package addresses

import com.greencatsoft.angularjs.{Filter, injectable}

@injectable("geographicPointRefactoringFilter")
class GeographicPointRefactoringFilter extends Filter[String] {

  override def filter(geographicPoint: String): String =
    geographicPoint.stripPrefix("POINT (").stripSuffix(")").replace(" ", ",")
}
