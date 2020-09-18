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

    private final String token = ""; ///HEMLIGHET FÖR BÖFVELEN.
    private final String ampProjectUrl = "https://api.github.com/repos/ampproject/amphtml/issues?q=renovate&sort=created&order=asc";
    RestTemplate restTemplate = new RestTemplate();

    private final String genericProjectUrl = "https://api.github.com/repos/{project}/issues";

    public String oneProject() throws IOException {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        headers.setBearerAuth(token);
        HttpEntity<String> entity = new HttpEntity<>("parameters", headers);
        ResponseEntity<String> result = restTemplate.exchange(ampProjectUrl, HttpMethod.GET, entity, String.class);
        return result.toString();
    }
    
    public String ampProjectIssues() throws IOException{
        return projectIssues("ampproject/amphtml");
    }
    
    public String projectIssues(String project) throws IOException {

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

}
