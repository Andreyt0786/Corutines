package dto

data class PostWithComments(
    val post :Post,
    val author: Author,
    val comments:List<Comment>
)
