package com.example.data.repository

import android.graphics.Bitmap
import android.util.Log
import com.example.data.model.AiDiagnosticReport
import com.example.data.model.EditalItemDraft
import com.example.data.model.EditalWeight
import com.example.data.model.ErrorCause
import com.example.data.model.ImportedAttemptDraft
import com.example.data.model.QuestionAttempt
import com.example.data.model.RevisionPriority
import com.example.data.model.RevisionTask
import com.example.data.model.SourceType
import com.example.data.model.UserProfile
import com.example.domain.PerformanceAnalyticsEngine
import com.example.domain.PerformanceAnalyticsSnapshot
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import kotlin.math.roundToInt

private const val TAG = "AiMentorService"
private const val MODEL_NAME = "gemini-3.8-flash"

class AiMentorService {

    private val editalSchema = Schema.obj(
        properties = mapOf(
            "editalTitle" to Schema.string("Nome do concurso ou cargo identificado no edital"),
            "items" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "discipline" to Schema.string("Nome da disciplina (ex: Direito Penal)"),
                        "topic" to Schema.string("Assunto principal do edital (ex: Teoria do Crime)"),
                        "subtopic" to Schema.string("Subassunto específico (ex: Culpabilidade)"),
                        "weight" to Schema.enumeration(listOf("ALTA", "MEDIA", "BAIXA"))
                    )
                )
            )
        )
    )

    private val diagnosticSchema = Schema.obj(
        properties = mapOf(
            "headline" to Schema.string("Diagnóstico metacognitivo direto citando o nome do estudante, disciplina, assunto e subassunto crítico"),
            "focusDiscipline" to Schema.string("Disciplina mais crítica"),
            "focusTopic" to Schema.string("Assunto mais crítico"),
            "focusSubtopic" to Schema.string("Subassunto ou microtópico mais crítico"),
            "whatIDontKnow" to Schema.string("Resposta objetiva para: O QUE EU AINDA NÃO DOMINO? (lacunas reais)"),
            "whereIAmFailing" to Schema.string("Resposta objetiva para: ONDE ESTOU ERRANDO? (disciplina, assunto, subassunto e % de erro)"),
            "whyIAmFailing" to Schema.string("Resposta objetiva para: POR QUE ESTOU ERRANDO? (metacognição: lacuna teórica vs desatenção)"),
            "whichSubtopicToStudy" to Schema.string("Resposta objetiva para: QUAL SUBTÓPICO PRECISO ESTUDAR E QUAIS PONTOS FORTES MANTER?"),
            "whatToReviewNow" to Schema.string("Resposta objetiva para: O QUE PRECISO ESTUDAR EM SEGUIDA PARA AUMENTAR MEU DESEMPENHO?"),
            "legalFoundations" to Schema.array(
                items = Schema.string(),
                description = "Lista de dispositivos legais, artigos de lei e fundamentos teóricos específicos para revisar"
            ),
            "jurisprudenceNotes" to Schema.array(
                items = Schema.string(),
                description = "Lista de súmulas ou entendimentos jurisprudenciais relevantes do STF/STJ ligados aos erros"
            )
        )
    )

    private val revisionPlanSchema = Schema.obj(
        properties = mapOf(
            "tasks" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "priority" to Schema.enumeration(listOf("CRITICA", "ALTA", "MEDIA", "MANUTENCAO")),
                        "discipline" to Schema.string(),
                        "topic" to Schema.string(),
                        "subtopic" to Schema.string(),
                        "rationale" to Schema.string("Por que este tópico deve ser priorizado nesta ordem"),
                        "studyActions" to Schema.string("Passo a passo prático de autorregulação e revisão"),
                        "legalReferences" to Schema.string("Artigos de lei, doutrina e jurisprudência essenciais"),
                        "estimatedMinutes" to Schema.integer("Tempo recomendado em minutos (15 a 120)"),
                        "targetQuestions" to Schema.integer("Meta de questões a resolver no QConcursos (5 a 40)")
                    )
                )
            )
        )
    )

    private val extractionSchema = Schema.obj(
        properties = mapOf(
            "records" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "qconcursosCode" to Schema.string("Código da questão tipo Q1234567 ou vazio"),
                        "discipline" to Schema.string("Disciplina identificada"),
                        "topic" to Schema.string("Assunto principal"),
                        "subtopic" to Schema.string("Subassunto específico"),
                        "microtopic" to Schema.string("Microtópico específico ou vazio"),
                        "examBoard" to Schema.string("Banca examinadora (ex: Cebraspe, FGV, FCC)"),
                        "questionsCount" to Schema.integer("Total de questões resolvidas neste item"),
                        "correctCount" to Schema.integer("Total de acertos neste item"),
                        "timeSpentSeconds" to Schema.integer("Tempo total estimado em segundos"),
                        "errorCause" to Schema.enumeration(
                            listOf("NONE", "LACUNA_CONCEITUAL", "DESATENCAO", "PEGADINHA_BANCA", "DUVIDA_ALTERNATIVAS", "ESQUECIMENTO")
                        ),
                        "notes" to Schema.string("Resumo do conceito cobrado ou motivo do erro")
                    )
                )
            )
        )
    )

    private val editalModel by lazy {
        Firebase.ai.generativeModel(
            modelName = MODEL_NAME,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = editalSchema
                temperature = 0.2f
            }
        )
    }

    private val diagnosticModel by lazy {
        Firebase.ai.generativeModel(
            modelName = MODEL_NAME,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = diagnosticSchema
                temperature = 0.3f
            }
        )
    }

    private val revisionModel by lazy {
        Firebase.ai.generativeModel(
            modelName = MODEL_NAME,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = revisionPlanSchema
                temperature = 0.35f
            }
        )
    }

    private val extractionModel by lazy {
        Firebase.ai.generativeModel(
            modelName = MODEL_NAME,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = extractionSchema
                temperature = 0.2f
            }
        )
    }

    private val mentorChatModel by lazy {
        Firebase.ai.generativeModel(
            modelName = MODEL_NAME,
            generationConfig = generationConfig {
                temperature = 0.45f
            }
        )
    }

    suspend fun generateDeepDiagnostic(
        profile: UserProfile?,
        snapshot: PerformanceAnalyticsSnapshot,
        recentAttempts: List<QuestionAttempt>
    ): Result<AiDiagnosticReport> {
        return try {
            val prompt = buildDiagnosticPrompt(profile, snapshot, recentAttempts)
            val response = withTimeout(60_000L) {
                diagnosticModel.generateContent(prompt)
            }
            val jsonText = response.text
                ?: return Result.failure(IllegalStateException("A IA retornou uma resposta vazia."))

            val json = JSONObject(jsonText)
            val legalList = json.optJSONArray("legalFoundations")?.let { arr ->
                (0 until arr.length()).map { arr.optString(it) }.filter { it.isNotBlank() }
            }.orEmpty()
            val jurisList = json.optJSONArray("jurisprudenceNotes")?.let { arr ->
                (0 until arr.length()).map { arr.optString(it) }.filter { it.isNotBlank() }
            }.orEmpty()

            Result.success(
                AiDiagnosticReport(
                    headline = json.optString("headline"),
                    focusDiscipline = json.optString("focusDiscipline"),
                    focusTopic = json.optString("focusTopic"),
                    focusSubtopic = json.optString("focusSubtopic"),
                    whatIDontKnow = json.optString("whatIDontKnow"),
                    whereIAmFailing = json.optString("whereIAmFailing"),
                    whyIAmFailing = json.optString("whyIAmFailing"),
                    whichSubtopicToStudy = json.optString("whichSubtopicToStudy"),
                    whatToReviewNow = json.optString("whatToReviewNow"),
                    legalFoundations = legalList,
                    jurisprudenceNotes = jurisList,
                    questionsAnalyzed = snapshot.totalQuestions.toLong()
                )
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Deep diagnostic timed out", e)
            Result.failure(Exception("A análise pedagógica excedeu 60 segundos. Tente novamente."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to generate deep diagnostic", e)
            Result.failure(Exception(e.localizedMessage ?: "Falha ao gerar diagnóstico com IA."))
        }
    }

    suspend fun generateRevisionPlan(
        profile: UserProfile?,
        snapshot: PerformanceAnalyticsSnapshot,
        recentAttempts: List<QuestionAttempt>
    ): Result<List<RevisionTask>> {
        return try {
            val prompt = buildRevisionPlanPrompt(profile, snapshot, recentAttempts)
            val response = withTimeout(60_000L) {
                revisionModel.generateContent(prompt)
            }
            val jsonText = response.text
                ?: return Result.failure(IllegalStateException("A IA retornou um plano vazio."))

            val json = JSONObject(jsonText)
            val tasksArray = json.optJSONArray("tasks")
                ?: return Result.failure(IllegalStateException("Nenhuma tarefa de revisão retornada."))

            val tasks = (0 until tasksArray.length()).mapNotNull { idx ->
                val obj = tasksArray.optJSONObject(idx) ?: return@mapNotNull null
                val discipline = obj.optString("discipline").trim()
                if (discipline.isBlank()) return@mapNotNull null
                RevisionTask(
                    orderIndex = idx.toLong(),
                    priority = RevisionPriority.fromWire(obj.optString("priority")).wireValue,
                    discipline = discipline,
                    topic = obj.optString("topic").ifBlank { discipline },
                    subtopic = obj.optString("subtopic").ifBlank { obj.optString("topic", discipline) },
                    rationale = obj.optString("rationale").ifBlank { "Prioridade definida pelos dados de erro." },
                    studyActions = obj.optString("studyActions").ifBlank { "Revisão direcionada e bateria de questões." },
                    legalReferences = obj.optString("legalReferences"),
                    estimatedMinutes = obj.optInt("estimatedMinutes", 40).coerceIn(15, 240).toLong(),
                    targetQuestions = obj.optInt("targetQuestions", 15).coerceIn(5, 80).toLong(),
                    completed = false
                )
            }

            if (tasks.isEmpty()) {
                Result.failure(IllegalStateException("Não foi possível estruturar tarefas de revisão."))
            } else {
                Result.success(tasks)
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Revision plan generation timed out", e)
            Result.failure(Exception("A geração do plano de revisão excedeu o tempo limite. Tente novamente."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to generate revision plan", e)
            Result.failure(Exception(e.localizedMessage ?: "Falha ao gerar plano de revisão com IA."))
        }
    }

    suspend fun extractFromQConcursosImage(
        bitmap: Bitmap,
        defaultBoard: String
    ): Result<List<ImportedAttemptDraft>> {
        return try {
            val promptText = """
                Você é um extrator estruturado de dados da plataforma brasileira QConcursos.
                Analise esta captura de tela (pode ser a tela de Estatísticas/Desempenho, Caderno de Questões,
                ou uma Questão resolvida com gabarito no QConcursos).
                Extraia todos os registros reais visíveis na imagem, identificando:
                - qconcursosCode (ex: Q2481920, se visível; senão "")
                - discipline (ex: Direito Penal, Direito Constitucional, etc.)
                - topic (assunto principal)
                - subtopic (subassunto específico)
                - microtopic (detalhamento granular se identificável no enunciado ou tabela)
                - examBoard (banca visível ou "$defaultBoard" se não informada)
                - questionsCount (número inteiro >= 1 de questões resolvidas naquele tópico/item)
                - correctCount (número inteiro de acertos, <= questionsCount)
                - timeSpentSeconds (tempo estimado em segundos; use 60 por questão se não visível)
                - errorCause (NONE se acertou tudo; senão classifique entre LACUNA_CONCEITUAL, DESATENCAO, PEGADINHA_BANCA, DUVIDA_ALTERNATIVAS, ESQUECIMENTO)
                - notes (breve resumo do conteúdo da questão ou estatística extraída)
                NÃO invente dados que não estejam na imagem. Se houver apenas uma questão resolvida, retorne 1 item com questionsCount=1.
            """.trimIndent()

            val response = withTimeout(60_000L) {
                extractionModel.generateContent(
                    content {
                        text(promptText)
                        image(bitmap)
                    }
                )
            }
            parseExtractedRecords(response.text, SourceType.IMPORT_PRINT_IA, defaultBoard)
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Screenshot extraction timed out", e)
            Result.failure(Exception("A leitura da imagem excedeu 60 segundos. Tente uma imagem mais nítida."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Screenshot extraction failed", e)
            Result.failure(Exception(e.localizedMessage ?: "Falha ao extrair dados da imagem."))
        }
    }

    suspend fun extractFromRawText(
        rawText: String,
        defaultBoard: String
    ): Result<List<ImportedAttemptDraft>> {
        return try {
            val promptText = """
                Extraia registros estruturados de desempenho de questões do QConcursos a partir do texto colado abaixo.
                Regras:
                - Não invente números que não existam no texto.
                - Identifique disciplina, assunto, subassunto, microtópico (se houver), banca (padrão "$defaultBoard"),
                  código Q... (se houver), total de questões (questionsCount) e acertos (correctCount).
                - Se o texto trouxer percentual de acertos e total de questões, calcule correctCount = round(total * pct / 100).
                - Se o texto trouxer acertos e erros, questionsCount = acertos + erros.

                TEXTO DO ESTUDANTE:
                ${rawText.take(6000)}
            """.trimIndent()

            val response = withTimeout(60_000L) {
                extractionModel.generateContent(promptText)
            }
            parseExtractedRecords(response.text, SourceType.IMPORT_TEXTO, defaultBoard)
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Text extraction timed out", e)
            Result.failure(Exception("A interpretação do relatório excedeu 60 segundos."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Text extraction failed", e)
            Result.failure(Exception(e.localizedMessage ?: "Falha ao interpretar relatório com IA."))
        }
    }

    suspend fun extractEditalFromText(
        rawText: String,
        fallbackTitle: String
    ): Result<List<EditalItemDraft>> {
        return try {
            val promptText = """
                Você é um especialista em estruturação de editais de concursos públicos brasileiros para a plataforma Método Nilo.
                Analise o conteúdo programático do edital abaixo e organize-o em uma hierarquia limpa de:
                - discipline (Disciplina, ex: Direito Penal)
                - topic (Assunto principal, ex: Teoria do Crime)
                - subtopic (Subassunto específico, ex: Culpabilidade)
                - weight (ALTA, MEDIA ou BAIXA conforme relevância típica em provas)
                Extraia no máximo 60 subassuntos mais representativos e fiéis ao texto fornecido. NÃO invente disciplinas ausentes.

                EDITAL FORNECIDO:
                ${rawText.take(14000)}
            """.trimIndent()

            val response = withTimeout(60_000L) {
                editalModel.generateContent(promptText)
            }
            parseExtractedEdital(response.text, fallbackTitle)
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Edital text extraction timed out", e)
            Result.failure(Exception("A leitura do edital excedeu 60 segundos. Tente enviar uma seção menor."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Edital text extraction failed", e)
            Result.failure(Exception(e.localizedMessage ?: "Falha ao estruturar o edital com IA."))
        }
    }

    suspend fun extractEditalFromPdfBytes(
        pdfBytes: ByteArray,
        fallbackTitle: String
    ): Result<List<EditalItemDraft>> {
        if (pdfBytes.isEmpty()) {
            return Result.failure(IllegalArgumentException("O arquivo PDF selecionado está vazio."))
        }
        if (pdfBytes.size > 18 * 1024 * 1024) {
            return Result.failure(IllegalArgumentException("O PDF excede 18 MB. Envie apenas as páginas do conteúdo programático."))
        }
        return try {
            val promptText = """
                Você é um especialista em concursos públicos brasileiros na plataforma Método Nilo.
                Leia este documento PDF de edital e extraia EXCLUSIVAMENTE o conteúdo programático estruturado em:
                - editalTitle (nome do concurso/cargo)
                - items: lista com discipline, topic, subtopic e weight (ALTA, MEDIA, BAIXA).
                Extraia até 60 subassuntos mais relevantes de forma hierárquica e fiel ao documento.
            """.trimIndent()

            val response = withTimeout(60_000L) {
                editalModel.generateContent(
                    content {
                        inlineData(pdfBytes, "application/pdf")
                        text(promptText)
                    }
                )
            }
            parseExtractedEdital(response.text, fallbackTitle)
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Edital PDF extraction timed out", e)
            Result.failure(Exception("A leitura do PDF excedeu 60 segundos. Tente anexar páginas mais curtas ou colar o texto."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Edital PDF extraction failed", e)
            Result.failure(Exception(e.localizedMessage ?: "Falha ao analisar o PDF do edital."))
        }
    }

    suspend fun extractEditalFromImage(
        bitmap: Bitmap,
        fallbackTitle: String
    ): Result<List<EditalItemDraft>> {
        return try {
            val promptText = """
                Leia esta imagem/captura de tela de um edital de concurso público brasileiro e extraia a estrutura hierárquica do conteúdo programático:
                - editalTitle (título do concurso se visível, senão "$fallbackTitle")
                - items: lista com discipline, topic, subtopic e weight (ALTA, MEDIA, BAIXA).
                Extraia apenas os tópicos realmente visíveis na imagem, sem inventar dados.
            """.trimIndent()

            val response = withTimeout(60_000L) {
                editalModel.generateContent(
                    content {
                        text(promptText)
                        image(bitmap)
                    }
                )
            }
            parseExtractedEdital(response.text, fallbackTitle)
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Edital image extraction timed out", e)
            Result.failure(Exception("A leitura da imagem do edital excedeu 60 segundos."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Edital image extraction failed", e)
            Result.failure(Exception(e.localizedMessage ?: "Falha ao extrair edital da imagem."))
        }
    }

    private fun parseExtractedEdital(
        jsonText: String?,
        fallbackTitle: String
    ): Result<List<EditalItemDraft>> {
        if (jsonText.isNullOrBlank()) {
            return Result.failure(IllegalStateException("Não foi possível extrair tópicos do edital enviado."))
        }
        val json = JSONObject(jsonText)
        val detectedTitle = json.optString("editalTitle").trim()
            .ifBlank { fallbackTitle.trim().ifBlank { "Meu Edital" } }
            .take(140)
        val array = json.optJSONArray("items")
            ?: return Result.failure(IllegalStateException("Nenhum assunto programático foi identificado."))

        val drafts = (0 until array.length()).mapNotNull { idx ->
            val obj = array.optJSONObject(idx) ?: return@mapNotNull null
            val disc = obj.optString("discipline").trim()
            val topic = obj.optString("topic").trim()
            val sub = obj.optString("subtopic").trim()
            if (disc.isBlank()) return@mapNotNull null
            val safeTopic = topic.ifBlank { disc }
            val safeSub = sub.ifBlank { safeTopic }
            EditalItemDraft(
                editalTitle = detectedTitle,
                discipline = disc.take(120),
                topic = safeTopic.take(140),
                subtopic = safeSub.take(160),
                weight = EditalWeight.fromWire(obj.optString("weight"))
            )
        }.distinctBy {
            "${it.discipline.lowercase()}|${it.topic.lowercase()}|${it.subtopic.lowercase()}"
        }.take(60)

        return if (drafts.isEmpty()) {
            Result.failure(IllegalStateException("Nenhum item hierárquico válido foi encontrado no edital."))
        } else {
            Result.success(drafts)
        }
    }

    fun streamMentorAnswer(
        question: String,
        profile: UserProfile?,
        snapshot: PerformanceAnalyticsSnapshot
    ): Flow<String> = flow {
        val telemetry = buildTelemetrySummary(profile, snapshot, emptyList())
        val prompt = """
            Você é o Mentor do Método Nilo, uma plataforma inteligente de estudos para concursos públicos fundamentada na metacognição e na autorregulação da aprendizagem.
            Responda em português brasileiro, de forma clara, acessível, empática, técnica e fundamentada nos dados reais do aluno abaixo (incluindo o cruzamento entre seu edital e seu histórico no QConcursos).
            Indique claramente pontos fortes a manter, lacunas prioritárias e próximos passos práticos. Cite dispositivos legais, doutrina ou súmulas do STF/STJ quando pertinente.

            DADOS REAIS DO ESTUDANTE:
            $telemetry

            PERGUNTA DO ESTUDANTE:
            $question
        """.trimIndent()

        val accumulated = StringBuilder()
        withTimeout(60_000L) {
            mentorChatModel.generateContentStream(prompt).collect { chunk ->
                val piece = chunk.text.orEmpty()
                if (piece.isNotEmpty()) {
                    accumulated.append(piece)
                    emit(accumulated.toString())
                }
            }
        }
    }

    private fun parseExtractedRecords(
        jsonText: String?,
        sourceType: SourceType,
        defaultBoard: String
    ): Result<List<ImportedAttemptDraft>> {
        if (jsonText.isNullOrBlank()) {
            return Result.failure(IllegalStateException("Nenhum dado encontrado no conteúdo enviado."))
        }
        val json = JSONObject(jsonText)
        val array = json.optJSONArray("records")
            ?: return Result.failure(IllegalStateException("Não foi possível identificar questões ou métricas."))

        val drafts = (0 until array.length()).mapNotNull { idx ->
            val obj = array.optJSONObject(idx) ?: return@mapNotNull null
            val discipline = obj.optString("discipline").trim()
            if (discipline.isBlank()) return@mapNotNull null
            val total = obj.optInt("questionsCount", 1).coerceIn(1, 500)
            val correct = obj.optInt("correctCount", 0).coerceIn(0, total)
            ImportedAttemptDraft(
                qconcursosCode = obj.optString("qconcursosCode").trim().uppercase().take(40),
                sourceType = sourceType,
                discipline = discipline.take(120),
                topic = obj.optString("topic").trim().ifBlank { discipline }.take(140),
                subtopic = obj.optString("subtopic").trim().ifBlank { obj.optString("topic", discipline) }.take(160),
                microtopic = obj.optString("microtopic").trim().take(160),
                examBoard = obj.optString("examBoard").trim().ifBlank { defaultBoard }.take(80),
                questionsCount = total,
                correctCount = correct,
                timeSpentSeconds = obj.optInt("timeSpentSeconds", total * 60).coerceIn(0, 86400),
                attemptNumber = 1,
                errorCause = if (correct == total) ErrorCause.NONE else ErrorCause.fromWire(obj.optString("errorCause")),
                notes = obj.optString("notes").trim().take(1000)
            )
        }
        return if (drafts.isEmpty()) {
            Result.failure(IllegalStateException("Nenhum registro estruturado foi detectado."))
        } else {
            Result.success(drafts)
        }
    }

    private fun buildDiagnosticPrompt(
        profile: UserProfile?,
        snapshot: PerformanceAnalyticsSnapshot,
        recentAttempts: List<QuestionAttempt>
    ): String {
        val telemetry = buildTelemetrySummary(profile, snapshot, recentAttempts)
        val studentName = profile?.studentName?.ifBlank { "Estudante" } ?: "Estudante"
        return """
            Atue como mentor pedagógico do Método Nilo, especialista em metacognição, autorregulação da aprendizagem, análise de dados educacionais e concursos públicos.
            Com base EXCLUSIVAMENTE nos dados reais abaixo do estudante $studentName (cruzando edital cadastrado e desempenho no QConcursos),
            gere um diagnóstico metacognitivo preciso, direto e acionável.

            O campo "headline" DEVE começar chamando o aluno pelo nome (ex: "$studentName, dentro de [Disciplina], em [Assunto], seu principal ponto de atenção é [Subassunto]...").
            Informe explicitamente o percentual de acertos e de erros nos gargalos e destaque também os pontos fortes que precisam apenas de manutenção.
            Aprofunde ao máximo nos subassuntos e microtópicos e diferencie lacuna teórica de desatenção / pegadinha de banca.
            Cruze os erros com fundamentos teóricos, dispositivos legais exatos (CF, CP, CPP, leis especiais) e jurisprudência/súmulas do STF/STJ.

            $telemetry
        """.trimIndent()
    }

    private fun buildRevisionPlanPrompt(
        profile: UserProfile?,
        snapshot: PerformanceAnalyticsSnapshot,
        recentAttempts: List<QuestionAttempt>
    ): String {
        val telemetry = buildTelemetrySummary(profile, snapshot, recentAttempts)
        val daysRemaining = profile?.examDateIso?.let { PerformanceAnalyticsEngine.daysUntilExam(it) }
        val urgencyNote = when {
            daysRemaining == null -> "Data da prova não informada: priorize correção estrutural de lacunas críticas, itens de peso alto no edital e alta incidência."
            daysRemaining <= 30 -> "RETA FINAL ($daysRemaining dias para a prova): foque em alto retorno, lei seca, jurisprudência recente e tópicos críticos de maior incidência na banca."
            daysRemaining <= 90 -> "MÉDIO PRAZO ($daysRemaining dias para a prova): equilibre reconstrução dos pontos críticos do edital com baterias direcionadas."
            else -> "PREPARAÇÃO DE BASE ($daysRemaining dias para a prova): aprofunde fundamentos teóricos dos subassuntos críticos, cubra itens não iniciados do edital e mantenha revisões espaçadas."
        }
        return """
            Elabore um Plano de Revisão Inteligente (Método Nilo — Autorregulação da Aprendizagem) com 4 a 7 tarefas priorizadas na ordem exata de estudo.
            Considere simultaneamente:
            1. Gravidade das dificuldades (menor % de acertos / maior % de erros e nível crítico/baixo)
            2. Frequência de erros repetidos e itens não estudados de peso ALTO no edital cadastrado
            3. Incidência histórica dos assuntos na banca examinadora alvo (${profile?.examBoard ?: "Cebraspe"})
            4. Proximidade da prova: $urgencyNote
            5. Inclua ao menos 1 tarefa de MANUTENCAO para consolidar um ponto forte se o estudante já tiver tópicos dominados.

            Para cada tarefa, explique claramente POR QUE ela deve ser priorizada agora, indique ações concretas de estudo,
            liste dispositivos legais/jurisprudência específicos e defina tempo estimado (minutos) e meta de questões no QConcursos.

            $telemetry
        """.trimIndent()
    }

    private fun buildTelemetrySummary(
        profile: UserProfile?,
        snapshot: PerformanceAnalyticsSnapshot,
        recentAttempts: List<QuestionAttempt>
    ): String = buildString {
        appendLine("PERFIL DO ESTUDANTE (MÉTODO NILO):")
        appendLine("- Nome: ${profile?.studentName ?: "Estudante"}")
        appendLine("- Concurso/Cargo alvo: ${profile?.targetExam?.ifBlank { "Não definido" } ?: "Não definido"}")
        appendLine("- Banca principal: ${profile?.examBoard ?: "Cebraspe"}")
        appendLine("- Data da prova: ${profile?.examDateIso?.ifBlank { "Não definida" } ?: "Não definida"}")
        appendLine("- Total de questões analisadas: ${snapshot.totalQuestions} (${snapshot.totalCorrect} acertos / ${snapshot.totalWrong} erros)")
        appendLine("- Aproveitamento global: ${snapshot.overallAccuracyPercent.roundToInt()}% de acertos | ${snapshot.overallErrorPercent.roundToInt()}% de erros (${snapshot.overallMastery.label})")
        appendLine("- Tempo médio por questão: ${snapshot.avgTimeSecondsPerQuestion}s")
        val edital = snapshot.editalCoverage
        if (edital.totalItems > 0) {
            appendLine()
            appendLine("COBERTURA DO EDITAL CADASTRADO (${edital.editalTitle}):")
            appendLine("- Itens mapeados: ${edital.totalItems} | Estudados: ${edital.studiedItems} (${edital.coveragePercent.roundToInt()}%) | Dominados: ${edital.masteredItems} | Críticos: ${edital.criticalItems}")
            if (edital.unstudiedHighWeightItems.isNotEmpty()) {
                appendLine("- Itens de PESO ALTO ainda NÃO iniciados:")
                edital.unstudiedHighWeightItems.take(6).forEach { item ->
                    appendLine("  * ${item.discipline} > ${item.topic} > ${item.subtopic}")
                }
            }
        }
        appendLine()
        appendLine("ETIOLOGIA DOS ERROS (LACUNA vs DESATENÇÃO):")
        appendLine("- Lacuna conceitual/dúvida: ${snapshot.errorEtiology.knowledgeGapCount} (${snapshot.errorEtiology.knowledgeGapPercent.roundToInt()}%)")
        appendLine("- Desatenção/leitura rápida: ${snapshot.errorEtiology.inattentionCount} (${snapshot.errorEtiology.inattentionPercent.roundToInt()}%)")
        appendLine("- Pegadinha de banca: ${snapshot.errorEtiology.boardTrapCount}")
        appendLine("- Esquecimento/revisão: ${snapshot.errorEtiology.forgettingCount}")
        appendLine()
        appendLine("PONTOS FRACOS — RANKING DE SUBASSUNTOS COM MAIOR DIFICULDADE:")
        snapshot.rankedDifficultSubtopics.take(12).forEachIndexed { i, sub ->
            val micros = sub.microtopics.joinToString("; ") {
                "${it.microtopic}: ${it.accuracyPercent.roundToInt()}% acertos / ${it.errorPercent.roundToInt()}% erros (${it.correctQuestions}/${it.totalQuestions})"
            }
            appendLine(
                "${i + 1}. ${sub.discipline} > ${sub.topic} > ${sub.subtopic} | " +
                    "Acertos: ${sub.accuracyPercent.roundToInt()}% | Erros: ${sub.errorPercent.roundToInt()}% (${sub.correctQuestions}/${sub.totalQuestions}) | " +
                    "Domínio: ${sub.mastery.label} | Tempo médio: ${sub.avgTimeSeconds}s | " +
                    "Erros repetidos: ${sub.repeatErrorCount} | Padrão: ${sub.inferredCauseLabel}" +
                    if (micros.isNotBlank()) " | Microtópicos: [$micros]" else ""
            )
        }
        if (snapshot.masteredSubtopics.isNotEmpty()) {
            appendLine()
            appendLine("PONTOS FORTES (DOMINADOS — MANTER EM REVISÃO ESPAÇADA):")
            snapshot.masteredSubtopics.take(6).forEach { strong ->
                appendLine("- ${strong.discipline} > ${strong.subtopic}: ${strong.accuracyPercent.roundToInt()}% acertos (${strong.correctQuestions}/${strong.totalQuestions})")
            }
        }
        if (snapshot.repeatedErrors.isNotEmpty()) {
            appendLine()
            appendLine("ERROS REPETIDOS DETECTADOS:")
            snapshot.repeatedErrors.take(8).forEach { rep ->
                appendLine("- ${rep.identifier} (${rep.discipline} > ${rep.subtopic}): ${rep.occurrences}x | Nota: ${rep.lastNotes.take(120)}")
            }
        }
        val errorNotes = recentAttempts.filter { it.wrongCount > 0 && it.notes.isNotBlank() }.take(10)
        if (errorNotes.isNotEmpty()) {
            appendLine()
            appendLine("ANOTAÇÕES RECENTES DE ERROS DO ESTUDANTE:")
            errorNotes.forEach { att ->
                appendLine("- [${att.qconcursosCode.ifBlank { att.examBoard }}] ${att.discipline} > ${att.subtopic} (${att.microtopic}): ${att.notes.take(180)}")
            }
        }
    }
}
