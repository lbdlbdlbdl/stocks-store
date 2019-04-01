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












