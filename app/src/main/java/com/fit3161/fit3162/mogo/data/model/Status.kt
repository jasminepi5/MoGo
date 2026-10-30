package com.fit3161.fit3162.mogo.data.model

/**
 * Values of the `status` enum in Postgres (rides.ride_status).
 * Kept as String constants so an unexpected value from the database can't crash decoding.
 */
object RideStatus {
    const val SCHEDULED = "scheduled"
    const val IN_PROGRESS = "in_progress"
    const val COMPLETED = "completed"
    const val CANCELLED = "cancelled"
}

/**
 * Values of the `booking_status` enum in Postgres (bookings.booking_status).
 */
object BookingStatus {
    const val PENDING = "pending"
    const val CONFIRMED = "confirmed"
    const val CANCELLED = "cancelled"
    const val COMPLETED = "completed"
}
