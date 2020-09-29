/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mycompany.issuehamster;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.boot.CommandLineRunner;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.Duration;
import java.time.Instant;

/**
 * Just some code to test small snippets
 *
 * @author rufset
 */
//@Component
public class testwrapper implements CommandLineRunner {

    int five = 0;
    private long secondsToSleep = 2;
    private long rateLimitReset;
    private Duration duration;
    private long nowInSeconds;

    @Override
    public void run(String... args) {
        try {
            doThings();
        } catch (IOException ex) {
            Logger.getLogger(testwrapper.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void doThings() throws IOException {

        five = five + 1;
        System.out.println("-------Here we go again---------");
        System.out.println("We're on round " + five);
        String token = Files.readString(Path.of(System.getProperty("user.home") + "/Issue-hamster-files/token.txt")).strip();
        String project = "rufset/issue-hamster";
        Fetcher fetcher = new Fetcher();
        String projectURI = fetcher.projectToUriToString(project);
        ResponseEntity<String> result = fetcher.request(projectURI, token);
        
        Logger.getLogger(testwrapper.class.getName()).log(Level.INFO, "Response status: " + result.getStatusCode(), "");
                Logger.getLogger(testwrapper.class.getName()).log(Level.INFO, "Response status code value: " + result.getStatusCodeValue(), "");
        //if (Integer.parseInt(result.getHeaders().getFirst("X-RateLimit-Remaining")) <= 1) {
        

        System.out.println("ratelimit " + result.getHeaders().getFirst("X-RateLimit-Reset"));
        
        nowInSeconds = Instant.now().toEpochMilli()/1000;
        System.out.println("Now " + nowInSeconds);
        rateLimitReset = Long.parseLong(result.getHeaders().getFirst("X-RateLimit-Reset"));
        
        
        duration = Duration.ofSeconds(rateLimitReset-nowInSeconds);
       // duration = Duration.between(epochtime, now);
        System.out.println(duration.toMillis());
        secondsToSleep = duration.toMillis();
        /*secondsToSleep = fetcher.milisToSleep(result.getHeaders().getFirst("X-RateLimit-Reset"));
        System.out.println("Seconds to sleep " + secondsToSleep);*/
        //}
        try {
            System.out.println("Going to bed");
            Thread.sleep(2);
            System.out.println("Woke up!");
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
        JSONArray arr = new JSONArray(result.getBody());
        
        JSONObject issue = arr.getJSONObject(0);
        String body = issue.getString("body");
        String title = issue.getString("title");
        System.out.println("-------Body---------");
        System.out.println(body);
        System.out.println("---------Title-------");
        System.out.println(title);
        ResponseEntity<String> commentsWithHeaders;
        String commentsUrl = issue.getString("comments_url");
        do {
            commentsWithHeaders = fetcher.request(commentsUrl, token);
            String comments = commentsWithHeaders.getBody();
            JSONArray commentsArr = new JSONArray(comments);//----Save this JSON
            System.out.println("--------CommentsUrl--------");
            System.out.println(commentsUrl);
            System.out.println("--------CommentsUrlContent--------");

            //Prints out one page of comments for current issue
            //remember to remove limit of 3 in case you keep it. 
            for (int j = 0; j < commentsArr.length() && j < 3; j++) {
                String commentsBody = commentsArr.getJSONObject(j).getString("body");
                System.out.println("-------Comments Body---------");
                System.out.println("----------------" + j);
                System.out.println(commentsBody);
            }
            String link = commentsWithHeaders.getHeaders().getFirst("link");
            System.out.println(link);
            commentsUrl = fetcher.extractURIByRel(link, "next");
        } while (commentsUrl != null);

    }

}
