/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mycompany.issuehamster;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
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
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.UnknownHttpStatusCodeException;
import org.springframework.web.util.UriComponentsBuilder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 *
 * @author rufset
 */
public class Fetcher {

    RestTemplate restTemplate = new RestTemplate();

    private final String genericProjectUrl = "https://api.github.com/repos/{project}/issues";
    private final String rateLimitUrl = "https://api.github.com/rate_limit";
    private final String searchApiUri = "https://api.github.com/search/issues?q={searchString}";

    /*   public String ampProjectIssues(String token) throws IOException {
        return projectIssues("ampproject/amphtml", token);
    }*/
    /**
     * Returns a link to all the issues in one project
     */
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
                .queryParam("state", "all")
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
                .queryParam("state", "all")
                .build()
                .toUri();
        return uri;
    }

    /**
     * Methods that also asks for creator of issue
     */
    public URI projectToUriByCreator(String creator, String project) {
        Map<String, String> param = new HashMap<>();
        param.put("project", project);

        URI uri = UriComponentsBuilder.fromUriString(genericProjectUrl)
                .buildAndExpand(param)
                .toUri();
        uri = UriComponentsBuilder
                .fromUri(uri)
                .queryParam("sort", "created")
                .queryParam("order", "asc")
                .queryParam("state", "all")
                .queryParam("creator", creator)
                .build()
                .toUri();
        return uri;
    }

    public URI projectToUriByCreator(String creator, String project, int page) {
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
                .queryParam("state", "all")
                .queryParam("creator", creator)
                .build()
                .toUri();
        return uri;
    }

    public URI searchPages(URI searchURI, int page) {

        URI uri = UriComponentsBuilder
                .fromUri(searchURI)
                .queryParam("page", page)
                .build()
                .toUri();
        return uri;
    }

    /**
     * HERE: involevs is a logical OR between author, assignee, mentions, and
     * commenter There's a limit to five query parameters in a query that is can
     * only select five bots at a time in one request* q = involves:USERNAME and
     * repo:USERNAME/REPOSITORY get /search/issues
     *
     * The Search API does not support queries that:
     *
     * are longer than 256 characters (not including operators or qualifiers).
     * have more than five AND, OR, or NOT operators. These search queries will
     * return a "Validation failed" error message.
     *
     * https://api.github.com/search/issues?q=repo:rufset/issue-hamster%20christmasTree
     * search for the search term ChristmasTree within the repo
     * rufset/issue-hamster
     * https://api.github.com/search/issues?q=repo:rufset/issue-hamster+involves:xLeitix
     * search for xleitix as logical OR between author, assignee, mentions, and
     * commenter in repo rufset/issue-hamster i can't make an "or" work so had
     * to add another involves if i want to search for more than one user.
     * https://api.github.com/search/issues?q=repo:rufset/issue-hamster+involves:xLeitix+involves:rufset
     *
     */
    /**
     * current code gives:
     * https://api.github.com/repos/repo:rufset/issue-hamster+involves:rufset/issues?sort=created&order=asc&state=all
     *
     */
    public URI projectToUriWithSearch(String searchTerm) {
        System.out.println("SearchTerm from Swagger" + searchTerm);
        Logger.getLogger(Fetcher.class.getName()).log(Level.FINER, "URI:" + searchTerm, "");

        //return UriComponentsBuilder.fromUriString(searchApiUri).buildAndExpand(searchTerm).toUri();
        URI temp
                = UriComponentsBuilder.fromUriString(searchApiUri)
                        .queryParam("sort", "created")
                        .queryParam("order", "asc")
                        .buildAndExpand(searchTerm).encode().toUri();
        System.out.println(temp.toString());
        return temp;

    }

    public ArrayList<String> searchStringMapping(ArrayList<String> users, String project) {
        ArrayList<String> queries = new ArrayList<>();
        for (String name : users) {
            queries.add("repo:" + project + "+involves:" + name);
        }
        return queries;
    }

    public String projectToUriWithSearchToString(String searchTerm) {
        return projectToUriWithSearch(searchTerm).toString();
    }

    private String encodeValue(String value) throws UnsupportedEncodingException {
        return URLEncoder.encode(value, StandardCharsets.UTF_8.toString());
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

        Logger.getLogger(Fetcher.class.getName()).log(Level.FINER, "URI:" + uri, "");
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        headers.setBearerAuth(token);
        HttpEntity<String> entity = new HttpEntity<>("parameters", headers);
        ResponseEntity<String> result;
        try {
            result = restTemplate.exchange(uri, HttpMethod.GET, entity, String.class);

        } catch (HttpClientErrorException | HttpServerErrorException e) {
            result = new ResponseEntity<>(e.getStatusCode());
        }
        return result;

    }

    /**
     * Method to extract the "next page" URI from a request
     */
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

    public long milisToSleep(String XRateLimitReset) {
        return Duration.ofSeconds((Long.parseLong(XRateLimitReset)) - (Instant.now().toEpochMilli() / 1000)).toMillis();
    }

    public String timeToReset(String token) {

        Logger.getLogger(Fetcher.class.getName()).log(Level.FINER, "URI:" + rateLimitUrl, "");
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        headers.setBearerAuth(token);
        HttpEntity<String> entity = new HttpEntity<>("parameters", headers);
        ResponseEntity<String> result;

        result = restTemplate.exchange(rateLimitUrl, HttpMethod.GET, entity, String.class);

        return result.getHeaders().getFirst("X-RateLimit-Reset");

    }

    public String requestsLeft(String token) {

        Logger.getLogger(Fetcher.class.getName()).log(Level.FINER, "URI:" + rateLimitUrl, "");
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        headers.setBearerAuth(token);
        HttpEntity<String> entity = new HttpEntity<>("parameters", headers);
        ResponseEntity<String> result;

        result = restTemplate.exchange(rateLimitUrl, HttpMethod.GET, entity, String.class);

        return result.getHeaders().getFirst("X-RateLimit-Remaining");

    }

    public int timeToResetSearch(String token) {

        Logger.getLogger(Fetcher.class.getName()).log(Level.FINER, "URI:" + rateLimitUrl, "");
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        headers.setBearerAuth(token);
        HttpEntity<String> entity = new HttpEntity<>("parameters", headers);
        ResponseEntity<String> result;

        result = restTemplate.exchange(rateLimitUrl, HttpMethod.GET, entity, String.class);
        JSONObject allRateLimits = new JSONObject(result.getBody());
        return allRateLimits.getJSONObject("resources").getJSONObject("search").getInt("reset");

    }

    public int requestsLeftSearch(String token) {

        Logger.getLogger(Fetcher.class.getName()).log(Level.FINER, "URI:" + rateLimitUrl, "");
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        headers.setBearerAuth(token);
        HttpEntity<String> entity = new HttpEntity<>("parameters", headers);
        ResponseEntity<String> result;

        result = restTemplate.exchange(rateLimitUrl, HttpMethod.GET, entity, String.class);

        JSONObject allRateLimits = new JSONObject(result.getBody());

        return allRateLimits.getJSONObject("resources").getJSONObject("search").getInt("remaining");

    }

}
