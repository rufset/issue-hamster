/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mycompany.issuehamster;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import org.json.*;
import org.springframework.http.ResponseEntity;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.Mongo;
import com.mongodb.MongoClient;
import com.mongodb.MongoException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.util.JSON;
import org.bson.Document;

/**
 *
 * @author e9linda
 */
public class MainWrapper {

    //get rate limit OR use the springboot scheduler? (if I can divide?)
    //first get list from bucket
//iterate over repositories in list
    //for each repository 
//check rate limit before each request
    //fetch the overview of all the issues (how many pages?)
    // .     check rate limit before each request
    // .     for each of the list fetch all of the data in each of the issues
    // .     save as either issue or PR.
    //private static final String project = "ampproject/amphtml";
    public static void main(String[] args) throws IOException {
        ResponseEntity<String> issuesWithHeaders;
        String issues;
        String token = Files.readString(Path.of(System.getProperty("user.home") + "/Issue-hamster-files/token.txt")).strip();
        String project;
        ResponseEntity<String> commentsWithHeaders;
        ResponseEntity<String> eventsWithHeaders;

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
                System.out.println("----------------");
                System.out.println("Read from project file: " + project);
                System.out.println("----------------");
                Fetcher fetcher = new Fetcher();

                // String issues = fetcher.projectIssues("ampproject/amphtml", token);
                String projectURI = fetcher.projectToUriToString(project);

                //Fetch each page of the project
                do {
                    issuesWithHeaders = fetcher.request(projectURI, token);
                    issues = issuesWithHeaders.getBody();
                    JSONArray arr = new JSONArray(issues); // ----Save this JSON

                    //for each issue in this page
                    //remember to remove limit of 3
                    for (int i = 0; i < arr.length() && i < 3; i++) {
                        JSONObject issue = arr.getJSONObject(i);
                        issuesCollection.insertOne(org.bson.Document.parse(issue.toString()));
                        System.exit(0);

                        //do-while-loop for commentpages consumption
                        String commentsUrl = issue.getString("comments_url");
                        do {
                            commentsWithHeaders = fetcher.request(commentsUrl, token);
                            String comments = commentsWithHeaders.getBody();
                            JSONArray commentsArr = new JSONArray(comments);//----Save this JSON

                            //adding the commments one by one to the db
                            //remember to remove limit of 3 
                            for (int j = 0; j < commentsArr.length() && j < 3; j++) {
                                issuesCollection.insertOne(org.bson.Document.parse(commentsArr.getJSONObject(j).toString()));
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
                            String events = eventsWithHeaders.getBody();
                            JSONArray eventsArr = new JSONArray(events); //----Save this JSON
                            System.out.println("--------EventsUrlContent--------");

                            //Adding the events one by one to the db
                            ///remember to remove limit of 3 
                            for (int j = 0; j < eventsArr.length() && j < 3; j++) {
                                issuesCollection.insertOne(org.bson.Document.parse(eventsArr.getJSONObject(j).toString()));
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
