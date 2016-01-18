package services

import com.vividsolutions.jts.geom.Geometry

abstract class SortableByGeographicPoint { val geographicPoint: Geometry }


trait SortByDistanceToPoint {

  def sortByDistanceToPoint[T <: SortableByGeographicPoint](geographicPoint: Geometry,
                                                            sortableByGeographicPoint: Seq[T]): Seq[T] = {

    val objectsWihRelationsSortedByGeoPoint = sortableByGeographicPoint
      .sortBy(element => geographicPoint.distance(element.geographicPoint))

    objectsWihRelationsSortedByGeoPoint
  }
}
