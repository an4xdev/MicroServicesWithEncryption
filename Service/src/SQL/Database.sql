drop table if exists users;
create table users(id serial primary key, login varchar(50) unique not null , pub_key text not null);
-- select * from users;

drop table if exists posts;
create table posts(id serial primary key, user_id integer references users(id), message text not null);
-- select * from posts;

