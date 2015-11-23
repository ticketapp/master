package addresses

import javax.inject.Inject

import models._
import play.api.db.slick.DatabaseConfigProvider
import play.api.mvc._
import services.Utilities

class AddressController @Inject()(dbConfigProvider: DatabaseConfigProvider,
                      val addressMethods: AddressMethods,
                      val utilities: Utilities) extends Controller {

}
