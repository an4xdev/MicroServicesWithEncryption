drop table if exists users;
create table users(id serial primary key, login varchar(50) unique not null , pub_key text not null);
select * from users;