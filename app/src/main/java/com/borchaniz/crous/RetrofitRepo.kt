package com.borchaniz.crous

import com.borchaniz.crous.models.Response
import com.google.gson.JsonObject
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface RetrofitRepo {
    @POST("/api/search/0315afe5-e102-11e9-8c39-005056941f86")
    fun get(@Body s: JsonObject): Call<Response>
}