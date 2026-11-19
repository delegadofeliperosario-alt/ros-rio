package com.example.domain

import com.example.data.model.EditalItem
import com.example.data.model.EditalWeight
import com.example.data.model.ErrorCause
import com.example.data.model.MasteryLevel
import com.example.data.model.QuestionAttempt
import com.example.data.model.UserProfile
import java.text.Normalizer
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

data class MicrotopicMetrics(
    val discipline: String,
    val topic: String,
    val subtopic: String,
    val microtopic: String,
    val totalQuestions: Int,
    val correctQuestions: Int,
    val wrongQuestions: Int,
    val accuracyPercent: Double,
    val avgTimeSeconds: Int,
    val mastery: MasteryLevel,
    val dominantErrorCause: ErrorCause
) {
    val errorPercent: Double get() = if (totalQuestions > 0) 100.0 - accuracyPercent else 0.0
}

data class SubtopicMetrics(
    val discipline: String,
    val topic: String,
    val subtopic: String,
    val totalQuestions: Int,
    val correctQuestions: Int,
    val wrongQuestions: Int,
    val accuracyPercent: Double,
    val avgTimeSeconds: Int,
    val repeatErrorCount: Int,
    val mastery: MasteryLevel,
    val severityScore: Double,
    val dominantErrorCause: ErrorCause,
    val inferredCauseLabel: String,
    val microtopics: List<MicrotopicMetrics>
) {
    val errorPercent: Double get() = if (totalQuestions > 0) 100.0 - accuracyPercent else 0.0
}

data class TopicMetrics(
    val discipline: String,
    val topic: String,
    val totalQuestions: Int,
    val correctQuestions: Int,
    val wrongQuestions: Int,
    val accuracyPercent: Double,
    val avgTimeSeconds: Int,
    val mastery: MasteryLevel,
    val subtopics: List<SubtopicMetrics>
) {
    val errorPercent: Double get() = if (totalQuestions > 0) 100.0 - accuracyPercent else 0.0
}

data class DisciplineMetrics(
    val discipline: String,
    val totalQuestions: Int,
    val correctQuestions: Int,
    val wrongQuestions: Int,
    val accuracyPercent: Double,
    val avgTimeSeconds: Int,
    val mastery: MasteryLevel,
    val topics: List<TopicMetrics>
) {
    val errorPercent: Double get() = if (totalQuestions > 0) 100.0 - accuracyPercent else 0.0
}

data class EditalSubtopicNode(
    val itemId: String,
    val editalTitle: String,
    val discipline: String,
    val topic: String,
    val subtopic: String,
    val weight: EditalWeight,
    val totalQuestions: Int,
    val correctQuestions: Int,
    val wrongQuestions: Int,
    val accuracyPercent: Double,
    val errorPercent: Double,
    val mastery: MasteryLevel?, // null means not yet studied (0 questions)
    val studied: Boolean
)

data class EditalTopicNode(
    val discipline: String,
    val topic: String,
    val subtopics: List<EditalSubtopicNode>,
    val totalQuestions: Int,
    val correctQuestions: Int,
    val wrongQuestions: Int,
    val accuracyPercent: Double,
    val errorPercent: Double,
    val studiedCount: Int
)

data class EditalDisciplineNode(
    val discipline: String,
    val topics: List<EditalTopicNode>,
    val totalSubtopics: Int,
    val studiedSubtopics: Int,
    val totalQuestions: Int,
    val correctQuestions: Int,
    val wrongQuestions: Int,
    val accuracyPercent: Double,
    val errorPercent: Double,
    val mastery: MasteryLevel?
)

data class EditalCoverageSummary(
    val editalTitle: String = "",
    val totalItems: Int = 0,
    val studiedItems: Int = 0,
    val masteredItems: Int = 0,
    val criticalItems: Int = 0,
    val coveragePercent: Double = 0.0,
    val disciplines: List<EditalDisciplineNode> = emptyList(),
    val unstudiedHighWeightItems: List<EditalSubtopicNode> = emptyList()
)

data class DailyEvolutionPoint(
    val dayKey: String,
    val shortLabel: String,
    val totalQuestions: Int,
    val correctQuestions: Int,
    val accuracyPercent: Double
)

