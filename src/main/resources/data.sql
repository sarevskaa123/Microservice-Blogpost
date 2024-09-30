INSERT INTO blog_post (title, text, author) VALUES ('First Blog Post', 'This is the content of the first post.', 'testuser'),
     ('Second Blog Post', 'This is the content of the second post.', 'testuser'),
     ('Third Blog Post', 'This is the content of the first post and it is longer than the two before it.', 'testuser');

INSERT INTO tag (tag_name, time_created) VALUES ('Tag1', NOW());
INSERT INTO tag (tag_name, time_created) VALUES ('Tag2', NOW());
INSERT INTO tag (tag_name, time_created) VALUES ('Tag3', NOW());

insert into blog_post_tags (blog_post_id, tag_id) values (1,1), (1,2), (2,2), (2,3), (3,3);



