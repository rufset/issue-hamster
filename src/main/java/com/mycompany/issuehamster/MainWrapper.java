/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mycompany.issuehamster;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import java.nio.file.Files;
import java.nio.file.Path;
import org.json.*;
import org.springframework.http.ResponseEntity;

import com.mongodb.MongoClient;
import com.mongodb.MongoException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 *
 * @author rufset
 */
@Component
public class MainWrapper implements CommandLineRunner {

    @Override
    public void run(String... args) {
        try {
            doThings();
        } catch (IOException ex) {
            Logger.getLogger(testwrapper.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void doThings() throws IOException {
        ResponseEntity<String> issuesWithHeaders;
        String issues;
        String token = Files.readString(Path.of(System.getProperty("user.home") + "/Issue-hamster-files/token.txt")).strip();
        String project;
        ResponseEntity<String> commentsWithHeaders;
        ResponseEntity<String> eventsWithHeaders;
        String projectURI;

        // MongoDB connection
        MongoClient mongoClient = new MongoClient("localhost", 27017);
        MongoDatabase db = mongoClient.getDatabase("gh-issues");
        MongoCollection projects = db.getCollection("projects");
        MongoCollection issuesCollection = db.getCollection("issues");
        MongoCollection commentsCollection = db.getCollection("comments");
        MongoCollection eventsCollection = db.getCollection("events");

        try (BufferedReader br = new BufferedReader(new FileReader(System.getProperty("user.home") + "/Issue-hamster-files/projects.txt"))) {

            //for each project in the file 
            while ((project = br.readLine()) != null) {
                project = project.strip();
                Logger.getLogger(MainWrapper.class.getName()).log(Level.INFO, "Reading from Project:" + project, "");
                Fetcher fetcher = new Fetcher();

                // String issues = fetcher.projectIssues("ampproject/amphtml", token);
                projectURI = fetcher.projectToUriToString(project);

                //Fetch each page of the project
                do {
                    Logger.getLogger(MainWrapper.class.getName()).log(Level.INFO, "Fetching issues on URI" + projectURI, "");

                    issuesWithHeaders = fetcher.request(projectURI, token);

                    //deal with non 200 responses and ratelimit
                    while (issuesWithHeaders.getStatusCodeValue() != 200) {
                        Logger.getLogger(MainWrapper.class.getName()).log(Level.INFO, "Response status code value: " + issuesWithHeaders.getStatusCodeValue(), "");
                        if (Integer.parseInt(issuesWithHeaders.getHeaders().getFirst("X-RateLimit-Remaining")) <= 1) {
                            try {
                                Thread.sleep(fetcher.milisToSleep(issuesWithHeaders.getHeaders().getFirst("X-RateLimit-Reset")) + 2000);
                            } catch (InterruptedException ie) {
                                Thread.currentThread().interrupt();
                                Logger.getLogger(MainWrapper.class.getName()).log(Level.SEVERE, "Sleep interrupted" + ie, "");
                            }
                        } else {
                            try {
                                //sleep one minute
                                Thread.sleep(60000);
                            } catch (InterruptedException ie) {
                                Thread.currentThread().interrupt();
                                Logger.getLogger(MainWrapper.class.getName()).log(Level.SEVERE, "Sleep interrupted" + ie, "");
                            }
                        }
                        issuesWithHeaders = fetcher.request(projectURI, token);

                    }
                    issues = issuesWithHeaders.getBody();
                    JSONArray arr = new JSONArray(issues);

                    //for each issue in this page
                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject issue = arr.getJSONObject(i);
                        issuesCollection.insertOne(org.bson.Document.parse(issue.toString()));

                        //do-while-loop for commentpages consumption
                        String commentsUrl = issue.getString("comments_url");
                        do {
                            commentsWithHeaders = fetcher.request(commentsUrl, token);

                            //deal with rate limit and non 200 requests
                            while (commentsWithHeaders.getStatusCodeValue() != 200) {
                                Logger.getLogger(MainWrapper.class.getName()).log(Level.INFO, "Response status code value: " + commentsWithHeaders.getStatusCodeValue(), "");
                                if (Integer.parseInt(commentsWithHeaders.getHeaders().getFirst("X-RateLimit-Remaining")) <= 1) {
                                    try {
                                        Thread.sleep(fetcher.milisToSleep(commentsWithHeaders.getHeaders().getFirst("X-RateLimit-Reset")) + 2000);
                                    } catch (InterruptedException ie) {
                                        Thread.currentThread().interrupt();
                                        Logger.getLogger(MainWrapper.class.getName()).log(Level.SEVERE, "Sleep interrupted" + ie, "");
                                    }
                                } else {
                                    try {
                                        //sleep one minute
                                        Thread.sleep(60000);
                                    } catch (InterruptedException ie) {
                                        Thread.currentThread().interrupt();
                                        Logger.getLogger(MainWrapper.class.getName()).log(Level.SEVERE, "Sleep interrupted" + ie, "");
                                    }
                                }
                                commentsWithHeaders = fetcher.request(projectURI, token);

                            }

                            String comments = commentsWithHeaders.getBody();
                            JSONArray commentsArr = new JSONArray(comments);

                            //adding the commments one by one to the db
                            for (int j = 0; j < commentsArr.length(); j++) {
                                commentsCollection.insertOne(org.bson.Document.parse(commentsArr.getJSONObject(j).toString()));
                            }
                            //Get next page of comments for this issue
                            String link = commentsWithHeaders.getHeaders().getFirst("link");
                            System.out.println(link);
                            commentsUrl = fetcher.extractURIByRel(link, "next");
                        } while (commentsUrl != null);

                        //do-while-loop for eventpages consumption
                        String eventsUrl = issue.getString("events_url");
                        do {
                            eventsWithHeaders = fetcher.request(eventsUrl, token);

                            //deal with rate limit and non 200 requests
                            while (eventsWithHeaders.getStatusCodeValue() != 200) {
                                Logger.getLogger(MainWrapper.class.getName()).log(Level.INFO, "Response status code value: " + eventsWithHeaders.getStatusCodeValue(), "");
                                if (Integer.parseInt(eventsWithHeaders.getHeaders().getFirst("X-RateLimit-Remaining")) <= 1) {
                                    try {
                                        Thread.sleep(fetcher.milisToSleep(eventsWithHeaders.getHeaders().getFirst("X-RateLimit-Reset")) + 2000);
                                    } catch (InterruptedException ie) {
                                        Thread.currentThread().interrupt();
                                        Logger.getLogger(MainWrapper.class.getName()).log(Level.SEVERE, "Sleep interrupted" + ie, "");
                                    }
                                } else {
                                    try {
                                        //sleep one minute
                                        Thread.sleep(60000);
                                    } catch (InterruptedException ie) {
                                        Thread.currentThread().interrupt();
                                        Logger.getLogger(MainWrapper.class.getName()).log(Level.SEVERE, "Sleep interrupted" + ie, "");
                                    }
                                }
                                eventsWithHeaders = fetcher.request(projectURI, token);

                            }
                            String events = eventsWithHeaders.getBody();
                            JSONArray eventsArr = new JSONArray(events);

                            //Adding the events one by one to the db
                            ///remember to remove limit of 3 
                            for (int j = 0; j < eventsArr.length() && j < 3; j++) {
                                eventsCollection.insertOne(org.bson.Document.parse(eventsArr.getJSONObject(j).toString()));
                            }
                            //Get next page of events for this issue
                            String link = eventsWithHeaders.getHeaders().getFirst("link");
                            System.out.println(link);
                            eventsUrl = fetcher.extractURIByRel(link, "next");
                        } while (eventsUrl != null);

                    }

                    //Get next page of issues for this project
                    String link = issuesWithHeaders.getHeaders().getFirst("link");
                    System.out.println(link);
                    projectURI = fetcher.extractURIByRel(link, "next");

                } while (projectURI != null); //while next page not null

            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }

    }

}
