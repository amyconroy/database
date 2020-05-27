package uk.ac.bris.cs.databases.cwk2;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
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

// CREATE TABLE Forum(
//  id          INTEGER        PRIMARY KEY   AUTO_INCREMENT,
//  title       VARCHAR(100)   NOT NULL      UNIQUE
//);
//
//CREATE TABLE Topic(
//  id          INTEGER        PRIMARY KEY   AUTO_INCREMENT,
//  title       VARCHAR(100)   NOT NULL,
//  forumId     INTEGER        NOT NULL,
//  CONSTRAINT  Forum_FK       FOREIGN KEY(forumId) REFERENCES Forum(id)
//);
//
//CREATE TABLE Person(
//  id        INTEGER          PRIMARY KEY   AUTO_INCREMENT,
//  name      VARCHAR(100)     NOT NULL,
//  username  VARCHAR(10)      NOT NULL      UNIQUE,
//  stuId     VARCHAR(10)      NULL
//);
//
//CREATE TABLE Post(
//  id          INTEGER        PRIMARY KEY   AUTO_INCREMENT,
//  timePosted  DATETIME       NOT NULL,
//  postText    VARCHAR(8000)  NOT NULL,
//  personId    INTEGER        NOT NULL,
//  topicId     INTEGER        NOT NULL,
//  CONSTRAINT  Person_FK      FOREIGN KEY(personId) REFERENCES Person(id),
//  CONSTRAINT  Topic_FK      FOREIGN KEY(topicId) REFERENCES Topic(id)
//);

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

    // todo how to check what topicId is
    @Override
    public Result<TopicView> getTopic(int topicId) {
        try (PreparedStatement p = c.prepareStatement(
            "SELECT title, Post.postNumber AS postNum, Person.name AS name, " +
                    "Post.postText AS text, Post.timePosted AS date FROM Topic" +
                    "INNER JOIN Post ON Post.topicId = Topic.id" +
                    "INNER JOIN Person ON Person.id = Post.personId" +
                    "WHERE Topic.id = ?"
        )) {
            p.setInt(1, topicId);
            ResultSet r = p.executeQuery();
            if(!r.next()){
                return Result.failure("Topic does not exist.");
            }
            String title = r.getString("title");
            ArrayList<SimplePostView> postView = new ArrayList<>();
            do{
                int postNum = r.getInt("postNum");
                String author = r.getString("name");
                String text = r.getString("text");
                String date = r.getString("date");
                SimplePostView simplePostView = new SimplePostView(postNum, author, text, date);
                postView.add(simplePostView);
            } while(r.next());
            TopicView topicView = new TopicView(topicId, title, postView);
            return Result.success(topicView);
        } catch (SQLException e) {
            return Result.fatal(e.getMessage());
        }
    }

    /* level 2 */
    // make list of topics in that forum

    /**
     * Create a new forum.
     * @param title - the title of the forum. Must not be null or empty and
     * no forum with this name must exist yet.
     * @return success if the forum was created, failure if the title was
     * null, empty or such a forum already existed; fatal on other errors.
     *
     * Difficulty: **
     * Used by: /newforum => /createforum (CreateForumHandler)
     */

    // CREATE TABLE Forum(
//  id          INTEGER        PRIMARY KEY   AUTO_INCREMENT,
//  title       VARCHAR(100)   NOT NULL      UNIQUE
//);

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
    public Result<ForumView> getForum(int id) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Result createPost(int topicId, String username, String text) {
        throw new UnsupportedOperationException("Not supported yet.");
    }


    /* level 3 */

    @Override
    public Result createTopic(int forumId, String username, String title, String text) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
