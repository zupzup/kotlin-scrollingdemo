package org.zupzup.scrollingdemo.web.service

import org.apache.commons.lang3.RandomStringUtils
import org.springframework.cache.CacheManager
import org.springframework.cache.concurrent.ConcurrentMapCacheManager
import org.springframework.stereotype.Service
import org.zupzup.scrollingdemo.web.controller.UserScrollParamNotFoundInCacheException
import org.zupzup.scrollingdemo.web.data.User
import java.util.*

const val CACHE_KEY_SCROLLING = "scrollRequests"
const val SCROLL_SIZE = 100

data class UserScrollingResult(
        var scrollParam: String,
        var users: List<User>
)

data class UserScrollRequest(
        var scrollParam: String,
        var cursor: Int
)

@Service
class UserService {
    private val listOfUsers: List<User> = initializeUserList()
    private val cacheManager: CacheManager = ConcurrentMapCacheManager(CACHE_KEY_SCROLLING)


    fun scrollUsers(scrollParam: String?): UserScrollingResult {
        val scrollRequest: UserScrollRequest
        val activeScrollParam = scrollParam ?: UUID.randomUUID().toString()
        scrollRequest = if (scrollParam == null) {
            putUserScrollRequestInCache(activeScrollParam)
        } else {
            getUserScrollRequestFromCache(scrollParam)
        }
        val users = fetchUsers(scrollRequest.cursor)
        if (users.isEmpty()) {
            evictUserScrollRequestFromCache(activeScrollParam)
        } else {
            updateUserScrollRequestCursorInCache(activeScrollParam, users.last().id)
        }
        return UserScrollingResult(activeScrollParam, users)
    }

    private fun fetchUsers(cursor: Int): List<User> {
        val result = ArrayList<User>()
        if (cursor > listOfUsers.size) return result
        for (user in listOfUsers) {
            if (user.id <= cursor) continue
            if (user.id >= cursor + SCROLL_SIZE) return result
            result.add(user)
        }
        return result
    }

    private fun getUserScrollRequestFromCache(scrollParam: String): UserScrollRequest {
        val value = cacheManager.getCache(CACHE_KEY_SCROLLING)!!.get(scrollParam)
        if (value != null && value.get() is UserScrollRequest) {
            return value.get() as UserScrollRequest
        }
        throw UserScrollParamNotFoundInCacheException(scrollParam)
    }

    private fun putUserScrollRequestInCache(
            scrollParam: String
    ): UserScrollRequest {
        val scrollRequest = UserScrollRequest(
                scrollParam = scrollParam,
                cursor = 0
        )
        cacheManager.getCache(CACHE_KEY_SCROLLING)!!.put(scrollParam, scrollRequest)
        return scrollRequest
    }

    private fun updateUserScrollRequestCursorInCache(scrollParam: String, cursor: Int): UserScrollRequest? {
        val value = cacheManager.getCache(CACHE_KEY_SCROLLING)!!.get(scrollParam)
        if (value == null || value.get() !is UserScrollRequest) {
            return null
        }
        val scrollRequest = value.get() as UserScrollRequest
        scrollRequest.cursor = cursor
        cacheManager.getCache(CACHE_KEY_SCROLLING)!!.put(scrollParam, scrollRequest)
        return scrollRequest
    }

    private fun evictUserScrollRequestFromCache(scrollParam: String) =
            cacheManager.getCache(CACHE_KEY_SCROLLING)!!.evict(scrollParam)

    private fun initializeUserList(): List<User> {
        val result = ArrayList<User>()
        for (i in 0..1000) {
            result.add(User(
                    i,
                    getRandomString(),
                    getRandomString(),
                    "${getRandomString()}@${getRandomString()}.${getRandomString()}"
            ))
        }
        return result
    }

    private fun getRandomString(): String = RandomStringUtils.randomAlphanumeric(3, 15)
}