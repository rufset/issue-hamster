/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mycompany.issuehamster;


import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

/**
 * 
 *
 * @author rufset
 */
public class Fetcher {
    private final String token =""; ///HEMLIGHET FÖR BÖFVELEN.
    private final String ampProjectUrl = "https://api.github.com/repos/ampproject/amphtml/issues?q=renovate&sort=created&order=asc";
    RestTemplate restTemplate = new RestTemplate();
    
    private final String genericProjectUrl = "https://api.github.com/repos/{project}";

    public String oneProject() throws IOException {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        headers.setBearerAuth(token);
       // HttpParameters parameters = new HttpParameters(); <- behöver sätta queryparametrar på nåt sätt. 
        
        HttpEntity<String> entity = new HttpEntity<>("parameters", headers);
        ResponseEntity<String> result = restTemplate.exchange(ampProjectUrl, HttpMethod.GET, entity, String.class);
        return result.toString();
    }
    
    public String genericProject() throws IOException {
        Map<String, String> param = new HashMap<>();
        param.put("project", "ampproject/amphtml");
        
        String result = restTemplate.getForObject(genericProjectUrl, String.class, param);
        return result;
    }
}
