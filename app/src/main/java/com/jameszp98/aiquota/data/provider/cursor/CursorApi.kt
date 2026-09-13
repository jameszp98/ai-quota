package com.jameszp98.aiquota.data.provider.cursor

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * Retrofit interface for Cursor API endpoints.
 *
 * UNOFFICIAL APIS - These are community-documented endpoints that may change without notice.
 */
interface CursorApi {

    /**
     * Primary endpoint: GetCurrentPeriodUsage
     *
     * URL: POST https://api2.cursor.sh/aiserver.v1.DashboardService/GetCurrentPeriodUsage
     * Headers:
     *   - Content-Type: application/json
     *   - Connect-Protocol-Version: 1
     *   - Authorization: Bearer <access_token>
     * Body: {}
     */
    @POST("aiserver.v1.DashboardService/GetCurrentPeriodUsage")
    suspend fun getCurrentPeriodUsage(
        @Header("Authorization") authorization: String,
        @Header("Connect-Protocol-Version") connectProtocolVersion: String = "1",
        @Body body: Map<String, String> = emptyMap()
    ): Response<CursorUsageResponse>
}

/**
 * Retrofit interface for Cursor web API (fallback).
 */
interface CursorWebApi {

    /**
     * Fallback endpoint: Usage Summary
     *
     * URL: GET https://cursor.com/api/usage-summary
     * Headers:
     *   - Cookie: WorkosCursorSessionToken=<token>
     */
    @GET("api/usage-summary")
    suspend fun getUsageSummary(
        @Header("Cookie") cookie: String
    ): Response<CursorUsageSummaryResponse>
}
