package app.desperse.data.repository

import app.desperse.core.network.ApiResult
import app.desperse.core.network.DesperseApi
import app.desperse.core.network.safeApiCall
import app.desperse.data.dto.response.SearchUser
import app.desperse.data.dto.response.SuggestedCreator
import app.desperse.data.model.Post
import javax.inject.Inject
import javax.inject.Singleton

data class TrendingData(
    val posts: List<Post>,
    val hasMore: Boolean,
    val nextOffset: Int?,
    val isFallback: Boolean,
    val sectionTitle: String
)

data class SearchData(
    val users: List<SearchUser>,
    val posts: List<Post>,
    val query: String
)

@Singleton
class ExploreRepository @Inject constructor(
    private val api: DesperseApi
) {

    suspend fun getSuggestedCreators(limit: Int = 8): Result<List<SuggestedCreator>> {
        return when (val result = safeApiCall { api.getSuggestedCreators(limit) }) {
            is ApiResult.Success -> Result.success(result.data.creators)
            is ApiResult.Error -> Result.failure(Exception(result.message))
        }
    }

    suspend fun getTrendingPosts(
        offset: Int = 0,
        limit: Int = 20
    ): Result<TrendingData> {
        return when (val result = safeApiCall { api.getTrendingPosts(offset, limit) }) {
            is ApiResult.Success -> Result.success(
                TrendingData(
                    posts = result.data.posts,
                    hasMore = result.data.hasMore,
                    nextOffset = result.data.nextOffset,
                    isFallback = result.data.isFallback,
                    sectionTitle = result.data.sectionTitle
                )
            )
            is ApiResult.Error -> Result.failure(Exception(result.message))
        }
    }

    suspend fun search(
        query: String,
        type: String = "all",
        limit: Int = 20
    ): Result<SearchData> {
        return when (val result = safeApiCall { api.search(query, type, limit) }) {
            is ApiResult.Success -> Result.success(
                SearchData(
                    users = result.data.users,
                    posts = result.data.posts,
                    query = result.data.query
                )
            )
            is ApiResult.Error -> Result.failure(Exception(result.message))
        }
    }
}
