package uk.ac.bris.cs.databases.cwk2;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
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
 * General/basic SQL queries and updates
 * are run in the separate Queries class
 * that handles any executing and updating of the
 * queries. More specific queries updated in the
 * method in the API.
 * Implementations author ac16888.
 *
 *
 * @author csxdb
 */
public class API implements APIProvider {
    private final Connection c;
    private final Queries query = new Queries(); // contains general SQL queries for this database schema
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
        // limits as specified in the create / drop script (checked to ensure they don't exceed)
        if (studentId != null && studentId.equals("")) {
            return Result.failure("StudentId can be null, but cannot be the empty string.");
        }
        // must be in two steps, checking length after checking not null (can't do at same time)
        if(studentId !=null && !(checkLength(studentId, 10))){
            return Result.failure("StudentId cannot exceed 10 characters.");
        }
        if (name == null || name.equals("") || !(checkLength(name, 100))) {
            return Result.failure("Name cannot be empty or exceed 100 characters.");
        }
        if (username == null || username.equals("") || !(checkLength(username, 10))) {
            return Result.failure("Username cannot be empty or exceed 10 characters.");
        }
        try {
            /* first check that the user does not exist - boolean and is false is user is an existing user.
            query needed to ensure username is not duplicated */
            if(!query.checkNotExistingUser(username, c)){
                return Result.failure("User with username" + username + "already exists.");
            }
        } catch (SQLException e) {
            return Result.fatal(e.getMessage());
        }
        try { query.insertPerson(name, username, studentId, c);
        } catch (SQLException e) {
            return rollBack(e);
        }
        return Result.success();
    }

    /* level 1 */

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
            // was not able to access the username (no results in result set)
            if (!r.next()) {
                return Result.failure("A user called " + username + "does not exist.");
            }
            String name = r.getString("name");
            String stuId = r.getString("stuId");
            // if null, change it to empty string, ensure no errors thrown when making the personView
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
                int id = r.getInt(1); // id is the first column in the table
                String title = r.getString(2); // title of the forum is second
                ForumSummaryView forumSummaryView = new ForumSummaryView(id, title);
                forumList.add(forumSummaryView); // returns a list of ForumSummaryView objects
            }
            return Result.success(forumList);
        } catch (SQLException e) {
            return Result.fatal(e.getMessage());
        }
    }

    @Override
    public Result<Integer> countPostsInTopic(int topicId) {
        try (PreparedStatement p = c.prepareStatement(
                // post.id unique per post so used to select a count of all posts
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
    // this is executed in one query by joining the post and person table
    public Result<TopicView> getTopic(int topicId) {
        try (PreparedStatement p = c.prepareStatement(
        "SELECT title, Person.name AS name, " +
                    "Post.postText AS text, Post.timePosted AS date " +
                    "FROM Topic " +
                    "INNER JOIN Post ON Post.topicId = Topic.id " +
                    "INNER JOIN Person ON Person.id = Post.personId " +
                    "WHERE Topic.id = ?"
        )) {
            p.setInt(1, topicId);
            ResultSet r = p.executeQuery();
            // already executing query so extra validation check to ensure that the topic exists
            if(!r.next()){
                return Result.failure("Topic does not exist.");
            }
            String title = r.getString("title");
            ArrayList<SimplePostView> postView = new ArrayList<>();
            int postNum = 1; // used to count the number of posts in the topic, iterates through results
           /* do while loop to account for r.next() already being called - no guard to check
            that there is a post as one post must be created when making topic */
            do{
                String author = r.getString("name");
                String text = r.getString("text");
                String date = r.getString("date");
                SimplePostView simplePostView = new SimplePostView(postNum, author, text, date);
                postView.add(simplePostView); // list of SimplePostView returned, add here
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
        // ensure that title is less than 100 characters
        if(!checkLength(title, 100)){
            return Result.failure("Please ensure title is less than 100 characters.");
        }
        // check that title does not already exist in forum despite forum title being unique
        try{
            // to allow user targeted error rather than SQL error - could be removed as title is UNIQUE
            if(!query.checkNotExistingForum(title, c)){
                return Result.failure("Forum named " + title + " already exists");
            }
            query.insertForum(title, c); //inserts new forum based on title in DB
        }
        catch (SQLException e) {
            return rollBack(e);
        }
        return Result.success();
    }

    @Override
    public Result<ForumView> getForum(int id) {
        try (PreparedStatement p = c.prepareStatement(
        "SELECT Forum.title AS forumTitle, Topic.id AS topicId, Topic.title AS topicTitle " +
                 "FROM Topic " +
                 "RIGHT JOIN Forum ON Forum.id = Topic.forumId " +
                 "WHERE Forum.id = ?"
        )) {
            p.setInt(1, id);
            ResultSet r = p.executeQuery();
            // as already executing query extra check to ensure that forum exists
            if(!r.next()) { return Result.failure("Forum does not exist."); }
            String forumTitle = r.getString("forumTitle");
            ArrayList<SimpleTopicSummaryView> summaryView = new ArrayList<>();
            // if there are no existing topics in the forum
            if(r.getString("topicId") != null){
                do{
                    int topicId = r.getInt("topicId");
                    String topicTitle = r.getString("topicTitle");
                    SimpleTopicSummaryView simpleTopicView = new SimpleTopicSummaryView(topicId, id, topicTitle);
                    summaryView.add(simpleTopicView);
                } while(r.next());
            }
            ForumView forumView = new ForumView(id, forumTitle, summaryView);
            return Result.success(forumView);
        } catch (SQLException e) {
            return Result.fatal(e.getMessage());
        }
    }

    @Override
    public Result createPost(int topicId, String username, String text) {
        Integer personId;
        // parsing of the various inputs
        if (username == null || username.equals("")) { return Result.failure("Username cannot be empty."); }
        // cant make an empty post
        if (text == null || text.equals("")) { return Result.failure("Post text cannot be empty."); }
        // limit based on create/drop script
        if(!checkLength(text, 8000)){
            return Result.failure("Post length is too long, maximum 8000 characters allowed.");
        }
        /* first checks that user exists and then gets their id number to create the post
        as the user id is necessary, also checks that valid user (no additional queries needed) */
        try {
            personId = query.getUserId(username, c);
            if(personId == null){
                return Result.failure("User does not exist."); // returns null if the username isnt found
            }
        }catch (SQLException e) {
            return Result.fatal(e.getMessage());
        }
        try { query.insertPost(text, personId, topicId, c); //insert the data in to post table
        } catch (SQLException e) {
            return rollBack(e);
        }
        return Result.success();
    }

    /* level 3 */

    @Override
    public Result createTopic(int forumId, String username, String title, String text) {
        int topicId;
        Integer personId;
        if(username == null || username.equals("")) { return Result.failure("Username cannot be empty."); }
        // cant make an empty post
        if(text == null || text.equals("")) { return Result.failure("First post text cannot be empty."); }
        if(title == null || title.equals("")) { return Result.failure("Title cannot be empty."); }
        // title has a limit of 100 characters, checks that it is less than that
        if(!checkLength(title, 100)){ return Result.failure("Title cannot exceed 100 characters."); }
        /* checks for existing user and gets the user id at the same time, checks existing user
        as already includes a need to select user id*/
        try {
            personId = query.getUserId(username, c);
            if(personId == null){
                return Result.failure("User does not exist."); // returns null if the username isnt found
            }
        } catch (SQLException e) {
            return Result.fatal(e.getMessage());
        }
        /* below are the execution queries - there is no check for valid forum to remove
        * redunant queries in the system. due to the forumId FK this will be checked
        * by the SQLException if there is an error when clicking on a forum that does not exist*/
        try {
            query.insertTopic(title, forumId, personId, c);
            topicId = query.getTopicId(c);
            query.insertPost(text, personId, topicId, c);
        }
        catch (SQLException e) {
            return rollBack(e);
        }
        return Result.success();
    }

    /// END OF INTERFACE METHODS ///
    /// Below are private methods to assist in functionalities of API methods///

    /**
     * Count chars to guard against SQL exceptions for .
     * @param input - the input to count the characters of
     * @param limit - the limit as specified by the schema
     * @return Boolean value, if characters in input is less than input (true),
     * false if not
     */
    private boolean checkLength(String input, int limit){
        int chars = input.length();
        return chars < limit;
    }

    /**
     * Excutes the rollback after catching the exception to avoid duplicated code
     * @param e - the SQLException that has been caught
     * @return Result type with appropriate message
     */
    private Result rollBack(SQLException e){
        try {
            c.rollback();
        } catch (SQLException f) {
            return Result.fatal("SQL error on rollback - [" + f +
                    "] from handling exception " + e);
        }
        return Result.fatal(e.getMessage());
    }
}
