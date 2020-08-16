package clients.ebay.mappers

import cats.implicits._
import domain.ItemDetails.Game
import domain.{Packaging, ListingDetails}

private[mappers] object GameDetailsMapper {

  private val LEVEL1_TITLE_WORDS_REPLACEMENTS = List(
    "(gold )?((greatest|playstation|ps) )?\\bhits\\b( range)?", "nintendo selects",
    "(?<=.{12})((new|rare|(very )?good) )?(\\b(for|((only|playable|plays) )?on)\\b )?(the )?((sony )?play( )?station( )?(\\d|one|move)|(?<!(Playstation(?s).*))\\bPS\\d\\b|(microsoft )?xb(ox)?( |-)(1|o(ne)?|360)|nintendo switch|(nintendo )?(?<!(\\bwii\\b(?s).*))\\bwii( u)?\\b)(?s).*",
    "\\bday\\b (one|1|zero|0)( (edition|\\be(d)?(i)?(t)?(i)?\\b))?(?s).*$",
    "(the )?(\\bHD\\b|lost|essential|exclusive|special|limited collectors|definitive|atlas|platinum|complete|standard|std|classic(s)?|(\\d+(th)?)? anniversary|remastered|elite|\\beu\\b|coll(ector(s)?)?|ltd|goty|(action )?game of the|legacy( pro)?|(un)?limited|premium|(digital )?deluxe|(\\w+)?ultimat).{0,20}(collection|edition|\\be(d)?(i)?(t)?(i)?\\b)(?s).*$",
    "(?<=.{5})(the )?((new|pristine|inc)\\s+)?(super|cheap(\\s+)?)?(free|fast|quick)?(\\s+)?(and )?(super( )?)?(prompt|free|fast|quick|(next|same|1|one) day|tracked|speedy|worldwide|\\bsc\\b|\\bfc\\b).{0,20}(dispatch|ship(ping)?|post(age)?|delivery|p(\\s+)?p).*$",
    "(?<=.{15})((brand\\s+)?new.{0,15})?(still )?((factory |un)?sealed|unopened|shrinkwrapped)(?s).*$",
    "(?<=.{15})\\b(hack\\s+slash|single player|Family Fun|basketball|((kids|fun) )?adventure|console single|tactical|3rd-person|rpg|fps|survival|(kids )?action|(kids )?racing|role|wrestling|fighting|multi(\\s+|-)?player)\\b.{0,20}game(?s).*"
  ).mkString("(?i)", "|", "")

  private val LEVEL2_TITLE_WORDS_REPLACEMENTS = List(
    "[^\\p{L}\\p{N}\\p{P}\\p{Z}]",
    "\\d{5,}(\\w+)?", "\\d{3,}\\s+\\d{4,}",
    "for (the )?playstation(\\s+)?vr", "((ps( )?)?(vr|move)|kinect) (needed|required|compatible)", "requires kinect( sensor)?",
    "((new|rare) )?((very )?good )?(\\b(for|((only|playable|plays) )?on)\\b )?(the )?(sony |microsoft )?(play( )?st(a)?(t)?(i)?(o)?(n)?(( )?(\\d|one|move))?|x( )?b(ox)?(( |-)?(live|o(ne)?|\\d+))?|\\bps\\d\\b|(nintendo )?(switch|\\bwii( u)?\\b))( (edition|version|action|wrestling|football))?(\\s+new)?((\\s+)?20\\d\\d)?",
    "(dbl|double|triple|twin|expansion|combo)( )?(pack|pk)", "new in (cellophane|packaging)",
    "(1st|2nd|first) class.*$", "(fully )?(boxed|complete) (\\bin\\b|with|case)(?s).*$", "exclusive to(?s).*$", "((supplied|comes) )?(with(out)?|\\bw(\\s+)?(o)?\\b|in original|no|missing|plus|has|inc(l)?(udes|uding)?) (booklet|original|instruction|box|map|case|manual)(?s).*$", "(the )?disc(s)? (are|is|in)(?s).*$",
    "(new )?(fully )?(((very|super) )?rare|limited run|(\\d+ )?new|pal|physical|great|boxed|full|complete|boxed( and)?\\s+complete) game(s)?( \\d+)?( new)?",
    "(in )?(near )?(great|(very )?good|incredible|ex(cellent)?|amazing|mint|superb|(full )?working|perfect|used|(fully )?tested|lovely|immaculate|fantastic|\\bfab\\b|fair|\\bV\\b)(?s).*(dis(c|k)?|working( perfectly)?|good|(working )?order|con(d)?(ition)?|value|prices)",
    "(\\bUK\\b|\\bEU\\b|genuine|european|platinum|original)(( |-)(release|new|only|seller|version|stock|import))?( 20\\d\\d)?",
    "Warner Bros", "ubisoft", "currys", "Take( |-)?(Two|2)( Interactive)?", "(EA|2k) (dice|music|sport(s)?|games)", "James Camerons", "\\bTom clancy(s)?\\b", "gamecube",
    "Bethesda(s)?( Softworks)?", "Hideo Kojima", "(bandai )?namco", "rockstar games", "James Bond", "Activision", "Peter Jacksons", "Naughty Dog", "Marvels", "\\bTHQ\\b",
    "Microsoft( 20\\d\\d)?", "sony", "(by )?elect(r)?onic arts", "nintendo( \\d+)?", "square enix", "Dreamworks", "Disneys", "Disney Pixar(s)?", "WB Games", "Bend Studio", "LucasArt(s)?",
    "Insomniac(s)?",
    "(?<=\\b(W)?(2k)?\\d+)(\\s+| - )(20\\d\\d|wrestling|basketball|footbal|formula)(?s).*",
    "(?<=FIFA) (soccer|football)", "(?<=WWE) wrestling", "(?<=F1)\\s+(Formula (one|1))( racing)?",
    "(?<=\\b20\\d\\d)(\\s+| - )(version|formula)(?s).*", "Formula (1|One)\\s+(?=F1)", "Marvel\\s+(?=Spider)",
    "(?<=\\b[ivx]{1,4}\\b)(\\s+| - )\\d+"
  ).mkString("(?i)", "|", "")

  private val LEVEL3_TITLE_WORDS_REPLACEMENTS = List(
    "Strategy\\s+Combat", "(First Person|FPS) Shooter", "(american|soccer) football( 20\\d\\d)?", "(auto|golf) sports", "Adventure\\s+role playing",
    "Sport\\s+(basketball|football)", "football soccer", "(servival )?Action Adventure( Open World)?", "(adventure )?survival horros", "fighting multiplayer", "Multi( |-)?Player",
    "(the )?(\\b(\\d player|Skateboarding|action|hit|official|console|gold|kids|children)\\b.{0,15})??\\b(video( )?)?game(s)?\\b( (for kids|series|good|boxed|console|of( the)? (year|olympic|movie)))?( 20\\d\\d)?", "nuevo", // focus on removing the word GAME
    "\\bpegi( \\d+)?\\b(?s).*$", "(\\d+th|(20|ten) year) (anniversary|celebration)", "(\\d|both)?( )?(dis(c|k)(s)?|cd(s)?)( (version|set|mint))?", "platinum", "(sealed )?brand new( (case|sealed))?( in packaging)?( 20\\d\\d)?", "\\bID\\d+\\w", "18\\s+years",
    "limited run( \\d+)?", "box( )?set", "pre(-|\\s+)?(owned|enjoyed|loved)", "compatible", "physical copy", "steel( )?box", "no scratches", "(manual|instructions) included", "100\\s+ebayer",
    "((barely|condition|never|hardly) )?(un)?used(( very)? good)?( (game|condition))?", "very good", "reorderable", "(posted|sent) same day", "in stock( now)?", "pre(\\s+)?release", "(only )?played once", "best price", "Special Reserve",
    "Expertly Refurbished Product", "(quality|value) guaranteed", "(trusted|eBay|best|from ebays biggest) Seller(s)?", "fully (working|tested)", "Order By 4pm", "Ultimate Fighting Championship",
    "remaster(ed)?", "directors cut", "\\bctr\\b", "original", "english", "deluxe", "standard", "\\bgoty\\b", "mult(i)?(-| )?lang(uage)?(s)?( in game)?", "(fast|free)( )?(dispatch|post)", "fast free",
    "blu-ray", "bonus level", "Console Exclusive", "playable on", "Definitive Experience", "Highly Rated", "essentials", "classic(s)?( hit(s)?)?", "boxed(?s).*(complete|manual)",
    "very rare", "award winning", "official licenced", "Unwanted Gift", "limited quantity", "region free", "gift idea", "in case", "add( |-)?on",
    "\\b(For )?age(s)? \\d+\\b", "must see", "see pics", "Backwards Compatible", "with bonus content", "Refurbished", "manual", "\\brated \\d+\\b",
    "\\bpal\\b(\\s+\\d+)?( (format|version))?", "\\ben\\b", "\\bcr\\b", "\\bnc\\b", "\\bfr\\b", "\\bes\\b", "\\bvg(c| con(d)?(ition)?)?\\b", "\\ban\\b", "\\bLTD\\b", "\\b\\w+VG\\b",
    "\\bns\\b", "\\bnsw\\b", "\\bsft\\b", "\\bsave s\\b", "\\bdmc\\b", "\\bBNI(B|P)\\b", "\\bNSO\\b", "\\bNM\\b", "\\bLRG\\b", "\\bUE\\b", "\\bBN\\b", "\\bRRP\\b(\\s|\\d)*",
    "\\bremake\\b( 20\\d\\d)?", "(ultra )?\\b(u)?hd(r)?\\b", "\\b4k\\b", "\\buns\\b", "\\bx360\\b", "\\bstd\\b", "\\bpsh\\b", "\\bAMP\\b", "\\bRPG\\b", "\\bBBFC\\b", "\\bPG(13)?\\b",
    "\\bDVD\\b", "\\bAND\\b", "\\bNTSC\\b", "\\bPA2\\b", "\\bWi1\\b", "\\bENG\\b", "\\bVGWO\\b", "\\bFPS\\b", "\\b(PS( )?)?VR\\b( version)?", "\\bSRG(\\d+)?\\b", "\\bEA(N)?\\b", "\\bGC\\b", "\\bCIB\\b",
    "SEALED$", "NEW$",
    "(100\\s+)?((all|fully) )?complete( (instructions|package))?"
  ).mkString("(?i)", "|", "")

  private val EDGE_WORDS_REPLACEMENTS = List(
    "^(\\s)?(((brand )?NEW|BNIB|Factory)\\s+)?(and )?SEALED( in Packaging)?",
    "^SALE", "(brand )?new$", "^BOXED", "^SALE", "^NEW", "^best", "^software", "un( |-)?opened$", "rare$", "^rare", "official$",
    "^bargain","bargain$", "(near )?mint$", "\\bfor\\b( the)?$", "premium$", "\\bvery\\b$", "\\bLIMITED\\b$", "(fully )?(un)?tested$", "\\bON\\b$", "\\bBY\\b$",
    "boxed$", "brand$", "good$", "excellent$", "immaculate$", "instructions$", "superb$"
  ).mkString("(?i)", "|", "")

  private val PLATFORMS_MATCH_REGEX = List(
    "PS\\d", "PLAYSTATION(\\s+)?(\\d|one)",
    "NINTENDO SWITCH", "SWITCH", "\\bWII( )?U\\b", "\\bWII\\b",
    "X( )?B(OX)?(\\s+)?(ONE|\\d+)", "X360", "XBOX"
  ).mkString("(?i)", "|", "").r

  private val BUNDLE_MATCH_REGEX = List(
    "(new|multiple|PS4|PS3|xbox one|switch|wii( u)?) games", "bundle", "job(\\s+)?lot"
  ).mkString("(?i)", "|", "").r

  private val PLATFORM_MAPPINGS: Map[String, String] = Map(
    "SONYPLAYSTATION4" -> "PS4",
    "PLAYSTATION4" -> "PS4",
    "SONYPLAYSTATION3" -> "PS3",
    "PLAYSTATION3" -> "PS3",
    "SONYPLAYSTATION2" -> "PS2",
    "PLAYSTATION2" -> "PS2",
    "SONYPLAYSTATION1" -> "PS1",
    "SONYPLAYSTATION" -> "PS",
    "PLAYSTATIONONE" -> "PS1",
    "PLAYSTATION" -> "PS",
    "NINTENDOSWITCH" -> "SWITCH",
    "XBOX1" -> "XBOX ONE",
    "XBOX360" -> "XBOX 360",
    "XB1" -> "XBOX ONE",
    "XB360" -> "XBOX 360",
    "X360" -> "XBOX 360",
    "XBOXONE" -> "XBOX ONE",
    "XBONE" -> "XBOX ONE",
    "MICROSOFTXBOXONE" -> "XBOX ONE",
    "MICROSOFTXBOX360" -> "XBOX 360",
    "MICROSOFTXBOX" -> "XBOX",
    "XBOX" -> "XBOX",
    "WIIU" -> "WII U",
    "WII" -> "WII"
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
    title
      .withoutSpecialChars
      .replaceAll(EDGE_WORDS_REPLACEMENTS, "")
      .replaceAll(LEVEL1_TITLE_WORDS_REPLACEMENTS, "")
      .replaceAll(LEVEL2_TITLE_WORDS_REPLACEMENTS, "")
      .replaceAll(LEVEL3_TITLE_WORDS_REPLACEMENTS, "")
      .replaceFirst("(?i)(the )?\\w+(?=\\s+(\\be(d)?(i)?(t)?(i)?(o)?(n)?\\b|coll(ection)?)) (\\be(d)?(i)?(t)?(i)?(o)?(n)?\\b|coll(ection)?)(?s).*$", "")
      .replaceAll("é", "e")
      .replaceAll("(?i)playerunknown", "Player Unknown")
      .replaceAll("(?i)(littlebigplanet)", "Little Big Planet")
      .replaceAll("(?i)(farcry)", "Far Cry")
      .replaceAll("(?i)(superheroes)", "Super Heroes")
      .replaceAll("(?i)(W2K)", "WWE 2k")
      .replaceAll("(?i)(NierAutomata)", "Nier Automata")
      .replaceAll("(?i)(Hello Neighbour)", "Hello Neighbor")
      .replaceAll("(?i)(fifa 20(?=\\d+))", "FIFA ")
      .replaceAll("(?i)(witcher iii)", "witcher 3")
      .replaceAll("(?i)(wolfenstein 2)", "Wolfenstein II")
      .replaceAll("(?i)(diablo 3)", "diablo iii")
      .replaceAll("(?i)(\\bnsane\\b)", "N Sane")
      .replaceAll("(?i)(\\bww2|ww11\\b)", "wwii")
      .replaceAll("(?i)(\\bcod\\b)", "Call of Duty ")
      .replaceAll("(?i)GTA(?=\\d)?", "Grand Theft Auto ")
      .replaceAll("(?i)(\\bMGS\\b)", "Metal Gear Solid ")
      .replaceAll("(?i)(\\bRainbow 6\\b)", "Rainbow Six ")
      .replaceAll("(?i)(\\bLEGO Star Wars III\\b)", "LEGO Star Wars 3 ")
      .replaceAll("(?i)(\\bEpisodes From Liberty City\\b)", "Liberty")
      .replaceAll("(?i)(\\bIIII\\b)", "4")
      .replaceAll("(?i)(\\bGW\\b)", "Garden Warfare ")
      .replaceAll("(?i)(\\bGW2\\b)", "Garden Warfare 2")
      .replaceAll("(?i)((the|\\ba\\b)? Telltale(\\s+series)?(\\s+season)?)", " Telltale")
      .replaceAll("-|:", " ")
      .replaceAll(" +", " ")
      .replaceAll("[^\\d\\w]$", "")
      .trim()
      .replaceAll(EDGE_WORDS_REPLACEMENTS, "")
      .trim()
      .some

  private def mapPlatform(listingDetails: ListingDetails): Option[String] = {
    PLATFORMS_MATCH_REGEX.findFirstIn(listingDetails.title.withoutSpecialChars)
      .orElse(listingDetails.properties.get("Platform").map(_.split(",|/")(0)))
      .map(_.toUpperCase.trim)
      .map(_.replaceAll(" |-", ""))
      .map(platform => PLATFORM_MAPPINGS.getOrElse(platform, platform))
      .map(_.trim)
  }

  private def mapGenre(listingDetails: ListingDetails): Option[String] = {
    listingDetails.properties.get("Genre").orElse(listingDetails.properties.get("Sub-Genre"))
  }

  implicit class StringOps(private val str: String) extends AnyVal {
    def withoutSpecialChars: String = str.replaceAll("[@~+%\"{}?_;`—–“”!•£&#,’'*()|.\\[\\]]", "").replaceAll("/", " ")
  }
}
