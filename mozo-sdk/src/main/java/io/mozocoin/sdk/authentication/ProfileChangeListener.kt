package io.mozocoin.sdk.authentication

import io.mozocoin.sdk.common.model.UserInfo

interface ProfileChangeListener {
    fun onProfileChanged(userInfo: UserInfo)
}