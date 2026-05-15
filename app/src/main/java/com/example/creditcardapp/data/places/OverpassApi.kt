package com.example.creditcardapp.data.places

import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.Headers
import retrofit2.http.POST

interface OverpassApi {
    // POST is the Overpass-recommended call style for larger queries.
    // Explicit Accept: */* avoids 406 responses from the Apache front-end,
    // which otherwise rejects the converter's default Accept: application/json.
    @FormUrlEncoded
    @Headers("Accept: */*")
    @POST("api/interpreter")
    suspend fun query(@Field("data") body: String): OverpassResponse
}