data class RepeatedErrorItem(
    val identifier: String,
    val discipline: String,
    val topic: String,
    val subtopic: String,
    val occurrences: Int,
    val lastNotes: String
)

data class ErrorEtiologySummary(
    val totalErrors: Int,
    val knowledgeGapCount: Int,
    val inattentionCount: Int,
    val boardTrapCount: Int,
    val forgettingCount: Int,
    val knowledgeGapPercent: Double,
    val inattentionPercent: Double,
    val primaryDiagnosis: String
)

data class PerformanceAnalyticsSnapshot(
    val totalQuestions: Int = 0,
    val totalCorrect: Int = 0,
    val totalWrong: Int = 0,
    val overallAccuracyPercent: Double = 0.0,
    val overallErrorPercent: Double = 0.0,
    val overallMastery: MasteryLevel = MasteryLevel.INTERMEDIARIO,
    val avgTimeSecondsPerQuestion: Int = 0,
    val todayQuestions: Int = 0,
    val studyStreakDays: Int = 0,
    val disciplines: List<DisciplineMetrics> = emptyList(),
    val rankedDifficultSubtopics: List<SubtopicMetrics> = emptyList(),
    val masteredSubtopics: List<SubtopicMetrics> = emptyList(),
    val repeatedErrors: List<RepeatedErrorItem> = emptyList(),
    val errorEtiology: ErrorEtiologySummary = ErrorEtiologySummary(0, 0, 0, 0, 0, 0.0, 0.0, ""),
    val dailyEvolution: List<DailyEvolutionPoint> = emptyList(),
    val editalCoverage: EditalCoverageSummary = EditalCoverageSummary(),
    val quickMentorHeadline: String = ""
)

object PerformanceAnalyticsEngine {

