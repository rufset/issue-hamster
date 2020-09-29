/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mycompany.issuehamster;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
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

    /*   public String ampProjectIssues(String token) throws IOException {
        return projectIssues("ampproject/amphtml", token);
    }*/
    public URI projectToUri(String project) {
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
        return uri;
    }

    public URI projectToUri(String project, int page) {
        Map<String, String> param = new HashMap<>();
        param.put("project", project);

        URI uri = UriComponentsBuilder.fromUriString(genericProjectUrl)
                .buildAndExpand(param)
                .toUri();
        uri = UriComponentsBuilder
                .fromUri(uri)
                .queryParam("sort", "created")
                .queryParam("order", "asc")
                .queryParam("page", page)
                .build()
                .toUri();
        return uri;
    }

    public String projectToUriToString(String project, int page) {
        return projectToUri(project, page).toString();

    }

    public String projectToUriToString(String project) {
        return projectToUri(project).toString();

    }

    public String requestProjectBody(String project, String token) throws IOException {
        return requestUri(projectToUri(project), token).getBody();
    }

    public ResponseEntity<String> request(String uri, String token) throws IOException {
        return requestUri(URI.create(uri), token);
    }

    public ResponseEntity<String> request(String uri, String token, int page) throws IOException {
        return requestUri(URI.create(uri), token);

    }

    public ResponseEntity<String> requestUri(URI uri, String token) throws IOException {

        Logger.getLogger(Fetcher.class.getName()).log(Level.INFO, "URI:" + uri, "");
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        headers.setBearerAuth(token);
        HttpEntity<String> entity = new HttpEntity<>("parameters", headers);
        ResponseEntity<String> result = restTemplate.exchange(uri, HttpMethod.GET, entity, String.class);
        return result;

    }

    public String extractURIByRel(String linkHeader, String rel) {
        if (linkHeader == null) {
            return null;
        }

        String uriWithSpecifiedRel = null;
        String[] links = linkHeader.split(", ");
        String linkRelation = null;
        for (String link : links) {
            int positionOfSeparator = link.indexOf(';');
            linkRelation = link.substring(positionOfSeparator + 1, link.length()).trim();
            if (extractTypeOfRelation(linkRelation).equals(rel)) {
                uriWithSpecifiedRel = link.substring(1, positionOfSeparator - 1);
                break;
            }
        }

        return uriWithSpecifiedRel;
    }

    private Object extractTypeOfRelation(String linkRelation) {
        int positionOfEquals = linkRelation.indexOf('=');
        return linkRelation.substring(positionOfEquals + 2, linkRelation.length() - 1).trim();
    }

    
    public long milisToSleep(String XRateLimitReset){
        return Duration.ofSeconds((Long.parseLong(XRateLimitReset))-(Instant.now().toEpochMilli()/1000)).toMillis();
    }

}
