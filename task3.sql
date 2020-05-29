DROP TABLE IF EXISTS PostLike;
DROP TABLE IF EXISTS TopicLike;

CREATE TABLE TopicLike(
  PRIMARY KEY (topicId, personId),
  topicId       INTEGER       NOT NULL,
  personId      INTEGER       NOT NULL,
  CONSTRAINT    Person_FK2     FOREIGN KEY(personId) REFERENCES Person(id),
  CONSTRAINT    Topic_FK      FOREIGN KEY(topicId) REFERENCES Topic(id)
);

CREATE TABLE PostLike(
  PRIMARY KEY (postId, personId),
  postId      INTEGER        NOT NULL,
  personId    INTEGER        NOT NULL,
  CONSTRAINT  Person_FK4     FOREIGN KEY(personId) REFERENCES Person(id),
  CONSTRAINT  Post_FK      FOREIGN KEY(postId)   REFERENCES Post(id)
);
