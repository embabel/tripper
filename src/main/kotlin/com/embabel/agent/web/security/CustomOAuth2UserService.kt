package com.embabel.agent.web.security

import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest
import org.springframework.security.oauth2.core.user.DefaultOAuth2User
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.stereotype.Service

@Service
class CustomOAuth2UserService : DefaultOAuth2UserService() {

    override fun loadUser(userRequest: OAuth2UserRequest): OAuth2User {
        val user = super.loadUser(userRequest)

        // Extract user attributes
        val attributes = user.attributes

        // Create authorities (roles)
        val authorities = listOf(SimpleGrantedAuthority("ROLE_USER"))

        // Return custom OAuth2User
        return DefaultOAuth2User(
            authorities,
            attributes,
            "name" // The attribute that contains the user's name
        )
    }
}