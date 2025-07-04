package com.embabel.tripper.web

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping

@Controller
@RequestMapping("/index")
class IndexController(
) {

    @GetMapping
    fun home(): String {
        return "home"
    }
}