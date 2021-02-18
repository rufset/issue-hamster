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
import java.net.URI;
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
@Component
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
        ArrayList<String> botUserArray = new ArrayList();
        String botUser;
        // MongoDB connection
        MongoClient mongoClient = new MongoClient("localhost", 27017);
        MongoDatabase db = mongoClient.getDatabase("gh-issues");
        MongoCollection projects = db.getCollection("projects");
        MongoCollection issuesCollection = db.getCollection("issues");
        MongoCollection commentsCollection = db.getCollection("comments");
        MongoCollection eventsCollection = db.getCollection("events");

        try (BufferedReader projectsReader = new BufferedReader(new FileReader(System.getProperty("user.home") + "/Issue-hamster-files/projects.txt"))) {
            try (BufferedReader usersReader = new BufferedReader(new FileReader(System.getProperty("user.home") + "/Issue-hamster-files/botUsers.txt"))) {

                //read the bot-users in to a datastructure, since this should be re-combined with each project
                for (int i = 0; ((botUser = usersReader.readLine()) != null); i++) {
                    botUserArray.add(i, botUser);

                }
                //Done with the Bot-users textfile. 
                //usersReader.close();<- closes both. 

                //for each PROJECT in the  projects-file 
                while ((project = projectsReader.readLine()) != null) {
                    project = project.strip();
                    Logger.getLogger(MainWrapperSearch.class.getName()).log(Level.INFO, "Searching within Project: " + project, "");
                    Fetcher fetcher = new Fetcher();

                    // create a list of queries that searches for of the bot users on form:
                    // https://api.github.com/search/issues?q=repo:rufset/issue-hamster+involves:xLeitix
                    ArrayList<String> searchStrings = fetcher.searchStringMapping(botUserArray, project);

                    //For each string in searchStrings
                    for (String searchURI : searchStrings) {

                        URI searchUriWithoutPage = fetcher.projectToUriWithSearch(searchURI);

                        issuesWithHeaders = fetcher.requestUri(searchUriWithoutPage, token);

                        //deal with non 200 responses and ratelimit
                        while (issuesWithHeaders.getStatusCodeValue() != 200) {
                            Logger.getLogger(MainWrapperSearch.class.getName()).log(Level.INFO, "Response status code value: " + issuesWithHeaders.getStatusCodeValue(), "");
                            takeABreak(token, fetcher);
                            issuesWithHeaders = fetcher.requestUri(searchUriWithoutPage, token);

                        }
                        Logger.getLogger(MainWrapperSearch.class.getName()).log(Level.INFO, "Completed request for: " + searchUriWithoutPage.toString(), "");
                        issues = issuesWithHeaders.getBody();
                        JSONObject body = new JSONObject(issues);

                        //get number of pages
                        int totalCount = body.getInt("total_count");
                        int totalPages = (totalCount / 30);
                        //add extra page for "leftover" if any. 
                        if ((totalCount % 30) != 0) {
                            totalPages = totalPages + 1;
                        }
                        Logger.getLogger(MainWrapperSearch.class.getName()).log(Level.INFO, "Total number of pages in result: " + totalPages, "");

                        //each SEACH PAGE. 
                        for (int page = 1; page <= totalPages; page++) {

                            JSONArray arr = body.getJSONArray("items");
                            //for each ISSUE in this page
                            for (int i = 0; i < arr.length(); i++) {
                                JSONObject issue = arr.getJSONObject(i);
                                issuesCollection.insertOne(org.bson.Document.parse(issue.toString()));

                                //do-while-loop for COMMENTPAGES consumption, handles the case of no comments. 
                                String commentsUrl = issue.getString("comments_url");
                                do {
                                    commentsWithHeaders = fetcher.request(commentsUrl, token);

                                    //deal with rate limit and non 200 requests
                                    while (commentsWithHeaders.getStatusCodeValue() != 200) {
                                        Logger.getLogger(MainWrapperSearch.class.getName()).log(Level.INFO, "Response status code value: " + commentsWithHeaders.getStatusCodeValue(), "");
                                        takeABreak(token, fetcher);
                                        commentsWithHeaders = fetcher.request(commentsUrl, token);

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
                                } while (commentsUrl != null); //end COMMENTS fetching

                                //do-while-loop for EVENTPAGES consumption, handles the case of no events. 
                                String eventsUrl = issue.getString("events_url");
                                do {
                                    eventsWithHeaders = fetcher.request(eventsUrl, token);

                                    //deal with rate limit and non 200 requests
                                    while (eventsWithHeaders.getStatusCodeValue() != 200) {
                                        Logger.getLogger(MainWrapperSearch.class.getName()).log(Level.INFO, "Response status code value: " + eventsWithHeaders.getStatusCodeValue(), "");
                                        takeABreak(token, fetcher);
                                        eventsWithHeaders = fetcher.request(eventsUrl, token);

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
                                } while (eventsUrl != null); //end EVENTS fetching

                            }//end looping through issues on one page

                            //Get next page of issues from the search query
                            issuesWithHeaders = fetcher.requestUri(fetcher.projectToUriWithSearch(searchURI), token);

                            //deal with non 200 responses and ratelimit
                            while (issuesWithHeaders.getStatusCodeValue() != 200) {
                                Logger.getLogger(MainWrapperSearch.class.getName()).log(Level.INFO, "Response status code value: " + issuesWithHeaders.getStatusCodeValue(), "");
                                takeABreak(token, fetcher);
                                issuesWithHeaders = fetcher.requestUri(fetcher.projectToUriWithSearch(searchURI), token);

                            }

                            issues = issuesWithHeaders.getBody();
                            body = new JSONObject(issues);

                            //if there's more pages, we have to fetch the next page. 
                            if (page != totalPages) {

                                issuesWithHeaders = fetcher.requestUri(fetcher.searchPages(searchUriWithoutPage, page), token);

                                //deal with non 200 responses and ratelimit
                                while (issuesWithHeaders.getStatusCodeValue() != 200) {
                                    Logger.getLogger(MainWrapperSearch.class.getName()).log(Level.INFO, "Response status code value: " + issuesWithHeaders.getStatusCodeValue(), "");
                                    takeABreak(token, fetcher);
                                    issuesWithHeaders = fetcher.requestUri(fetcher.searchPages(searchUriWithoutPage, page), token);

                                }
                                Logger.getLogger(MainWrapperSearch.class.getName()).log(Level.INFO, "Completed request for: " + searchUriWithoutPage.toString() + " on page:" + (page + 1), "");
                                issues = issuesWithHeaders.getBody();
                                body = new JSONObject(issues);
                            }

                        } //end going over the pages for one search

                    }//end going over the searches for the different bot-users

                }//end going over the list of projects. 
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
