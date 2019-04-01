create table "User" (
    "id"                 serial        primary key,
    "login"              varchar(64)   not null unique,
    "passwordHash"       varchar(128)  not null,
    "salt"               varchar(128)  not null,
    check("login" ~ '^[a-z|A-Z|\d|_]')
);
CREATE TABLE "Stock" (
	"id" serial  ,
	"code" varchar NOT NULL,
	"name" varchar NOT NULL,
	"iconUrl" varchar DEFAULT 'icon.jpg',
	"sale" numeric  NOT NULL,
	"buy" numeric  NOT NULL,
	CONSTRAINT Stock_pk PRIMARY KEY ("id")
);

CREATE TABLE "Storage" (
	"id" serial ,
	"login" varchar NOT NULL,
	"idStock" integer NOT NULL,
	"count" integer NOT NULL,
	CONSTRAINT Storage_pk PRIMARY KEY ("id")
	);

INSERT INTO "Stock" (code,name,sale,buy)
VALUES
('TCS','TCS Group (Tinkoff)', 23, 22.7),
('GAZP','ПAO Гaзпpoм', 125, 123.3);
