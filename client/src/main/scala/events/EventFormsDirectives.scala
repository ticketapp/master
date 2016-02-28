package events

import com.greencatsoft.angularjs.{ElementDirective, TemplatedDirective, injectable}

import scala.scalajs.js.annotation.JSExport

@JSExport
@injectable("eventFormFindById")
class EventFormFindByIdDirective() extends ElementDirective with TemplatedDirective {
  override val templateUrl = "assets/templates/events/eventFormFindById.html"
}

@JSExport
@injectable("eventFormFindByGeoPoint")
class EventFormFindByGeoPoint() extends ElementDirective with TemplatedDirective {
  override val templateUrl = "assets/templates/events/eventFormFindByGeoPoint.html"
}

@JSExport
@injectable("eventFormFindInHourInterval")
class EventFormFindInHourInterval() extends ElementDirective with TemplatedDirective {
  override val templateUrl = "assets/templates/events/eventFormFindInHourInterval.html"
}

@JSExport
@injectable("eventFormFindPassedInInterval")
class EventFormFindPassedInInterval() extends ElementDirective with TemplatedDirective {
  override val templateUrl = "assets/templates/events/eventFormFindPassedInInterval.html"
}

@JSExport
@injectable("eventFormFindAllContaining")
class EventFormFindAllContaining() extends ElementDirective with TemplatedDirective {
  override val templateUrl = "assets/templates/events/eventFormFindAllContaining.html"
}

@JSExport
@injectable("eventFormFindByCityPattern")
class EventFormFindByCityPattern() extends ElementDirective with TemplatedDirective {
  override val templateUrl = "assets/templates/events/eventFormFindByCityPattern.html"
}

@JSExport
@injectable("eventFormFindNearCity")
class EventFormFindNearCity() extends ElementDirective with TemplatedDirective {
  override val templateUrl = "assets/templates/events/eventFormFindNearCity.html"
}

@JSExport
@injectable("eventFormCreateByFacebookId")
class EventFormCreateByFacebookId() extends ElementDirective with TemplatedDirective {
  override val templateUrl = "assets/templates/events/eventFormCreateByFacebookId.html"
}