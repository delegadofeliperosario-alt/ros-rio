package com.example.domain

import com.example.data.model.ErrorCause
import com.example.data.model.ImportedAttemptDraft
import com.example.data.model.SourceType
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

data class ParsedQConcursosUrl(
    val normalizedUrl: String,
    val questionCode: String,
    val pageKind: String
)

object QConcursosParser {

    const val HOME_URL = "https://www.qconcursos.com"
    const val QUESTOES_URL = "https://www.qconcursos.com/questoes-de-concursos/questoes"
    const val ESTATISTICAS_URL = "https://www.qconcursos.com/usuario/estatisticas"
    const val CADERNOS_URL = "https://www.qconcursos.com/usuario/cadernos"
    const val SIMULADOS_URL = "https://www.qconcursos.com/questoes-de-concursos/simulados"

    private val questionCodeRegex = Regex("""(?:^|[/?=&_-])Q?(\d{5,9})(?:$|[/?&#_-])""", RegexOption.IGNORE_CASE)
    private val explicitQCodeRegex = Regex("""\bQ\d{4,9}\b""", RegexOption.IGNORE_CASE)

    fun parseUrl(rawUrl: String): ParsedQConcursosUrl {
        val trimmed = rawUrl.trim()
        val normalized = when {
            trimmed.isBlank() -> QUESTOES_URL
            trimmed.startsWith("http://") || trimmed.startsWith("https://") -> trimmed
            else -> "https://$trimmed"
        }

        val explicitMatch = explicitQCodeRegex.find(normalized)?.value?.uppercase()
        val pathQuestionMatch = if (normalized.contains("/questoes/", ignoreCase = true)) {
            questionCodeRegex.find(normalized.substringAfter("/questoes/"))
                ?.groupValues?.getOrNull(1)
                ?.let { "Q$it" }
        } else null

        val code = explicitMatch ?: pathQuestionMatch ?: ""
        val kind = when {
            normalized.contains("estatisticas", ignoreCase = true) -> "Estatísticas de Desempenho"
            normalized.contains("cadernos", ignoreCase = true) -> "Cadernos de Questões"
            normalized.contains("simulados", ignoreCase = true) -> "Simulados"
            code.isNotBlank() -> "Questão $code"
            normalized.contains("questoes", ignoreCase = true) -> "Banco de Questões"
            else -> "Portal QConcursos"
        }
        return ParsedQConcursosUrl(normalizedUrl = normalized, questionCode = code, pageKind = kind)
    }

    fun buildSearchUrl(discipline: String, subtopic: String, examBoard: String): String {
        val query = listOf(discipline, subtopic, examBoard)
            .filter { it.isNotBlank() }
            .joinToString(" ")
        val encoded = URLEncoder.encode(query, StandardCharsets.UTF_8.name())
        return "$QUESTOES_URL?search=$encoded"
    }

    /**
     * Parses CSV or semicolon-separated QConcursos exported/pasted tables:
     * Expected columns: Disciplina,Assunto,Subassunto,Banca,Acertos,Erros[,TempoMedioSeg][,CodigoQ][,Microtopico]
     */
    fun parseCsvReport(rawText: String, defaultBoard: String = "Cebraspe"): List<ImportedAttemptDraft> {
        val lines = rawText.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }

        val drafts = mutableListOf<ImportedAttemptDraft>()
        for ((index, line) in lines.withIndex()) {
            val delimiter = when {
                line.contains(';') -> ';'
                line.contains('\t') -> '\t'
                line.contains(',') -> ','
                else -> continue
            }
            val cols = line.split(delimiter).map { it.trim().trim('"') }
            if (cols.size < 4) continue

            // Skip header row
            if (index == 0 && (cols[0].equals("disciplina", ignoreCase = true) ||
                    cols[0].equals("materia", ignoreCase = true))
            ) {
                continue
            }

            val discipline = cols.getOrNull(0).orEmpty()
            val topic = cols.getOrNull(1).orEmpty().ifBlank { discipline }
            val subtopic = cols.getOrNull(2).orEmpty().ifBlank { topic }
            if (discipline.isBlank()) continue

            // Detect numeric columns cleanly
            val fourthInt = cols.getOrNull(3)?.toIntOrNull()
            val (board, correct, wrong, avgSec, qCode, micro) = if (fourthInt != null) {
                // Format: Disciplina;Assunto;Subassunto;Acertos;Erros
                val c = fourthInt.coerceAtLeast(0)
                val w = (cols.getOrNull(4)?.toIntOrNull() ?: 0).coerceAtLeast(0)
                val sec = (cols.getOrNull(5)?.toIntOrNull() ?: 60).coerceIn(5, 600)
                ParsedRow(defaultBoard, c, w, sec, cols.getOrNull(6).orEmpty(), "")
            } else {
                // Format: Disciplina;Assunto;Subassunto;Banca;Acertos;Erros;TempoSeg;CodigoQ;Microtopico
                val b = cols.getOrNull(3).orEmpty().ifBlank { defaultBoard }
                val c = (cols.getOrNull(4)?.toIntOrNull() ?: 0).coerceAtLeast(0)
                val w = (cols.getOrNull(5)?.toIntOrNull() ?: 0).coerceAtLeast(0)
                val sec = (cols.getOrNull(6)?.toIntOrNull() ?: 60).coerceIn(5, 600)
                val code = cols.getOrNull(7).orEmpty()
                val microtopic = cols.getOrNull(8).orEmpty()
                ParsedRow(b, c, w, sec, code, microtopic)
            }

            val total = (correct + wrong).coerceIn(1, 500)
            val safeCorrect = correct.coerceIn(0, total)
            drafts += ImportedAttemptDraft(
                qconcursosCode = qCode.take(40),
                sourceType = SourceType.IMPORT_TEXTO,
                discipline = discipline.take(120),
                topic = topic.take(140),
                subtopic = subtopic.take(160),
                microtopic = micro.take(160),
                examBoard = board.take(80),
                questionsCount = total,
                correctCount = safeCorrect,
                timeSpentSeconds = (avgSec * total).coerceIn(0, 86400),
                attemptNumber = 1,
                errorCause = if (safeCorrect < total) ErrorCause.LACUNA_CONCEITUAL else ErrorCause.NONE,
                notes = "Importado via relatório tabular QConcursos"
            )
        }
        return drafts
    }

    private data class ParsedRow(
        val board: String,
        val correct: Int,
        val wrong: Int,
        val avgSec: Int,
        val code: String,
        val micro: String
    )
}