    fun analyze(
        attempts: List<QuestionAttempt>,
        editalItems: List<EditalItem>,
        profile: UserProfile?
    ): PerformanceAnalyticsSnapshot {
        val editalCoverage = computeEditalCoverage(editalItems, attempts)

        if (attempts.isEmpty()) {
            return PerformanceAnalyticsSnapshot(
                editalCoverage = editalCoverage
            )
        }

        val totalQuestions = attempts.sumOf { it.questionsCount.toInt() }
        val totalCorrect = attempts.sumOf { it.correctCount.toInt() }
        val totalWrong = (totalQuestions - totalCorrect).coerceAtLeast(0)
        val overallAccuracy = if (totalQuestions > 0) (totalCorrect * 100.0) / totalQuestions else 0.0
        val overallError = if (totalQuestions > 0) 100.0 - overallAccuracy else 0.0
        val totalTime = attempts.sumOf { it.timeSpentSeconds.toInt() }
        val avgTimePerQ = if (totalQuestions > 0) (totalTime.toDouble() / totalQuestions).roundToInt() else 0

        val dayFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val shortDayFormatter = SimpleDateFormat("dd/MM", Locale.forLanguageTag("pt-BR"))
        val todayKey = dayFormatter.format(Date())

        val attemptsWithDate = attempts.map { attempt ->
            val date = attempt.createdAt?.toDate() ?: Date()
            attempt to dayFormatter.format(date)
        }

        val todayQuestions = attemptsWithDate
            .filter { it.second == todayKey }
            .sumOf { it.first.questionsCount.toInt() }

        val activeDayKeys = attemptsWithDate.map { it.second }.toSet()
        val streakDays = computeStreakDays(activeDayKeys, dayFormatter)

        val dailyEvolution = attemptsWithDate
            .groupBy { it.second }
            .toSortedMap()
            .entries
            .toList()
            .takeLast(14)
            .map { (dayKey, items) ->
                val q = items.sumOf { it.first.questionsCount.toInt() }
                val c = items.sumOf { it.first.correctCount.toInt() }
                val acc = if (q > 0) (c * 100.0) / q else 0.0
                val parsedDate = runCatching { dayFormatter.parse(dayKey) }.getOrNull() ?: Date()
                DailyEvolutionPoint(
                    dayKey = dayKey,
                    shortLabel = shortDayFormatter.format(parsedDate),
                    totalQuestions = q,
                    correctQuestions = c,
                    accuracyPercent = acc
                )
            }

        val disciplineMetrics = attempts
            .groupBy { it.discipline.ifBlank { "Geral" } }
            .map { (discipline, discAttempts) ->
                val discQ = discAttempts.sumOf { it.questionsCount.toInt() }
                val discC = discAttempts.sumOf { it.correctCount.toInt() }
                val discW = (discQ - discC).coerceAtLeast(0)
                val discAcc = if (discQ > 0) (discC * 100.0) / discQ else 0.0
                val discAvgTime = if (discQ > 0) {
                    (discAttempts.sumOf { it.timeSpentSeconds.toDouble() } / discQ).roundToInt()
                } else 0

                val topics = discAttempts
                    .groupBy { it.topic.ifBlank { "Assunto Geral" } }
                    .map { (topic, topicAttempts) ->
                        val topQ = topicAttempts.sumOf { it.questionsCount.toInt() }
                        val topC = topicAttempts.sumOf { it.correctCount.toInt() }
                        val topW = (topQ - topC).coerceAtLeast(0)
                        val topAcc = if (topQ > 0) (topC * 100.0) / topQ else 0.0
                        val topAvgTime = if (topQ > 0) {
                            (topicAttempts.sumOf { it.timeSpentSeconds.toDouble() } / topQ).roundToInt()
                        } else 0

                        val subtopics = topicAttempts
                            .groupBy { it.subtopic.ifBlank { topic } }
                            .map { (subtopic, subAttempts) ->
                                buildSubtopicMetrics(discipline, topic, subtopic, subAttempts)
                            }
                            .sortedByDescending { it.severityScore }

                        TopicMetrics(
                            discipline = discipline,
                            topic = topic,
                            totalQuestions = topQ,
                            correctQuestions = topC,
                            wrongQuestions = topW,
                            accuracyPercent = topAcc,
                            avgTimeSeconds = topAvgTime,
                            mastery = MasteryLevel.fromAccuracy(topAcc),
                            subtopics = subtopics
                        )
                    }
                    .sortedBy { it.accuracyPercent }

                DisciplineMetrics(
                    discipline = discipline,
                    totalQuestions = discQ,
                    correctQuestions = discC,
                    wrongQuestions = discW,
                    accuracyPercent = discAcc,
                    avgTimeSeconds = discAvgTime,
                    mastery = MasteryLevel.fromAccuracy(discAcc),
                    topics = topics
                )
            }
            .sortedBy { it.accuracyPercent }

        val allSubtopics = disciplineMetrics.flatMap { d -> d.topics.flatMap { t -> t.subtopics } }
        val rankedDifficult = allSubtopics
            .filter { it.wrongQuestions > 0 || it.accuracyPercent < 78.0 }
            .sortedByDescending { it.severityScore }
        val mastered = allSubtopics
            .filter { it.accuracyPercent >= 78.0 && it.totalQuestions >= 1 }
            .sortedByDescending { it.accuracyPercent }

        val repeatedErrors = detectRepeatedErrors(attempts)
        val etiology = computeErrorEtiology(attempts, allSubtopics)

        val studentFirstName = profile?.studentName
            ?.trim()
            ?.substringBefore(" ")
            ?.ifBlank { "Estudante" }
            ?: "Estudante"

        val headline = buildQuickHeadline(studentFirstName, rankedDifficult.firstOrNull())

        return PerformanceAnalyticsSnapshot(
            totalQuestions = totalQuestions,
            totalCorrect = totalCorrect,
            totalWrong = totalWrong,
            overallAccuracyPercent = overallAccuracy,
            overallErrorPercent = overallError,
            overallMastery = MasteryLevel.fromAccuracy(overallAccuracy),
            avgTimeSecondsPerQuestion = avgTimePerQ,
            todayQuestions = todayQuestions,
            studyStreakDays = streakDays,
            disciplines = disciplineMetrics,
            rankedDifficultSubtopics = rankedDifficult,
            masteredSubtopics = mastered,
            repeatedErrors = repeatedErrors,
            errorEtiology = etiology,
            dailyEvolution = dailyEvolution,
            editalCoverage = editalCoverage,
            quickMentorHeadline = headline
        )
    }

