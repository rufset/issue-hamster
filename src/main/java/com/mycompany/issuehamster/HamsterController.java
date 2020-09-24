/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mycompany.issuehamster;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author rufset
 */
@RestController
public class HamsterController {

    @GetMapping("/projectIssues")
    public String projectIssues(String project, String token) {
        try {
            Fetcher fetch = new Fetcher();
            return fetch.projectIssues(project, token);
        } catch (IOException ex) {
            Logger.getLogger(HamsterController.class.getName()).log(Level.SEVERE, null, ex);
            return ex.getMessage();
        }

    }

    @GetMapping("/ampProjectIssues")
    public String ampProjectIssues(String token) {
        try {
            Fetcher fetch = new Fetcher();
            return fetch.ampProjectIssues(token);
        } catch (IOException ex) {
            Logger.getLogger(HamsterController.class.getName()).log(Level.SEVERE, null, ex);
            return ex.getMessage();
        }

    }
    
        @GetMapping("/issueComments")
    public String issueComments(String url, String token) {
        try {
            Fetcher fetch = new Fetcher();
            return fetch.issueComments(url, token);
        } catch (IOException ex) {
            Logger.getLogger(HamsterController.class.getName()).log(Level.SEVERE, null, ex);
            return ex.getMessage();
        }

    }
}
