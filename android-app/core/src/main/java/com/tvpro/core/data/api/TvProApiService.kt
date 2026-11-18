package com.tvpro.core.data.api

import com.tvpro.core.data.model.MovieDetailResponse
import com.tvpro.core.data.model.MovieListResponse
import com.tvpro.core.data.model.MovieSearchResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface TvProApiService {

    @GET("list")
    suspend fun getCatalog(
        @Query("page") page: Int = 1
    ): MovieListResponse

    @GET("extract")
    suspend fun extractMovie(
        @Query("url") movieUrl: String
    ): MovieDetailResponse

    @GET("search")
    suspend fun searchMovies(
        @Query("q") query: String
    ): MovieSearchResponse
}
