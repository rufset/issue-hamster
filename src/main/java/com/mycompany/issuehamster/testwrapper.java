/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mycompany.issuehamster;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.http.ResponseEntity;

/**
 * Just some code to test small snippets
 *
 * @author e9linda
 */
public class testwrapper {

    public static void main(String[] args) throws IOException {

        String token = Files.readString(Path.of(System.getProperty("user.home") + "/Issue-hamster-files/token.txt")).strip();
        String project = "rufset/issue-hamster";
        Fetcher fetcher = new Fetcher();
        String projectURI = fetcher.projectToUriToString(project);
        ResponseEntity<String> result = fetcher.request(projectURI, token);
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
