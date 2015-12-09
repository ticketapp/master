package organizersDomain

import addresses.{Address, AddressFormsTrait}
import play.api.data.Form
import play.api.data.Forms._


trait organizerFormsTrait extends AddressFormsTrait {

  val organizerBindingForm = Form(
    mapping(
      "facebookId" -> optional(nonEmptyText(2)),
      "name" -> nonEmptyText(2),
      "description" -> optional(nonEmptyText(2)),
      "websites" -> optional(nonEmptyText(4)),
      "imagePath" -> optional(nonEmptyText(2)),
      "address" -> mapping (
        "city" -> optional(text(2)),
        "zip" -> optional(text(2)),
        "street" -> optional(text(2))
      )(addressFormApply)(addressFormUnapply)
    )(organizerFormApply)(organizerFormUnapply))

  def organizerFormApply(facebookId: Option[String], name: String, description: Option[String], websites: Option[String],
                imagePath: Option[String], address: Option[Address]): OrganizerWithAddress =
    OrganizerWithAddress(
      Organizer(None, facebookId, name, description = description, websites = websites, imagePath = imagePath), address)

  def organizerFormUnapply(organizerWithAddress: OrganizerWithAddress) =
    Some((organizerWithAddress.organizer.facebookId, organizerWithAddress.organizer.name,
      organizerWithAddress.organizer.description, organizerWithAddress.organizer.websites,
      organizerWithAddress.organizer.imagePath, organizerWithAddress.address))
}