    /**
     * Cruza automaticamente os itens do Edital cadastrado (Disciplina -> Assunto -> Subassunto)
     * com o histórico real de questões resolvidas, usando normalização sem acentos/maiúsculas.
     */
    private fun computeEditalCoverage(
        editalItems: List<EditalItem>,
        attempts: List<QuestionAttempt>
    ): EditalCoverageSummary {
        if (editalItems.isEmpty()) return EditalCoverageSummary()

        val title = editalItems.firstOrNull { it.editalTitle.isNotBlank() }?.editalTitle ?: "Edital Cadastrado"

        val subtopicNodes = editalItems.map { item ->
            val normDisc = normalizeKey(item.discipline)
            val normTopic = normalizeKey(item.topic)
            val normSub = normalizeKey(item.subtopic)

            val matched = attempts.filter { att ->
                val aDisc = normalizeKey(att.discipline)
                val aTopic = normalizeKey(att.topic)
                val aSub = normalizeKey(att.subtopic)
                val aMicro = normalizeKey(att.microtopic)

                val discMatch = aDisc == normDisc || aDisc.contains(normDisc) || normDisc.contains(aDisc)
                val subMatch = aSub == normSub || aSub.contains(normSub) || normSub.contains(aSub) ||
                    (aMicro.isNotBlank() && (aMicro.contains(normSub) || normSub.contains(aMicro))) ||
                    (normSub == normTopic && (aTopic == normTopic || aTopic.contains(normTopic)))
                discMatch && subMatch
            }

            val q = matched.sumOf { it.questionsCount.toInt() }
            val c = matched.sumOf { it.correctCount.toInt() }
            val w = (q - c).coerceAtLeast(0)
            val acc = if (q > 0) (c * 100.0) / q else 0.0
            val err = if (q > 0) 100.0 - acc else 0.0
            EditalSubtopicNode(
                itemId = item.id,
                editalTitle = item.editalTitle,
                discipline = item.discipline,
                topic = item.topic,
                subtopic = item.subtopic,
                weight = EditalWeight.fromWire(item.weight),
                totalQuestions = q,
                correctQuestions = c,
                wrongQuestions = w,
                accuracyPercent = acc,
                errorPercent = err,
                mastery = if (q > 0) MasteryLevel.fromAccuracy(acc) else null,
                studied = q > 0
            )
        }

        val disciplines = subtopicNodes
            .groupBy { it.discipline }
            .map { (disc, nodes) ->
                val topics = nodes
                    .groupBy { it.topic }
                    .map { (topic, tNodes) ->
                        val tQ = tNodes.sumOf { it.totalQuestions }
                        val tC = tNodes.sumOf { it.correctQuestions }
                        val tW = (tQ - tC).coerceAtLeast(0)
                        val tAcc = if (tQ > 0) (tC * 100.0) / tQ else 0.0
                        EditalTopicNode(
                            discipline = disc,
                            topic = topic,
                            subtopics = tNodes,
                            totalQuestions = tQ,
                            correctQuestions = tC,
                            wrongQuestions = tW,
                            accuracyPercent = tAcc,
                            errorPercent = if (tQ > 0) 100.0 - tAcc else 0.0,
                            studiedCount = tNodes.count { it.studied }
                        )
                    }
                val dQ = nodes.sumOf { it.totalQuestions }
                val dC = nodes.sumOf { it.correctQuestions }
                val dW = (dQ - dC).coerceAtLeast(0)
                val dAcc = if (dQ > 0) (dC * 100.0) / dQ else 0.0
                EditalDisciplineNode(
                    discipline = disc,
                    topics = topics,
                    totalSubtopics = nodes.size,
                    studiedSubtopics = nodes.count { it.studied },
                    totalQuestions = dQ,
                    correctQuestions = dC,
                    wrongQuestions = dW,
                    accuracyPercent = dAcc,
                    errorPercent = if (dQ > 0) 100.0 - dAcc else 0.0,
                    mastery = if (dQ > 0) MasteryLevel.fromAccuracy(dAcc) else null
                )
            }

        val totalItems = subtopicNodes.size
        val studiedItems = subtopicNodes.count { it.studied }
        val masteredItems = subtopicNodes.count { it.studied && it.accuracyPercent >= 78.0 }
        val criticalItems = subtopicNodes.count { it.studied && it.accuracyPercent < 65.0 }
        val coveragePct = if (totalItems > 0) (studiedItems * 100.0) / totalItems else 0.0
        val unstudiedHighWeight = subtopicNodes.filter { !it.studied && it.weight == EditalWeight.ALTA }

        return EditalCoverageSummary(
            editalTitle = title,
            totalItems = totalItems,
            studiedItems = studiedItems,
            masteredItems = masteredItems,
            criticalItems = criticalItems,
            coveragePercent = coveragePct,
            disciplines = disciplines,
            unstudiedHighWeightItems = unstudiedHighWeight
        )
    }

