/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mycompany.issuehamster;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author e9linda
 */
@RestController
public class HamsterController {
    @GetMapping("/")
    public String hej(){
        return "hej";
    }
    
    
}
