package database

import com.github.tminglei.slickpg._

trait MyPostgresDriver extends ExPostgresDriver
with PgArraySupport
with PgDate2Support
with PgPlayJsonSupport
with PgNetSupport
with PgLTreeSupport
with PgRangeSupport
with PgHStoreSupport
with PgSearchSupport
with PgPostGISSupport {

  override val pgjson = "jsonb"
  override lazy val Implicit = new ImplicitsPlus {}
  override val simple = new SimpleQLPlus {}

  trait ImplicitsPlus extends Implicits
  with ArrayImplicits
  with DateTimeImplicits
  with RangeImplicits
  with HStoreImplicits
  with JsonImplicits
  with SearchImplicits
  with PostGISImplicits

  trait SimpleQLPlus extends SimpleQL
  with ImplicitsPlus
  with SearchAssistants
  with PostGISAssistants
  ///
  override val api = new API with ArrayImplicits
    with DateTimeImplicits
    with PlayJsonImplicits
    with NetImplicits
    with LTreeImplicits
    with RangeImplicits
    with HStoreImplicits
    with SearchImplicits
    with PostGISImplicits
    with SearchAssistants {}
}

object MyPostgresDriver extends MyPostgresDriver