package models

import java.util.UUID

case class Playlist(playlistId: Option[Long], userId: UUID, name: String, tracks: Seq[Track])

object Playlist {

  case class TrackUUIDAndRank(UUID: UUID, rank: BigDecimal)
  def idAndRankFormApply(stringUUID: String, rank: BigDecimal) = TrackUUIDAndRank(UUID.fromString(stringUUID), rank)
  def idAndRankFormUnapply(trackIdAndRank: TrackUUIDAndRank) = Option((trackIdAndRank.UUID.toString, trackIdAndRank.rank))

  case class PlaylistNameTracksIdAndRank(name: String, tracksIdAndRank: Seq[TrackUUIDAndRank])
  def formApply(name: String, tracksIdAndRank: Seq[TrackUUIDAndRank]) =
    PlaylistNameTracksIdAndRank(name, tracksIdAndRank)
  def formUnapply(playlistNameAndTracksId: PlaylistNameTracksIdAndRank) =
    Option((playlistNameAndTracksId.name, playlistNameAndTracksId.tracksIdAndRank))

/*
  def find(playlistId: Long): Option[Playlist] = try {
    DB.withConnection { implicit connection =>
      SQL(
        """SELECT * FROM playlists
          | WHERE playlistId = {playlistId}""".stripMargin)
        .on('playlistId -> playlistId)
        .as(playlistParser.singleOpt)
        .map(playlist => playlist.copy(tracks = Track.findByPlaylistId(playlist.playlistId)))
    }
  } catch {
    case e: Exception => throw new DAOException("Playlist.findByUserId: " + e.getMessage)
  }

  def findByUserId(userUUID: UUID): Seq[Playlist] = try {
    DB.withConnection { implicit connection =>
      SQL(
        """SELECT * FROM playlists
          | WHERE userId = {userId}""".stripMargin)
      .on('userId -> userUUID)
      .as(playlistParser.*)
      .map(playlist => playlist.copy(tracks = Track.findByPlaylistId(playlist.playlistId)))
    }
  } catch {
    case e: Exception => throw new DAOException("Playlist.findByUserId: " + e.getMessage)
  }

  def delete(userId: UUID, playlistId: Long): Try[Int] = Try {
    deleteTracksRelations(userId, playlistId) match {
      case Success(_) =>
        DB.withConnection { implicit connection =>
          SQL(
            """DELETE FROM playlists
              |  WHERE userId = {userId}
              |  AND playlistId = {playlistId}""".stripMargin)
            .on(
              'userId -> userId,
              'playlistId -> playlistId)
            .executeUpdate()
        }
      case Failure(exception) =>
        Logger.error("Playlist.delete", exception)
        throw exception
    }
  }

  def deleteTracksRelations(userId: UUID, playlistId: Long): Try[Int] = Try {
    DB.withConnection { implicit connection =>
      SQL(
        """DELETE FROM playlistsTracks
          |  WHERE playlistId = {playlistId}""".stripMargin)
        .on(
          'userId -> userId,
          'playlistId -> playlistId)
        .executeUpdate()
    }
  }

  def save(playlist: Playlist): Try[Option[Long]] = Try {
    DB.withConnection { implicit connection =>
      SQL("""INSERT INTO playlists(userId, name) VALUES({userId}, {name})""")
        .on(
          'userId -> playlist.userId,
          'name -> playlist.name)
        .executeInsert()
    }
  }

  def saveWithTrackRelation(userId: UUID, playlistNameTracksIdAndRank: PlaylistNameTracksIdAndRank): Long = {
    save(Playlist(None, userId, playlistNameTracksIdAndRank.name, Seq.empty)) match {
      case Success(Some(playlistId: Long)) =>
        playlistNameTracksIdAndRank.tracksIdAndRank.foreach(trackIdAndRank =>
          saveTrackRelation(playlistId, trackIdAndRank))
        playlistId
      case _ =>
        throw new DAOException("Playlist.saveWithTrackRelation")
      }
  }

  def saveTrackRelation(playlistId: Long, trackIdAndRank: TrackUUIDAndRank): Option[Long] = try {
    DB.withConnection { implicit connection =>
      SQL(
        """INSERT INTO playlistsTracks (playlistId, trackId, trackRank)
          |  VALUES ({playlistId}, {trackId}, {trackRank})""".stripMargin)
        .on(
          'playlistId -> playlistId,
          'trackId -> trackIdAndRank.UUID,
          'trackRank -> trackIdAndRank.rank.bigDecimal)
        .executeInsert()
    }
  } catch {
    case e: Exception => throw new DAOException("Track.saveTrackRelation: " + e.getMessage)
  }

  def deleteTrackRelation(playlistId: Long, trackId: UUID): Int = try {
    DB.withConnection { implicit connection =>
      SQL(
        """DELETE FROM playlistsTracks
          | WHERE playlistId = {playlistId}""".stripMargin)
        .on('playlistId -> playlistId)
        .executeUpdate()
    }
  } catch {
    case e: Exception => throw new DAOException("deleteTrackRelation: " + e.getMessage)
  }

  case class TrackInfo(trackId: UUID, action: String, trackRank: Option[BigDecimal])
  def trackInfoFormApply(trackId: String, action: String, trackRank: Option[BigDecimal]) =
    TrackInfo(UUID.fromString(trackId), action, trackRank)
  def trackInfoFormUnapply(trackInfo: TrackInfo) =
    Option((trackInfo.trackId.toString, trackInfo.action, trackInfo.trackRank))

  case class PlaylistIdAndTracksInfo(id: Long, tracksInfo: Seq[TrackInfo])
  def updateFormApply(id: Long, tracksInfo: Seq[TrackInfo]) =
    PlaylistIdAndTracksInfo(id, tracksInfo)
  def updateFormUnapply(playlistIdAndTracksInfo: PlaylistIdAndTracksInfo) =
    Option((playlistIdAndTracksInfo.id, playlistIdAndTracksInfo.tracksInfo))

  def existsPlaylistForUser(userId: UUID, playlistId: Long)(implicit connection: Connection): Boolean = try {
    SQL(
      """SELECT exists(SELECT 1 FROM playlists
        |  WHERE userId = {userId} AND playlistId = {playlistId})""".stripMargin)
      .on(
        "userId" -> userId,
        "playlistId" -> playlistId)
      .as(scalar[Boolean].single)
  } catch {
    case e: Exception => throw new DAOException("Artist.isArtistFollowed: " + e.getMessage)
  }

  def update(userId: UUID, playlistIdAndTracksInfo: PlaylistIdAndTracksInfo): Unit = try {
    DB.withConnection { implicit connection =>
      if (existsPlaylistForUser(userId, playlistIdAndTracksInfo.id)) {
        for (trackInfo <- playlistIdAndTracksInfo.tracksInfo)
          proceedTrackUpdateDependingOfAction(playlistIdAndTracksInfo.id, trackInfo)
      } else {
        throw new PlaylistDoesNotExistException("There is no playlist for this user Id and this playlist Id.")
      }
    }
  } catch {
    case e: Exception => throw new DAOException("Playlist.delete: " + e.getMessage)
  }

  def proceedTrackUpdateDependingOfAction(playlistId: Long, trackInfo: TrackInfo): Unit = trackInfo match {
    case trackToUpdate: TrackInfo if trackInfo.action == "M" =>
      updateTrackRank(playlistId, trackToUpdate)
    case trackToDelete: TrackInfo if trackInfo.action == "D" =>
      deleteTrackRelation(playlistId, trackToDelete.trackId)
    case trackToAdd: TrackInfo if trackInfo.action == "A" =>
      try {
        saveTrackRelation(playlistId, TrackUUIDAndRank(trackToAdd.trackId, trackToAdd.trackRank.get))
      } catch {
        case e: NoSuchElementException => PlaylistUpdateTrackWithoutRankException("Playlist.update")
      }
  }

  def updateTrackRank(playlistId: Long, trackInfo: TrackInfo): Unit = try {
    DB.withConnection { implicit connection =>
      SQL(
        """UPDATE playlistsTracks
          | SET rank = {trackRank}
          | WHERE playlistId = {playlistId} AND trackId = {trackId}""".stripMargin)
        .on(
          'playlistId -> playlistId,
          'trackId -> trackInfo.trackId,
          'trackRank -> trackInfo.trackRank)
        .executeUpdate()
    }
  } catch {
    case e: Exception => throw new DAOException("Playlist.updateTrackRank: " + e.getMessage)
  }*/
/*
  def addTracksInPlaylist(userId: String, playlistIdAndTracksId: PlaylistIdAndTracksId): Unit = {
    playlistIdAndTracksId.tracksId.foreach(trackId =>
      Track.savePlaylistTrackRelation(playlistIdAndTracksId.id, trackId))
  }

  def deleteTracksInPlaylist(userId: String, playlistIdAndTracksId: PlaylistIdAndTracksId): Unit = {
    playlistIdAndTracksId.tracksId.foreach(trackId =>
      Track.deletePlaylistTrackRelation(playlistIdAndTracksId.id, trackId))
  }*/
}
