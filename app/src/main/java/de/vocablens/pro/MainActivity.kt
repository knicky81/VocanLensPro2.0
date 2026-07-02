package de.vocablens.pro

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import de.vocablens.pro.databinding.ActivityMainBinding
import java.io.File
import java.text.Normalizer
import java.util.Locale
import kotlin.math.max
import kotlin.random.Random

data class Vocab(
    val english: String,
    val example: String,
    val german: String,
    var correct: Int = 0,
    var wrong: Int = 0
)

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {
    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: SharedPreferences
    private val gson = Gson()
    private var tts: TextToSpeech? = null
    private var photoUri: Uri? = null

    private val vocabs = mutableListOf<Vocab>()
    private val quiz = mutableListOf<Vocab>()
    private var quizIndex = 0
    private var current: Vocab? = null
    private var alreadyChecked = false

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        if (r.resultCode == Activity.RESULT_OK) r.data?.data?.let { runOcr(it) }
    }

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        if (r.resultCode == Activity.RESULT_OK) photoUri?.let { runOcr(it) }
    }

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) openCamera() else status("Kamera-Berechtigung fehlt.")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = getSharedPreferences("vocablens", MODE_PRIVATE)
        tts = TextToSpeech(this, this)

        binding.countSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, listOf("10", "20", "30", "Alle"))

        binding.cameraButton.setOnClickListener { requestCamera() }
        binding.galleryButton.setOnClickListener { openGallery() }
        binding.demoButton.setOnClickListener { loadDemo() }
        binding.applyEditorButton.setOnClickListener { applyEditor() }
        binding.saveListButton.setOnClickListener { saveList() }
        binding.loadListButton.setOnClickListener { loadList() }
        binding.startQuizButton.setOnClickListener { startQuiz() }
        binding.checkButton.setOnClickListener { check() }
        binding.nextButton.setOnClickListener { next() }
        binding.speakButton.setOnClickListener { speakEnglish() }

        updateStats()
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) tts?.language = Locale.US
    }

    private fun requestCamera() {
        val ok = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        if (ok) openCamera() else permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    private fun openCamera() {
        val file = File.createTempFile("vocablens_", ".jpg", cacheDir)
        photoUri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
        val intent = Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE).apply {
            putExtra(android.provider.MediaStore.EXTRA_OUTPUT, photoUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        }
        cameraLauncher.launch(intent)
    }

    private fun openGallery() {
        galleryLauncher.launch(Intent(Intent.ACTION_GET_CONTENT).apply { type = "image/*" })
    }

    private fun runOcr(uri: Uri) {
        status("OCR läuft…")
        val image = InputImage.fromFilePath(this, uri)
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            .process(image)
            .addOnSuccessListener {
                val parsed = SmartParser.parse(it)
                vocabs.clear()
                vocabs.addAll(parsed)
                syncEditor()
                status("${vocabs.size} Vokabeln erkannt. Bitte unten prüfen/bearbeiten.")
            }
            .addOnFailureListener { status("OCR-Fehler: ${it.localizedMessage ?: "unbekannt"}") }
    }

    private fun loadDemo() {
        vocabs.clear()
        vocabs.addAll(listOf(
            Vocab("run", "I run every morning.", "laufen"),
            Vocab("take", "I take the bus.", "nehmen"),
            Vocab("decide", "We decide together.", "entscheiden"),
            Vocab("important", "This is an important question.", "wichtig"),
            Vocab("because", "I stayed home because I was tired.", "weil"),
            Vocab("arrive", "They arrive at school.", "ankommen")
        ))
        syncEditor()
        status("Demo geladen.")
    }

    private fun syncEditor() {
        binding.editorText.setText(vocabs.joinToString("\n") { "${it.english} | ${it.example} | ${it.german}" })
    }

    private fun applyEditor() {
        val parsed = binding.editorText.text.toString().lines().mapNotNull { line ->
            val parts = line.split("|").map { it.trim() }
            if (parts.size >= 3 && parts[0].isNotBlank() && parts[2].isNotBlank()) {
                Vocab(parts[0], parts.subList(1, parts.size - 1).joinToString(" | "), parts.last())
            } else null
        }
        vocabs.clear()
        vocabs.addAll(parsed)
        syncEditor()
        status("${vocabs.size} Vokabeln übernommen.")
    }

    private fun saveList() {
        applyEditor()
        val name = binding.listNameInput.text.toString().ifBlank { "Unit 1" }
        prefs.edit().putString("list_$name", gson.toJson(vocabs)).putString("lastList", name).apply()
        status("Liste „$name“ gespeichert.")
    }

    private fun loadList() {
        val name = binding.listNameInput.text.toString().ifBlank { prefs.getString("lastList", "Unit 1") ?: "Unit 1" }
        val json = prefs.getString("list_$name", null)
        if (json == null) {
            status("Keine gespeicherte Liste mit diesem Namen gefunden.")
            return
        }
        val type = object : TypeToken<List<Vocab>>() {}.type
        vocabs.clear()
        vocabs.addAll(gson.fromJson(json, type))
        syncEditor()
        status("Liste „$name“ geladen.")
    }

    private fun startQuiz() {
        applyEditor()
        if (vocabs.isEmpty()) {
            status("Keine Vokabeln vorhanden.")
            return
        }

        var pool = vocabs.toList()
        if (binding.onlyWrongCheck.isChecked) {
            pool = pool.filter { it.wrong > 0 }
            if (pool.isEmpty()) {
                status("Noch keine falschen Wörter vorhanden. Starte erst ein normales Quiz.")
                return
            }
        }

        pool = if (binding.hardWordsCheck.isChecked) {
            pool.sortedByDescending { it.wrong * 2 - it.correct }
        } else {
            pool.shuffled()
        }

        val countRaw = binding.countSpinner.selectedItem.toString()
        val count = if (countRaw == "Alle") pool.size else countRaw.toInt().coerceAtMost(pool.size)

        quiz.clear()
        quiz.addAll(pool.take(count).shuffled(Random(System.currentTimeMillis())))
        quizIndex = 0
        showQuestion()
    }

    private fun showQuestion() {
        current = quiz.getOrNull(quizIndex)
        alreadyChecked = false
        val v = current ?: return
        val deToEn = binding.deToEnRadio.isChecked
        binding.progressText.text = "Frage ${quizIndex + 1} von ${quiz.size}"
        binding.questionText.text = if (deToEn) v.german else v.english
        binding.exampleText.text = if (deToEn) "" else v.example
        binding.answerInput.setText("")
        binding.resultText.text = ""
    }

    private fun check() {
        val v = current ?: return
        if (alreadyChecked) {
            status("Schon gewertet. Bitte weiter.")
            return
        }
        val deToEn = binding.deToEnRadio.isChecked
        val expected = if (deToEn) v.english else v.german
        val ok = AnswerChecker.ok(binding.answerInput.text.toString(), expected)
        if (ok) {
            v.correct++
            binding.resultText.text = "✅ Richtig"
        } else {
            v.wrong++
            binding.resultText.text = "❌ Richtig wäre: $expected"
        }
        alreadyChecked = true
        updateGlobalStats(ok)
        updateStats()
    }

    private fun next() {
        if (quizIndex < quiz.lastIndex) {
            quizIndex++
            showQuestion()
        } else {
            binding.progressText.text = "Quiz beendet."
            binding.resultText.text = "Fertig 🎉"
        }
    }

    private fun speakEnglish() {
        val text = current?.english ?: vocabs.firstOrNull()?.english
        if (text == null) {
            status("Kein englisches Wort vorhanden.")
            return
        }
        tts?.language = Locale.US
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "speak")
    }

    private fun updateGlobalStats(ok: Boolean) {
        val c = prefs.getInt("correct", 0)
        val w = prefs.getInt("wrong", 0)
        prefs.edit()
            .putInt("correct", if (ok) c + 1 else c)
            .putInt("wrong", if (ok) w else w + 1)
            .apply()
    }

    private fun updateStats() {
        val c = prefs.getInt("correct", 0)
        val w = prefs.getInt("wrong", 0)
        val total = c + w
        val rate = if (total == 0) 0 else c * 100 / total
        val wrongLocal = vocabs.count { it.wrong > 0 }
        binding.statsText.text = "Statistik: $c richtig / $w falsch · $rate% · Fehlerwörter in Liste: $wrongLocal"
    }

    private fun status(s: String) {
        binding.statusText.text = s
    }

    override fun onDestroy() {
        tts?.shutdown()
        super.onDestroy()
    }
}

