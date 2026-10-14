package com.example.bankapputomo.util

/**
 * Compatibility holder for the legacy Realtime Database endpoint.
 *
 * Business data is stored in Cloud Firestore through BackOfficeRepository.
 * Realtime Database is kept only because the maintenance screen still reads
 * the existing /config/maintenance flag during app startup.
 */
object FirebaseSync {
    const val FIREBASE_DATABASE_URL =
        "https://utomobank-ab445-default-rtdb.asia-southeast1.firebasedatabase.app"
}
