package app.errand.ui

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import app.errand.container
import app.errand.data.Aisle
import app.errand.data.Client
import app.errand.data.Item
import app.errand.data.Leg
import app.errand.data.Line
import app.errand.data.Price
import app.errand.data.Run
import app.errand.data.Settings
import app.errand.data.Shop
import app.errand.domain.Archive
import app.errand.domain.Badges
import app.errand.domain.Notebook
import app.errand.domain.Rank
import app.errand.domain.Route
import app.errand.domain.Rules
import app.errand.ru
import app.errand.ui.archive.ArchiveScreen
import app.errand.ui.components.rememberNow
import app.errand.ui.depot.DepotScreen
import app.errand.ui.file.FileScreen
import app.errand.ui.notebook.NotebookScreen
import app.errand.ui.office.OfficeScreen
import app.errand.ui.onboarding.GuideScreen
import app.errand.ui.onboarding.WelcomeScreen
import app.errand.ui.run.RunScreen
import app.errand.ui.settings.SettingsScreen
import app.errand.ui.slip.SlipScreen
import app.errand.ui.theme.S
import app.errand.ui.waybill.WaybillScreen

/** Everything in the books, plus what follows from it, at one moment. Screens read through here. */
@Immutable
class Home(
    val items: List<Item>,
    val runs: List<Run>,
    val clients: List<Client>,
    val shops: List<Shop>,
    val aisles: List<Aisle>,
    val prices: List<Price>,
    val settings: Settings,
    val now: Long,
    val ru: Boolean,
) {
    val currency = settings.currency
    fun money(minor: Long) = currency.format(minor)

    /** The waybill: everything ordered and not yet carried, oldest first, rush orders on top. */
    val pending: List<Item> = items.filter { it.lineOf == Line.PENDING && !it.offList }.sortedWith(compareBy({ !it.rush }, { it.addedAt }))
    val open: Run? = runs.firstOrNull { it.legOf == Leg.OUT }
    val done: List<Run> = runs.filter { it.legOf == Leg.DONE }.sortedByDescending { it.endedAt }
    val number: Int = open?.number ?: Rules.nextNumber(runs)

    val me: Client? = clients.firstOrNull { it.me }
    fun client(id: Long): Client? = clients.firstOrNull { it.id == id } ?: me
    fun shop(id: Long): Shop? = shops.firstOrNull { it.id == id }
    fun item(id: Long) = items.firstOrNull { it.id == id }
    fun run(id: Long) = runs.firstOrNull { it.id == id }

    val grams: Int = Rules.grams(pending)
    val heavy: Boolean = grams > settings.carryKg * 1000

    val owned = settings.owned
    val balance: Int = Rules.balance(runs, owned)
    val stats = Badges.stats(runs, items, prices.size)
    val rank: Rank = Rank.of(stats.runs)
    val badges = Badges.earned(runs, items, prices.size, owned)

    /** On a run: what's due, what's in the bag. */
    val due: List<Item> = open?.let { Rules.due(it, items) }.orEmpty()
    val bag: List<Item> = open?.let { Rules.taken(it, items) }.orEmpty().sortedBy { it.takenAt }

    /** The walk through [shopId] (0 = a shop not in the books): what's due there, in shelf order. */
    fun walk(shopId: Long): List<Item> {
        val here = due.filter { it.shopId == 0L || it.shopId == shopId }
        return Route.order(here, aisles.filter { it.shopId == shopId }, settings.coldLast)
    }

    fun taken(run: Run) = items.filter { it.runId == run.id && it.lineOf == Line.TAKEN }.sortedBy { it.takenAt }

    val notebook = Notebook.rows(prices)
    val months = Archive.months(runs, items)
}

