create table "User" (
    "id"                 serial        primary key,
    "login"              varchar(64)   not null unique,
    "passwordHash"       varchar(128)  not null,
    "salt"               varchar(128)  not null,
    "iconUrl"  varchar(128),
    "balance" double not null,

    check("login" ~ '^[a-z|A-Z|\d|_]'),
    check ("balance" >= 0)
);

create table "Stock" (
"id"                 serial        primary key,
"name" varchar(64) not null,
"code" varchar(4) not null,
"iconUrl" varchar(128),
"salePrice" double not null , /*цена продажи */
"buyPrice" double not null /*цена покупки > цена продажи*/,

check ("salePrice">0, "buyPrice" >0)
);

create table "StocksPackage" (
"id" serial primary key,
"userId" integer not null references "User"("id"),
"stockId" integer not null references "Stock"("id"),
"count" integer not null,

check ("count" > 0)
);

create table "TransactionHistory"(
"id" serial primary key,
"login" varchar(64) not null references "User"("login"),
"stockId" not null references "Stock"("id"),
"amount" integer  not null,
"totalPrice" double not null,
"date" timestamp   not null,
 "type" varchar(8)   not null),

 check ("amount" > 0);

create table "PriceHistory" (
"id" serial primary key,
"stockId" integer not null references "Stock"("id"),
"date" timestamp  not null,
"salePrice" double not null ,
"buyPrice" double not null,

check ("salePrice">0, "buyPrice" >0)
);