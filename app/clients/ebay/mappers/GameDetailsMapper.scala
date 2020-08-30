package clients.ebay.mappers

import cats.implicits._
import domain.ItemDetails.Game
import domain.{ListingDetails, Packaging}

private[mappers] object GameDetailsMapper {

  private val CONSOLE_REGEX_PATTERN =
    "((new|rare|official) )?((very )?good )?(\\b(for|((only|playable|plays) )?on)\\b )?(the )?" +
      "((sony )?play( )?st(a)?(t)?(i)?(o)?(n)?(( )?(\\d|one|move))?|(microsoft )?x( )?b(ox)?(( )?(live|o(ne)?|\\d+))?|\\bps( )?\\d\\b|(nintendo )?(switch|\\bwii( u)?\\b))" +
      "( game(s)?)?( (formula (1|one)|only|basketball|exclusive|console|edition|version|action|wrestling|football))?( game(s)?)?( new)?( 20\\d\\d)?"

  private val LEVEL1_TITLE_WORDS_REPLACEMENTS = List(
    "(gold )?((greatest|playstation|ps) )?\\bhits\\b( range)?",
    "nintendo selects",
    s"(?<=.{12})$CONSOLE_REGEX_PATTERN(?s).*",
    "\\bday\\b (one|1|zero|0)( (edition|\\be(d)?(i)?(t)?(i)?\\b))?(?s).*$",
    "(the )?(\\bHD\\b|steel case|nuketown|wild run|lost|essential|exclusive|special|limited collectors|definitive|atlas|platinum|complete|standard|std|classic(s)?|(\\d+(th)?)? anniversary|remastered|elite|\\beu\\b|coll(ector(s)?)?|ltd|goty|(action )?game of|legacy( pro)?|(un)?limited|premium|(digital )?deluxe|(\\w+)?ultimat).{0,20}(collection|edition|\\be(d)?(i)?(t)?(i)?\\b)(?s).*$",
    "(?<=.{5})(the )?((new|pristine|inc) )?(super|cheap( )?)?(free|fast|quick)?( )?(and )?(super( )?)?(prompt|free|fast|quick|(next|same|1|one) day|tracked|recorded|speedy|worldwide|\\bsc\\b|\\bfc\\b).{0,20}(dispatch|ship(ping)?|post(age)?|delivery|p( )?p).*$",
    "(?<=.{15})((brand )?new.{0,15})?(still )?((factory |un)?sealed|unopened|shrink( )?wrap)(?s).*$",
    "(?<=.{15})\\b(kids( \\w+)?|hack slash|single player|open world|Family Fun|basketball|(fun )?adventure|console single|tactical|3rd person|rpg|fps|survival|action|racing|role|wrestling|fighting|multi( )?player)\\b.{0,20}game(?s).*"
  ).mkString("(?i)", "|", "")

  private val LEVEL2_TITLE_WORDS_REPLACEMENTS = List(
    CONSOLE_REGEX_PATTERN,
    "[^\\p{L}\\p{N}\\p{P}\\p{Z}]",
    "\\d{5,}(\\w+)?",
    "\\d{3,}\\s+\\d{4,}",
    "for (the )?playstation( )?vr", "((ps( )?)?(vr|move)|kinect) (needed|required|compatible)", "requires kinect( sensor)?",
    "(dbl|double|triple|twin|expansion|combo)( )?(pack|pk)",
    "new in (cellophane|packaging|box)( still wrapped)?",
    "Now Released(?s).*$",
    "includes.{0,20}pack(?s).*$",
    "(royal mail )?(1st|2nd|first) class.*$",
    "(?<=\\w+ )(fully )?(boxed|complete) (\\bin\\b|with|case)(?s).*$",
    "exclusive to(?s).*$",
    "((with|inc(ludes)?) ).{0,15}(content|bonus)(?s).*$",
    "((supplied|comes) )?(with(out)?|\\bW( )?(O)?\\b|in original|no|missing|plus|has|inc(l)?(udes|uding)?)( game)? (strategy guide|booklet|original|instruction|box|map|(slip )?case|manual)(?s).*$",
    "(the )?disc(s)? (are|is|in)(?s).*$",
    "((new|all) )?(fully )?(((very|super) )?rare|limited run|(\\d+ )?new|pal|physical|great|boxed|full|complete|boxed( and)? complete|\\b\\d\\b) game(s)?( \\d+)?( new)?",
    "(in )?(near )?(great|(very )?good|incredible|ex(cellent)?|amazing|nice|mint|superb|(full )?working|perfect|used|(fully )?tested|lovely|immaculate|fantastic|\\bfab\\b|decent|fair|\\bV\\b)(?s).*(dis(c|k)?(s)?|working( (perfectly|fine))?|good|(working )?order|con(d)?(ition)?|value|prices)",
    "(\\bUK\\b|\\bEU\\b|genuine|european|platinum|original)( (edition|region|release|new|only|seller|version|stock|import))?( 20\\d\\d)?",
    "Warner Bros", "ubisoft", "currys", "Take (Two|2)( Interactive)?", "(EA|2k) (dice|music|sport(s)?|games)", "James Camerons",
    "\\bTom clancy(s)?\\b", "gamecube", "Bethesda(s)?( Softworks)?", "Hideo Kojima", "(bandai )?namco", "EastAsiaSoft",
    "rockstar games( present(s)?)?", "James Bond", "Activision", "Peter Jacksons", "Naughty Dog", "Marvels", "\\bTHQ\\b",
    "Microsoft( 20\\d\\d)?", "sony", "(by )?elect(r)?onic arts", "nintendo( \\d+)?", "square enix", "Dreamworks", "Disneys",
    "Disney( )?Pixar(s)?", "WB Games", "Bend Studio", "LucasArt(s)?", "Insomniac(s)?",
    "(?<=\\b(W)?(2k)?\\d+)\\s+(20\\d\\d|wrestling|basketball|footbal|formula)(?s).*",
    "(?<=FIFA) (soccer|football)", "(?<=NBA) basketball", "(?<=WWE) wrestling", "(?<=FIFA )20(?=\\d\\d)",
    "(?<=F1)\\s+(Formula (one|1))( racing)?", "(?<=\\b20\\d\\d)(\\s+)(version|formula)(?s).*",
    "(?<=Turismo( (\\d|sport))?) \\bGT(\\d|S)?\\b", "(?<=Sonic) The Hedgehog", "Formula (1|One)\\s+(?=F1)", "Marvel(s)?\\s+(?=(deadpool|Spider))",
    "(?<=\\b[ivx]{1,4}\\b)(\\s+)\\d+", "(?<=\\d) \\b[ivx]{1,4}\\b"
  ).mkString("(?i)", "|", "")

  private val LEVEL3_TITLE_WORDS_REPLACEMENTS = List(
    // removes the word GAME
    "(the )?(\\b(\\d player|kids( \\w+)?|football sport|shooting|hacker|racing|Skateboarding|action|hit|official|console|gold|children)\\b.{0,15})??\\b(video( )?)?game(s)?\\b( (for kids|series|good|boxed|console|of( the)? (year|olympic|movie)))?( 20\\d\\d)?",
    // removes the word USED
    "((barely|condition|never|hardly) )?(un)?used(( very)? good)?( (game|condition))?",
    "(the )?(official )?Strategy Combat( guide)?", "(First Person|FPS) Shooter", "(american|soccer) football( 20\\d\\d)?", "(racing|auto|golf|football) sport(s)?",
    "Adventure role playing", "ice hockey", "shoot em up", "Sport(s)? (basketball|football)", "football soccer", "action stealth",
    "((family fun|survival) )?Action Adventure( Open World)?", "(adventure )?survival horror", "fighting multiplayer", "Multi Player", "life simulation",
    "\\bpegi( \\d+)?\\b(?s).*$", "(\\d+th|(20|ten) year) (anniversary|celebration)", "(\\d|both)?( )?(dis(c|k)(s)?|cd(s)?)( (version|set|mint))?",
    "(sealed )?brand new( (case|sealed))?( in packaging)?( 20\\d\\d)?",
    "\\bID\\d+\\w",
    "platinum", "(16|18) years", "limited run( \\d+)?", "box( )?set", "pre( )?(release|owned|enjoyed|loved)",
    "compatible", "physical copy", "nuevo", "(big|steel)( )?box( version)?", "no scratches", "(manual|instructions)( (is|are))? (included|missing)",
    "100 ebayer", "(condition )?very good", "reorderable", "(posted|sent|dispatched) same day", "in stock( now)?",
    "(only )?played (once|twice)", "best price", "Special Reserve", "Expertly Refurbished Product", "(quality|value) guaranteed",
    "(trusted|eBay|best|from ebays biggest) Seller(s)?", "fully (working|tested)", "Order By 4pm", "Ultimate Fighting Championship",
    "remaster(ed)?", "directors cut", "original", "english", "deluxe", "standard", "Officially Licenced",
    "\\bctr\\b", "\\bgoty\\b", "mult(i)?( )?lang(uage)?(s)?( in game)?", "(with )?(fast|free|same day)( )?(delivery|dispatch|post)",
    "fast free", "blu( )?ray", "Console Exclusive", "playable on", "Definitive Experience", "Highly Rated", "essentials",
    "classic(s)?( (hit(s)?|version))?", "box.{0,20}(complete|manual)", "very rare", "award winning", "official licenced",
    "Unwanted Gift", "limited quantity", "region free", "gift idea", "in case", "add( |-)?on", "jeu console", "\\b(For )?age(s)? \\d+\\b",
    "must see", "see pics", "Backwards Compatible", "Refurbished", "manual", "shrink( )?wrapped", "\\bcert( )?\\d+\\b",
    "\\brated \\d+\\b", "\\d supplied", "((region|europe) )?(\\bPAL\\b|\\bNTSC\\b)( \\d+)?( (region|format|version))?", "\\ben\\b", "\\bcr\\b", "\\bnc\\b",
    "\\bfr\\b", "\\bes\\b", "\\bvg(c| con(d)?(ition)?)?\\b", "\\ban\\b", "\\bLTD\\b", "\\b\\w+VG\\b", "\\bns\\b", "\\bBNW(O)?T\\b",
    "\\bnsw\\b", "\\bsft\\b", "\\bsave s\\b", "\\bdmc\\b", "\\bBNI(B|P)\\b", "\\bNSO\\b", "\\bNM\\b", "\\bLRG\\b(( )?\\d+)?",
    "\\bUE\\b", "\\bBN\\b", "\\bRRP\\b(\\s|\\d)*", "\\bremake\\b( 20\\d\\d)?", "(ultra )?\\b(u)?hd(r)?\\b", "\\b4k\\b( enhanced)?",
    "\\buns\\b", "\\bx360\\b", "\\bstd\\b", "\\bpsh\\b", "\\bAMP\\b", "\\bRPG\\b", "\\bBBFC\\b", "\\bPG(13)?\\b", "\\bDVD\\b", "\\bSE\\b",
    "\\bAND\\b", "\\bPA2\\b", "\\bWi1\\b", "\\bENG\\b", "\\bVGWO\\b", "\\bFPS\\b", "\\b(PS( )?)?VR\\b( version)?",
    "\\bSRG(\\d+)?\\b", "\\bEA(N)?\\b", "\\bGC\\b", "\\bCIB\\b", "\\bFOR PC\\b",
    "(100 )?((all|fully) )?complete( (instructions|package))?", "SEALED(\\s+)?$", "NEW(\\s+)?$"
  ).mkString("(?i)", "|", "")

  private val EDGE_WORDS_REPLACEMENTS = List(
    s"^$CONSOLE_REGEX_PATTERN",
    "Playstation( \\d)?\\s+(?=PS)",
    "^(((brand )?NEW|BNIB|Factory) )?(and )?SEALED( in Packaging)?",
    "Standart$", "^SALE", "(brand )?new$", "^BOXED", "^SALE", "^NEW", "^best", "^software", "^un( )?opened",
    "un( )?opened$", "rare$", "^rare", "official$", "^bargain", "bargain$", "(near )?mint$", "\\bfor\\b( the)?$",
    "premium$", "\\bvery\\b$", "\\bLIMITED\\b$", "(cleaned )?(fully )?(un)?tested$", "\\bON\\b$", "\\bBY\\b$", "^cheapest",
    "boxed$", "brand$", "good$", "excellent$", "immaculate$", "instructions$", "superb$", "marvel$", "^mint"
  ).mkString("(?i)", "|", "")

  private val PLATFORMS_MATCH_REGEX = List(
    "PS\\d", "PLAYSTATION(\\s+)?(\\d|one)",
    "NINTENDO SWITCH", "SWITCH",
    "\\bWII( )?U\\b", "\\bWII\\b",
    "X( )?B(OX)?(\\s+)?(ONE|\\d+)", "X360", "XBOX"
  ).mkString("(?i)", "|", "").r

  private val BUNDLE_MATCH_REGEX = List(
    "(new|multiple|PS4|PS3|xbox one|switch|wii( u)?) games", "bundle", "job(\\s+)?lot"
  ).mkString("(?i)", "|", "").r

  private val PLATFORM_MAPPINGS: Map[String, String] = Map(
    "SONYPLAYSTATION4" -> "PS4",
    "PLAYSTATION4"     -> "PS4",
    "SONYPLAYSTATION3" -> "PS3",
    "PLAYSTATION3"     -> "PS3",
    "SONYPLAYSTATION2" -> "PS2",
    "PLAYSTATION2"     -> "PS2",
    "SONYPLAYSTATION1" -> "PS1",
    "SONYPLAYSTATION"  -> "PS",
    "PLAYSTATIONONE"   -> "PS1",
    "PLAYSTATION"      -> "PS",
    "NINTENDOSWITCH"   -> "SWITCH",
    "XBOX1"            -> "XBOX ONE",
    "XBOX360"          -> "XBOX 360",
    "XB1"              -> "XBOX ONE",
    "XB360"            -> "XBOX 360",
    "X360"             -> "XBOX 360",
    "XBOXONE"          -> "XBOX ONE",
    "XBONE"            -> "XBOX ONE",
    "MICROSOFTXBOXONE" -> "XBOX ONE",
    "MICROSOFTXBOX360" -> "XBOX 360",
    "MICROSOFTXBOX"    -> "XBOX",
    "XBOX"             -> "XBOX",
    "WIIU"             -> "WII U",
    "WII"              -> "WII"
  )

  def from(listingDetails: ListingDetails): Game = {
    val isBundle = BUNDLE_MATCH_REGEX.findFirstIn(listingDetails.title.withoutSpecialChars).isDefined
    Game(
      name = sanitizeTitle(listingDetails.title),
      platform = mapPlatform(listingDetails),
      genre = mapGenre(listingDetails),
      releaseYear = listingDetails.properties.get("Release Year"),
      packaging = if (isBundle) Packaging.Bundle else Packaging.Single
    )
  }

  private def sanitizeTitle(title: String): Option[String] =
    title.withoutSpecialChars
      .replaceAll(EDGE_WORDS_REPLACEMENTS, "")
      .replaceAll(LEVEL1_TITLE_WORDS_REPLACEMENTS, "")
      .replaceAll(LEVEL2_TITLE_WORDS_REPLACEMENTS, "")
      .replaceAll(LEVEL3_TITLE_WORDS_REPLACEMENTS, "")
      .replaceFirst("(?<=\\w+ )(?i)(the )?\\w+(?=\\s+(\\be(d)?(i)?(t)?(i)?(o)?(n)?\\b|coll(ection)?)) (\\be(d)?(i)?(t)?(i)?(o)?(n)?\\b|coll(ection)?)(?s).*$", "")
      .replaceAll("(?i)playerunknown", "Player Unknown")
      .replaceAll("(?i)(littlebigplanet)", "Little Big Planet")
      .replaceAll("(?i)(farcry)", "Far Cry")
      .replaceAll("(?i)(superheroes)", "Super Heroes")
      .replaceAll("(?i)(W2K)", "WWE 2k")
      .replaceAll("(?i)(NierAutomata)", "Nier Automata")
      .replaceAll("(?i)(Hello Neighbour)", "Hello Neighbor")
      .replaceAll("(?i)(witcher iii)", "witcher 3")
      .replaceAll("(?i)(wolfenstein 2)", "Wolfenstein II")
      .replaceAll("(?i)(diablo 3)", "diablo iii")
      .replaceAll("(?i)(\\bnsane\\b)", "N Sane")
      .replaceAll("(?i)(\\bww2|ww11\\b)", "wwii")
      .replaceAll("(?i)(\\bcod\\b)", "Call of Duty ")
      .replaceAll("(?i)(\\bmysims\\b)", "my sims")
      .replaceAll("(?i)RDR(?=\\d)?", "Red Dead Redemption ")
      .replaceAll("(?i)GTA(?=\\d)?", "Grand Theft Auto ")
      .replaceAll("(?i)(\\bMGS\\b)", "Metal Gear Solid ")
      .replaceAll("(?i)(\\bRainbow 6\\b)", "Rainbow Six ")
      .replaceAll("(?i)(\\bLEGO Star Wars III\\b)", "LEGO Star Wars 3 ")
      .replaceAll("(?i)(\\bEpisodes From Liberty City\\b)", "Liberty")
      .replaceAll("(?i)(\\bIIII\\b)", "4")
      .replaceAll("(?i)(\\bGW\\b)", "Garden Warfare ")
      .replaceAll("(?i)(\\bGW2\\b)", "Garden Warfare 2")
      .replaceAll("(?i)((the|\\ba\\b)? Telltale(\\s+series)?(\\s+season)?)", " Telltale")
      .replaceAll(" +", " ")
      .replaceAll("[^\\d\\w]$", "")
      .trim()
      .replaceAll(EDGE_WORDS_REPLACEMENTS, "")
      .trim()
      .some
      .filterNot(_.isBlank)

  private def mapPlatform(listingDetails: ListingDetails): Option[String] =
    PLATFORMS_MATCH_REGEX
      .findFirstIn(listingDetails.title.withoutSpecialChars)
      .orElse(listingDetails.properties.get("Platform").map(_.split(",|/")(0)))
      .map(_.toUpperCase.trim)
      .map(_.replaceAll(" |-", ""))
      .map(platform => PLATFORM_MAPPINGS.getOrElse(platform, platform))
      .map(_.trim)

  private def mapGenre(listingDetails: ListingDetails): Option[String] =
    List(
      listingDetails.properties.get("Genre"),
      listingDetails.properties.get("Sub-Genre")
    )
      .filter(_.isDefined)
      .sequence
      .map(_.mkString(" / "))
      .filter(_.nonEmpty)

  implicit class StringOps(private val str: String) extends AnyVal {
    def withoutSpecialChars: String =
      str
        .replaceAll("é", "e")
        .replaceAll("\\P{Print}", "")
        .replaceAll("\\\\x\\p{XDigit}{2}", "")
        .replaceAll("[@~+%\"{}?_;`—–“”!•£&#’'*|.\\[\\]]", "")
        .replaceAll("[()/,:-]", " ")
        .replaceAll(" +", " ")
        .trim
  }
}
