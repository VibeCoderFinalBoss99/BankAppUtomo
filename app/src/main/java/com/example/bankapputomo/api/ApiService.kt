package com.example.bankapputomo.api

import retrofit2.Call
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface ApiService {
    @GET("get_budget.php")
    fun getBudgetPlans(@Query("user_id") userId: String): Call<List<BudgetPlanner>>

    @FormUrlEncoded
    @POST("insert_budget.php")
    fun insertBudgetPlan(
        @Field("user_id") userId: String,
        @Field("saving_name") savingName: String,
        @Field("target_amount") targetAmount: String,
        @Field("current_amount") currentAmount: String,
        @Field("target_date") targetDate: String,
        @Field("monthly_plan") monthlyPlan: String
    ): Call<ApiResponse>

    @GET("get_favorite.php")
    fun getFavoriteTransactions(
        @Query("user_id") userId: String,
        @Query("type") type: String
    ): Call<List<FavoriteTransaction>>

    @FormUrlEncoded
    @POST("insert_favorite.php")
    fun insertFavoriteTransaction(
        @Field("user_id") userId: String,
        @Field("transaction_type") transactionType: String,
        @Field("target_name") targetName: String,
        @Field("target_account") targetAccount: String,
        @Field("account_owner") accountOwner: String
    ): Call<ApiResponse>
}