object AnswerChecker {
    fun ok(input: String, expected: String): Boolean {
        val a = norm(input)
        val b = norm(expected)
        if (a == b) return true
        if (stripTo(a) == stripTo(b)) return true
        val aa = stripArticle(stripTo(a))
        val bb = stripArticle(stripTo(b))
        val d = lev(aa, bb)
        val len = max(aa.length, bb.length)
        val allowed = when {
            len <= 4 -> 0
            len <= 8 -> 1
            else -> 2
        }
        return d <= allowed
    }

    private fun norm(s: String): String {
        return Normalizer.normalize(s.lowercase(Locale.ROOT), Normalizer.Form.NFD)
            .replace("\\p{Mn}+".toRegex(), "")
            .replace("[!?.,;:()\\[\\]{}]".toRegex(), " ")
            .replace("\\s+".toRegex(), " ")
            .trim()
    }

    private fun stripTo(s: String) = s.replace("^to\\s+".toRegex(), "").trim()
    private fun stripArticle(s: String) = s.replace("^(a|an|the|der|die|das|ein|eine|einen|einem)\\s+".toRegex(), "").trim()

    private fun lev(a: String, b: String): Int {
        val dp = Array(a.length + 1) { IntArray(b.length + 1) }
        for (i in 0..a.length) dp[i][0] = i
        for (j in 0..b.length) dp[0][j] = j
        for (i in 1..a.length) for (j in 1..b.length) {
            val cost = if (a[i - 1] == b[j - 1]) 0 else 1
            dp[i][j] = minOf(dp[i - 1][j] + 1, dp[i][j - 1] + 1, dp[i - 1][j - 1] + cost)
        }
        return dp[a.length][b.length]
    }
}

