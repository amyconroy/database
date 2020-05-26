DROP TABLE IF EXISTS Post;
DROP TABLE IF EXISTS Person;
DROP TABLE IF EXISTS Topic;
DROP TABLE IF EXISTS Forum;

CREATE TABLE Forum(
  id          INTEGER        PRIMARY KEY   AUTO_INCREMENT,
  title       VARCHAR(100)   NOT NULL      UNIQUE
);

CREATE TABLE Topic(
  id          INTEGER        PRIMARY KEY   AUTO_INCREMENT,
  title       VARCHAR(100)   NOT NULL,
  forumId     INTEGER        NOT NULL,
  CONSTRAINT  Forum_FK       FOREIGN KEY(forumId) REFERENCES Forum(id)
);

CREATE TABLE Person(
  id        INTEGER          PRIMARY KEY   AUTO_INCREMENT,
  name      VARCHAR(100)     NOT NULL,
  username  VARCHAR(10)      NOT NULL      UNIQUE,
  stuId     VARCHAR(10)      NULL
);

CREATE TABLE Post(
  id          INTEGER        PRIMARY KEY   AUTO_INCREMENT,
  timePosted  DATETIME       NOT NULL,
  postText    VARCHAR(8000)  NOT NULL,
  personId    INTEGER        NOT NULL,
  topicId     INTEGER        NOT NULL,
  CONSTRAINT  Person_FK      FOREIGN KEY(personId) REFERENCES Person(id),
  CONSTRAINT  Topic_FK      FOREIGN KEY(topicId) REFERENCES Topic(id)
);
