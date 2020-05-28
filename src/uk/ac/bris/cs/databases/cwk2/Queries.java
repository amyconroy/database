package uk.ac.bris.cs.databases.cwk2;

import uk.ac.bris.cs.databases.api.Result;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import uk.ac.bris.cs.databases.api.APIProvider;
import uk.ac.bris.cs.databases.api.ForumSummaryView;
import uk.ac.bris.cs.databases.api.ForumView;
import uk.ac.bris.cs.databases.api.Result;
import uk.ac.bris.cs.databases.api.PersonView;
import uk.ac.bris.cs.databases.api.SimplePostView;
import uk.ac.bris.cs.databases.api.SimpleTopicSummaryView;
import uk.ac.bris.cs.databases.api.TopicView;

/**
 * Separate class for SQL Queries specifically
 * to allow use across API methods
 *
 * @author ac16888
 */
public class Queries {
    public boolean checkNotExistingUser(String username, Connection c) throws SQLException {
        try (PreparedStatement p = c.prepareStatement(
        "SELECT count(1) AS c FROM Person WHERE username = ?"
        )) {
            p.setString(1, username);
            ResultSet r = p.executeQuery();
            if (r.next() && r.getInt("c") > 0) {
                // user already exists
                return false;
            }
        }
    return true;
    }

    public int getPersonId(String username, Connection c) throws SQLException{
       int personId;
       try (PreparedStatement p = c.prepareStatement(
       "SELECT id FROM Person WHERE username = ?"
        )) {
            p.setString(1, username);
            ResultSet r = p.executeQuery();
            r.next();
            personId = r.getInt("id");
        }
       return personId;
    }

    public boolean checkNotExistingForum(String forumTitle, Connection c) throws SQLException {
        try (PreparedStatement p = c.prepareStatement(
        "SELECT count(1) AS count FROM Forum WHERE title = ?"
        )) {
            p.setString(1, forumTitle);
            ResultSet r = p.executeQuery();
            // if count greater than 0 then the forum exists
            if (r.next() && r.getInt("count") > 0) {
                return false;
            }
        }
        return true;
    }

    public void insertPerson(String name, String username, String stuId, Connection c) throws SQLException {
        try (PreparedStatement p = c.prepareStatement(
        "INSERT INTO Person (name, username, stuId) VALUES (?, ?, ?)"
        )) {
            p.setString(1, name);
            p.setString(2, username);
            p.setString(3, stuId);
            p.executeUpdate();
            c.commit();
        }
    }

    public void insertForum(String title, Connection c) throws SQLException{
        try (PreparedStatement p = c.prepareStatement(
        "INSERT INTO Forum (title) VALUES (?)"
        )) {
            p.setString(1, title);
            p.executeUpdate();
            c.commit();
        }
    }

    public void insertTopic(String title, int forumId, Connection c) throws SQLException {
        try (PreparedStatement s = c.prepareStatement(
        "INSERT INTO Topic (title, forumId) VALUES (?, ?)"
        )) {
            s.setString(1, title);
            s.setInt(2, forumId);
            s.executeUpdate();
            c.commit();
        }
    }

    public void insertPost(String text, int personId, int topicId, Connection c)throws SQLException{
        try (PreparedStatement p = c.prepareStatement(
        "INSERT INTO Post (timePosted, postText, personId, topicId) VALUES (now(), ?, ?, ?)"
        )) {
            p.setString(1, text);
            p.setInt(2, personId);
            p.setInt(3, topicId);
            p.executeUpdate();
            c.commit();
        }
    }

    public int getTopicId(Connection c) throws SQLException{
        int topicId;
        try (PreparedStatement p = c.prepareStatement(
        "SELECT id AS topicId FROM Topic " +
                "ORDER BY id DESC LIMIT 1"
        )) {
            ResultSet r = p.executeQuery();
            r.next();
            topicId = r.getInt("topicId");
        }
        return topicId;
    }


}
