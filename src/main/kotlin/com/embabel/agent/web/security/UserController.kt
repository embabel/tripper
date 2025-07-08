package com.embabel.agent.web.security

import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping

@Controller
class UserController {

    @GetMapping("/user")
    fun getUserInfo(@AuthenticationPrincipal oAuth2User: OAuth2User, model: Model): String {
        model.addAttribute("name", oAuth2User.getAttribute<String>("name"))
        model.addAttribute("email", oAuth2User.getAttribute<String>("email"))
        oAuth2User.attributes["picture"]?.let { picture ->
            model.addAttribute("picture", picture)
        }
        model.addAttribute("attributes", oAuth2User.attributes)
        return "common/user-info"
    }
}