import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dto.Author
import dto.Comment
import dto.Post
import dto.PostWithComments
import io.reactivex.internal.operators.completable.CompletableDefer
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

private val gson = Gson()
private val BASE_URL = "http://127.0.0.1:9999"
private val client = OkHttpClient.Builder()
    .addInterceptor(HttpLoggingInterceptor(::println).apply {
        level = HttpLoggingInterceptor.Level.BODY
    })
    .connectTimeout(50, TimeUnit.SECONDS)
    .build()

suspend fun OkHttpClient.apiCall(url: String): Response {
    return suspendCoroutine { continuation ->
        Request.Builder()
            .url(url)
            .build()
            .let(this::newCall)
            .enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    continuation.resumeWithException(e)
                }

                override fun onResponse(call: Call, response: Response) {
                    continuation.resume(response)
                }

            })
    }
}

suspend fun <T> makeRequest(url: String, typeToken: TypeToken<T>): T =
    gson.fromJson(client.apiCall(url).body?.string(), typeToken)


fun main() {
    var postWithComments: List<PostWithComments> = emptyList()
    with(CoroutineScope(EmptyCoroutineContext)) {
        launch {
            val posts = makeRequest(url = "$BASE_URL/api/posts", object : TypeToken<List<Post>>() {})
            posts.map { post ->
                async {
                    postWithComments = listOf(PostWithComments(post,
                        makeRequest(url = "$BASE_URL/api/authors/${post.id}", object : TypeToken<Author>() {}),
                        makeRequest(url = "$BASE_URL/api/posts/${post.id}/comments",
                            object : TypeToken<List<Comment>>() {})))
                }
            }.awaitAll()
        }
    }
    Thread.sleep(50_000L)
}