DROP TABLE IF EXISTS PostLike;
DROP TABLE IF EXISTS Post;
DROP TABLE IF EXISTS TopicLike;
DROP TABLE IF EXISTS Topic;
DROP TABLE IF EXISTS Person;
DROP TABLE IF EXISTS Forum;

CREATE TABLE Forum(
  id          INTEGER        PRIMARY KEY   AUTO_INCREMENT,
  title       VARCHAR(100)   NOT NULL      UNIQUE
);

CREATE TABLE Person(
  id        INTEGER          PRIMARY KEY   AUTO_INCREMENT,
  name      VARCHAR(100)     NOT NULL,
  username  VARCHAR(10)      NOT NULL      UNIQUE,
  stuId     VARCHAR(10)      NULL
);

CREATE TABLE Topic(
  id          INTEGER        PRIMARY KEY   AUTO_INCREMENT,
  title       VARCHAR(100)   NOT NULL,
  forumId     INTEGER        NOT NULL,
  personId    INTEGER        NOT NULL,
  CONSTRAINT  Person_FK      FOREIGN KEY(personId) REFERENCES Person(id),
  CONSTRAINT  Forum_FK       FOREIGN KEY(forumId) REFERENCES Forum(id)
);

CREATE TABLE TopicLike(
  topicId       INTEGER       NOT NULL,
  personId      INTEGER       NOT NULL,
  PRIMARY KEY (topicId, personId),
  CONSTRAINT    Person_FK2     FOREIGN KEY(personId) REFERENCES Person(id),
  CONSTRAINT    Topic_FK2      FOREIGN KEY(topicId) REFERENCES Topic(id)
);

CREATE TABLE Post(
  id          INTEGER        PRIMARY KEY   AUTO_INCREMENT,
  timePosted  DATETIME       NOT NULL,
  postText    VARCHAR(8000)  NOT NULL,
  personId    INTEGER        NOT NULL,
  topicId     INTEGER        NOT NULL,
  CONSTRAINT  Person_FK3      FOREIGN KEY(personId) REFERENCES Person(id),
  CONSTRAINT  Topic_FK3       FOREIGN KEY(topicId) REFERENCES Topic(id)
);

CREATE TABLE PostLike(
  PRIMARY KEY (postId, personId),
  postId      INTEGER        NOT NULL,
  personId    INTEGER        NOT NULL,
  CONSTRAINT  Person_FK4     FOREIGN KEY(personId) REFERENCES Person(id),
  CONSTRAINT  Post_FK4       FOREIGN KEY(postId)   REFERENCES Post(id)
);
