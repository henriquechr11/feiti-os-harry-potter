package com.example.harry_potter.network

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Query

interface HogwartsApi {
    @GET("api/spells")
    suspend fun getSpells(
        @Query("page_size") pageSize: Int = 100
    ): HogwartsSpellsResponse
}

data class HogwartsSpellsResponse(
    @SerializedName("data") val data: List<SpellDto> = emptyList()
)

data class SpellDto(
    val id: String,
    val slug: String?,
    val name: String,
    val incantation: String?,
    val effect: String?,
    val category: String?,
    val hand: String?
)
