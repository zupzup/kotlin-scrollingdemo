package org.zupzup.scrollingdemo.web.controller

import com.fasterxml.jackson.annotation.JsonInclude
import mu.KLogging
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.zupzup.scrollingdemo.web.data.User
import org.zupzup.scrollingdemo.web.service.UserService
import java.lang.RuntimeException

@JsonInclude(JsonInclude.Include.NON_NULL)
class ResultData<out T>(
    val data: T? = null,
    val error: ErrorDTO? = null
)

class ErrorDTO(
    val message: String
)

data class UserScrollingDTO(
        val scrollParam: String,
        val users: List<User>
)

fun <T> createSuccess(res: T): ResponseEntity<ResultData<T>> =
        ResponseEntity.ok(ResultData(res))

fun <T> createError(errorMessage: String): ResponseEntity<ResultData<T>> =
        ResponseEntity.badRequest()
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .body(ResultData(error = ErrorDTO(message = errorMessage)))

class UserScrollParamNotFoundInCacheException(scrollParam: String): RuntimeException(
        "Unable to find scrollParam $scrollParam in cache"
)

@RestController
@RequestMapping("rest/v1/user")
class ScrollingController(
        private val userService: UserService
) {
    companion object : KLogging()

    @GetMapping("/scroll")
    fun listUsers(
            @RequestParam(name = "scrollParam", required = false) scrollParam: String?
    ): ResponseEntity<ResultData<UserScrollingDTO>> {
        return try {
            val result = userService.scrollUsers(scrollParam)
            createSuccess(UserScrollingDTO(scrollParam = result.scrollParam, users = result.users))
        } catch (e: UserScrollParamNotFoundInCacheException) {
            return createError("You must provide an active scrollParam, the scrollParam `$scrollParam` does not exist")
        }
    }
}