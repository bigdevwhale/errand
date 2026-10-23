package app.errand.domain

/**
 * Where things lie in an ordinary shop, from the door to the tills. A shop the courier hasn't
 * learned yet is walked in this order; the cold shelves come last so the bag stays cool.
 */
enum class Shelf(val pos: Float, val cold: Boolean = false) {
    PRODUCE(0.08f),
    BREAD(0.18f),
    GROCERY(0.32f),
    CANS(0.40f),
    SWEETS(0.48f),
    DRINKS(0.56f),
    HOUSEHOLD(0.64f),
    HYGIENE(0.70f),
    PHARMACY(0.74f),
    PETS(0.62f),
    OTHER(0.50f),
    MEAT(0.80f, true),
    DAIRY(0.88f, true),
    FROZEN(0.96f, true);

    companion object {
        fun of(s: String?) = entries.firstOrNull { it.name == s } ?: OTHER
    }
}

/** What the catalogue knows about a thing: its shelf, a usual weight and whether it wants the cold box. */
data class Guess(val shelf: Shelf, val grams: Int, val cold: Boolean)

/**
 * A rough goods catalogue, Russian and English, so a line typed as "молоко" already weighs a kilo and rides
 * in the cold box. Stems are matched at the start of a word; the longest match wins. Anything unknown is
 * half a kilo on the "other" shelf until the courier says otherwise.
 */
object Goods {
    private class G(val shelf: Shelf, val grams: Int, val stems: List<String>, val cold: Boolean = shelf.cold)

    private fun g(shelf: Shelf, grams: Int, ru: String, en: String, cold: Boolean = shelf.cold) =
        G(shelf, grams, (ru.split('|') + en.split('|')).map { it.trim() }.filter { it.isNotEmpty() }, cold)

