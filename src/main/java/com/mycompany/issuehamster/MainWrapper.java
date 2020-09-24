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

        String token = Files.readString(Path.of(System.getProperty("user.home") + "/Issue-hamster-files/token.txt")).strip();
        String project;

        try (BufferedReader br = new BufferedReader(new FileReader(System.getProperty("user.home") + "/Issue-hamster-files/projects.txt"))) {
            while ((project = br.readLine()) != null) {
                project = project.strip();
                System.out.println("----------------");
                System.out.println("Read from project file: " + project);
                System.out.println("----------------");
                Fetcher fetcher = new Fetcher();
                // String issues = fetcher.projectIssues("ampproject/amphtml", token);
                String issues = fetcher.projectIssues(project, token);
                JSONArray arr = new JSONArray(issues);

                for (int i = 0; i < 3; i++) {

                    //for each 
                    String body = arr.getJSONObject(i).getString("body");
                    String title = arr.getJSONObject(i).getString("title");
                    String commentsUrl = arr.getJSONObject(i).getString("comments_url");
                    String eventsUrl = arr.getJSONObject(i).getString(body);
                    System.out.println("-------Body---------");

                    System.out.println(body);
                    System.out.println("---------Title-------");

                    System.out.println(title);
                    System.out.println("--------CommentsUrl--------");

                    System.out.println(commentsUrl);
                    System.out.println("--------CommentsUrlContent--------");

                    String comments = fetcher.issueComments(commentsUrl, token);
                    JSONArray commentsArr = new JSONArray(comments);

                    //for (int j = 0; j < commentsArr.length(); j++) {
                    for (int j = 0; j < commentsArr.length() && j < 3; j++) {
                        String commentsBody = commentsArr.getJSONObject(j).getString("body");
                        System.out.println("-------Comments Body---------");
                        System.out.println("----------------" + j);
                        System.out.println(commentsBody);
                    }
                    //JSONArray commentsArr = new JSONArray(comments);
                    System.out.println("--------CommentsUrlContent--------");
                    //for (int j = 0; j < commentsArr.length(); j++) {
                    for (int j = 0; j < commentsArr.length() && j < 3; j++) {
                        String commentsBody = commentsArr.getJSONObject(j).getString("body");
                        System.out.println("-------Comments Body---------");
                        System.out.println("----------------" + j);
                        System.out.println(commentsBody);
                    }

                }
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }

    }
}
