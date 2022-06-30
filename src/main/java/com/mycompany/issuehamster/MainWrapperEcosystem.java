
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
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import java.io.BufferedWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.springframework.boot.CommandLineRunner;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.StringTokenizer;
import org.springframework.stereotype.Component;

/**
 *
 * @author rufset
 */
@Component
public class MainWrapperEcosystem implements CommandLineRunner {

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

        String projectURI;
        //ArrayList<ArrayList<String>> botUserArray = new ArrayList();
        HashMap<String, ArrayList<String>> botUserKeyword = new HashMap();
        String botUser;
        // MongoDB connection
        MongoClient mongoClient = new MongoClient("localhost", 27017);
        MongoDatabase db = mongoClient.getDatabase("gh-issues");
        MongoCollection projects = db.getCollection("projects");
        MongoCollection issuesCollection = db.getCollection("issues");
        MongoCollection commentsCollection = db.getCollection("comments");
        MongoCollection eventsCollection = db.getCollection("events");

        try (BufferedReader projectsReader = new BufferedReader(new FileReader(System.getProperty("user.home") + "/Issue-hamster-files/projectsE.txt"))) {
            try (BufferedReader usersReader = new BufferedReader(new FileReader(System.getProperty("user.home") + "/Issue-hamster-files/botKeywordAndUserE.txt"))) {
                try (BufferedWriter projectsWith422 = new BufferedWriter(new FileWriter(System.getProperty("user.home") + "/Issue-hamster-files/projectsWith422.txt", true))) {
                    try (BufferedWriter projectsWithOtherErrors = new BufferedWriter(new FileWriter(System.getProperty("user.home") + "/Issue-hamster-files/projectsWithOtherErrors.txt", true))) {

                        //read the bot-users in to a datastructure, since this should be re-combined with each project
                        //the bot user file should be on form "name linebreak name"
                        
                        
                        ArrayList<String> user = new ArrayList();
                        while ((botUser = usersReader.readLine()) != null) {
                            botUser = botUser.strip();
                            user.add(botUser);
                        }
                        //Done with the Bot-users textfile. 
                        usersReader.close();

                       

                        //for each PROJECT in the  projects-file 
                        while ((project = projectsReader.readLine()) != null) {
                            project = project.strip();
                            Logger.getLogger(MainWrapperEcosystem.class.getName()).log(Level.INFO, "Searching within Project: " + project, "");
                            Fetcher fetcher = new Fetcher();
                            //--------********------
                            //--------Search endpoint------
                            // create a list of queries that searches for of the bot users on form
                            // https://api.github.com/search/issues?q=repo:%22kubernetes/kubernetes%22+involves:%22k8s-triage-robot%22&sort=created&order=asc&page=7
                            ArrayList<String> searchStrings = fetcher.searchStringMapping(user, project);

                            //For each string in searchStrings
                            for (String searchURI : searchStrings) {

                                URI searchUriWithoutPage = fetcher.projectToUriWithSearch(searchURI);

                                issuesWithHeaders = fetcher.requestUri(searchUriWithoutPage, token);

                                //deal with ratelimit
                                while (issuesWithHeaders.getStatusCodeValue() == 403) {
                                    Logger.getLogger(MainWrapperEcosystem.class.getName()).log(Level.INFO, "Response status code value: " + issuesWithHeaders.getStatusCodeValue(), "");
                                    takeABreak(token, fetcher);
                                    issuesWithHeaders = fetcher.requestUri(searchUriWithoutPage, token);

                                }
                                //deal with redirects of projects
                                if (issuesWithHeaders.getStatusCodeValue() == 422) {
                                    //Write search to file and skip to next, probably there's an naming error in the project file. 
                                    projectsWith422.append(searchUriWithoutPage.toString());
                                    projectsWith422.newLine();
                                    projectsWith422.flush();
                                    Logger.getLogger(MainWrapperEcosystem.class.getName()).log(Level.INFO, "wrote search to file due to error code: " + issuesWithHeaders.getStatusCodeValue() + " for URI: " + searchUriWithoutPage.toString(), "");

                                    Logger.getLogger(MainWrapperEcosystem.class.getName()).log(Level.INFO, "Completed request for: " + searchUriWithoutPage.toString(), "");
                                    //deal with other errors and non 200 responses
                                } else if (issuesWithHeaders.getStatusCodeValue() != 200) {
                                    projectsWithOtherErrors.append(searchUriWithoutPage.toString());
                                    projectsWithOtherErrors.newLine();
                                    projectsWithOtherErrors.flush();
                                    Logger.getLogger(MainWrapperEcosystem.class.getName()).log(Level.INFO, "wrote search to file due to error code: " + issuesWithHeaders.getStatusCodeValue() + " for URI: " + searchUriWithoutPage.toString(), "");

                                } else {
                                    issues = issuesWithHeaders.getBody();
                                    JSONObject body = new JSONObject(issues);

                                    //get number of pages
                                    int totalCount = body.getInt("total_count");
                                    int totalPages = (totalCount / 30);
                                    //add extra page for "leftover" if any. 
                                    if ((totalCount % 30) != 0) {
                                        totalPages = totalPages + 1;
                                    }
                                    if (totalPages > 34){
                                        Logger.getLogger(MainWrapperEcosystem.class.getName()).log(Level.INFO, "Too many results, will get first 1000 was: " + totalPages, "");
                                        totalPages = 34;
                                    }
                                    Logger.getLogger(MainWrapperEcosystem.class.getName()).log(Level.INFO, "Total number of pages in result: " + totalPages, "");
                                    //each SEACH PAGE. 
                                    for (int page = 1; page <= totalPages; page++) {
                                        Logger.getLogger(MainWrapperEcosystem.class.getName()).log(Level.INFO, "Getting page " + page, "");
                                        JSONArray arr = body.getJSONArray("items");
                                        //for each ISSUE in this page
                                        for (int i = 0; i < arr.length(); i++) {
                                            JSONObject issue = arr.getJSONObject(i);
                                            issuesCollection.insertOne(org.bson.Document.parse(issue.toString()));

                                            commentOrEventpagesConsumption(issue, fetcher, token, commentsCollection, "comments_url");
                                            commentOrEventpagesConsumption(issue, fetcher, token, eventsCollection, "events_url");

                                        }//end looping through issues on one page

                                        //if there's more pages, we have to fetch the next page. 
                                        if (page != totalPages) {
                                            Logger.getLogger(MainWrapperEcosystem.class.getName()).log(Level.INFO, "Finished page" + page, "");
                                            URI searchURIWithPage = fetcher.projectToUriWithSearchandPage(searchURI, page);
                                            Logger.getLogger(MainWrapperEcosystem.class.getName()).log(Level.INFO, "New SearchUri: " + searchURIWithPage.toString(), "");

                                            issuesWithHeaders = fetcher.requestUri(searchURIWithPage, token);

                                            if (issuesWithHeaders.getStatusCodeValue() == 422) {
                                                //skriv ner projektet till fil och kolla nästa projekt istället
                                                projectsWith422.append(searchURIWithPage.toString());
                                                projectsWith422.newLine();
                                                projectsWith422.flush();
                                                Logger.getLogger(MainWrapperEcosystem.class.getName()).log(Level.INFO, "wrote search to file due to error: " + searchURIWithPage.toString() + " when trying to fetch page: " + (page+1), "");
                                                try {
                                                    Logger.getLogger(MainWrapperEcosystem.class.getName()).log(Level.INFO, body.getString("message"), "");
                                                } catch (JSONException e) {
                                                    Logger.getLogger(MainWrapperEcosystem.class.getName()).log(Level.INFO, "No message found", "");
                                                }
                                            } else {
                                                //deal with non 200 responses and ratelimit
                                                while (issuesWithHeaders.getStatusCodeValue() != 200) {
                                                    Logger.getLogger(MainWrapperEcosystem.class.getName()).log(Level.INFO, "Response status code value: " + issuesWithHeaders.getStatusCodeValue(), "");
                                                    takeABreak(token, fetcher);
                                                    issuesWithHeaders = fetcher.requestUri(searchURIWithPage, token);

                                                }
                                                Logger.getLogger(MainWrapperEcosystem.class.getName()).log(Level.INFO, "Completed request for: " + searchURIWithPage.toString(), "");
                                                issues = issuesWithHeaders.getBody();
                                                body = new JSONObject(issues);
                                            }
                                        }

                                    } //end going over the pages for one search
                                }//end 422 etc
                            }//end going over the searches for the different bot-users

                            
                        }//end going over the list of projects. 
                    } catch (IOException e) {
                        Logger.getLogger(MainWrapperEcosystem.class.getName()).log(Level.SEVERE, Arrays.toString(e.getStackTrace()) + "An error occurred while trying to create a file.", "");
                    }
                } catch (IOException e) {
                    Logger.getLogger(MainWrapperEcosystem.class.getName()).log(Level.SEVERE, Arrays.toString(e.getStackTrace()) + "An error occurred while trying to create a file.", "");
                }
            } catch (IOException e) {
                Logger.getLogger(MainWrapperEcosystem.class.getName()).log(Level.SEVERE, Arrays.toString(e.getStackTrace()) + "An Error Occurred while trying to read user file", "");
            }
        } catch (IOException e) {
            Logger.getLogger(MainWrapperEcosystem.class.getName()).log(Level.SEVERE, Arrays.toString(e.getStackTrace()) + "An Error Occurred while trying to read project file", "");
        }

    }

    private void commentOrEventpagesConsumption(JSONObject issue, Fetcher fetcher, String token, MongoCollection commentsCollection, String whatToGet) throws JSONException, IOException {
        //do-while-loop for comment or eventpages consumption.
        ResponseEntity<String> responseWithHeaders;
        String commentsOrEventsUrl = issue.getString(whatToGet);
        do {
            responseWithHeaders = fetcher.request(commentsOrEventsUrl, token);

            //deal with rate limit and non 200 requests
            while (responseWithHeaders.getStatusCodeValue() != 200) {
                Logger.getLogger(MainWrapperEcosystem.class.getName()).log(Level.INFO, "Response status code value: " + responseWithHeaders.getStatusCodeValue() + " when fetching " + commentsOrEventsUrl, "");
                takeABreak(token, fetcher);
                responseWithHeaders = fetcher.request(commentsOrEventsUrl, token);

            }

            String commentsOrEvents = responseWithHeaders.getBody();
            JSONArray commentsOrEventsArr = new JSONArray(commentsOrEvents);

            //adding the commments/events one by one to the db
            for (int j = 0; j < commentsOrEventsArr.length(); j++) {
                commentsCollection.insertOne(org.bson.Document.parse(commentsOrEventsArr.getJSONObject(j).toString()));
            }
            //Get next page of comments/events for this issue
            String link = responseWithHeaders.getHeaders().getFirst("link");
            commentsOrEventsUrl = fetcher.extractURIByRel(link, "next");
        } while (commentsOrEventsUrl != null); //end fetching
    }

    private void takeABreak(String token, Fetcher fetcher) {

        if (Integer.parseInt(fetcher.requestsLeft(token)) <= 1 || fetcher.requestsLeftSearch(token) <= 1) {
            try {

                Thread.sleep(fetcher.milisToSleep(Math.max((Long.parseLong(fetcher.timeToReset(token))), fetcher.timeToResetSearch(token))) + 2000);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                Logger.getLogger(MainWrapperEcosystem.class
                        .getName()).log(Level.SEVERE, "Sleep interrupted" + ie, "");
            }
        } else {
            try {
                //sleep one minute
                Thread.sleep(60000);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                Logger.getLogger(MainWrapperEcosystem.class
                        .getName()).log(Level.SEVERE, "Sleep interrupted" + ie, "");
            }
        }
    }

}
