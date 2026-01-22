package org.example.project.home.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import org.example.project.home.data.remote.dto.ActiveIssuesDto
import org.example.project.home.data.remote.dto.PostWithProfileDto
import org.example.project.home.domain.models.PostLevel

/**
 * Remote data source for fetching posts and active issues from Supabase
 */
class FeedRemoteDataSource(
    private val supabaseClient: SupabaseClient
) {
    /**
     * Fetch posts for a specific level from Supabase
     * Includes profile information via foreign key
     */
    suspend fun fetchPosts(postLevel: PostLevel): List<PostWithProfileDto> {
        return supabaseClient
            .from("posts")
            .select(
                columns = Columns.raw("""
                    id,
                    user_id,
                    post_level,
                    location,
                    post_text,
                    media_type,
                    media_url,
                    likes,
                    comments,
                    created_at,
                    profiles!posts_user_id_fkey (
                        id,
                        name,
                        image_url
                    )
                """.trimIndent())
            ) {
                filter {
                    eq("post_level", postLevel.name)
                }
                order(column = "created_at", order = Order.DESCENDING)
                limit(count = 50) // Limit to 50 most recent posts
            }
            .decodeList<PostWithProfileDto>()
    }

    /**
     * Fetch active issues count for a specific level from Supabase
     */
    suspend fun fetchActiveIssuesCount(postLevel: PostLevel): Int {
        val result = supabaseClient
            .from("active_issues_count")
            .select {
                filter {
                    eq("level", postLevel.name)
                }
                single()
            }
            .decodeAs<ActiveIssuesDto>()

        return result.totalActiveIssues
    }

    /**
     * Fetch all posts (used for debugging or initial load)
     */
    suspend fun fetchAllPosts(): List<PostWithProfileDto> {
        return supabaseClient
            .from("posts")
            .select(
                columns = Columns.raw("""
                    id,
                    user_id,
                    post_level,
                    location,
                    post_text,
                    media_type,
                    media_url,
                    likes,
                    comments,
                    created_at,
                    profiles!posts_user_id_fkey (
                        id,
                        name,
                        image_url
                    )
                """.trimIndent())
            ) {
                order(column = "created_at", order = Order.DESCENDING)
                limit(count = 100)
            }
            .decodeList<PostWithProfileDto>()
    }
}

