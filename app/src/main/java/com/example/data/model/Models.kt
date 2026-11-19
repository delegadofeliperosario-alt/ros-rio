package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

sealed interface UiState<out T> {
    data object Idle : UiState<Nothing>
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class SourceType(val wireValue: String, val label: String) {
    PORTAL_QCONCURSOS("PORTAL_QCONCURSOS", "Portal QConcursos"),
    IMPORT_PRINT_IA("IMPORT_PRINT_IA", "Print QConcursos (IA)"),
    IMPORT_TEXTO("IMPORT_TEXTO", "Relatório / CSV"),
    REGISTRO_MANUAL("REGISTRO_MANUAL", "Registro Manual");

    companion object {
        fun fromWire(raw: String?): SourceType =
            entries.firstOrNull { it.wireValue == raw } ?: REGISTRO_MANUAL
    }
}

enum class ErrorCause(val wireValue: String, val label: String, val isConceptualGap: Boolean) {
    NONE("NONE", "Acerto / Sem erro", false),
    LACUNA_CONCEITUAL("LACUNA_CONCEITUAL", "Falta de conhecimento / Lacuna teórica", true),
    DESATENCAO("DESATENCAO", "Desatenção / Leitura precipitada", false),
    PEGADINHA_BANCA("PEGADINHA_BANCA", "Pegadinha / Estilo da banca", false),
    DUVIDA_ALTERNATIVAS("DUVIDA_ALTERNATIVAS", "Dúvida entre duas alternativas", true),
    ESQUECIMENTO("ESQUECIMENTO", "Esquecimento / Falta de revisão", true);

    companion object {
        fun fromWire(raw: String?): ErrorCause =
            entries.firstOrNull { it.wireValue == raw } ?: NONE
    }
}

enum class RevisionPriority(val wireValue: String, val label: String, val rank: Int) {
    CRITICA("CRITICA", "Crítica", 0),
    ALTA("ALTA", "Alta", 1),
    MEDIA("MEDIA", "Média", 2),
    MANUTENCAO("MANUTENCAO", "Manutenção", 3);

    companion object {
        fun fromWire(raw: String?): RevisionPriority =
            entries.firstOrNull { it.wireValue == raw } ?: MEDIA
    }
}

enum class MasteryLevel(val label: String, val shortLabel: String, val severityWeight: Double) {
    CRITICO("Crítico", "Crítico", 5.0),
    BAIXO("Baixo", "Baixo", 4.0),
    INTERMEDIARIO("Intermediário", "Interm.", 2.8),
    BOM("Bom", "Bom", 1.5),
    EXCELENTE("Excelente", "Excelente", 0.5);

