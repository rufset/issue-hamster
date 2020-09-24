/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mycompany.issuehamster;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 *
 *
 * @author rufset
 */
public class Fetcher {

    RestTemplate restTemplate = new RestTemplate();

    private final String genericProjectUrl = "https://api.github.com/repos/{project}/issues";

    public String ampProjectIssues(String token) throws IOException {
        return projectIssues("ampproject/amphtml", token);
    }

    public String projectIssues(String project, String token) throws IOException {

        Map<String, String> param = new HashMap<>();
        param.put("project", project);

        URI uri = UriComponentsBuilder.fromUriString(genericProjectUrl)
                .buildAndExpand(param)
                .toUri();
        uri = UriComponentsBuilder
                .fromUri(uri)
                .queryParam("sort", "created")
                .queryParam("order", "asc")
                .build()
                .toUri();

        Logger.getLogger(Fetcher.class.getName()).log(Level.INFO, "URI:" + uri, "");
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        headers.setBearerAuth(token);
        HttpEntity<String> entity = new HttpEntity<>("parameters", headers);
        ResponseEntity<String> result = restTemplate.exchange(uri, HttpMethod.GET, entity, String.class);
        return result.getBody();

    }
    
   /* public String projectIssues(String project, String token, int page){
        
    }*/

    public String issueComments(String url, String token) throws IOException {

        Logger.getLogger(Fetcher.class.getName()).log(Level.INFO, "URL:" + url, "");
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        headers.setBearerAuth(token);
        HttpEntity<String> entity = new HttpEntity<>("parameters", headers);
        ResponseEntity<String> result = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
        return result.getBody();

    }
    

}
