package uk.ac.bris.cs.databases.cwk2;

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
 * This class has been updated to provide
 * based on use case. Here results and parsing
 * of input occurs.
 *
 * SQL queries and updates
 * are run in the separate Queries class
 * that handles any executing and updating of the
 * queries. Implementations author ac16888.
 *
 *
 * @author csxdb
 */
public class API implements APIProvider {
    private final Connection c;
    private Queries query = new Queries();

    public API(Connection c) {
        this.c = c;
    }


    /* predefined methods */

    @Override
    public Result<Map<String, String>> getUsers() {
        try (Statement s = c.createStatement()) {
            ResultSet r = s.executeQuery("SELECT name, username FROM Person");
            Map<String, String> data = new HashMap<>();
            while (r.next()) {
                data.put(r.getString("username"), r.getString("name"));
            }
            return Result.success(data);
        } catch (SQLException ex) {
            return Result.fatal("database error - " + ex.getMessage());
        }
    }

    @Override
    public Result addNewPerson(String name, String username, String studentId) {
        if (studentId != null && studentId.equals("")) {
            return Result.failure("StudentId can be null, but cannot be the empty string.");
        }
        if (name == null || name.equals("")) {
            return Result.failure("Name cannot be empty.");
        }
        if (username == null || username.equals("")) {
            return Result.failure("Username cannot be empty.");
        }
        try {
            if(!query.checkNotExistingUser(username, c)){
                return Result.failure("User with username" + username + "already exists.");
            }
        } catch (SQLException e) {
            return Result.fatal(e.getMessage());
        }
        try { query.insertPerson(name, username, studentId, c);
        } catch (SQLException e) {
            try {
                c.rollback();
            } catch (SQLException f) {
                return Result.fatal("SQL error on rollback - [" + f +
                "] from handling exception " + e);
            }
            return Result.fatal(e.getMessage());
        }
        return Result.success();
    }

    /* level 1 */

    //todo make with empty id

    @Override
    public Result<PersonView> getPersonView(String username) {
        if (username == null || username.equals("")) {
            return Result.failure("Username cannot be empty.");
        }
        try (PreparedStatement p = c.prepareStatement(
        "SELECT name, stuId FROM Person WHERE username = ?"
        )) {
            p.setString(1, username);
            ResultSet r = p.executeQuery();
            if (!r.next()) {
                return Result.failure("A user called " + username + "does not exist.");
            }
            String name = r.getString(1);
            String stuId = r.getString(2);
            if(stuId == null) stuId = "";
            PersonView personView = new PersonView(name, username, stuId);
            return Result.success(personView);
        } catch (SQLException e) {
            return Result.fatal(e.getMessage());
        }
    }

    @Override
    public Result<List<ForumSummaryView>> getForums() {
        try (PreparedStatement p = c.prepareStatement(
        "SELECT * FROM Forum"
        )) {
            ArrayList<ForumSummaryView> forumList = new ArrayList<>();
            ResultSet r = p.executeQuery();
            while (r.next()) {
                int id = r.getInt(1);
                String title = r.getString(2);
                ForumSummaryView forumSummaryView = new ForumSummaryView(id, title);
                forumList.add(forumSummaryView);
            }
            return Result.success(forumList);
        } catch (SQLException e) {
            return Result.fatal(e.getMessage());
        }
    }

    @Override
    public Result<Integer> countPostsInTopic(int topicId) {
        try (PreparedStatement p = c.prepareStatement(
        "SELECT Count(Post.id) FROM Post WHERE topicId = ?"
        )) {
            p.setInt(1, topicId);
            ResultSet r = p.executeQuery();
            Integer postCount = r.getInt(1);
            return Result.success(postCount);
        } catch (SQLException e) {
            return Result.fatal(e.getMessage());
        }
    }

    @Override
    public Result<TopicView> getTopic(int topicId) {
        try (PreparedStatement p = c.prepareStatement(
        "SELECT title, Post.id AS postNum, Person.name AS name, " +
                    "Post.postText AS text, Post.timePosted AS date FROM Topic " +
                    "INNER JOIN Post ON Post.topicId = Topic.id " +
                    "INNER JOIN Person ON Person.id = Post.personId " +
                    "WHERE Topic.id = ?"
        )) {
            p.setInt(1, topicId);
            ResultSet r = p.executeQuery();
            if(!r.next()){
                return Result.failure("Topic does not exist.");
            }
            String title = r.getString("title");
            ArrayList<SimplePostView> postView = new ArrayList<>();
            int postNum = 1;
            do{
                String author = r.getString("name");
                String text = r.getString("text");
                String date = r.getString("date");
                SimplePostView simplePostView = new SimplePostView(postNum, author, text, date);
                postView.add(simplePostView);
                postNum++;
            } while(r.next());
            TopicView topicView = new TopicView(topicId, title, postView);
            return Result.success(topicView);
        } catch (SQLException e) {
            return Result.fatal(e.getMessage());
        }
    }

