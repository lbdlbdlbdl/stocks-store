create table "User" (
    "id"                 serial        primary key,
    "login"              varchar(64)   not null unique,
    "passwordHash"       varchar(128)  not null,
    "salt"               varchar(128)  not null,
    "iconUrl"  varchar(128),
    "balance" double precision not null,

    check("login" ~ '^[a-z|A-Z|\d|_]'),
    check ("balance" >= 0)
);

create table "Stock" (
"id"                 serial        primary key,
"name" varchar(64) not null,
"code" varchar(4) not null,
"iconUrl" varchar(128),
"salePrice" double precision  not null , /*цена продажи */
"buyPrice" double precision  not null /*цена покупки > цена продажи*/,

check ("salePrice">0),
check("buyPrice" >0)
);

create table "StocksPackage" (
"id" serial primary key,
"userId" integer not null references "User"("id"),
"stockId" integer not null references "Stock"("id"),
"count" integer not null

check("count" >= 0)
);

create table "TransactionHistory"(
"id" serial primary key,
"login" varchar(64) not null references "User"("login"),
"stockId" integer not null references "Stock"("id"),
"amount" integer  not null,
"totalPrice" double precision  not null,
"date" timestamp  not null,
 "type" varchar(8)   not null,

 check ("amount" > 0)
);

create table "PriceHistory" (
"id" serial primary key,
"stockId" integer not null references "Stock"("id"),
"date" timestamp  not null,
"salePrice" double precision  not null,
"buyPrice" double precision  not null,

check ("salePrice">0),
check ("buyPrice" >0)
);

INSERT INTO "Stock"("name", "code", "iconUrl","buyPrice","salePrice") VALUES
('TCS Group (Tinkoff)', 'TCS', 'https://pr-bank.ru/images/upload/tinkoff-bank-logo_thumb512.jpg', 35.20, 30.00),
('Raiffeisen Bank', 'RBIV', 'https://cdn.worldvectorlogo.com/logos/raiffeisen-1.svg', 20.08, 15.97),
('Sberbank', 'SBER', 'http://www.logobank.ru/images/ph/ru/s/sberbank_2009.png', 217.08, 150.00),
('TESLA', 'TSL', 'https://upload.wikimedia.org/wikipedia/commons/thumb/b/bd/Tesla_Motors.svg/800px-Tesla_Motors.svg.png', 267.70, 170.00),
('МегаФон', 'MFON', 'http://megafon.ru/i/logos/share_ru_537x537.png?v=1555211377', 267.70, 170.00),
('Ростелеком', 'RTKM', 'http://sdelaycomp.ru/uploads/posts/2017-01/1485775772_nastroyka-wifi-routera-rostelekom-12.png', 267.70, 170.00),
('ВТБ', 'VTBR', 'https://upload.wikimedia.org/wikipedia/commons/f/f1/VTB_logo_2018.png', 89.95, 80.00),
('Газпром Нефть', 'SIBN', 'http://www.gazprom-neft.com/local/templates/mainframe/images/logo-big-ru.png', 150.00, 80.00),
('QIWI', 'QIWI', 'http://www.seoded.ru/image/qiwi.png', 144.30, 90.85),
('Яндекс', 'YNDX', 'https://cdn.worldvectorlogo.com/logos/yandex-2.svg', 500.00, 350.00),
('МТС', 'MTSS', 'https://upload.wikimedia.org/wikipedia/commons/thumb/c/c2/MTS_logo.svg/1280px-MTS_logo.svg.png', 60.65, 60.00),
('Роснефть', 'ROSN', 'https://upload.wikimedia.org/wikipedia/commons/thumb/c/c2/MTS_logo.svg/1280px-MTS_logo.svg.png', 60.65, 60.00);