    private val all = listOf(
        // Fruit and vegetables.
        g(Shelf.PRODUCE, 2500, "картошк|картофел", "potato"),
        g(Shelf.PRODUCE, 1000, "морков|свекл|свёкл", "carrot|beet"),
        g(Shelf.PRODUCE, 1000, "лук", "onion"),
        g(Shelf.PRODUCE, 100, "чеснок", "garlic"),
        g(Shelf.PRODUCE, 1000, "капуст", "cabbage"),
        g(Shelf.PRODUCE, 700, "огурц|огурец", "cucumber"),
        g(Shelf.PRODUCE, 700, "помидор|томат", "tomato"),
        g(Shelf.PRODUCE, 500, "перец|перц", "pepper"),
        g(Shelf.PRODUCE, 800, "кабачк|баклажан", "zucchini|courgette|eggplant|aubergine"),
        g(Shelf.PRODUCE, 1000, "яблок|груш", "apple|pear"),
        g(Shelf.PRODUCE, 1000, "банан", "banana"),
        g(Shelf.PRODUCE, 1000, "апельсин|мандарин|грейпфрут", "orange|mandarin|tangerine|clementine|grapefruit"),
        g(Shelf.PRODUCE, 150, "лимон|лайм", "lemon|lime"),
        g(Shelf.PRODUCE, 500, "виноград", "grape"),
        g(Shelf.PRODUCE, 300, "ягод|клубник|малин|черник|голубик", "berr|strawberr|raspberr|blueberr"),
        g(Shelf.PRODUCE, 250, "авокадо|киви|манго", "avocado|kiwi|mango"),
        g(Shelf.PRODUCE, 2000, "арбуз|дын", "watermelon|melon"),
        g(Shelf.PRODUCE, 100, "зелен|укроп|петрушк|кинз|базилик|салат|шпинат|руккол", "herb|dill|parsley|coriander|cilantro|basil|lettuce|salad|spinach|rocket|arugula"),
        g(Shelf.PRODUCE, 400, "гриб|шампиньон", "mushroom"),
        g(Shelf.PRODUCE, 300, "имбир", "ginger"),
        g(Shelf.PRODUCE, 300, "цветы|букет", "flowers|bouquet"),
        // Bread.
        g(Shelf.BREAD, 400, "хлеб|батон|багет|буханк|лаваш|чиабат", "bread|loaf|baguette|pita|ciabatta|bagel"),
        g(Shelf.BREAD, 300, "булочк|булк|круассан|плюшк", "bun|roll|croissant"),
        g(Shelf.BREAD, 250, "пирож|пирог|ватрушк", "pie|pastr"),
        g(Shelf.BREAD, 200, "сухар|хлебц", "rusk|crispbread|crackers"),
        // Dry goods.
        g(Shelf.GROCERY, 900, "греч|рис|пшен|перловк|булгур|кускус|киноа|овсянк|геркулес|хлопь|мюсли|крупа|крупы", "buckwheat|rice|millet|bulgur|couscous|quinoa|oat|porridge|cereal|muesli|granola|grain"),
        g(Shelf.GROCERY, 450, "макарон|спагетти|паст|лапш|вермишел", "pasta|spaghetti|noodle|macaroni|penne|fusilli"),
        g(Shelf.GROCERY, 1000, "мука|муки|сахар", "flour|sugar"),
        g(Shelf.GROCERY, 1000, "соль|соли", "salt"),
        g(Shelf.GROCERY, 900, "масло подсолн|масло растит|оливков|растительн", "olive oil|sunflower oil|vegetable oil|cooking oil"),
        g(Shelf.GROCERY, 100, "чай|чая", "tea"),
        g(Shelf.GROCERY, 250, "кофе", "coffee"),
        g(Shelf.GROCERY, 100, "специ|приправ|перец молот|корица|лавров|ванил", "spice|seasoning|cinnamon|vanilla|paprika|cumin"),
        g(Shelf.GROCERY, 400, "кетчуп|майонез|горчиц|соус|уксус", "ketchup|mayo|mustard|sauce|vinegar|dressing"),
        g(Shelf.GROCERY, 300, "орех|семечк|миндал|фисташк|изюм|курага|финик|сухофрукт", "nut|seed|almond|pistachio|raisin|apricot|date|dried fruit|peanut"),
        g(Shelf.GROCERY, 300, "мёд|мед|варень|джем|повидл|паста ореховая|арахисов", "honey|jam|marmalade|peanut butter|spread"),
        g(Shelf.GROCERY, 400, "чечевиц|фасол|горох|нут", "lentil|bean|pea|chickpea"),
        g(Shelf.GROCERY, 200, "дрожж|разрыхлит|сода|крахмал|желатин", "yeast|baking|soda|starch|gelatin"),
        // Tins and jars.
        g(Shelf.CANS, 400, "консерв|тушёнк|тушенк|шпрот|сайр|тунец|кукуруз|горошек|оливк|маслин", "tin|canned|tuna|sardine|corn|olive|beans in"),
        g(Shelf.CANS, 700, "огурцы маринов|солень|маринад|лечо|аджик|томатн", "pickle|gherkin|passata|tomato paste"),
        // Sweets and snacks.
        g(Shelf.SWEETS, 100, "шоколад|конфет|батончик|мармелад|зефир|пастил", "chocolate|candy|sweets|bar|gummy|marshmallow"),
        g(Shelf.SWEETS, 300, "печень|пряник|вафл|сушк|баранк|торт|пирожн|кекс", "biscuit|cookie|gingerbread|wafer|cake|muffin|brownie"),
        g(Shelf.SWEETS, 150, "чипс|сухарик|попкорн|крекер", "chips|crisps|popcorn|pretzel|snack"),
        g(Shelf.SWEETS, 500, "мороженое|пломбир|эскимо", "ice cream|gelato", cold = true),
        // Drinks.
        g(Shelf.DRINKS, 6000, "вода 6|упаковка воды|бутыл воды 5", "water 6|6 l water|water pack|case of water"),
        g(Shelf.DRINKS, 1500, "вода|минералк|газировк", "water|sparkling|soda water|mineral"),
        g(Shelf.DRINKS, 1000, "сок|морс|компот|нектар", "juice|nectar|smoothie"),
        g(Shelf.DRINKS, 1000, "лимонад|кола|квас|тоник|энергетик", "lemonade|cola|coke|tonic|energy drink|pop"),
        g(Shelf.DRINKS, 1500, "пиво|сидр", "beer|cider|lager|ale"),
        g(Shelf.DRINKS, 1200, "вино|шампанск|игрист", "wine|champagne|prosecco|cava"),
        g(Shelf.DRINKS, 700, "коньяк|водк|виски|ром|джин|ликёр|ликер|настойк", "brandy|vodka|whisk|rum|gin|liqueur|cognac"),
        // Household.
        g(Shelf.HOUSEHOLD, 1500, "порошок|стиральн|кондиционер для бель|капсулы для стир", "laundry|detergent|softener|washing powder|washing capsule"),
        g(Shelf.HOUSEHOLD, 500, "средство для посуд|фейри|губк|моющ|чистящ|отбелив|хлорк|освежит", "washing-up|dish soap|sponge|cleaner|bleach|spray|freshener"),
        g(Shelf.HOUSEHOLD, 800, "таблетки для посуд|для посудомо", "dishwasher"),
        g(Shelf.HOUSEHOLD, 1000, "туалетн бумаг|туалетная|бумажн полотен|полотенца бумаж|салфетк", "toilet paper|toilet roll|loo roll|kitchen roll|paper towel|napkin|tissues"),
        g(Shelf.HOUSEHOLD, 300, "пакеты для мусора|мешки для мусора|фольг|плёнк|пленк|пакет", "bin bag|trash bag|garbage bag|foil|cling|wrap|bags"),
        g(Shelf.HOUSEHOLD, 200, "батарейк|лампочк|свеч", "batter|bulb|candle"),
        g(Shelf.HOUSEHOLD, 300, "спички|зажигалк|перчатк|тряпк", "matches|lighter|gloves|cloth"),
        // Personal care.
        g(Shelf.HYGIENE, 300, "шампун|бальзам|кондиционер для волос|гель для душ|мыло|пена для ванн", "shampoo|conditioner|shower gel|soap|body wash|bubble bath"),
        g(Shelf.HYGIENE, 150, "зубная паст|зубн|паста зуб|щётк|щетк|нить зуб|ополаск", "toothpaste|toothbrush|floss|mouthwash"),
        g(Shelf.HYGIENE, 200, "дезодорант|крем|лосьон|бритв|пена для брит|прокладк|тампон|ватн|ватные", "deodorant|cream|lotion|razor|shaving|pads|tampon|cotton"),
        g(Shelf.HYGIENE, 1200, "подгузник|памперс", "nappies|diaper|pampers"),
        g(Shelf.HYGIENE, 600, "влажн салфет", "wet wipes|baby wipes"),
        // Chemist.
        g(Shelf.PHARMACY, 80, "таблетк|лекарств|витамин|аспирин|ибупрофен|парацетамол|пластыр|бинт|капли|сироп|йод|зелёнк|зеленк|термометр|маск", "pill|tablet|medicine|vitamin|aspirin|ibuprofen|paracetamol|plaster|bandage|drops|syrup|thermometer|mask"),
        // Pets.
        g(Shelf.PETS, 1500, "корм|наполнител", "pet food|cat food|dog food|litter|kibble"),
        // Meat and fish.
        g(Shelf.MEAT, 1000, "курица|курин|окороч|бедр|грудк|крыл", "chicken|thigh|breast|wing|drumstick"),
        g(Shelf.MEAT, 800, "мясо|говядин|свинин|баранин|индейк|телятин|фарш|котлет|стейк|шашлык|рёбр|ребр", "meat|beef|pork|lamb|turkey|veal|mince|steak|chop|ribs|burger"),
        g(Shelf.MEAT, 400, "колбас|сосиск|сардельк|ветчин|бекон|салями|буженин", "sausage|ham|bacon|salami|hot dog|frankfurter|chorizo"),
        g(Shelf.MEAT, 600, "рыба|рыбы|лосос|сёмг|семг|форел|треск|минтай|скумбри|сельд|селёдк|селедк|креветк|кальмар|мидии", "fish|salmon|trout|cod|haddock|mackerel|herring|prawn|shrimp|squid|mussel"),
        // Dairy and eggs.
        g(Shelf.DAIRY, 1000, "молок|кефир|ряженк|айран|простокваш", "milk|kefir|buttermilk"),
        g(Shelf.DAIRY, 600, "яйц|яиц|яйко", "egg"),
        g(Shelf.DAIRY, 400, "творог|творож|сырок|сырник", "cottage cheese|quark|curd"),
        g(Shelf.DAIRY, 300, "сыр|моцарелл|пармезан|брынз|фет", "cheese|mozzarella|parmesan|cheddar|feta|brie|halloumi"),
        g(Shelf.DAIRY, 400, "сметан|сливк|йогурт|десерт творож", "sour cream|cream|yogurt|yoghurt|creme fraiche"),
        g(Shelf.DAIRY, 200, "масло слив|сливочное масло|масло", "butter|margarine"),
        g(Shelf.DAIRY, 400, "тесто|хумус|тофу", "dough|hummus|tofu"),
        // Freezer.
        g(Shelf.FROZEN, 1000, "пельмен|вареник|хинкал|манты", "dumpling|ravioli|pierogi|gyoza"),
        g(Shelf.FROZEN, 400, "заморож|мороженн|лёд|лед для", "frozen|ice cubes"),
        g(Shelf.FROZEN, 400, "пицца", "pizza"),
    )

