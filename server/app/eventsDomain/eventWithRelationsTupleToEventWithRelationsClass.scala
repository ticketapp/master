package eventsDomain

import addresses.Address
import artistsDomain.{Artist, ArtistWithWeightedGenres}
import database._
import genresDomain.Genre
import organizersDomain.{Organizer, OrganizerWithAddress}
import placesDomain.{Place, PlaceWithAddress}

trait eventWithRelationsTupleToEventWithRelationsClass {
  def eventWithRelationsTupleToEventWithRelationClass(eventWithRelations: Seq[((Event, Option[(EventOrganizerRelation, Organizer)]),
    Option[(EventArtistRelation, Artist)], Option[(EventPlaceRelation, Place)], Option[(EventGenreRelation, Genre)],
    Option[(EventAddressRelation, Address)])])
  : Vector[EventWithRelations] = {
    val groupedByEvents = eventWithRelations.groupBy(_._1._1)

    groupedByEvents.map { eventWithOptionalRelations =>
      val event = eventWithOptionalRelations._1
      val relations = eventWithOptionalRelations._2
      val organizers = (relations collect {
        case ((_, Some((_, organizer: Organizer))), _, _, _, _) => organizer
      }).distinct
      val artists = (relations collect {
        case ((_, _), Some((_, artist: Artist)), _, _, _) => artist
      }).distinct
      val places = (relations collect {
        case ((_, _), _, Some((_, place: Place)), _, _) => place
      }).distinct
      val genres = (relations collect {
        case ((_, _), _, _, Some((_, genre: Genre)), _) => genre
      }).distinct
      val addresses = (relations collect {
        case ((_, _), _, _, _, Some((_, address: Address))) => address
      }).distinct

      EventWithRelations(
        event,
        organizers map (OrganizerWithAddress(_)),
        artists map (ArtistWithWeightedGenres(_)),
        places map (PlaceWithAddress(_)),
        genres,
        addresses)
    }.toVector
  }
}