/** Everything a screen can open. */
class Go(private val nav: NavHostController) {
    fun office() { nav.popBackStack("office", false) }
    fun waybill() = nav.navigate("waybill") { launchSingleTop = true }
    fun run() = nav.navigate("run") { launchSingleTop = true; popUpTo("office") }
    fun slip(id: Long, fresh: Boolean = false) = nav.navigate("slip/$id/$fresh") {
        launchSingleTop = true
        if (fresh) popUpTo("office")
    }
    fun archive() = nav.navigate("archive") { launchSingleTop = true }
    fun notebook() = nav.navigate("notebook") { launchSingleTop = true }
    fun depot() = nav.navigate("depot") { launchSingleTop = true }
    fun file() = nav.navigate("file") { launchSingleTop = true }
    fun settings() = nav.navigate("settings") { launchSingleTop = true }
    fun guide() = nav.navigate("guide") { launchSingleTop = true }
    fun back() { nav.popBackStack() }
}

@Composable
fun ErrandRoot(settings: Settings) {
    val context = ctx()
    val container = context.container
    val nav = rememberNavController()
    val start = remember { if (settings.onboarded) "office" else "welcome" }

    val dao = container.db.dao()
    val items by dao.observeItems().collectAsStateWithLifecycle(initialValue = emptyList())
    val runs by dao.observeRuns().collectAsStateWithLifecycle(initialValue = emptyList())
    val clients by dao.observeClients().collectAsStateWithLifecycle(initialValue = emptyList())
    val shops by dao.observeShops().collectAsStateWithLifecycle(initialValue = emptyList())
    val aisles by dao.observeAisles().collectAsStateWithLifecycle(initialValue = emptyList())
    val prices by dao.observePrices().collectAsStateWithLifecycle(initialValue = emptyList())
    val now = rememberNow(30_000)
    val ru = context.ru
    val home = remember(items, runs, clients, shops, aisles, prices, settings, now, ru) { Home(items, runs, clients, shops, aisles, prices, settings, now, ru) }
    val go = remember(nav) { Go(nav) }

    // Onboarded elsewhere (the demo seed): leave the welcome screen.
    val route = nav.currentBackStackEntryAsState().value?.destination?.route
    LaunchedEffect(settings.onboarded, route) {
        if (settings.onboarded && route == "welcome") nav.navigate("office") { popUpTo("welcome") { inclusive = true }; launchSingleTop = true }
    }
    // A list shared in from a messenger: the waybill, with the list to enter.
    val incoming by container.incoming.collectAsStateWithLifecycle()
    LaunchedEffect(incoming, settings.onboarded, route) {
        if (incoming != null && settings.onboarded && route != null && route != "waybill") go.waybill()
    }

    Box(Modifier.fillMaxSize().background(S.c.bg)) {
        NavHost(
            navController = nav,
            startDestination = start,
            modifier = Modifier.background(S.c.bg),
            enterTransition = { fadeIn(tween(240)) },
            exitTransition = { fadeOut(tween(160)) },
            popEnterTransition = { fadeIn(tween(240)) },
            popExitTransition = { fadeOut(tween(160)) },
        ) {
            composable("welcome") { WelcomeScreen { nav.navigate("office") { popUpTo("welcome") { inclusive = true } } } }
            composable("guide") { GuideScreen { nav.popBackStack() } }
            composable("office") { OfficeScreen(home, go) }
            composable("waybill") { WaybillScreen(home, go) }
            composable("run") { RunScreen(home, go) }
            composable("slip/{id}/{fresh}", arguments = listOf(navArgument("id") { type = NavType.LongType }, navArgument("fresh") { type = NavType.BoolType })) { e ->
                SlipScreen(e.arguments?.getLong("id") ?: 0L, e.arguments?.getBoolean("fresh") ?: false, home, go)
            }
            composable("archive") { ArchiveScreen(home, go) }
            composable("notebook") { NotebookScreen(home, go) }
            composable("depot") { DepotScreen(home, go) }
            composable("file") { FileScreen(home, go) }
            composable("settings") { SettingsScreen(home, go) }
        }
    }
}
