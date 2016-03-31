package com.nightnight.spring.quickstart.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequestMapping("/quickstart")
public class QuickstartController {
    
    @RequestMapping(value = "/hello", method = RequestMethod.GET)
    public ModelAndView hello(String name) {
        ModelAndView mav = new ModelAndView("quickstart/hello");
        mav.addObject("name", name);
        return mav;
    }

}
