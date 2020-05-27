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
 *
 * @author csxdb
 */
public class API implements APIProvider {
    private final Connection c;
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
        try (PreparedStatement p = c.prepareStatement(
       "SELECT count(1) AS c FROM Person WHERE username = ?"
        )) {
            p.setString(1, username);
            ResultSet r = p.executeQuery();
            if (r.next() && r.getInt("c") > 0) {
                return Result.failure("A user called " + username + " already exists.");
            }
        } catch (SQLException e) {
            return Result.fatal(e.getMessage());
        }

        try (PreparedStatement p = c.prepareStatement(
        "INSERT INTO Person (name, username, stuId) VALUES (?, ?, ?)"
        )) {
            p.setString(1, name);
            p.setString(2, username);
            p.setString(3, studentId);
            p.executeUpdate();

            c.commit();
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
        // check that title does not already exist in forum despite forum being unique
        try (PreparedStatement p = c.prepareStatement(
        "SELECT count(1) AS count FROM Forum WHERE title = ?"
        )) {
            p.setString(1, title);
            ResultSet r = p.executeQuery();
            // if count greater than 0 then the forum exists
            if (r.next() && r.getInt("count") > 0) {
                return Result.failure("A forum called " + title + " already exists.");
            }
        } catch (SQLException e) {
            return Result.fatal(e.getMessage());
        }
        try (PreparedStatement p = c.prepareStatement(
         "INSERT INTO Forum (title) VALUES (?)"
        )) {
            p.setString(1, title);
            p.executeUpdate();
            c.commit();
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
        try (PreparedStatement p = c.prepareStatement(
        "SELECT id FROM Person WHERE username = ?"
        )) {
            p.setString(1, username);
            ResultSet r = p.executeQuery();
            if (!r.next()) {
                return Result.failure("A user called " + username + "does not exist.");
            }
            personId = r.getInt("id");
        } catch (SQLException e) {
            return Result.fatal(e.getMessage());
        }
        try (PreparedStatement p = c.prepareStatement(
        "INSERT INTO Post (timePosted, postText, personId, topicId) VALUES (now(), ?, ?, ?)"
        )) {
            p.setString(1, text);
            p.setInt(2, personId);
            p.setInt(3, topicId);
            p.executeUpdate();
            c.commit();
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
        // createPost checks for the userName and that text can't be null and userName
        try (PreparedStatement p = c.prepareStatement(
        "SELECT id FROM Person WHERE username = ?"
        )) {
            p.setString(1, username);
            ResultSet r = p.executeQuery();
            if (!(r.next()) && !(r.getInt("c") > 0)) {
                return Result.failure("A user called " + username + " does not exist.");
            }
            personId = r.getInt("id");
        } catch (SQLException e) {
            return Result.fatal(e.getMessage());
        }
        getForum(forumId); // check that the forum exists
        try (PreparedStatement s = c.prepareStatement(
        "INSERT INTO Topic (title, forumId) VALUES (?, ?)"
        )) {
            s.setString(1, title);
            s.setInt(2, forumId);
            s.executeUpdate();
            c.commit();
        } catch (SQLException e) {
            try {
                c.rollback();
            } catch (SQLException f) {
                return Result.fatal("SQL error on rollback - [" + f +
                        "] from handling exception " + e);
            }
            return Result.fatal(e.getMessage());
        }
        // get the topicId
        try (PreparedStatement p = c.prepareStatement(
        "SELECT id AS topicId FROM Topic " +
                "ORDER BY id DESC LIMIT 1"
        )) {
            ResultSet r = p.executeQuery();
            r.next();
            topicId = r.getInt("topicId");
            try (PreparedStatement t = c.prepareStatement(
            "INSERT INTO Post (timePosted, postText, personId, topicId) VALUES (now(), ?, ?, ?)"
            )) {
                t.setString(1, text);
                t.setInt(2, personId);
                t.setInt(3, topicId);
                t.executeUpdate();
                c.commit();
            } catch (SQLException e) {
                try {
                    c.rollback();
                } catch (SQLException f) {
                    return Result.fatal("SQL error on rollback - [" + f +
                            "] from handling exception " + e);
                }
                return Result.fatal(e.getMessage());
            }
        } catch (SQLException e) {
            return Result.fatal(e.getMessage());
        }
        return Result.success();
    }

    // PRIVATE METHODS //

    private

}
