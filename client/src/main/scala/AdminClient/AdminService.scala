package AdminClient

import com.greencatsoft.angularjs.core.HttpService
import com.greencatsoft.angularjs.{Factory, Service, injectable}

@injectable("adminService")
class AdminService(http: HttpService) extends Service {
  require(http != null, "Missing argument 'http'.")

}
@injectable("adminServiceFactory")
class AdminServiceFactory(http: HttpService) extends Factory[AdminService] {

  override def apply(): AdminService = new AdminService(http)
}
