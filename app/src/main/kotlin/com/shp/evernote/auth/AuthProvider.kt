package com.shp.evernote.auth

/**
 * How the app knows who the user is. The sign-in method is not decided, so
 * nothing above this interface may assume one. Until a real provider lands
 * there is a single anonymous local user.
 */
interface AuthProvider {
    /** Stable ID of the signed-in user, or null when nobody is signed in. */
    fun currentUserId(): String?
}

class LocalOnlyAuthProvider : AuthProvider {
    override fun currentUserId(): String? = null
}
