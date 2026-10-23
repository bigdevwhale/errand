package app.errand

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider
import app.errand.data.DemoSeed
import app.errand.domain.Rules
import app.errand.domain.Upgrade
import app.errand.ui.Go
import app.errand.ui.Home
import app.errand.ui.archive.ArchiveScreen
import app.errand.ui.art.Art
import app.errand.ui.depot.DepotScreen
import app.errand.ui.file.FileScreen
import app.errand.ui.notebook.NotebookScreen
import app.errand.ui.office.OfficeScreen
import app.errand.ui.onboarding.GuideScreen
import app.errand.ui.onboarding.WelcomeScreen
import app.errand.ui.run.RunScreen
import app.errand.ui.slip.SlipScreen
import app.errand.ui.theme.ErrandTheme
import app.errand.ui.theme.S
import app.errand.ui.waybill.WaybillScreen
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Every main screen over the demo office, drawn to PNGs (run with `-Prender`), in both languages. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
abstract class ScreensBase {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()
    abstract val lang: String
    private val out by lazy { File(System.getProperty("render.out") ?: "build/art", "screens-$lang").apply { mkdirs() } }

    @Before fun still() {
        Art.still = true
    }

    private val app get() = ApplicationProvider.getApplicationContext<ErrandApp>()

    private fun home(onRun: Boolean = false, hour: Int? = null): Home = runBlocking {
        val c = app.container
        DemoSeed.seedIfEmpty(app, c)
        val dao = c.db.dao()
        if (onRun && dao.runs().none { it.leg == "OUT" }) {
            // Halfway round the corner shop: four things in the bag, one shelf empty.
            val shop = dao.shops().first().id
            val runId = c.office.start(if (app.ru) 250_000 else 6_000, listOf(shop), System.currentTimeMillis() - 14 * Rules.MINUTE)
            val run = dao.run(runId)!!
            val h = Home(dao.items(), dao.runs(), dao.clients(), dao.shops(), emptyList(), dao.prices(), c.settings.now(), System.currentTimeMillis(), app.ru)
            val walk = h.walk(shop)
            val last = dao.prices().groupBy { it.key }.mapValues { it.value.last().price }
            walk.take(4).forEachIndexed { i, it -> c.office.take(it, run, shop, if (i == 1) 0 else last[it.key] ?: 0, System.currentTimeMillis() - (10 - i) * Rules.MINUTE) }
            walk.getOrNull(5)?.let { c.office.out(dao.item(it.id)!!, run, true) }
        }
        val now = hour?.let { java.time.LocalDate.now().atTime(it, 12).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli() } ?: System.currentTimeMillis()
        Home(dao.items(), dao.runs(), dao.clients(), dao.shops(), dao.aisles(dao.shops().first().id) + dao.aisles(dao.shops()[1].id), dao.prices(), c.settings.now(), now, app.ru)
    }

    private fun shot(name: String, h: Home, content: @Composable (Home, Go) -> Unit) {
        rule.mainClock.autoAdvance = false
        rule.setContent {
            val nav = rememberNavController()
            ErrandTheme { Box(Modifier.fillMaxSize().background(S.c.bg)) { content(h, Go(nav)) } }
        }
        rule.mainClock.advanceTimeBy(2_000)
        rule.waitForIdle()
        rule.mainClock.advanceTimeBy(500)
        rule.runOnUiThread {
            val view = rule.activity.window.decorView
            val image = android.graphics.Bitmap.createBitmap(view.width, view.height, android.graphics.Bitmap.Config.ARGB_8888)
            view.draw(android.graphics.Canvas(image))
            File(out, "$name.png").outputStream().use { image.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        }
    }

    @Test fun welcome() = home().let { shot("0-welcome", it) { _, _ -> WelcomeScreen {} } }
    @Test fun guide1() = home().let { shot("0-guide-1", it) { _, _ -> GuideScreen(startPage = 0) {} } }
    @Test fun guide2() = home().let { shot("0-guide-2", it) { _, _ -> GuideScreen(startPage = 1) {} } }
    @Test fun guide3() = home().let { shot("0-guide-3", it) { _, _ -> GuideScreen(startPage = 2) {} } }
    @Test fun guide4() = home().let { shot("0-guide-4", it) { _, _ -> GuideScreen(startPage = 3) {} } }
    @Test fun guide5() = home().let { shot("0-guide-5", it) { _, _ -> GuideScreen(startPage = 4) {} } }
    @Test fun office() = home(hour = 11).let { shot("1-office", it) { h, go -> OfficeScreen(h, go) } }
    @Test fun waybill() = home().let { shot("2-waybill", it) { h, go -> WaybillScreen(h, go) } }
    @Test fun accept() = home().let { shot("2-accept", it) { h, go -> WaybillScreen(h, go, startAccept = true) } }
    @Test fun run() = home(onRun = true).let { h -> shot("3-run", h) { hh, go -> RunScreen(hh, go, startShop = hh.shops.first().id) } }
    @Test fun slip() = home().let { h -> shot("4-slip", h) { hh, go -> SlipScreen(hh.done.first().id, false, hh, go) } }
    @Test fun archive() = home().let { shot("5-archive", it) { h, go -> ArchiveScreen(h, go) } }
    @Test fun notebook() = home().let { shot("6-notebook", it) { h, go -> NotebookScreen(h, go) } }
    @Test fun depot() = home(hour = 19).let { shot("7-depot", it) { h, go -> DepotScreen(h, go, startPreview = Upgrade.SCOOTER) } }
    @Test fun file() = home().let { shot("8-file", it) { h, go -> FileScreen(h, go) } }
    @Test fun evening() = home(hour = 21).let { shot("9-evening", it) { h, go -> OfficeScreen(h, go) } }
}

class RenderArtScreensEn : ScreensBase() {
    override val lang = "en"
}

@Config(qualifiers = "ru-w411dp-h891dp-xxhdpi")
class RenderArtScreensRu : ScreensBase() {
    override val lang = "ru"
}
