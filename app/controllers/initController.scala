package controllers

import play.api.mvc.{Controller}

object InitController extends Controller {
  def init() = {
    fillDatabase.InsertFrenchCities.insertFrenchCities
    fillDatabase.InsertGenres.insertGenres
    fillDatabase.InsertTwoLettersArtists.insertTwoLettersArtists
  }
}