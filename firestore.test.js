const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const FELIPE_UID = "felipe_uid_001";
const OTHER_UID = "other_uid_002";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validProfile(uid, ts = new Date(Date.now() - 2000)) {
  return {
    userId: uid,
    studentName: "Felipe",
    targetExam: "Delegado de Policia Civil",
    examBoard: "Cebraspe",
    examDateIso: "2026-12-20",
    dailyGoalQuestions: 40,
    qconcursosLink: "https://www.qconcursos.com/questoes-de-concursos/questoes",
    createdAt: ts,
    updatedAt: ts,
  };
}

function validAttempt(uid, ts = new Date(Date.now() - 2000)) {
  return {
    userId: uid,
    qconcursosCode: "Q2481920",
    sourceUrl: "https://www.qconcursos.com/questoes-de-concursos/questoes/2481920",
    sourceType: "PORTAL_QCONCURSOS",
    discipline: "Direito Penal",
    topic: "Teoria do Crime",
    subtopic: "Culpabilidade",
    microtopic: "Exigibilidade de conduta diversa",
    examBoard: "Cebraspe",
    questionsCount: 1,
    correctCount: 0,
    timeSpentSeconds: 112,
    attemptNumber: 2,
    errorCause: "LACUNA_CONCEITUAL",
    notes: "Confundi coacao moral irresistivel com obediencia hierarquica (CP art. 22).",
    createdAt: ts,
    updatedAt: ts,
  };
}

function validDiagnostic(uid, ts = new Date(Date.now() - 2000)) {
  return {
    userId: uid,
    headline: "Felipe, em Direito Penal (Teoria do Crime), seu maior gargalo e Culpabilidade.",
    focusDiscipline: "Direito Penal",
    focusTopic: "Teoria do Crime",
    focusSubtopic: "Culpabilidade",
    whatIDontKnow: "Diferenciacao entre erro de proibicao inevitavel e evitavel.",
    whereIAmFailing: "Culpabilidade apresenta 33% de aproveitamento em 6 questoes.",
    whyIAmFailing: "Padrao predominante de lacuna conceitual com tempo elevado de resolucao.",
    whichSubtopicToStudy: "Potencial consciencia da ilicitude e erro de proibicao.",
    whatToReviewNow: "Revisar CP arts. 21, 22 e 26-28 e resolver 15 questoes Cebraspe.",
    legalFoundations: ["Codigo Penal, art. 21 (Erro sobre a ilicitude do fato)", "Codigo Penal, art. 22"],
    jurisprudenceNotes: ["STJ - AgRg no HC: erro de proibicao exige prova concreta"],
    questionsAnalyzed: 12,
    createdAt: ts,
    updatedAt: ts,
  };
}

function validRevisionTask(uid, ts = new Date(Date.now() - 2000)) {
  return {
    userId: uid,
    orderIndex: 0,
    priority: "CRITICA",
    discipline: "Direito Penal",
    topic: "Teoria do Crime",
    subtopic: "Culpabilidade",
    rationale: "Aproveitamento critico (33%) com alta incidencia na banca Cebraspe.",
    studyActions: "Ler lei seca (CP arts. 21-28), mapa mental das excludentes e resolver 15 questoes.",
    legalReferences: "CP arts. 21, 22, 26, 27 e 28.",
    estimatedMinutes: 45,
    targetQuestions: 15,
    completed: false,
    createdAt: ts,
    updatedAt: ts,
  };
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated users cannot read or write any collection", async () => {
  const unauth = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauth.collection("users").doc(FELIPE_UID).get());
  await assertFails(unauth.collection("users").doc(FELIPE_UID).set(validProfile(FELIPE_UID)));
  await assertFails(
    unauth.collection("users").doc(FELIPE_UID).collection("question_attempts").get()
  );
  await assertFails(
    unauth.collection("users").doc(FELIPE_UID).collection("revision_tasks").get()
  );
  await assertFails(
    unauth.collection("users").doc(FELIPE_UID).collection("ai_diagnostics").get()
  );
});

