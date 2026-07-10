package com.edwin.inventorysync.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    @GetMapping({"/demo", "/demo/"})
    public String demo() {
        return "forward:/demo/index.html";
    }
}
