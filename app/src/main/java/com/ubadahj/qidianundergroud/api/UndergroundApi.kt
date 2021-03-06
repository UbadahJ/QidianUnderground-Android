package com.ubadahj.qidianundergroud.api

import com.ubadahj.qidianundergroud.api.models.undeground.BookJson
import com.ubadahj.qidianundergroud.api.models.undeground.ChapterGroupJson
import retrofit2.http.GET
import retrofit2.http.Path

interface UndergroundApi {

    @GET("public/")
    suspend fun getBooks(): List<BookJson>

    @GET("public/{id}/chapters/")
    suspend fun getChapters(@Path("id") id: String): List<ChapterGroupJson>

}
