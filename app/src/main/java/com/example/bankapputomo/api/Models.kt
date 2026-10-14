package com.example.bankapputomo.api

import com.google.gson.annotations.SerializedName

data class ApiResponse(
    val success: Boolean,
    val message: String
)

data class BudgetPlanner(
    val id: String,
    @SerializedName("user_id") val userId: String,
    @SerializedName("saving_name") val savingName: String,
    @SerializedName("target_amount") val targetAmount: String,
    @SerializedName("current_amount") val currentAmount: String,
    @SerializedName("target_date") val targetDate: String,
    @SerializedName("monthly_plan") val monthlyPlan: String
)

data class FavoriteTransaction(
    val id: String,
    @SerializedName("user_id") val userId: String,
    @SerializedName("transaction_type") val transactionType: String,
    @SerializedName("target_name") val targetName: String,
    @SerializedName("target_account") val targetAccount: String,
    @SerializedName("account_owner") val accountOwner: String
)
