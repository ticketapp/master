package application

import java.io.File
import javax.inject.Inject

import play.api.db.slick.DatabaseConfigProvider
import play.api.mvc._

class Admin @Inject()(dbConfigProvider: DatabaseConfigProvider) extends Controller {

}
