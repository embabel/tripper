package com.embabel.tripper.config.security

import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService
import org.springframework.security.oauth2.core.user.DefaultOAuth2User
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.stereotype.Service

@Service
class OAuth2UserService : OAuth2UserService<OAuth2UserRequest, OAuth2User> {
    
    private val delegate = DefaultOAuth2UserService()
    
    override fun loadUser(userRequest: OAuth2UserRequest): OAuth2User {
        val user = delegate.loadUser(userRequest)
        
        // Extract user details from OAuth2 provider
        val attributes = user.attributes
        
        // Create authorities (roles)
        val authorities = setOf(SimpleGrantedAuthority("ROLE_USER"))
        
        // Create a custom OAuth2User with the extracted information
        return DefaultOAuth2User(
            authorities,
            attributes,
            "email" // Name attribute key - typically "email" for Google
        )
    }
}