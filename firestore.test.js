const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const PEMBINA_UID = "pembina_123";
const ANGGOTA_UID = "anggota_456";
const OTHER_UID = "other_789";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

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
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(PEMBINA_UID).get());
  await assertFails(unauthDb.collection("gudep").doc("01_001").get());
  await assertFails(
    unauthDb.collection("gudep").doc("01_001").set({
      ownerId: PEMBINA_UID,
      ownerName: "Kak Budi",
      institutionName: "SMPN 1 Nusantara",
      gudepNumber: "01.001",
      address: "Jl. Pramuka No. 1",
      createdAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("User profile and backup PII are strictly isolated to owner", async () => {
  const pembinaDb = testEnv.authenticatedContext(PEMBINA_UID).firestore();
  const anggotaDb = testEnv.authenticatedContext(ANGGOTA_UID).firestore();
  const now = new Date(Date.now() - 2000);

  await assertSucceeds(
    pembinaDb.collection("users").doc(PEMBINA_UID).set({
      userId: PEMBINA_UID,
      fullName: "Kak Budi Santoso",
      username: "budi_pembina",
      email: "budi@example.com",
      jenjang: "Pembina",
      institutionName: "SMPN 1 Nusantara",
      gudepNumber: "01.001",
      unitName: "Gudep 01.001",
      xp: 120,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Partial update succeeds while preserving immutable fields
  await assertSucceeds(
    pembinaDb.collection("users").doc(PEMBINA_UID).update({
      unitName: "Pasukan Garuda",
      xp: 150,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Cannot write extra @DocumentId field or invalid jenjang
  await assertFails(
    anggotaDb.collection("users").doc(ANGGOTA_UID).set({
      id: ANGGOTA_UID,
      userId: ANGGOTA_UID,
      fullName: "Siti",
      username: "siti",
      email: "siti@example.com",
      jenjang: "Penggalang",
      institutionName: "",
      gudepNumber: "",
      unitName: "",
      xp: 0,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertFails(
    anggotaDb.collection("users").doc(ANGGOTA_UID).set({
      userId: ANGGOTA_UID,
      fullName: "Siti",
      username: "siti",
      email: "siti@example.com",
      jenjang: "InvalidRank",
      institutionName: "",
      gudepNumber: "",
      unitName: "",
      xp: 0,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Cross-user read/write on profile & backups must fail
  await assertFails(anggotaDb.collection("users").doc(PEMBINA_UID).get());
  await assertFails(anggotaDb.collection("users").doc(PEMBINA_UID).update({ xp: 999 }));

  await assertSucceeds(
    pembinaDb.collection("users").doc(PEMBINA_UID).collection("backups").doc("latest").set({
      payloadJson: "{\"version\":1}",
      deviceLabel: "Android",
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertFails(
    anggotaDb.collection("users").doc(PEMBINA_UID).collection("backups").doc("latest").get()
  );
});

test("Gudep, materials, and tasks enforce Pembina ownership and aligned member queries", async () => {
  const pembinaDb = testEnv.authenticatedContext(PEMBINA_UID).firestore();
  const anggotaDb = testEnv.authenticatedContext(ANGGOTA_UID).firestore();
  const otherDb = testEnv.authenticatedContext(OTHER_UID).firestore();
  const now = new Date(Date.now() - 5000);
  const gudepId = "01_001";

  // Pembina registers Gudep
  await assertSucceeds(
    pembinaDb.collection("gudep").doc(gudepId).set({
      ownerId: PEMBINA_UID,
      ownerName: "Kak Budi",
      institutionName: "SMPN 1 Nusantara",
      gudepNumber: "01.001",
      address: "Jl. Merdeka No. 10",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Anggota can look up Gudep by exact ID; cannot overwrite or list all gudep unfiltered
  await assertSucceeds(anggotaDb.collection("gudep").doc(gudepId).get());
  await assertFails(anggotaDb.collection("gudep").get());
  await assertFails(
    anggotaDb.collection("gudep").doc(gudepId).update({
      institutionName: "Hijacked",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Pembina lists own Gudep
  await assertSucceeds(
    pembinaDb.collection("gudep").where("ownerId", "==", PEMBINA_UID).get()
  );

  // Pembina publishes material & draft material
  await assertSucceeds(
    pembinaDb.collection("gudep").doc(gudepId).collection("materials").doc("mat_1").set({
      ownerId: PEMBINA_UID,
      gudepId: gudepId,
      title: "Latihan Sandi Morse",
      category: "Sandi & Morse",
      jenjangTarget: "Penggalang",
      summary: "Dasar titik dan garis morse",
      content: "Pelajari huruf A sampai Z dengan ritme ketukan.",
      published: true,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(
    pembinaDb.collection("gudep").doc(gudepId).collection("materials").doc("mat_draft").set({
      ownerId: PEMBINA_UID,
      gudepId: gudepId,
      title: "Draf Pionering",
      category: "Pionering",
      jenjangTarget: "Penggalang",
      summary: "Masih disusun",
      content: "Materi belum final.",
      published: false,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Non-owner cannot create material in Pembina's Gudep
  await assertFails(
    otherDb.collection("gudep").doc(gudepId).collection("materials").doc("mat_evil").set({
      ownerId: OTHER_UID,
      gudepId: gudepId,
      title: "Spam",
      category: "Lainnya",
      jenjangTarget: "Semua",
      summary: "Spam",
      content: "Spam",
      published: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Member query for published materials in their Gudep succeeds; unfiltered draft list fails
  await assertSucceeds(
    anggotaDb
      .collection("gudep")
      .doc(gudepId)
      .collection("materials")
      .where("gudepId", "==", gudepId)
      .where("published", "==", true)
      .get()
  );
  await assertFails(
    anggotaDb.collection("gudep").doc(gudepId).collection("materials").get()
  );

  // Pembina creates task and member queries tasks for their Gudep
  await assertSucceeds(
    pembinaDb.collection("gudep").doc(gudepId).collection("tasks").doc("task_1").set({
      ownerId: PEMBINA_UID,
      gudepId: gudepId,
      title: "Praktik Simpul Pangkal",
      description: "Dokumentasikan simpul pangkal pada tongkat.",
      jenjangTarget: "Penggalang",
      dueDateLabel: "30 Nov 2026",
      xpReward: 50,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(
    anggotaDb
      .collection("gudep")
      .doc(gudepId)
      .collection("tasks")
      .where("gudepId", "==", gudepId)
      .get()
  );

  // Future timestamp rejected
  await assertFails(
    pembinaDb.collection("gudep").doc(gudepId).collection("tasks").doc("task_future").set({
      ownerId: PEMBINA_UID,
      gudepId: gudepId,
      title: "Future",
      description: "Test",
      jenjangTarget: "Semua",
      dueDateLabel: "Besok",
      xpReward: 10,
      createdAt: new Date(Date.now() + 60000),
      updatedAt: new Date(Date.now() + 60000),
    })
  );
});
