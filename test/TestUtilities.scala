import org.scalatest.concurrent.ScalaFutures._
import org.scalatest.time.{Seconds, Span}
import services.Utilities._
import org.scalatestplus.play._
import org.scalatest._
import Matchers._
import play.api.libs.json.{JsValue, Json}
import models.Artist._

class TestUtilities extends PlaySpec with OneAppPerSuite {

  "A utilities" must {

    "normalize string with the method normalizeString" in {
      val strings = List("éh'=)àç_è-(aék", "abc de#f")

      val normalizedString: List[String] = strings.map { normalizeString }

      val expectedResult = List("éh'=)àç_è-(aék", "abc de#f")

      normalizedString mustBe expectedResult
    }
  }

  "normalize urls" in {
    val urls = Seq("abc.fr", "www.abc.com", "http://cde.org", "https://jkl.wtf", "http://www.claude.cool",
      "https://www.claude.music")

    val expectedUrls = Seq("abc.fr", "abc.com", "cde.org", "jkl.wtf", "claude.cool", "claude.music")

    val normalizedUrls = urls map { normalizeUrl }

    normalizedUrls mustBe expectedUrls
  }

  "create a new instance of GeographicPoints" in {
    GeographicPoint("(0,0)")
    GeographicPoint("(0.4,0)")
    GeographicPoint("(0.4,0.5784)")
    GeographicPoint("(9,0.5784)")
    GeographicPoint("(-9,0.5784)")
    GeographicPoint("(-9,-0.5784)")
    GeographicPoint("(9,-0.5784)")
    GeographicPoint("(-48.87965412354687,-145.5754545484)")
  }

  "throw exceptions while instantiating these geographicPoints" in {
    an [IllegalArgumentException] should be thrownBy GeographicPoint("0,0")
  }

  "refactor events or places names" in {
    val eventsName = Seq("abc", "abcdef @transbordeur abc", "abcdef@hotmail.fr")
    val expectedEventsName = Seq("abc", "abcdef", "abcdef@hotmail.fr")

    eventsName map  { refactorEventOrPlaceName } mustBe expectedEventsName
  }

  "return an optional string from a set" in {
    setToOptionString(Set.empty) mustBe None
    setToOptionString(Set("a")) mustBe Some("a")
    setToOptionString(Set("a", "b", "c")) mustBe Some("a,b,c")
  }

  "return a set from an optional string" in {
    optionStringToSet(None) mustBe Set.empty
    optionStringToSet(Some("a")) mustBe Set("a")
    optionStringToSet(Some("a,b,c")) mustBe Set("a", "b", "c")
  }

