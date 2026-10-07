package com.example.fastcash.data.remote

import retrofit2.Response
import retrofit2.http.*

interface FastCashApiService {

    @GET("api/status")
    suspend fun getStatus(): Response<StatusResponse>

    @GET("api/transactions")
    suspend fun getTransactions(
        @Query("status") status: String? = null
    ): Response<List<TransactionDto>>

    @GET("api/transactions/{ref}")
    suspend fun getTransactionByReference(
        @Path("ref") ref: String
    ): Response<TransactionDto>

    @POST("api/deposits")
    suspend fun createDeposit(
        @Body request: DepositRequest
    ): Response<TransactionResponse>

    @POST("api/withdrawals")
    suspend fun createWithdrawal(
        @Body request: WithdrawalRequest
    ): Response<TransactionResponse>

    @PATCH("api/admin/transactions/{id}")
    suspend fun updateTransactionStatus(
        @Path("id") id: Long,
        @Body request: AdminUpdateStatusRequest
    ): Response<Map<String, Any>>

    @GET("api/agent-accounts")
    suspend fun getAgentAccounts(): Response<List<AgentAccountDto>>

    @GET("api/notifications")
    suspend fun getNotifications(): Response<List<NotificationDto>>

    @POST("api/notifications/read-all")
    suspend fun markAllNotificationsAsRead(): Response<Map<String, Any>>

    @GET("api/settings")
    suspend fun getSettings(): Response<AppSettingsDto>

    @POST("api/slips/verify-ocr")
    suspend fun verifySlipOcr(
        @Body request: SlipVerifyRequest
    ): Response<SlipVerifyResponse>
}