    /* level 2 */
    @Override
    public Result createForum(String title) {
        if (title == null || title.equals("")) {
            return Result.failure("Title cannot be empty.");
        }
        int chars = countCharacters(title);
        if(chars > 100){
            return Result.failure("Please ensure title is less than 100 characters.");
        }
        // check that title does not already exist in forum despite forum being unique
        try {
            if(!query.checkNotExistingForum(title, c)){
                return Result.failure("Forum named" + title + "already exists");
            }
        } catch (SQLException e) {
            return Result.fatal(e.getMessage());
        }
        try{
            query.insertForum(title, c);
        }
        catch (SQLException e) {
            try {
                c.rollback();
            } catch (SQLException f) {
                return Result.fatal("SQL error on rollback - [" + f +
                        "] from handling exception " + e);
            }
            return Result.fatal(e.getMessage());
        }
        return Result.success();
    }

    @Override
    // todo change the check
    public Result<ForumView> getForum(int id) {
        String forumTitle;
        //  public SimpleTopicSummaryView(int topicId, int forumId, String title)
        //  public ForumView(int id, String title, List<SimpleTopicSummaryView> topics)
        try (PreparedStatement p = c.prepareStatement(
        "SELECT title AS forumTitle FROM Forum WHERE id = ?"
        )) {
            p.setInt(1, id);
            ResultSet r = p.executeQuery();
            if (!r.next()) {
                return Result.failure("Forum does not exist.");
            }
            forumTitle = r.getString("forumTitle");
        } catch (SQLException e) {
            return Result.fatal(e.getMessage());
        }
        try (PreparedStatement p = c.prepareStatement(
       "SELECT Topic.id AS topicId, Topic.title AS topicTitle FROM Topic " +
                        "WHERE Topic.forumId = ?"
        )) {
            p.setInt(1, id);
            ResultSet r = p.executeQuery();
            ArrayList<SimpleTopicSummaryView> summaryView = new ArrayList<>();
            while(r.next()){
                int topicId = r.getInt("topicId");
                String topicTitle = r.getString("topicTitle");
                SimpleTopicSummaryView simpleTopicView = new SimpleTopicSummaryView(topicId, id, topicTitle);
                summaryView.add(simpleTopicView);
            }
            ForumView forumView = new ForumView(id, forumTitle, summaryView);
            return Result.success(forumView);
        } catch (SQLException e) {
            return Result.fatal(e.getMessage());
        }
    }

    @Override
    public Result createPost(int topicId, String username, String text) {
        int personId;
        // parsing of the various inputs
        if (username == null || username.equals("")) {
            return Result.failure("Username cannot be empty.");
        } // cant make an empty post
        if (text == null || text.equals("")) {
            return Result.failure("Post text cannot be empty.");
        }
        // first check that user exists and get their id number to create the post
        try{
            // user does not exist
            if(query.checkNotExistingUser(username, c)){
                return Result.failure("No user existing.");
            }
            personId = query.getPersonId(username, c);
        } catch (SQLException e) {
            return Result.fatal(e.getMessage());
        }
        try {
            query.insertPost(text, personId, topicId, c);
        } catch (SQLException e) {
            try {
                c.rollback();
            } catch (SQLException f) {
                return Result.fatal("SQL error on rollback - [" + f +
                        "] from handling exception " + e);
            }
            return Result.fatal(e.getMessage());
        }
        return Result.success();
    }

    /* level 3 */

    @Override
    public Result createTopic(int forumId, String username, String title, String text) {
        int topicId;
        int personId;
        if (username == null || username.equals("")) {
            return Result.failure("Username cannot be empty.");
        } // cant make an empty post
        if (text == null || text.equals("")) {
            return Result.failure("First post text cannot be empty.");
        }
        if (title == null || title.equals("")) {
            return Result.failure("Title cannot be empty.");
        }
        try{
            // user does not exist
            // todo check not at same time SELECT AS () subselect
            if(query.checkNotExistingUser(username, c)){
                return Result.failure("No user existing.");
            }
            personId = query.getPersonId(username, c);
        } catch (SQLException e) {
            return Result.fatal(e.getMessage());
        }
        //todo this getforum doesnt perform the check
        getForum(forumId); // check that the forum exists
        try {
            query.insertTopic(title, forumId, personId, c);
            topicId = query.getTopicId(c);
            query.insertPost(text, personId, topicId, c);
        }
        catch (SQLException e) {
            try {
                c.rollback();
            } catch (SQLException f) {
                return Result.fatal("SQL error on rollback - [" + f +
                        "] from handling exception " + e);
            }
            return Result.fatal(e.getMessage());
        }
        return Result.success();
    }

    /**
    /* private methods to assist in functionalities, primarily for checking correct input */

    // this method is used to count chars to guard against database exceptions
    private int countCharacters(String input){
        return input.length();
    }
}
