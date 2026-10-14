package com.example.bankapputomo.model

import com.google.firebase.Timestamp

data class TicketModel(
    var id: String = "",
    val userId: String = "",
    val subject: String = "",
    val category: String = "",
    val priority: String = "normal",
    var status: String = "open",
    val assignedTo: String? = null,
    val assignedToName: String? = null,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
    val lastMessageAt: Timestamp? = null,
    val lastMessageBy: String? = null,
    var unreadForUser: Boolean = false,
    var unreadForStaff: Boolean = true,
    var rating: Int? = null,
    val customerLanguage: String = "id"
)

data class TicketMessageModel(
    var id: String = "",
    val senderType: String = "customer",
    // Kept for compatibility with older Admin Panel messages.
    val senderRole: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val text: String = "",
    val createdAt: Timestamp? = null
)

data class InAppNotificationModel(
    var id: String = "",
    val type: String = "",
    val params: Map<String, Any?> = emptyMap(),
    var read: Boolean = false,
    val createdAt: Timestamp? = null
)
