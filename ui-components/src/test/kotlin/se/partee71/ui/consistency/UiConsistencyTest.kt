package se.partee71.ui.consistency

import com.lemonappdev.konsist.api.Konsist
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail
import org.junit.Test

/**
 * Regel 4 i biblioteket: samma element ser likadant ut överallt. Mönstren står bara i
 * `ui-forbidden.txt`, undantagen bara i `ui-allowlist.txt` (båda i repots rot), med samma
 * format som apparnas filer.
 */
class UiConsistencyTest {

    private val repoRoot = File("").absoluteFile.let { if (it.name.startsWith("ui-")) it.parentFile else it }
    private val patterns = parsePatterns(File(repoRoot, "ui-forbidden.txt").readText())
    private val allowlist by lazy { parseAllowlist(File(repoRoot, "ui-allowlist.txt").readText()) }
    private val production = Konsist.scopeFromProduction(moduleName = "ui-components") +
        Konsist.scopeFromProduction(moduleName = "ui-theme") +
        Konsist.scopeFromProduction(moduleName = "ui-common") +
        Konsist.scopeFromProduction(moduleName = "ui-testing")

    @Test
    fun `mönsterfilen har alla scope och en ersättning per mönster`() {
        assertEquals(setOf("components", "theme", "expressive"), patterns.keys)
        patterns.values.flatten().forEach { assertTrue(it.replacement.isNotBlank(), it.regex.pattern) }
    }

    @Test
    fun `ingen kod i bibliotekets main använder förbjudna mönster`() {
        val files = production.files.toList()
        assertTrue(listOf("/ui-theme/", "/ui-components/", "/ui-common/", "/ui-testing/").all { module -> files.any { it.path.contains(module) } }, "hittade inte alla moduler")
        val hits = files.flatMap { file ->
            val rel = File(file.path).relativeTo(repoRoot).invariantSeparatorsPath
            findViolations(rel, file.text).map { (line, match, replacement) -> "  $rel:$line  `${match.trim()}`  → använd $replacement" }
        }
        if (hits.isNotEmpty()) {
            fail("Regel 4: förbjudna mönster.\n" + hits.joinToString("\n") + "\nUndantag bara via ui-allowlist.txt med motivering och användarens ok.")
        }
    }

    @Test
    fun `varje undantag används fortfarande`() {
        val texts = production.files.associate { File(it.path).relativeTo(repoRoot).invariantSeparatorsPath to it.text }
        val stale = allowlist.filter { allowed -> texts[allowed.file]?.contains(allowed.symbol) != true }
        assertEquals(emptyList(), stale, "ta bort raderna ur ui-allowlist.txt")
    }

    @Test
    fun `scope per sökväg`() {
        assertEquals(listOf("components", "expressive"), scopesFor("ui-components/src/main/kotlin/se/partee71/ui/components/X.kt"))
        assertEquals(listOf("theme", "expressive"), scopesFor("ui-theme/src/main/kotlin/se/partee71/ui/theme/X.kt"))
        assertEquals(listOf("expressive"), scopesFor("ui-common/src/main/kotlin/se/partee71/ui/common/X.kt"))
        assertEquals(listOf("expressive"), scopesFor("ui-testing/src/main/kotlin/se/partee71/ui/testing/X.kt"))
        assertEquals(emptyList(), scopesFor("ui-components/src/test/kotlin/se/partee71/ui/components/X.kt"))
    }

    @Test
    fun `mönstren fäller hårdkodat utseende men inte temats värden`() {
        val file = "ui-components/src/main/kotlin/se/partee71/ui/components/X.kt"
        listOf(
            "Box(Modifier.background(Color(0xFFFFFFFF)))",
            "val shape = RoundedCornerShape(12.dp)",
            "Text(text, fontSize = 14.sp)",
            "Modifier.padding(horizontal = 12.dp)",
            "Modifier.size(20.dp)",
            "style = MaterialTheme.typography.bodyLarge"
        ).forEach { assertTrue(findViolations(file, it).isNotEmpty(), it) }
        listOf(
            "Box(Modifier.background(AppTheme.extendedColors.card, AppTheme.shapes.row))",
            "Text(text, style = AppTheme.typography.button)",
            "Modifier.padding(horizontal = Spacing.m).size(ICON_SIZE)",
            "// Color(0xFF000000) i en kommentar"
        ).forEach { assertEquals(emptyList(), findViolations(file, it), it) }
    }