    private fun normalizeKey(raw: String): String {
        val decomposed = Normalizer.normalize(raw.trim().lowercase(Locale.ROOT), Normalizer.Form.NFD)
        return decomposed.replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
            .replace(Regex("[^a-z0-9]+"), " ")
            .trim()
    }

    private fun buildSubtopicMetrics(
        discipline: String,
        topic: String,
        subtopic: String,
        subAttempts: List<QuestionAttempt>
    ): SubtopicMetrics {
        val q = subAttempts.sumOf { it.questionsCount.toInt() }
        val c = subAttempts.sumOf { it.correctCount.toInt() }
        val w = (q - c).coerceAtLeast(0)
        val acc = if (q > 0) (c * 100.0) / q else 0.0
        val avgTime = if (q > 0) {
            (subAttempts.sumOf { it.timeSpentSeconds.toDouble() } / q).roundToInt()
        } else 0
        val mastery = MasteryLevel.fromAccuracy(acc)

        val repeatErrors = subAttempts.count { it.wrongCount > 0 && it.attemptNumber > 1 } +
            (subAttempts.count { it.wrongCount > 0 } - 1).coerceAtLeast(0)

        val dominantCause = subAttempts
            .filter { it.wrongCount > 0 }
            .map { ErrorCause.fromWire(it.errorCause) }
            .filter { it != ErrorCause.NONE }
            .groupingBy { it }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key ?: inferBehavioralCause(acc, avgTime, repeatErrors)

        val inferredCauseLabel = when {
            dominantCause == ErrorCause.DESATENCAO || (acc >= 75.0 && avgTime in 1..30 && w > 0) ->
                "Desatenção / Leitura precipitada"
            dominantCause == ErrorCause.PEGADINHA_BANCA ->
                "Pegadinha recorrente da banca"
            dominantCause == ErrorCause.ESQUECIMENTO ->
                "Curva de esquecimento (revisão pendente)"
            dominantCause == ErrorCause.DUVIDA_ALTERNATIVAS ->
                "Insegurança conceitual entre alternativas"
            else -> "Lacuna teórica / Falta de domínio"
        }

        val errorRate = 100.0 - acc
        val confidenceFactor = (q.coerceAtMost(20).toDouble() / 6.0).coerceIn(0.6, 2.2)
        val severityScore = (errorRate * 0.65) +
            (mastery.severityWeight * 6.0) +
            (repeatErrors * 5.0) +
            (w * 1.4 * confidenceFactor)

        val microtopics = subAttempts
            .filter { it.microtopic.isNotBlank() }
            .groupBy { it.microtopic }
            .map { (micro, microAttempts) ->
                val mq = microAttempts.sumOf { it.questionsCount.toInt() }
                val mc = microAttempts.sumOf { it.correctCount.toInt() }
                val mw = (mq - mc).coerceAtLeast(0)
                val mAcc = if (mq > 0) (mc * 100.0) / mq else 0.0
                val mTime = if (mq > 0) {
                    (microAttempts.sumOf { it.timeSpentSeconds.toDouble() } / mq).roundToInt()
                } else 0
                val mCause = microAttempts
                    .filter { it.wrongCount > 0 }
                    .map { ErrorCause.fromWire(it.errorCause) }
                    .firstOrNull { it != ErrorCause.NONE }
                    ?: inferBehavioralCause(mAcc, mTime, 0)

                MicrotopicMetrics(
                    discipline = discipline,
                    topic = topic,
                    subtopic = subtopic,
                    microtopic = micro,
                    totalQuestions = mq,
                    correctQuestions = mc,
                    wrongQuestions = mw,
                    accuracyPercent = mAcc,
                    avgTimeSeconds = mTime,
                    mastery = MasteryLevel.fromAccuracy(mAcc),
                    dominantErrorCause = mCause
                )
            }
            .sortedBy { it.accuracyPercent }

        return SubtopicMetrics(
            discipline = discipline,
            topic = topic,
            subtopic = subtopic,
            totalQuestions = q,
            correctQuestions = c,
            wrongQuestions = w,
            accuracyPercent = acc,
            avgTimeSeconds = avgTime,
            repeatErrorCount = repeatErrors,
            mastery = mastery,
            severityScore = severityScore,
            dominantErrorCause = dominantCause,
            inferredCauseLabel = inferredCauseLabel,
            microtopics = microtopics
        )
    }

