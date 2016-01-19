package Contact

import com.greencatsoft.angularjs.core.HttpService
import com.greencatsoft.angularjs.{Factory, Service, injectable}

@injectable("contactService")
class ContactService(http: HttpService) extends Service {
  require(http != null, "Missing argument 'http'.")

}
@injectable("contactService")
class ContactServiceFactory(http: HttpService) extends Factory[ContactService] {

  override def apply(): ContactService = new ContactService(http)
}