    /** Lower case, ё as е, single spaces: the form names are learned and priced under. */
    fun key(name: String): String = name.lowercase().replace('ё', 'е').replace(Regex("[^\\p{L}\\p{N} ]"), " ").replace(Regex("\\s+"), " ").trim()

    fun guess(name: String): Guess {
        val k = " " + key(name)
        var best: G? = null
        var len = 0
        for (gd in all) for (stem in gd.stems) {
            val s = key(stem)
            if (s.length > len && k.contains(" $s")) {
                best = gd
                len = s.length
            }
        }
        val b = best ?: return Guess(Shelf.OTHER, 500, false)
        return Guess(b.shelf, scale(b.grams, name), b.cold)
    }

    /** "молоко 2 л", "картошка 3 кг", "яйца 20 шт": a stated amount beats the catalogue's guess. */
    private fun scale(grams: Int, name: String): Int {
        val m = Qty.amount(name) ?: return grams
        return when (m.unit) {
            Unit.KG, Unit.L -> (m.value * 1000).toInt()
            Unit.G, Unit.ML -> m.value.toInt()
            // "2 pcs" of a pack is two packs; "10 pcs" of eggs is the pack the catalogue already weighs.
            Unit.PCS -> if (m.value in 2.0..6.0) (grams * m.value).toInt() else grams
        }.coerceIn(10, 30_000)
    }
}

