create table "User" (
    "id"                 serial        primary key,
    "login"              varchar(64)   not null unique,
    "passwordHash"       varchar(128)  not null,
    "salt"               varchar(128)  not null,
    "iconUrl"  varchar(128),
    "balance" float not null,

    check("login" ~ '^[a-z|A-Z|\d|_]')
);

create table "Stock" (
"id"                 serial        primary key,
"name" varchar(64) not null,
"code" varchar(4) not null,
"iconUrl" varchar(128),
"salePrice" float not null , /*цена продажи */
"buyPrice" float not null /*цена покупки > цена продажи*/
);


create table "StocksPackage" (
"id" serial primary key,
"userId" integer not null references "User"("id"),
"stockId" integer not null references "Stock"("id"),
"count" integer  null
);

create table "TransactionHistory"(
"id" serial primary key,
"login" varchar(64) not null references "User"("login"),
"stockId" serial not null references "Stock"("id"),
"amount" integer  not null,
"totalPrice" float not null,
"date" varchar(32)   not null,
 "type" varchar(8)   not null);

create table "PriceHistory" (
"id" serial primary key,
"stockId" integer not null references "Stock"("id"),
"date" timestamp  not null,
"salePrice" float not null ,
"buyPrice" float not null
)
-- INSERT INTO "Stock" VALUES (1, "TCS Group (Tinkoff)", "TCS", "icon.jpg", 35.20, 30.00);
-- INSERT INTO "Stock" VALUES (2, "Raiffeisen Bank", "EUR", "icon.jpg", 20.08, 15.97);
-- INSERT INTO "Stock" VALUES (3, "Sberbank", "RUB", "icon.jpg", 217.08, 150.00);
-- INSERT INTO "Stock"(id,  name, code, iconUrl,salePrice,buyPrice) VALUES ("TCS Group (Tinkoff)", "TCS", "icon.jpg", 35.20, 30.00);2