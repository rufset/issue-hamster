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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 *
 * @author rufset
 */
//@Component
public class MainWrapperSearch implements CommandLineRunner {

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
        ArrayList userArray = new ArrayList();
        String user;
        // MongoDB connection
        MongoClient mongoClient = new MongoClient("localhost", 27017);
        MongoDatabase db = mongoClient.getDatabase("gh-issues");
        MongoCollection projects = db.getCollection("projects");
        MongoCollection issuesCollection = db.getCollection("issues");
        MongoCollection commentsCollection = db.getCollection("comments");
        MongoCollection eventsCollection = db.getCollection("events");

        try (BufferedReader projectsReader = new BufferedReader(new FileReader(System.getProperty("user.home") + "/Issue-hamster-files/projects.txt"))) {
            try (BufferedReader usersReader = new BufferedReader(new FileReader(System.getProperty("user.home") + "/Issue-hamster-files/botUsers.txt"))) {

                //read the users in to a datastructure that makes sense
                for (int i = 1; ((user = usersReader.readLine()) != null); i++) {
                    userArray.add(i, user);

                }
                usersReader.close();

                //for each project in the file 
                while ((project = projectsReader.readLine()) != null) {
                    project = project.strip();
                    Logger.getLogger(MainWrapperSearch.class.getName()).log(Level.INFO, "Searching within Project:" + project, "");
                    Fetcher fetcher = new Fetcher();

                    // String issues = fetcher.projectIssues("ampproject/amphtml", token);
                    // create a query that searches for each of the bot users
                    projectURI = fetcher.projectToUriToString(project);

                    //Fetch each page of the project
                    do {
                        Logger.getLogger(MainWrapperSearch.class.getName()).log(Level.INFO, "Fetching issues on URI " + projectURI, "");
                        issuesWithHeaders = fetcher.request(projectURI, token);

                        //deal with non 200 responses and ratelimit
                        while (issuesWithHeaders.getStatusCodeValue() != 200) {
                            Logger.getLogger(MainWrapperSearch.class.getName()).log(Level.INFO, "Response status code value: " + issuesWithHeaders.getStatusCodeValue(), "");
                            takeABreak(token, fetcher);
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
                                    Logger.getLogger(MainWrapperSearch.class.getName()).log(Level.INFO, "Response status code value: " + commentsWithHeaders.getStatusCodeValue(), "");
                                    takeABreak(token, fetcher);
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
                                commentsUrl = fetcher.extractURIByRel(link, "next");
                            } while (commentsUrl != null);

                            //do-while-loop for eventpages consumption
                            String eventsUrl = issue.getString("events_url");
                            do {
                                eventsWithHeaders = fetcher.request(eventsUrl, token);

                                //deal with rate limit and non 200 requests
                                while (eventsWithHeaders.getStatusCodeValue() != 200) {
                                    Logger.getLogger(MainWrapperSearch.class.getName()).log(Level.INFO, "Response status code value: " + eventsWithHeaders.getStatusCodeValue(), "");
                                    takeABreak(token, fetcher);
                                    eventsWithHeaders = fetcher.request(projectURI, token);

                                }
                                String events = eventsWithHeaders.getBody();
                                JSONArray eventsArr = new JSONArray(events);

                                //Adding the events one by one to the db
                                for (int j = 0; j < eventsArr.length(); j++) {
                                    eventsCollection.insertOne(org.bson.Document.parse(eventsArr.getJSONObject(j).toString()));
                                }
                                //Get next page of events for this issue
                                String link = eventsWithHeaders.getHeaders().getFirst("link");
                                eventsUrl = fetcher.extractURIByRel(link, "next");
                            } while (eventsUrl != null);

                        }

                        //Get next page of issues for this project
                        String link = issuesWithHeaders.getHeaders().getFirst("link");
                        projectURI = fetcher.extractURIByRel(link, "next");

                    } while (projectURI != null); //while next page not null

                }
            } catch (IOException e) {
                Logger.getLogger(MainWrapperSearch.class.getName()).log(Level.SEVERE, Arrays.toString(e.getStackTrace()) + "An Error Occurred while trying to file", "");
            }
        } catch (IOException e) {
            Logger.getLogger(MainWrapperSearch.class.getName()).log(Level.SEVERE, Arrays.toString(e.getStackTrace()) + "An Error Occurred while trying to read file", "");
        }

    }

    public void takeABreak(String token, Fetcher fetcher) {

        if (Integer.parseInt(fetcher.requestsLeft(token)) <= 1) {
            try {
                Thread.sleep(fetcher.milisToSleep(fetcher.timeToReset(token)) + 2000);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                Logger.getLogger(MainWrapperSearch.class.getName()).log(Level.SEVERE, "Sleep interrupted" + ie, "");
            }
        } else {
            try {
                //sleep one minute
                Thread.sleep(60000);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                Logger.getLogger(MainWrapperSearch.class.getName()).log(Level.SEVERE, "Sleep interrupted" + ie, "");
            }
        }
    }

}
