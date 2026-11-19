package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.core.OperationType
import com.example.core.handleFirestoreError
import com.example.data.model.AiDiagnosticReport
import com.example.data.model.EditalItem
import com.example.data.model.EditalItemDraft
import com.example.data.model.ErrorCause
import com.example.data.model.ImportedAttemptDraft
import com.example.data.model.QuestionAttempt
import com.example.data.model.RevisionPriority
import com.example.data.model.RevisionTask
import com.example.data.model.SourceType
import com.example.data.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class MentorRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("Usuário precisa estar autenticado com Google.")
    }

    private fun userDoc(uid: String) = db.collection("users").document(uid)
    private fun editalCol(uid: String) = userDoc(uid).collection("edital_items")
    private fun attemptsCol(uid: String) = userDoc(uid).collection("question_attempts")
    private fun diagnosticsCol(uid: String) = userDoc(uid).collection("ai_diagnostics")
    private fun revisionTasksCol(uid: String) = userDoc(uid).collection("revision_tasks")

    // --- Real-time Observers ---

    fun observeProfile(userId: String): Flow<UserProfile?> = flow {
        val path = "users/$userId"
        emitAll(
            userDoc(userId)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            UserProfile::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else null
                }
                .catch { error ->
                    handleFlowError(error, OperationType.GET, path, userId)
                    throw error
                }
        )
    }

    fun observeEditalItems(userId: String): Flow<List<EditalItem>> = flow {
        val path = "users/$userId/edital_items"
        emitAll(
            editalCol(userId)
                .orderBy("orderIndex", Query.Direction.ASCENDING)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        EditalItem::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, path, userId)
                    throw error
                }
        )
    }

    fun observeAttempts(userId: String): Flow<List<QuestionAttempt>> = flow {
        val path = "users/$userId/question_attempts"
        emitAll(
            attemptsCol(userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        QuestionAttempt::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, path, userId)
                    throw error
                }
        )
    }

    fun observeDiagnostics(userId: String): Flow<List<AiDiagnosticReport>> = flow {
        val path = "users/$userId/ai_diagnostics"
        emitAll(
            diagnosticsCol(userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(10)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        AiDiagnosticReport::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, path, userId)
                    throw error
                }
        )
    }

    fun observeRevisionTasks(userId: String): Flow<List<RevisionTask>> = flow {
        val path = "users/$userId/revision_tasks"
        emitAll(
            revisionTasksCol(userId)
                .orderBy("orderIndex", Query.Direction.ASCENDING)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        RevisionTask::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, path, userId)
                    throw error
                }
        )
    }

    private fun handleFlowError(
        error: Throwable,
        operationType: OperationType,
        path: String,
        expectedUid: String
    ) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == expectedUid) {
            handleFirestoreError(exceptionToLog, operationType, path)
        }
    }

    // --- Profile Mutations ---

    suspend fun ensureUserProfile(defaultName: String): Result<Unit> {
        var path = "users/unauthenticated"
        return try {
            val uid = requireUserId()
            path = "users/$uid"
            val docRef = userDoc(uid)
            val existing = docRef.get().await()
            if (!existing.exists()) {
                val safeName = defaultName.trim().ifBlank { "Estudante" }.take(100)
                val payload = mapOf(
                    "userId" to uid,
                    "studentName" to safeName,
                    "targetExam" to "Carreiras Jurídicas / Policiais",
                    "examBoard" to "Cebraspe",
                    "examDateIso" to "",
                    "dailyGoalQuestions" to 30L,
                    "qconcursosLink" to "https://www.qconcursos.com/questoes-de-concursos/questoes",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun updateUserProfile(
        studentName: String,
        targetExam: String,
        examBoard: String,
        examDateIso: String,
        dailyGoalQuestions: Int,
        qconcursosLink: String
    ): Result<Unit> {
        var path = "users/unauthenticated"
        return try {
            val uid = requireUserId()
            path = "users/$uid"
            val docRef = userDoc(uid)
            val safeName = studentName.trim().ifBlank { "Estudante" }.take(100)
            val safeBoard = examBoard.trim().ifBlank { "Cebraspe" }.take(80)
            val safeGoal = dailyGoalQuestions.coerceIn(1, 500).toLong()
            val safeTarget = targetExam.trim().take(140)
            val safeDate = examDateIso.trim().take(20)
            val safeLink = qconcursosLink.trim().take(400)

            val snap = docRef.get().await()
            if (snap.exists()) {
                docRef.update(
                    mapOf(
                        "studentName" to safeName,
                        "targetExam" to safeTarget,
                        "examBoard" to safeBoard,
                        "examDateIso" to safeDate,
                        "dailyGoalQuestions" to safeGoal,
                        "qconcursosLink" to safeLink,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "studentName" to safeName,
                        "targetExam" to safeTarget,
                        "examBoard" to safeBoard,
                        "examDateIso" to safeDate,
                        "dailyGoalQuestions" to safeGoal,
                        "qconcursosLink" to safeLink,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // --- Edital Hierarchical Items Mutations ---

    suspend fun addEditalItem(draft: EditalItemDraft, orderIndex: Int = 0): Result<Unit> {
        var path = "users/unauthenticated/edital_items"
        return try {
            val uid = requireUserId()
            val docRef = editalCol(uid).document()
            path = docRef.path
            docRef.set(buildEditalPayload(uid, draft, orderIndex)).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun addEditalItemsBatch(
        drafts: List<EditalItemDraft>,
        replaceExisting: Boolean = false
    ): Result<Int> {
        var path = "users/unauthenticated/edital_items"
        return try {
            val uid = requireUserId()
            path = "users/$uid/edital_items"
            val validDrafts = drafts.filter { it.discipline.isNotBlank() }.take(120)
            if (validDrafts.isEmpty()) {
                return Result.failure(IllegalArgumentException("Nenhum item de edital válido para salvar."))
            }
            val col = editalCol(uid)
            val existingDocs = col.get().await().documents
            val batch = db.batch()
            val startIndex = if (replaceExisting) {
                existingDocs.forEach { doc -> batch.delete(doc.reference) }
                0
            } else {
                existingDocs.size
            }
            validDrafts.forEachIndexed { idx, draft ->
                val docRef = col.document()
                batch.set(docRef, buildEditalPayload(uid, draft, startIndex + idx))
            }
            batch.commit().await()
            Result.success(validDrafts.size)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteEditalItem(itemId: String): Result<Unit> {
        var path = "users/unauthenticated/edital_items/$itemId"
        return try {
            val uid = requireUserId()
            require(itemId.isNotBlank()) { "ID do item do edital inválido." }
            val docRef = editalCol(uid).document(itemId)
            path = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun clearEdital(): Result<Unit> {
        var path = "users/unauthenticated/edital_items"
        return try {
            val uid = requireUserId()
            path = "users/$uid/edital_items"
            val existingDocs = editalCol(uid).get().await().documents
            if (existingDocs.isNotEmpty()) {
                val batch = db.batch()
                existingDocs.forEach { batch.delete(it.reference) }
                batch.commit().await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    private fun buildEditalPayload(
        uid: String,
        draft: EditalItemDraft,
        orderIndex: Int
    ): Map<String, Any> {
        val safeTitle = draft.editalTitle.trim().ifBlank { "Meu Edital" }.take(140)
        val safeDiscipline = draft.discipline.trim().ifBlank { "Geral" }.take(120)
        val safeTopic = draft.topic.trim().ifBlank { safeDiscipline }.take(140)
        val safeSubtopic = draft.subtopic.trim().ifBlank { safeTopic }.take(160)
        return mapOf(
            "userId" to uid,
            "editalTitle" to safeTitle,
            "discipline" to safeDiscipline,
            "topic" to safeTopic,
            "subtopic" to safeSubtopic,
            "weight" to draft.weight.wireValue,
            "orderIndex" to orderIndex.coerceIn(0, 5000).toLong(),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
    }

    // --- Question Attempts Mutations ---

    suspend fun addAttempt(draft: ImportedAttemptDraft): Result<Unit> {
        var path = "users/unauthenticated/question_attempts"
        return try {
            val uid = requireUserId()
            val docRef = attemptsCol(uid).document()
            path = docRef.path
            docRef.set(buildAttemptPayload(uid, draft)).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun addAttemptsBatch(drafts: List<ImportedAttemptDraft>): Result<Int> {
        var path = "users/unauthenticated/question_attempts"
        return try {
            val uid = requireUserId()
            path = "users/$uid/question_attempts"
            val validDrafts = drafts.filter { it.discipline.isNotBlank() }.take(100)
            if (validDrafts.isEmpty()) {
                return Result.failure(IllegalArgumentException("Nenhum registro válido para importar."))
            }
            val batch = db.batch()
            validDrafts.forEach { draft ->
                val docRef = attemptsCol(uid).document()
                batch.set(docRef, buildAttemptPayload(uid, draft))
            }
            batch.commit().await()
            Result.success(validDrafts.size)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteAttempt(attemptId: String): Result<Unit> {
        var path = "users/unauthenticated/question_attempts/$attemptId"
        return try {
            val uid = requireUserId()
            require(attemptId.isNotBlank()) { "ID do registro não pode estar vazio." }
            val docRef = attemptsCol(uid).document(attemptId)
            path = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    private fun buildAttemptPayload(uid: String, draft: ImportedAttemptDraft): Map<String, Any> {
        val safeDiscipline = draft.discipline.trim().ifBlank { "Geral" }.take(120)
        val safeTopic = draft.topic.trim().ifBlank { safeDiscipline }.take(140)
        val safeSubtopic = draft.subtopic.trim().ifBlank { safeTopic }.take(160)
        val safeMicrotopic = draft.microtopic.trim().take(160)
        val safeBoard = draft.examBoard.trim().ifBlank { "Cebraspe" }.take(80)
        val safeQuestions = draft.questionsCount.coerceIn(1, 500).toLong()
        val safeCorrect = draft.correctCount.coerceIn(0, safeQuestions.toInt()).toLong()
        val safeTime = draft.timeSpentSeconds.coerceIn(0, 86400).toLong()
        val safeAttemptNum = draft.attemptNumber.coerceIn(1, 50).toLong()
        val safeCause = if (safeCorrect == safeQuestions) {
            ErrorCause.NONE.wireValue
        } else {
            draft.errorCause.wireValue
        }

        return mapOf(
            "userId" to uid,
            "qconcursosCode" to draft.qconcursosCode.trim().uppercase().take(40),
            "sourceUrl" to draft.sourceUrl.trim().take(400),
            "sourceType" to draft.sourceType.wireValue,
            "discipline" to safeDiscipline,
            "topic" to safeTopic,
            "subtopic" to safeSubtopic,
            "microtopic" to safeMicrotopic,
            "examBoard" to safeBoard,
            "questionsCount" to safeQuestions,
            "correctCount" to safeCorrect,
            "timeSpentSeconds" to safeTime,
            "attemptNumber" to safeAttemptNum,
            "errorCause" to safeCause,
            "notes" to draft.notes.trim().take(1000),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
    }

    // --- AI Diagnostic Reports ---

    suspend fun saveDiagnosticReport(report: AiDiagnosticReport): Result<Unit> {
        var path = "users/unauthenticated/ai_diagnostics"
        return try {
            val uid = requireUserId()
            val docRef = diagnosticsCol(uid).document()
            path = docRef.path
            val payload = mapOf(
                "userId" to uid,
                "headline" to report.headline.trim().ifBlank { "Diagnóstico pedagógico" }.take(600),
                "focusDiscipline" to report.focusDiscipline.trim().take(120),
                "focusTopic" to report.focusTopic.trim().take(140),
                "focusSubtopic" to report.focusSubtopic.trim().take(160),
                "whatIDontKnow" to report.whatIDontKnow.trim().take(2500),
                "whereIAmFailing" to report.whereIAmFailing.trim().take(2500),
                "whyIAmFailing" to report.whyIAmFailing.trim().take(2500),
                "whichSubtopicToStudy" to report.whichSubtopicToStudy.trim().take(2500),
                "whatToReviewNow" to report.whatToReviewNow.trim().take(2500),
                "legalFoundations" to report.legalFoundations.map { it.trim().take(300) }.filter { it.isNotBlank() }.take(15),
                "jurisprudenceNotes" to report.jurisprudenceNotes.map { it.trim().take(300) }.filter { it.isNotBlank() }.take(15),
                "questionsAnalyzed" to report.questionsAnalyzed.coerceIn(0L, 100000L),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    // --- Revision Plan Tasks ---

    suspend fun replaceRevisionPlan(tasks: List<RevisionTask>): Result<Unit> {
        var path = "users/unauthenticated/revision_tasks"
        return try {
            val uid = requireUserId()
            path = "users/$uid/revision_tasks"
            val col = revisionTasksCol(uid)
            val existingDocs = col.get().await()
            val batch = db.batch()
            existingDocs.documents.forEach { doc ->
                batch.delete(doc.reference)
            }
            tasks.take(25).forEachIndexed { index, task ->
                val docRef = col.document()
                val safePriority = RevisionPriority.fromWire(task.priority).wireValue
                val payload = mapOf(
                    "userId" to uid,
                    "orderIndex" to index.toLong(),
                    "priority" to safePriority,
                    "discipline" to task.discipline.trim().ifBlank { "Geral" }.take(120),
                    "topic" to task.topic.trim().ifBlank { "Revisão" }.take(140),
                    "subtopic" to task.subtopic.trim().ifBlank { task.topic.ifBlank { "Tópico" } }.take(160),
                    "rationale" to task.rationale.trim().ifBlank { "Prioridade baseada no desempenho." }.take(1000),
                    "studyActions" to task.studyActions.trim().ifBlank { "Revisão ativa e questões." }.take(1500),
                    "legalReferences" to task.legalReferences.trim().take(1000),
                    "estimatedMinutes" to task.estimatedMinutes.coerceIn(5L, 480L),
                    "targetQuestions" to task.targetQuestions.coerceIn(0L, 200L),
                    "completed" to false,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                batch.set(docRef, payload)
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun toggleRevisionTaskCompleted(taskId: String, completed: Boolean): Result<Unit> {
        var path = "users/unauthenticated/revision_tasks/$taskId"
        return try {
            val uid = requireUserId()
            require(taskId.isNotBlank()) { "Task ID inválido." }
            val docRef = revisionTasksCol(uid).document(taskId)
            path = docRef.path
            docRef.update(
                mapOf(
                    "completed" to completed,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteRevisionTask(taskId: String): Result<Unit> {
        var path = "users/unauthenticated/revision_tasks/$taskId"
        return try {
            val uid = requireUserId()
            require(taskId.isNotBlank()) { "Task ID inválido." }
            val docRef = revisionTasksCol(uid).document(taskId)
            path = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }
}