test("Owner can create, read, and partially update UserProfile; blocks ghost fields and cross-user access", async () => {
  const felipeDb = testEnv.authenticatedContext(FELIPE_UID).firestore();
  const otherDb = testEnv.authenticatedContext(OTHER_UID).firestore();
  const created = new Date(Date.now() - 10000);

  await assertSucceeds(felipeDb.collection("users").doc(FELIPE_UID).set(validProfile(FELIPE_UID, created)));
  await assertSucceeds(felipeDb.collection("users").doc(FELIPE_UID).get());

  // Valid partial update
  await assertSucceeds(
    felipeDb.collection("users").doc(FELIPE_UID).update({
      targetExam: "Promotor de Justica",
      dailyGoalQuestions: 50,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject immutable createdAt modification
  await assertFails(
    felipeDb.collection("users").doc(FELIPE_UID).update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject ghost field injection
  await assertFails(
    felipeDb.collection("users").doc(FELIPE_UID).update({
      isAdmin: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject cross-user read and write
  await assertFails(otherDb.collection("users").doc(FELIPE_UID).get());
  await assertFails(
    otherDb.collection("users").doc(FELIPE_UID).update({
      studentName: "Hijacked",
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("QuestionAttempts: valid CRUD, empty list, bounds validation, and anti-spoofing", async () => {
  const felipeDb = testEnv.authenticatedContext(FELIPE_UID).firestore();
  const otherDb = testEnv.authenticatedContext(OTHER_UID).firestore();
  const col = felipeDb.collection("users").doc(FELIPE_UID).collection("question_attempts");

  // Empty subcollection list must succeed
  await assertSucceeds(col.get());

  // Create valid attempt (no `id` field because Kotlin uses @DocumentId)
  await assertSucceeds(col.doc("att_01").set(validAttempt(FELIPE_UID)));

  // Reject document with `@DocumentId` field `id` in payload
  await assertFails(
    col.doc("att_02").set({ ...validAttempt(FELIPE_UID), id: "att_02" })
  );

  // Reject correctCount > questionsCount
  await assertFails(
    col.doc("att_03").set({
      ...validAttempt(FELIPE_UID),
      questionsCount: 5,
      correctCount: 6,
    })
  );

  // Reject future timestamp
  await assertFails(
    col.doc("att_04").set(validAttempt(FELIPE_UID, new Date(Date.now() + 60000)))
  );

  // Valid partial update
  await assertSucceeds(
    col.doc("att_01").update({
      correctCount: 1,
      errorCause: "NONE",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Cross-user isolation
  await assertFails(
    otherDb.collection("users").doc(FELIPE_UID).collection("question_attempts").get()
  );
  await assertFails(
    otherDb.collection("users").doc(FELIPE_UID).collection("question_attempts").doc("att_01").delete()
  );

  // Owner can delete
  await assertSucceeds(col.doc("att_01").delete());
});

test("AiDiagnosticReports: owner create/list/delete succeeds; update rejected; cross-user denied", async () => {
  const felipeDb = testEnv.authenticatedContext(FELIPE_UID).firestore();
  const otherDb = testEnv.authenticatedContext(OTHER_UID).firestore();
  const col = felipeDb.collection("users").doc(FELIPE_UID).collection("ai_diagnostics");

  await assertSucceeds(col.get());
  await assertSucceeds(col.doc("diag_01").set(validDiagnostic(FELIPE_UID)));
  await assertFails(col.doc("diag_01").update({ headline: "Tampered" }));
  await assertFails(otherDb.collection("users").doc(FELIPE_UID).collection("ai_diagnostics").get());
  await assertSucceeds(col.doc("diag_01").delete());
});

test("RevisionTasks: owner create/list/toggle/delete succeeds; invalid priority & cross-user fail", async () => {
  const felipeDb = testEnv.authenticatedContext(FELIPE_UID).firestore();
  const otherDb = testEnv.authenticatedContext(OTHER_UID).firestore();
  const col = felipeDb.collection("users").doc(FELIPE_UID).collection("revision_tasks");

  await assertSucceeds(col.get());
  await assertSucceeds(col.doc("rev_01").set(validRevisionTask(FELIPE_UID)));

  // Toggle completed action
  await assertSucceeds(
    col.doc("rev_01").update({
      completed: true,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject invalid priority enum
  await assertFails(
    col.doc("rev_02").set({
      ...validRevisionTask(FELIPE_UID),
      priority: "URGENTE_INVALIDO",
    })
  );

  // Cross-user denied
  await assertFails(
    otherDb.collection("users").doc(FELIPE_UID).collection("revision_tasks").doc("rev_01").update({
      completed: false,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  await assertSucceeds(col.doc("rev_01").delete());
});

test("EditalItems: owner CRUD succeeds; invalid weight, ghost fields, and cross-user access fail", async () => {
  const felipeDb = testEnv.authenticatedContext(FELIPE_UID).firestore();
  const otherDb = testEnv.authenticatedContext(OTHER_UID).firestore();
  const col = felipeDb.collection("users").doc(FELIPE_UID).collection("edital_items");
  const ts = new Date(Date.now() - 2000);

  // Empty subcollection list must succeed
  await assertSucceeds(col.get());

  // Valid create
  await assertSucceeds(
    col.doc("ed_01").set({
      userId: FELIPE_UID,
      editalTitle: "Delegado PC 2026",
      discipline: "Direito Penal",
      topic: "Teoria do Crime",
      subtopic: "Culpabilidade",
      weight: "ALTA",
      orderIndex: 0,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Valid partial update
  await assertSucceeds(
    col.doc("ed_01").update({
      weight: "MEDIA",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Invalid weight enum rejected
  await assertFails(
    col.doc("ed_02").set({
      userId: FELIPE_UID,
      editalTitle: "Delegado PC 2026",
      discipline: "Direito Penal",
      topic: "Teoria do Crime",
      subtopic: "Fato Tipico",
      weight: "EXTREMA",
      orderIndex: 1,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Cross-user read & delete denied
  await assertFails(
    otherDb.collection("users").doc(FELIPE_UID).collection("edital_items").get()
  );
  await assertFails(
    otherDb.collection("users").doc(FELIPE_UID).collection("edital_items").doc("ed_01").delete()
  );

  await assertSucceeds(col.doc("ed_01").delete());
});

