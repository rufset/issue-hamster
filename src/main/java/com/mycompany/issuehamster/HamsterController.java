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

    @GetMapping("/requestProject")
    public String requestProject(String project, String token) {
        try {
            Fetcher fetch = new Fetcher();
            return fetch.requestProjectBody(fetch.projectToUri(project).toString(), token);
        } catch (IOException ex) {
            Logger.getLogger(HamsterController.class.getName()).log(Level.SEVERE, null, ex);
            return ex.getMessage();
        }

    }

    @GetMapping("/timeToReset")
    public String timeToReset(String token) {
        Fetcher fetch = new Fetcher();

        return fetch.timeToReset(token);

    }

}

/* @GetMapping("/ampProjectIssues")
    public String ampProjectIssues(String token) {
        try {
            Fetcher fetch = new Fetcher();
            return fetch.ampProjectIssues(token);
        } catch (IOException ex) {
            Logger.getLogger(HamsterController.class.getName()).log(Level.SEVERE, null, ex);
            return ex.getMessage();
        }

    }*/