object SmartParser {
    fun parse(text: Text): List<Vocab> {
        val rows = mutableListOf<Row>()
        text.textBlocks.forEach { block ->
            block.lines.forEach { line ->
                val words = line.elements.mapNotNull { e ->
                    val b = e.boundingBox
                    if (b == null || e.text.isBlank()) null else Word(e.text.trim(), b.left, b.right, b.top)
                }.sortedBy { it.left }
                if (words.size >= 3) rows.add(Row(words))
            }
        }
        val byBoxes = rows.mapNotNull { parseRowByBigGaps(it) }
        if (byBoxes.size >= 2) return dedupe(byBoxes)
        return parsePlain(text.text)
    }

    private fun parseRowByBigGaps(row: Row): Vocab? {
        val words = row.words
        val gaps = words.zipWithNext().mapIndexed { i, p -> Gap(i, p.second.left - p.first.right) }
            .filter { it.size > 8 }
            .sortedByDescending { it.size }
        if (gaps.size < 2) return null
        val cuts = gaps.take(2).map { it.index }.sorted()
        val en = words.subList(0, cuts[0] + 1).joinToString(" ") { it.text }.clean()
        val ex = words.subList(cuts[0] + 1, cuts[1] + 1).joinToString(" ") { it.text }.clean()
        val de = words.subList(cuts[1] + 1, words.size).joinToString(" ") { it.text }.clean()
        return if (looks(en, ex, de)) Vocab(en, ex, de) else null
    }

    private fun parsePlain(raw: String): List<Vocab> {
        return raw.lines().mapNotNull { line ->
            val parts = line.trim().split("\\s{2,}|\\|".toRegex()).map { it.trim() }.filter { it.isNotBlank() }
            if (parts.size >= 3) {
                val en = parts.first()
                val de = parts.last()
                val ex = parts.drop(1).dropLast(1).joinToString(" ")
                if (looks(en, ex, de)) Vocab(en, ex, de) else null
            } else null
        }
    }

    private fun looks(en: String, ex: String, de: String): Boolean {
        if (en.length !in 1..34 || de.length !in 1..46 || ex.length !in 3..170) return false
        if (en.split(" ").size > 4 || de.split(" ").size > 6) return false
        val bad = listOf("unit", "exercise", "aufgabe", "page", "grammar", "reading", "chapter", "text", "story")
        if (bad.any { en.lowercase(Locale.ROOT).startsWith(it) }) return false
        return en.any { it.isLetter() } && de.any { it.isLetter() }
    }

    private fun dedupe(list: List<Vocab>) = list.distinctBy { it.english.lowercase(Locale.ROOT) + "|" + it.german.lowercase(Locale.ROOT) }
    private fun String.clean() = replace("|", "").replace("\\s+".toRegex(), " ").trim()
}

data class Word(val text: String, val left: Int, val right: Int, val top: Int)
data class Row(val words: List<Word>)
data class Gap(val index: Int, val size: Int)