    private fun inferBehavioralCause(acc: Double, avgTimeSeconds: Int, repeatErrors: Int): ErrorCause {
        return when {
            acc >= 75.0 && avgTimeSeconds in 1..30 -> ErrorCause.DESATENCAO
            repeatErrors >= 2 || acc < 60.0 -> ErrorCause.LACUNA_CONCEITUAL
            avgTimeSeconds > 95 -> ErrorCause.DUVIDA_ALTERNATIVAS
            else -> ErrorCause.ESQUECIMENTO
        }
    }

    private fun detectRepeatedErrors(attempts: List<QuestionAttempt>): List<RepeatedErrorItem> {
        val result = mutableListOf<RepeatedErrorItem>()
        val byCode = attempts
            .filter { it.qconcursosCode.isNotBlank() && it.wrongCount > 0 }
            .groupBy { it.qconcursosCode.uppercase(Locale.US) }

        byCode.forEach { (code, list) ->
            val maxAttempt = list.maxOf { it.attemptNumber.toInt() }
            if (list.size >= 2 || maxAttempt >= 2) {
                val latest = list.first()
                result += RepeatedErrorItem(
                    identifier = code,
                    discipline = latest.discipline,
                    topic = latest.topic,
                    subtopic = latest.subtopic,
                    occurrences = maxOf(list.size, maxAttempt),
                    lastNotes = latest.notes
                )
            }
        }

        attempts
            .filter { it.wrongCount > 0 }
            .groupBy { Triple(it.discipline, it.topic, it.subtopic) }
            .forEach { (key, list) ->
                val totalWrongs = list.sumOf { it.wrongCount.toInt() }
                if (list.size >= 2 || totalWrongs >= 3) {
                    val alreadyCovered = result.any {
                        it.discipline == key.first && it.subtopic == key.third
                    }
                    if (!alreadyCovered) {
                        result += RepeatedErrorItem(
                            identifier = key.third,
                            discipline = key.first,
                            topic = key.second,
                            subtopic = key.third,
                            occurrences = totalWrongs,
                            lastNotes = list.firstOrNull { it.notes.isNotBlank() }?.notes.orEmpty()
                        )
                    }
                }
            }

        return result.sortedByDescending { it.occurrences }.take(10)
    }