    companion object {
        fun fromAccuracy(accuracyPercent: Double): MasteryLevel = when {
            accuracyPercent < 50.0 -> CRITICO
            accuracyPercent < 65.0 -> BAIXO
            accuracyPercent < 78.0 -> INTERMEDIARIO
            accuracyPercent < 88.0 -> BOM
            else -> EXCELENTE
        }
    }
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val studentName: String = "",
    val targetExam: String = "",
    val examBoard: String = "Cebraspe",
    val examDateIso: String = "",
    val dailyGoalQuestions: Long = 30L,
    val qconcursosLink: String = "https://www.qconcursos.com/questoes-de-concursos/questoes",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class QuestionAttempt(
    @DocumentId val id: String = "",
    val userId: String = "",
    val qconcursosCode: String = "",
    val sourceUrl: String = "",
    val sourceType: String = SourceType.REGISTRO_MANUAL.wireValue,
    val discipline: String = "",
    val topic: String = "",
    val subtopic: String = "",
    val microtopic: String = "",
    val examBoard: String = "Cebraspe",
    val questionsCount: Long = 1L,
    val correctCount: Long = 0L,
    val timeSpentSeconds: Long = 60L,
    val attemptNumber: Long = 1L,
    val errorCause: String = ErrorCause.NONE.wireValue,
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val wrongCount: Long get() = (questionsCount - correctCount).coerceAtLeast(0L)
    val accuracyPercent: Double
        get() = if (questionsCount <= 0L) 0.0 else (correctCount.toDouble() / questionsCount.toDouble()) * 100.0
}

@Keep
@IgnoreExtraProperties
data class AiDiagnosticReport(
    @DocumentId val id: String = "",
    val userId: String = "",
    val headline: String = "",
    val focusDiscipline: String = "",
    val focusTopic: String = "",
    val focusSubtopic: String = "",
    val whatIDontKnow: String = "",
    val whereIAmFailing: String = "",
    val whyIAmFailing: String = "",
    val whichSubtopicToStudy: String = "",
    val whatToReviewNow: String = "",
    val legalFoundations: List<String> = emptyList(),
    val jurisprudenceNotes: List<String> = emptyList(),
    val questionsAnalyzed: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class RevisionTask(
    @DocumentId val id: String = "",
    val userId: String = "",
    val orderIndex: Long = 0L,
    val priority: String = RevisionPriority.ALTA.wireValue,
    val discipline: String = "",
    val topic: String = "",
    val subtopic: String = "",
    val rationale: String = "",
    val studyActions: String = "",
    val legalReferences: String = "",
    val estimatedMinutes: Long = 40L,
    val targetQuestions: Long = 15L,
    val completed: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

enum class EditalWeight(val wireValue: String, val label: String) {
    ALTA("ALTA", "Peso Alto"),
    MEDIA("MEDIA", "Peso Médio"),
    BAIXA("BAIXA", "Peso Baixo");

    companion object {
        fun fromWire(raw: String?): EditalWeight =
            entries.firstOrNull { it.wireValue == raw } ?: MEDIA
    }
}

@Keep
@IgnoreExtraProperties
data class EditalItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val editalTitle: String = "",
    val discipline: String = "",
    val topic: String = "",
    val subtopic: String = "",
    val weight: String = EditalWeight.MEDIA.wireValue,
    val orderIndex: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class EditalItemDraft(
    val editalTitle: String = "Meu Edital",
    val discipline: String = "",
    val topic: String = "",
    val subtopic: String = "",
    val weight: EditalWeight = EditalWeight.MEDIA
)

data class ImportedAttemptDraft(
    val qconcursosCode: String = "",
    val sourceUrl: String = "",
    val sourceType: SourceType = SourceType.IMPORT_PRINT_IA,
    val discipline: String = "",
    val topic: String = "",
    val subtopic: String = "",
    val microtopic: String = "",
    val examBoard: String = "Cebraspe",
    val questionsCount: Int = 1,
    val correctCount: Int = 0,
    val timeSpentSeconds: Int = 60,
    val attemptNumber: Int = 1,
    val errorCause: ErrorCause = ErrorCause.NONE,
    val notes: String = ""
)

object ConcursoTaxonomy {
    val examBoards: List<String> = listOf(
        "Cebraspe",
        "FGV",
        "FCC",
        "Vunesp",
        "Instituto AOCP",
        "IBFC",
        "Cesgranrio",
        "Quadrix",
        "Fumarc",
        "Outra"
    )

    // Disciplina -> Assunto -> Subassunto -> Microtópicos (Catálogo oficial de referência para agilizar o registro)
    val hierarchy: Map<String, Map<String, Map<String, List<String>>>> = linkedMapOf(
        "Direito Penal" to linkedMapOf(
            "Teoria do Crime" to linkedMapOf(
                "Culpabilidade" to listOf(
                    "Imputabilidade",
                    "Potencial consciência da ilicitude",
                    "Exigibilidade de conduta diversa",
                    "Causas de exclusão da culpabilidade",
                    "Erro sobre a ilicitude do fato"
                ),
                "Fato Típico" to listOf(
                    "Conduta e ausência de conduta",
                    "Nexo de causalidade e concausas",
                    "Tipicidade formal e material",
                    "Erro de tipo essencial e acidental"
                ),
                "Ilicitude (Antijuridicidade)" to listOf(
                    "Legítima defesa",
                    "Estado de necessidade",
                    "Estrito cumprimento do dever legal",
                    "Exercício regular de direito"
                ),
                "Iter Criminis e Tentativa" to listOf(
                    "Desistência voluntária e arrependimento eficaz",
                    "Arrependimento posterior",
                    "Crime impossível"
                )
            ),
            "Concurso de Pessoas e Crimes" to linkedMapOf(
                "Concurso de Pessoas" to listOf("Autoria e participação", "Comunicabilidade das circunstâncias"),
                "Concurso de Crimes" to listOf("Concurso material", "Concurso formal", "Crime continuado")
            ),
            "Crimes contra a Administração Pública" to linkedMapOf(
                "Crimes praticados por Funcionário Público" to listOf("Peculato", "Concussão", "Corrupção passiva", "Prevaricação"),
                "Crimes praticados por Particular" to listOf("Corrupção ativa", "Desacato", "Descaminho e contrabando")
            ),
            "Crimes contra a Pessoa e Patrimônio" to linkedMapOf(
                "Crimes contra a Vida" to listOf("Homicídio qualificado", "Feminicídio", "Induzimento ao suicídio"),
                "Crimes contra o Patrimônio" to listOf("Furto qualificado", "Roubo e latrocínio", "Estelionato")
            )
        ),
        "Direito Processual Penal" to linkedMapOf(
            "Inquérito Policial" to linkedMapOf(
                "Características e Arquivamento" to listOf("Juiz das Garantias", "Arquivamento pelo MP (art. 28 CPP)", "Indiciamento"),
                "Investigação Criminal" to listOf("Valor probatório do IP", "Prazos de conclusão", "Acesso do advogado (SV 14)")
            ),
            "Provas" to linkedMapOf(
                "Teoria Geral da Prova" to listOf("Provas ilícitas e derivadas", "Cadeia de custódia", "Ônus da prova"),
                "Meios de Prova" to listOf("Busca e apreensão", "Reconhecimento pessoal (art. 226)", "Interceptação telefônica")
            ),
            "Prisões e Medidas Cautelares" to linkedMapOf(
                "Prisão em Flagrante" to listOf("Espécies de flagrante", "Audiência de custódia", "Conversão em preventiva"),
                "Prisão Preventiva e Temporária" to listOf("Requisitos do art. 312", "Contemporaneidade", "Revisão nonagesimal")
            )
        ),
        "Direito Constitucional" to linkedMapOf(
            "Direitos e Garantias Fundamentais" to linkedMapOf(
                "Direitos Individuais (Art. 5º)" to listOf("Inviolabilidade domiciliar", "Sigilo de dados", "Remédios constitucionais"),
                "Nacionalidade e Direitos Políticos" to listOf("Brasileiros natos e naturalizados", "Inelegibilidades")
            ),
            "Controle de Constitucionalidade" to linkedMapOf(
                "Controle Concentrado" to listOf("ADI e ADC", "ADPF", "Modulação de efeitos"),
                "Controle Difuso" to listOf("Cláusula de reserva de plenário (SV 10)", "Repercussão geral")
            ),
            "Organização do Estado e Poderes" to linkedMapOf(
                "Repartição de Competências" to listOf("Competência privativa da União", "Competência concorrente"),
                "Segurança Pública (Art. 144)" to listOf("Atribuições da Polícia Civil", "Atribuições da Polícia Federal")
            )
        ),
        "Direito Administrativo" to linkedMapOf(
            "Atos Administrativos" to linkedMapOf(
                "Requisitos e Atributos" to listOf("Competência e delegação", "Autoexecutoriedade", "Presunção de legitimidade"),
                "Extinção dos Atos" to listOf("Anulação e convalidação", "Revogação", "Cassação")
            ),
            "Poderes Administrativos" to linkedMapOf(
                "Poder de Polícia" to listOf("Ciclo de polícia", "Delegação a pessoas jurídicas de direito privado"),
                "Poder Disciplinar e Hierárquico" to listOf("Avocação", "Sindicância e PAD")
            ),
            "Licitações e Improbidade" to linkedMapOf(
                "Nova Lei de Licitações (14.133/2021)" to listOf("Modalidades", "Dispensa e inexigibilidade", "Critérios de julgamento"),
                "Improbidade Administrativa (Lei 14.230/21)" to listOf("Exigência de dolo específico", "Prescrição", "Sanções")
            )
        ),
        "Legislação Penal Especial" to linkedMapOf(
            "Leis Penais Extravagantes" to linkedMapOf(
                "Lei de Drogas (11.343/06)" to listOf("Tráfico privilegiado", "Desclassificação para uso", "Flagrante preparado/esperado"),
                "Lei Maria da Penha (11.340/06)" to listOf("Medidas protetivas de urgência", "Natureza da ação penal", "Súmulas STJ"),
                "Organizações Criminosas (12.850/13)" to listOf("Colaboração premiada", "Ação controlada", "Infiltração de agentes"),
                "Abuso de Autoridade (13.869/19)" to listOf("Elemento subjetivo especial", "Crimes em espécie")
            )
        ),
        "Língua Portuguesa" to linkedMapOf(
            "Interpretação e Gramática" to linkedMapOf(
                "Sintaxe e Pontuação" to listOf("Uso da vírgula", "Concordância verbal e nominal", "Regência e crase"),
                "Compreensão Textual" to listOf("Inferência textual", "Coesão e coerência", "Reescrita de frases")
            )
        )
    )

    fun disciplines(): List<String> = hierarchy.keys.toList()
    fun topicsFor(discipline: String): List<String> = hierarchy[discipline]?.keys?.toList().orEmpty()
    fun subtopicsFor(discipline: String, topic: String): List<String> =
        hierarchy[discipline]?.get(topic)?.keys?.toList().orEmpty()
    fun microtopicsFor(discipline: String, topic: String, subtopic: String): List<String> =
        hierarchy[discipline]?.get(topic)?.get(subtopic).orEmpty()
}