    /** En komponent ritar något: en publik `@Composable` som returnerar `Unit`. En som ger ett värde (t.ex. `chipColors()`) är ingen komponent. */
    @Test
    fun `varje publik komponent har skärmdump ljust och mörkt och står i README`() {
        val components = production.functions()
            .filter { it.resideInPackage("..ui.components..") && it.hasPublicOrDefaultModifier && it.hasAnnotationWithName("Composable") }
            .filter { it.returnType.let { type -> type == null || type.name == "Unit" } }
            .map { it.name }
            .toSet()
        assertTrue(components.isNotEmpty(), "hittade inga komponenter")
        val screenshots = File(repoRoot, "ui-components/src/test/screenshots").list().orEmpty().toSet()
        val readme = File(repoRoot, "README.md").readText()
        val problems = components.flatMap { name ->
            listOfNotNull(
                "$name saknar skärmdump ${name}_*_light.png".takeIf { screenshots.none { it.startsWith("${name}_") && it.endsWith("_light.png") } },
                "$name saknar skärmdump ${name}_*_dark.png".takeIf { screenshots.none { it.startsWith("${name}_") && it.endsWith("_dark.png") } },
                "$name saknas i README:s komponentlista".takeIf { !readme.contains("| `$name` |") }
            )
        }
        assertTrue(problems.isEmpty(), problems.joinToString("\n"))
    }

    private data class Rule(val regex: Regex, val replacement: String)

    private data class Allowed(val file: String, val symbol: String)

    private fun scopesFor(relPath: String): List<String> {
        if (!relPath.endsWith(".kt") || !relPath.contains("/src/main/")) return emptyList()
        return listOfNotNull(
            "components".takeIf { relPath.startsWith("ui-components/") },
            "theme".takeIf { relPath.startsWith("ui-theme/") },
            "expressive"
        )
    }

    private fun findViolations(relPath: String, content: String): List<Triple<Int, String, String>> {
        val active = scopesFor(relPath).filter { it in patterns }
        return content.lines().flatMapIndexed { i, line ->
            val trimmed = line.trim()
            if (trimmed.startsWith("//") || trimmed.startsWith("*") || trimmed.startsWith("import ")) return@flatMapIndexed emptyList()
            active.flatMap { scope ->
                patterns.getValue(scope).mapNotNull { rule ->
                    val match = rule.regex.find(line) ?: return@mapNotNull null
                    val allowed = allowlist.any { it.file == relPath && (match.value.contains(it.symbol) || it.symbol.contains(match.value.trim())) }
                    if (allowed) null else Triple(i + 1, match.value, rule.replacement)
                }
            }
        }
    }

    /** Sektionerad fil: `[namn]` följt av `<regex><TAB><ersättning>`; tomma rader och `#`-rader hoppas över. */
    private fun parsePatterns(text: String): Map<String, List<Rule>> {
        val sections = linkedMapOf<String, MutableList<Rule>>()
        var current: MutableList<Rule>? = null
        text.lines().filter { it.isNotBlank() && !it.startsWith("#") }.forEach { line ->
            val section = Regex("""^\[([\w-]+)]\s*$""").find(line)
            if (section != null) {
                current = mutableListOf<Rule>().also { sections[section.groupValues[1]] = it }
            } else {
                val parts = line.split("\t")
                current?.add(Rule(Regex(parts[0]), parts.getOrElse(1) { "" }))
            }
        }
        return sections
    }

    /** Format per rad: `<sökväg>:<symbol> – <motivering>`. En rad utan symbol eller motivering är ett fel. */
    private fun parseAllowlist(text: String): List<Allowed> = text.lines().mapIndexedNotNull { i, line ->
        if (line.isBlank() || line.startsWith("#")) return@mapIndexedNotNull null
        val parts = line.split(Regex("""\s+[–-]\s+"""))
        val idx = parts[0].lastIndexOf(':')
        val file = if (idx > 0) parts[0].substring(0, idx).trim() else ""
        val symbol = if (idx > 0) parts[0].substring(idx + 1).trim() else ""
        require(file.isNotEmpty() && symbol.isNotEmpty() && parts.drop(1).joinToString("").isNotBlank()) {
            "ui-allowlist.txt rad ${i + 1}: ogiltig rad, formatet är \"<sökväg>:<symbol> – <motivering>\""
        }
        Allowed(file, symbol)
    }
}