enum class Unit { KG, G, L, ML, PCS }

data class Amount(val value: Double, val unit: Unit)

/** Amounts written into a line: "2 л", "500 г", "3 шт", "x2", "2x". */
object Qty {
    private val unitWords = listOf(
        Unit.KG to listOf("кг", "kg", "кило"),
        Unit.G to listOf("г", "гр", "g", "gr"),
        Unit.ML to listOf("мл", "ml"),
        Unit.L to listOf("л", "l", "литр", "литра", "литров", "litre", "liter", "litres", "liters"),
        Unit.PCS to listOf("шт", "штук", "штуки", "pcs", "pc"),
    )
    private val number = "(\\d+(?:[.,]\\d+)?)"
    private val unitAlt = unitWords.flatMap { it.second }.sortedByDescending { it.length }.joinToString("|") { Regex.escape(it) }
    private val after = Regex("(?i)(?<![\\p{L}\\d])$number\\s*($unitAlt)(?![\\p{L}])\\.?")
    private val times = Regex("(?i)(?<![\\p{L}\\d])(?:[xх×]\\s*$number|$number\\s*[xх×])(?![\\p{L}])")

    fun amount(text: String): Amount? {
        after.find(text)?.let { m ->
            val v = m.groupValues[1].replace(',', '.').toDoubleOrNull() ?: return null
            val word = m.groupValues[2].lowercase()
            val unit = unitWords.first { (_, w) -> word in w }.first
            return Amount(v, unit)
        }
        times.find(text)?.let { m ->
            val v = (m.groupValues[1].ifEmpty { m.groupValues[2] }).replace(',', '.').toDoubleOrNull() ?: return null
            return Amount(v, Unit.PCS)
        }
        return null
    }

    /** Splits "Молоко 2 л" into ("Молоко", "2 л"), "3x яблоки" into ("Яблоки", "3 шт"). */
    fun split(text: String, ru: Boolean): Pair<String, String> {
        val t = text.trim()
        after.find(t)?.let { m ->
            val name = (t.removeRange(m.range)).trim(' ', ',', '-', '—', ':').replace(Regex("\\s+"), " ")
            if (name.isNotEmpty()) return cap(name) to m.value.trim().replace(Regex("\\s+"), " ").let { if (it.first().isDigit()) spaced(it) else it }
        }
        times.find(t)?.let { m ->
            val name = (t.removeRange(m.range)).trim(' ', ',', '-', '—', ':').replace(Regex("\\s+"), " ")
            val n = m.groupValues[1].ifEmpty { m.groupValues[2] }
            if (name.isNotEmpty()) return cap(name) to "$n ${if (ru) "шт" else "pcs"}"
        }
        return cap(t) to ""
    }

    /** "2л" → "2 л". */
    private fun spaced(s: String): String = s.replace(Regex("^(\\d+(?:[.,]\\d+)?)\\s*"), "$1 ")

    private fun cap(s: String) = s.replaceFirstChar { it.uppercaseChar() }
}