    private fun computeErrorEtiology(
        attempts: List<QuestionAttempt>,
        subtopics: List<SubtopicMetrics>
    ): ErrorEtiologySummary {
        var knowledgeGap = 0
        var inattention = 0
        var boardTrap = 0
        var forgetting = 0

        val subtopicAccuracyMap = subtopics.associateBy(
            keySelector = { Triple(it.discipline, it.topic, it.subtopic) },
            valueTransform = { it.accuracyPercent }
        )

        attempts.forEach { attempt ->
            val wrongs = attempt.wrongCount.toInt()
            if (wrongs <= 0) return@forEach

            val cause = ErrorCause.fromWire(attempt.errorCause)
            val avgPerQ = if (attempt.questionsCount > 0) {
                attempt.timeSpentSeconds / attempt.questionsCount
            } else 60L
            val topicAcc = subtopicAccuracyMap[
                Triple(attempt.discipline, attempt.topic, attempt.subtopic)
            ] ?: attempt.accuracyPercent

            when (cause) {
                ErrorCause.LACUNA_CONCEITUAL, ErrorCause.DUVIDA_ALTERNATIVAS -> knowledgeGap += wrongs
                ErrorCause.DESATENCAO -> inattention += wrongs
                ErrorCause.PEGADINHA_BANCA -> boardTrap += wrongs
                ErrorCause.ESQUECIMENTO -> forgetting += wrongs
                ErrorCause.NONE -> {
                    if (topicAcc >= 75.0 && avgPerQ in 1L..30L) {
                        inattention += wrongs
                    } else if (topicAcc < 60.0 || avgPerQ > 95L || attempt.attemptNumber > 1) {
                        knowledgeGap += wrongs
                    } else {
                        forgetting += wrongs
                    }
                }
            }
        }

        val totalErrors = knowledgeGap + inattention + boardTrap + forgetting
        val gapPct = if (totalErrors > 0) (knowledgeGap * 100.0) / totalErrors else 0.0
        val inattPct = if (totalErrors > 0) (inattention * 100.0) / totalErrors else 0.0

        val primaryDiagnosis = when {
            totalErrors == 0 -> "Sem erros registrados até o momento."
            gapPct >= 50.0 -> "Predomínio de lacuna conceitual (${gapPct.roundToInt()}%): priorize revisão de base teórica e lei seca."
            inattPct >= 40.0 -> "Alto índice de desatenção (${inattPct.roundToInt()}%): você domina o conteúdo, mas perde pontos por leitura rápida."
            boardTrap >= knowledgeGap && boardTrap >= inattention -> "Padrão de pegadinhas da banca: reforce a leitura do comando e exceções jurisprudenciais."
            else -> "Erros distribuídos entre revisão pendente e consolidação: aplique revisões espaçadas."
        }

        return ErrorEtiologySummary(
            totalErrors = totalErrors,
            knowledgeGapCount = knowledgeGap,
            inattentionCount = inattention,
            boardTrapCount = boardTrap,
            forgettingCount = forgetting,
            knowledgeGapPercent = gapPct,
            inattentionPercent = inattPct,
            primaryDiagnosis = primaryDiagnosis
        )
    }

    private fun buildQuickHeadline(
        studentName: String,
        worstSubtopic: SubtopicMetrics?
    ): String {
        if (worstSubtopic == null) {
            return "$studentName, seu aproveitamento está sólido nos tópicos avaliados. Mantenha revisões periódicas e avance nos itens pendentes do seu edital."
        }
        val accPct = worstSubtopic.accuracyPercent.roundToInt()
        val errPct = worstSubtopic.errorPercent.roundToInt()
        val microDetail = worstSubtopic.microtopics
            .minByOrNull { it.accuracyPercent }
            ?.let { " — com foco em ${it.microtopic} (${it.accuracyPercent.roundToInt()}% de acertos)" }
            .orEmpty()

        return "$studentName, dentro de ${worstSubtopic.discipline}, em ${worstSubtopic.topic}, " +
            "seu principal ponto de atenção é ${worstSubtopic.subtopic}$microDetail. " +
            "Você registra $accPct% de acertos e $errPct% de erros (domínio ${worstSubtopic.mastery.label}) e deve priorizá-lo na próxima revisão."
    }

    private fun computeStreakDays(dayKeys: Set<String>, formatter: SimpleDateFormat): Int {
        if (dayKeys.isEmpty()) return 0
        val cal = Calendar.getInstance()
        var streak = 0
        val todayKey = formatter.format(cal.time)
        if (!dayKeys.contains(todayKey)) {
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }
        while (dayKeys.contains(formatter.format(cal.time))) {
            streak++
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }
        return streak
    }

    fun daysUntilExam(examDateIso: String): Long? {
        if (examDateIso.isBlank()) return null
        return try {
            val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }
            val examDate = fmt.parse(examDateIso.trim()) ?: return null
            val today = fmt.parse(fmt.format(Date())) ?: return null
            val diffMillis = examDate.time - today.time
            diffMillis / (1000L * 60L * 60L * 24L)
        } catch (_: Exception) {
            null
        }
    }
}
