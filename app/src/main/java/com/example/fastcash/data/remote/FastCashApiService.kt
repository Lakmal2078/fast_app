package com.example.fastcash.data.remote

import retrofit2.Response
import retrofit2.http.*

interface FastCashApiService {

    @GET("api/session")
    suspend fun session(): Response<CustomerSession>

    @GET("api/agent/requests/{ref}")
    suspend fun review(@Path("ref") ref: String): Response<AgentReview>

    @POST("api/agent/requests/{ref}/decision")
    suspend fun decide(@Path("ref") ref: String, @Body decision: AdminUpdateStatusRequest): Response<Map<String, Boolean>>

    @POST("api/agent/accounts")
    suspend fun publishAccount(@Body account: AgentBankInput): Response<Map<String, Boolean>>

    @POST("api/agent/accounts/{id}/disable")
    suspend fun disableAccount(@Path("id") id: Long): Response<Map<String, Boolean>>

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
        @Header("Idempotency-Key") key: String,
        @Body request: DepositRequest
    ): Response<TransactionDto>

    @POST("api/withdrawals")
    suspend fun createWithdrawal(
        @Header("Idempotency-Key") key: String,
        @Body request: WithdrawalRequest
    ): Response<TransactionDto>

    @PATCH("api/admin/transactions/{id}")
    suspend fun updateTransactionStatus(
        @Path("id") id: Long,
        @Body request: AdminUpdateStatusRequest
    ): Response<Map<String, Any>>

    @GET("api/agent-accounts")
    suspend fun getAgentAccounts(): Response<List<AgentAccountDto>>

    @GET("api/notifications")
    suspend fun getNotifications(): Response<List<PlayerDecision>>

    @POST("api/notifications/read-all")
    suspend fun markAllNotificationsAsRead(): Response<Map<String, Any>>

    @GET("api/settings")
    suspend fun getSettings(): Response<AppSettingsDto>

    @POST("api/slips/verify-ocr")
    suspend fun verifySlipOcr(
        @Body request: SlipVerifyRequest
    ): Response<SlipVerifyResponse>
}
