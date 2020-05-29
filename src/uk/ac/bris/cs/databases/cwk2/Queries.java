package uk.ac.bris.cs.databases.cwk2;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Connection;

/**
 * Separate class for SQL Queries specifically
 * to allow use across API methods. More specific
 * SQL queries are kept in their relevant API
 * method (ie ones that search for multiple
 * results specific to that functionality, or
 * others not likely to be used across tables)
 *
 * @author ac16888
 */
public class Queries {

    /**
     * Checks if the user does NOT exist
     * @param username - username to validate
     * @return - returns boolean to check that the user does not exist
     * (false if they do exist, true if they do not)
     *
     * Used by /addNewPerson
     */
    public boolean checkNotExistingUser(String username, Connection c) throws SQLException {
        // selects count to see if there are any results for that username
        try (PreparedStatement p = c.prepareStatement(
        "SELECT count(1) AS c FROM Person WHERE username = ?"
        )) {
            p.setString(1, username);
            ResultSet r = p.executeQuery();
            // if count greater than 0 then the user exists
            if (r.next() && r.getInt("c") > 0) {
                return false; //user already exists
            }
        }
        return true;
    }

    /**
     * Checks that the forum does NOT exist to avoid duplicated forum titles
     * @param forumTitle - checks for matching title, used rather than id because UNIQUE
     * @return - returns boolean to check that the forum does not exist
     * (false if it does exist, true if it does not)
     *
     * Used by /createForum
     */
    public boolean checkNotExistingForum(String forumTitle, Connection c) throws SQLException {
        // similar to check not existing user, count based on forumTitle (forumTitles are unique)
        try (PreparedStatement p = c.prepareStatement(
        "SELECT count(1) AS count FROM Forum WHERE title = ?"
        )) {
            p.setString(1, forumTitle);
            ResultSet r = p.executeQuery();
            // if count greater than 0 then the forum exists
            if (r.next() && r.getInt("count") > 0) {
                return false; //forum already exists
            }
        }
        return true;
    }

    /**
     * insert new person in to the database, rollback caught in the calling method
     * @param name - name of the user - not null
     * @param username - username, checked prior to inserting that it does not exist
     * @param stuId - can be null
     *
     * Used by /addNewPerson
     */
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

    /**
     * insert new forum in to the database, rollback caught in the calling method
     * @param title - title of the forum, already ensured that it does not exist, can not be null
     *
     * Used by /createForum
     */
    public void insertForum(String title, Connection c) throws SQLException{
        try (PreparedStatement p = c.prepareStatement(
        "INSERT INTO Forum (title) VALUES (?)"
        )) {
            p.setString(1, title);
            p.executeUpdate();
            c.commit();
        }
    }

    /**
     * insert new topic in to the database, rollback caught in the calling method
     * @param title - title of the topic, can not be null
     * @param forumId - FK for the forum, can not be null
     * @param personId - id of the person who wrote the topic, can not be null
     *
     * Used by /createTopic
     */
    public void insertTopic(String title, int forumId, int personId, Connection c) throws SQLException {
        try (PreparedStatement s = c.prepareStatement(
        "INSERT INTO Topic (title, forumId, personId) VALUES (?, ?, ?)"
        )) {
            s.setString(1, title);
            s.setInt(2, forumId);
            s.setInt(3, personId);
            s.executeUpdate();
            c.commit();
        }
    }

    /**
     * insert new post in to the database, rollback caught in the calling method
     * @param text - body of the post, cannot be null/empty
     * @param topicId - FK for the topic, cannot be null
     * @param personId - id of the person who wrote the post, cannot be null
     *
     * Used by /createTopic, /createPost
     */
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

    /**
     * insert new post in to the database, rollback caught in the calling method
     * the topicId is retrieved by getting the most recently insterted topics
     * by ordering to create the first post
     * @return - int value, that is the topic id
     *
     * used by /createTopic
     */
    public int getTopicId(Connection c) throws SQLException{
        int topicId;
        // gets the topicId by getting the most recent topicId (Used to create new post just after topic
        // is created therefore most recent topicId is the correct one)
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
