const functions = require("firebase-functions");
const admin = require("firebase-admin");
admin.initializeApp();

// Fungsi berjalan saat ada dokumen baru di koleksi 'schedules'
exports.sendNewScheduleNotification = functions.firestore
    .document("schedules/{scheduleId}")
    .onCreate(async (snap, context) => {

        // 1. Ambil data yang baru saja disimpan
        const event = snap.data();

        // 2. Siapkan Pesan Notifikasi
        // Menggunakan data dari database: event.title, event.speaker, dll
        const payload = {
            notification: {
                title: "Jadwal Baru: " + event.title,
                body: "Bersama " + event.speaker + " pukul " + event.time,
                sound: "default"
            },
            // Dikirim ke topik 'jamaah' (Sesuai kode di Android MainActivity)
            topic: "jamaah"
        };

        // 3. Kirim ke Semua HP
        try {
            const response = await admin.messaging().send(payload);
            console.log("Notifikasi berhasil dikirim:", response);
            return null;
        } catch (error) {
            console.error("Gagal mengirim notifikasi:", error);
            return null;
        }
    });