  "return a list of normalized websites from a text" in {

    val expectedWebsites = Set(
      "facebook.com/cruelhand",
      "facebook.com/alexsmokemusic",
      "facebook.com/nemo.nebbia",
      "youtube.com/watch?v=t5mhwqwypva",
      "facebook.com/nosajthing",
      "youtube.com/watch?v=0o663ex_5ts",
      "discogs.com/artist/2922409-binny-2",
      "discogs.com/artist/1156643-lee-holman",
      "facebook.com/kunamaze",
      "youtube.com/watch?v=evogdhdpgvw",
      "mixcloud.com/la_face_b",
      "youtube.com/watch?v=jesdtqr3cko",
      "facebook.com/burningdownalaska",
      "facebook.com/diane-454634964631595/timeline",
      "facebook.com/beingasanocean",
      "soundcloud.com/dianecytochrome",
      "nosajthing.com",
      "facebook.com/theoceancollective",
      "soundcloud.com/osunlade",
      "facebook.com/woodwireproject",
      "youtube.com/watch?v=d4cad8sj6gc",
      "youtube.com/watch?v=wpd6foowana",
      "youtube.com/watch?v=r8n8uy5kmvu",
      "facebook.com/lotfilafaceb",
      "soundcloud.com/woodwire",
      "facebook.com/loheem?fref=ts",
      "yorubarecords.net",
      "soundcloud.com/alexsmoke",
      "facebook.com/monoofjapan",
      "vimeo.com/irwinb",
      "facebook.com/fitforakingband",
      "facebook.com/jp-manova",
      "youtube.com/watch?v=at6mnjcy3co",
      "youtube.com/watch?v=yrjooroce1w",
      "facebook.com/solstafirice",
      "discogs.com/label/447040-clft",
      "facebook.com/theamityafflictionofficial",
      "facebook.com/defeaterband",
      "soundcloud.com/kuna-maze",
      "soundcloud.com/paulatemple",
      "facebook.com/musicseptembre?fref=ts",
      "facebook.com/paulatempleofficial")

    val exampleDescription =
      """La programmation d’artistes Coup de Cœur peut s’avérer être un choix cornélien. Entre le nombre accru de
        |propositions et les semaines qui ne comportent pas assez de jours pour pouvoir faire tout ce qu’on veut, il
        |faut savoir faire le bon choix.      JP Manova a débarqué sur la scène rap il y a près de 20 ans... pour ne
        |sortir son premier album 19h07 en 2015. Lattente fut longue mais 19h07 nous a mis une telle claque quil nous
        |paraissait impensable de ne pas inviter JP à un Concert Coup de Coeur. Evidemment, on pense fortement à
        |MC Solaar à lécoute de lalbum, mais JP nen garde pas moins une personnalité forte jamais avare en critiques
        |sur lactu doù se dégagent une poésie et une atmosphère souvent trop rares sur la scène rap actuelle.
        |Cest le grenoblois Nemo Nebbia (lui aussi, véritable électron libre brillant et discret) qui assurera
        |louverture accompagné de Dj P.
        |JP Manova : facebook.com/jp-manova
        |Nemo Nebbia : facebook.com/nemo.nebbia
        |★ SEPTEMBRE ★
        |Il y a tout juste un an, une page se tournait pour les quatre membres du groupe Phyltre. Une décision,
        |la fin dun cycle, sans rien regretter. Mais malgré tout, lenvie dune nouvelle naissance.
        |Après un an de travail, décriture, denregistrement, de répétitions, les quatre avignonnais donnent enfin vie,
        |en cette rentrée 2015, à SEPTEMBRE. Leur Septembre, leur recommencement.
        |facebook.com/musicseptembre?fref=ts
        |________    ★ LOHEEM ★    De la pop brute, sexy, écrite et jouée par un duo qui ne fait qu’un : c’est Loheem.
        |Pour attaquer la rentrée en douceur, ce tandem attachant met de côté la guitare et la batterie pour proposer
        |un set coloré et électronique. Une belle occasion de découvrir les chansons de leur premier album fraîchement
        |enregistré.
        |facebook.com/loheem?fref=ts
        |________    ★ UGO MARTINEZ ★
        |En préambule de la soirée, retrouvez lex-animateur de la radio Raje, aujourdhui animateur chez France Bleu
        |Vaucluse également connu sous le nom de Professeur Martinez pour une sélection musicale aux petits oignons.
        | — OSUNLADE    Originaire de Saint-Louis dans le Missouri, ville réputée pour être le berceau du Jazz, Osunlade
        | y découvrira le piano dès lâge de 7 ans. C’est ainsi que naîtra son destin. Car si la musique a
        | incontestablement une âme, la House a son chaman. Boss du label Yoruba records, Osunlade est un homme qui
        | perfectionne l’Art. Son album Pyrography, sorti en 2001, est dailleurs une pièce dorfèvre. Sa musique unifie
        | les mélodies et se manifeste par un subtile mélange entre vitalité et sagesse, et ses sets sont toujours une
        | expérience, aux confins dune house deep and sweet, de la transe africaine et dun jazz solaire. Depuis 30 ans,
        | il a aussi bien travaillé avec Patti Labelle, Salif Keita, Roy Ayers ou Cesaria Evora.    — LOTFI
        | Aka La Face B (Lyon/Fr) est un DJ à multiple facettes musicales. Que ce soit soul, funk, afro, bossa nova,
        | latin groove, disco, rockabilly, northern soul, house & deep house, rock, pop et bien sûr le jazz, pour
        | lui tout est bon dans le bon son et sa seule et unique ligne directrice est le groove.
        | ▬▬▬▬▬    23:30 — 02:00    Lotfi (Lyon, fr) › La face B
        | facebook.com/lotfilafaceb    mixcloud.com/la_face_b
        | 02:00 — 05:00    Osunlade (Missouri, usa) › Atjazz Record Company, Yoruba records
        | yorubarecords.net    soundcloud.com/osunlade
        | C’est avec un immense plaisir que nous vous présenterons le nouveau live A/V de Nosaj Thing en collaboration
        | avec le Transbordeur le mardi 3 novembre au Club Transbo. Ce format concert sera l’occasion de déguster les
        | lives respectifs de Kuna Maze et woodwire, deux pointures du collectif Orbit.
        |  — NOSAJ THING,    Sensible et légère, la musique de Nosaj Thing cache une information essentielle :
        |  Jason Chung est une bête de scène. En live, ses mains bondissent entre les potards, obligeant le reste de
        |  son corps à exécuter une danse ondulatoire qui contamine le public… Qu’on se rassure, rien d’ésotérique ici.
        |  Ce deuxième album soigné le confirme : Nosaj Thing ne doit son statut d’étoile montante qu’à lui-même.
        |  — KUNA MAZE,    Producteur lyonnais, Kuna Maze propose une musique aux multiples influences. Les sonorités
        |  de ce trompettiste de formation, entre hip-hop, électro, expérimentale, jazz, et nourrit aux samples dans la
        |  tradition du crate digging, oscillent entre nappes hypnotiques et beat déconstruit. Influencé par des
        |  artistes tel que Jay Dee, Flying Lotus, Shlohmo ou encore Sun Ra pour ne citer queux, il crée une musique
        |  et un univers qui lui sont propres.    — WOODWIRE,
        |  Beatmaker lyonnais, Woodwire s’inspire des artistes multi-instrumentaux tels que Baths, Prefuse 73,
        |  Gold Panda ou encore Shigeto.      Il présentera un live virvoltant entre offbeat déstructuré et
        |  downtempo mélancolique, décorant lensemble dun timbre électronique enrobé dans des nappes aériennes.
        |  ▬▬▬▬▬    20:00— 20:45    Woodwire (Lyon, fr) › Orbit
        |  facebook.com/woodwireproject    soundcloud.com/woodwire    20:45 — 21:30    Kuna Maze (Lyon, fr) › Orbit,
        |  Cascade record      facebook.com/kunamaze    soundcloud.com/kuna-maze    21:30 — 22:30
        |  Nosaj Thing (Detroit, usa) › Timetable, Innovative Leisure      facebook.com/nosajthing    nosajthing.com
        |  PAULA TEMPLE / R&S - Noise Manifesto / UK      Depuis ses débuts, la grande prêtresse de la techno
        |  originaire de Manchester, cultive un univers sonore dune rare et douce brutalité.      Paula Temple est une
        |  habituée des plus grands clubs et festivals européens.    Ses performances hybrides, doù jaillissent des
        |  sons profonds et audacieux, sont aujourdhui devenues sa marque de fabrique. Produisant une techno futuriste
        |  de qualité dont elle seule a le secret, elle propulse son public dans une autre dimension.
        |  En 2012, Paula fonde Noise Manifesto, un label musical et une plateforme destinée aux collaborations
        |  électroniques innovantes et hors-normes.      A lorigine du projet techno féministe « Spank Protest »,
        |  lartiste rejoint R&S Records avec le sombre Colonized EP et dans la foulée létonnant remix de Pilgrim de
        |  Fink ou encore Planningtorock.
        |  facebook.com/paulatempleofficial    soundcloud.com/paulatemple    ALEX SMOKE / R&S / UK      Figure
        |  incontournable de la scène techno internationale, lécossais Alex Smoke a à son actif deux albums et de
        |  multiples singles et remixes, parus chez les écossais de Soma Records, le label allemand Vakant et
        |  dernièrement un Ep Stauner paru sur Optimo Trax ).    Cet insatiable producteur, précurseur du son minimal
        |  met son talent versatile au service de titres optimistes, dansants et planants, deux facettes quil sait mêler
        |  avec harmonie.      Aujourdhui, Alex Smoke revient avec un nouvel album prévu début Novembre sur le label
        |  R&S.    facebook.com/alexsmokemusic    soundcloud.com/alexsmoke
        |  DIANE / Cytochrome / FR    Formée au conservatoire, Diane se passionne très tôt pour les cultures
        |  électroniques et abandonne le piano et le violoncelle pour les platines. Lesthétique recherchée : trouver
        |  la lumière dans lobscur. Lunivers Techno simpose vite comme une évidence.      Remarquée aux côtés dartistes
        |  comme The Hacker ou Surgeon, remarquable en ouverture de lédition 2013 de Nuits Sonores, elle a également
        |  donné la cadence de nos nuits au cours de sa résidence Cytochrome au Terminal Club.
        |  facebook.com/diane-454634964631595/timeline/      soundcloud.com/dianecytochrome
        |  Longtemps, nous navons trouvé domicile en terres lyonnaises. Nul nest prophète en son pays, certes, mais
        |  quatre années de vagabondages, ça commence à être long. Tout vient à point qui sait attendre, donc, et
        |  cest au Transbordeur que nous poserons bagages. On en rêvait, nous y voilà autorisés. Soyez tous les
        |  bienvenus.    - - -    LEE HOLMAN (Clft + Ferox + Kawl / Wexord)
        |  discogs.com/artist/1156643-lee-holman
        |  BINNY (Clft + Orbis + Monnom Black / Liverpool)    discogs.com/artist/2922409-binny-2
        |  CLFT MILITIA (Clft + Ravesodie / Lyon)    discogs.com/label/447040-clft
        |  + VJING par IRWIN BARBÉ (Clft + Blocaus + Latency / Paris)
        |  vimeo.com/irwinb    - - -    Tarif : 10€ sur place. 08€ en prévente.    Soirée réservée aux personnes
        |  majeures et munies dune pièce didentité. Pour vous rendre au Transbordeur, prenez le C5 depuis Hotel de
        |  Ville, le C2 depuis Charpennes, ou encore le C4, depuis Foch, et descendez à Cité Internationale.
        |  Pour toutes autres informations (demande de partenariat, jeux concours, référencements et agenda, etc...)
        |   contactez directement CLFT via clftmilita@gmail.com    ► MONO    Le groupe japonais de post-rock
        |   instrumental Mono débute sa carrière en 1999. Le premier album, « Under The Pipal Tree », propose une
        |   musique alambiquée, furieuse et psychédélique. Les opus suivants verront Mono perfectionner et polir
        |   cette formule jusqu’à finir par l’abandonner au profit d’une composition plus chargée en envolées
        |   orchestrales. Mono ira même jusqu’à donner plusieurs concerts dans des salles très prestigieuses
        |   accompagné d’un orchestre, proposant ainsi un spectacle très mélancolique et mélodramatique. L’épilogue
        |   de ce cycle s’est effectué avec l’album « For My Parents » sorti en 2012. Mono choisi de revenir à ses
        |   origines : une musique plus simple et dépouillée, alliant légèreté et violence. Cette réorientation
        |   stylistique se traduit par leurs deux nouveaux albums sortis simultanément en 2014 : « The Last Dawn »
        |   qui montre la facette la plus lumineuse du groupe, et « Rays Of Darkness » qui voit Mono délivrer la
        |   composition la plus sombre de sa carrière. Ces deux opus sont opposés et pourtant parfaitement
        |   complémentaires, explorant des thèmes chers au groupe tels que l’espoir et le désespoir, l’amour et la
        |   perte, l’immense bonheur et la douleur indescriptible.    Après un passage en 2013 à lEpicerie Moderne de
        |   Feyzin, Mono revient à Lyon aux côtés de Sólstafir pour présenter ces deux nouveaux opus !
        |   Facebook : facebook.com/monoofjapan
        |   Dream Odyssey : youtube.com/watch?v=d4cad8sj6gc
        |   ► SÓLSTAFIR    Sólstafir voit le jour à Reykjavik courant 1994. Des débuts black metal au rock progressif
        |   et atmosphérique dans lequel le groupe évolue aujourdhui, les islandais ont conservé les thématiques et
        |   sonorités païennes et issues de leur folklore local. Après quelques démos et des prestations live en
        |   Islande, le groupe sort son premier album, Í Blóði og Anda en 2002. Sólstafir commence à donner des
        |   concerts en Europe en 2004, sort lalbum “Masterpiece Of Bitterness” en 2005 chez Spinefarm, puis Khöld en
        |   2009 et Svartir Sandar en 2011 chez Season Of Mist.      Au fil de sa discographie, Sólstafir se dirige
        |   vers un metal teinté de post-rock, de rock progressif et de pop rock. A la fois sombre et lumineuse,
        |   brute et richement travaillée, la musique de Sólstafir est difficile à placer dans une case, et cest ce qui
        |   fait tout le charme de la formation islandaise. Le dernier opus du combo, Ótta, est sorti chez Season of
        |   Mist fin août 2014 et a été encensé tant par les critiques que par le public .
        |   Facebook : facebook.com/solstafirice    Lágnætti : youtube.com/watch?v=r8n8uy5kmvu
        |   ► THE OCEAN    Depuis ses débuts en 2001, The Ocean puise son inspiration dans des influences très
        |   variées allant du rock progressif au sludge, proposant une musique très personnelle et maîtrisée à la
        |   perfection. A l’origine conçu comme un collectif, The Ocean prend vie dans les sous-sols d’une ancienne
        |   fabrique d’aluminium à Berlin, servant de studio mais aussi de résidence pour les membres du projet mené
        |   par Robin Staps. Les quatre premiers albums de The Ocean sont enregistrés dans cet endroit surnommé
        |   Oceanland, avant que le groupe en soit évincé en 2008. The Ocean tourne dans le monde entier aux côtés de
        |   groupes tels qu’Opeth, Anathema, Cult Of Luna, Between The Buried and Me, Devin Townsend ou encore The
        |   Dillinger Escape Plan. Avec l’arrivée au chant de Loïc Rossetti en 2009, The Ocean passe de la forme de
        |   collectif à celle de groupe et sort en avril et novembre 2010 les opus « Hellocentric » et
        |   « Anthropocentric ». Le sixième album studio de The Ocean, « Pelagial », est sorti en 2013 chez Metal Blade.
        |   Facebook : facebook.com/theoceancollective
        |    Bathyalpelagic III: Disequillibrated : youtube.com/watch?v=0o663ex_5ts
        |    ► THE AMITY AFFLICTION    Formation australienne de post-hardcore/metalcore, The Amity Affliction
        |    voit le jour en 2003 sous l’impulsion d’Ahren Stringer et Troy Brady. Après une démo, deux EP et de
        |    nombreux concerts, le groupe sort son premier album, « Severed Ties », en 2008. Celui-ci sera suivi
        |    par« Youngbloods » et « Chasing Ghosts » en 2010 et 2012. Le quatrième opus de The Amity Affliction,
        |    « Let The Ocean Take Me », sort en 2014 chez Roadrunner Records. Il est le reflet d’un groupe plus motivé
        |    que jamais, qui a su faire franchir un nouveau palier à sa musique. Agressif, accrocheur et varié, le son
        |    de The Amity Affliction est renforcé par la grande prestation vocale livrée par Joel Birch et ce dernier
        |    album offre aux fans les plus acharnés du groupe ce qu’ils désirent et méritent.
        |    Facebook : facebook.com/theamityafflictionofficial    Deaths Hand : youtube.com/watch?v=t5mhwqwypva
        |    ► DEFEATER    Initialement nommé Sluts lors de sa formation en 2004, Defeater prend ce nom en 2008 alors
        |    que son line-up se stabilise. Un premier opus, « Travels », sort cette même année via Topshelf Records.
        |    Celui-ci obtient un excellent accueil et permet au groupe d’effectuer de nombreux concerts aux Etats-Unis
        |    et de partir en tournée européenne aux côtés de Comeback Kid. L’EP « Lost Ground » sort en 2009 chez
        |    Bridge Nine Records, suivi par les albums « Empty Days & Sleepless Nights » et « Letters Home » en 2011
        |    et 2013. Devenu en quelques albums l’un des fers de lance du hardcore moderne, Defeater appose des
        |    mélodies tortueuses et nerveuses sur des riffs puissants et incisifs tout en faisant la part belle au
        |    vocaliste Derek Archambault qui incarne avec passion les portraits de personnages brisés dépeints dans
        |    chacun de ces concept-albums. Le groupe a récemment signé chez Epitaph Records, chez qui sortira
        |    prochainement le quatrième opus du quintet américain.
        |    Facebook : facebook.com/defeaterband    No Savior : youtube.com/watch?v=evogdhdpgvw
        |    ► BEING AS AN OCEAN    Being As An Ocean voit le jour en Californie début 2012. Avec la volonté de
        |    proposer un hardcore mélodique et passionnel dans la lignée de Defeater, le groupe sort rapidement
        |    un premier opus, « Dear G-d… » chez Invogue Records. Grâce aux bonnes critiques obtenues par celui-ci,
        |    Being As An Ocean tourne avec Counterparts, Liferuiner et Hundredth. C’est avec un line-up remodelé que
        |    le groupe enregistre son deuxième album, « How We Both Wondrously Perish », qui sort en 2014. Gagnant en
        |    maturité, celui-ci voit le groupe évoluer vers un registre Post-Hardcore / Post-Rock, et avec une plus
        |    grande variété au niveau vocal.    « Being As An Ocean », troisième album éponyme très attendu, sortira
        |    le 30 juin, toujours chez Invogue Records.
        |    Facebook : facebook.com/beingasanocean    Little Richie : youtube.com/watch?v=wpd6foowana
        |    ► CRUEL HAND    Fondé en 2006 aux Etats-Unis, Cruel Hand s’inspire aussi bien du Hardcore New-Yorkais
        |    que du thrash de la côte ouest américaine, tout en apportant à ce mélange une touche bien personnelle.
        |    Leur premier album, « Without A Pulse », sort en 2007 chez 6131 Records. Le groupe commence alors à
        |    tourner aux Etats-Unis mais aussi en Europe. Le quintet sort ensuite « Prying Eyes » en 2008 puis
        |    « Lock & Key » en 2010 chez Bridge 9 Records, tout en multipliant les concerts et les tournées. Le
        |    quatrième album fort attendu du combo, « The Negatives », est sorti en septembre 2014 chez Hopeless
        |    Records. Le groupe marque un tournant dans sa carrière avec cet opus, incorporant à sa musique des
        |    mélodies punk et des passages en chant clair.
        |    Facebook : facebook.com/cruelhand    Cheap Life : youtube.com/watch?v=at6mnjcy3co
        |    ► FIT FOR A KING    L’histoire de Fit For A King débute en 2007 au Texas. Le groupe de christian
        |     metalcore sort deux EP et un premier album autoproduit, « Descendants », avant de signer chez Solid
        |      State Records qui fait paraître l’album Creation/Destruction en 2013. L’opus obtient un excellent
        |      accueil, se classe en 3ème position du classement Billboard Heatseekers et se vend à 3100 copies la
        |      première semaine de sa sortie. Son successeur, « Slave To Nothing », sort en octobre 2014 chez Solid
        |       State Records. Cet album, considéré comme le plus abouti de la discographie du quartet américain, fait
        |       preuve d’une grande maturité, notamment avec l’apport de parties très aérées et mélodiques.
        |       Facebook : facebook.com/fitforakingband    Hooked : youtube.com/watch?v=yrjooroce1w
        |       ► BURNING DOWN ALASKA    Dans une scène où tout semble avoir déjà été dit, Burning Down Alaska est
        |       parvenu à imposer son propre style qu’il qualifie de New Wave Hardcore. Le premier EP du combo allemand,
        |        « Values & Virtues », offre des mélodiques oniriques, des riffs complexes et un chant extrême
        |        particulièrement puissant et sensible. Burning Down Alaska s’inscrit dans une démarche d’innovation et
        |        n’est pas impressionné par la difficulté du challenge qui se présente. Le groupe place d’ores et déjà toutes
        |        les chances de son côté en collaborant avec des invités de marque tels que Michael McGough (Being As An Ocean)
        |         et Michael Lawler (In Vice Versa), et en publiant plusieurs clips de qualité.
        |         Facebook : facebook.com/burningdownalaska    Phantoms : youtube.com/watch?v=jesdtqr3cko
        |         -----------------------------------------------------------------------""".stripMargin

    getNormalizedWebsitesInText(Option(exampleDescription)) mustBe expectedWebsites
  }
}