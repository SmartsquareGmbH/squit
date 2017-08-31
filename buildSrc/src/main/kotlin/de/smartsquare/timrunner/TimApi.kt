package de.smartsquare.timrunner

import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * [retrofit2.Retrofit] interface of the tim API.
 *
 * @author Ruben Gees
 */
interface TimApi {

    @Headers("Content-Type: application/soap+xml; charset=utf-8")
    @POST("/tim/{endpoint}?WSDL")
    fun request(@Path("endpoint") endpoint: String, @Body body: String): Call<ResponseBody>
}
