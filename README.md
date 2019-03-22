# Тех. документация:

[API](https://app.swaggerhub.com/apis/enp0s23/stocks/0.0.1#/)
[Общие требования](https://drive.google.com/file/d/1aRMDVJx16FRiQO2mJ24xAA2_tduwbblp/view)
[Требования для команды фронтэнда](Diplom_specs.md)


# Шаблон проекта для курсовой

Подключены основные библиотеки, которые вам понадобятся. 

Так же, нужно будет где-то развернуть само приложение. В принципе, неважно где, можете использовать что угодно, от Google Cloud до старого ноутбука под кроватью, но проще всего это сделать с помощью [heroku](https://heroku.com).

Еще подключен плагин [`sbt-revolver`](https://github.com/spray/sbt-revolver), который позволяет удобно перезапускать приложение при разработке. Можете запустить `reStart` и `reStop` соответственно (и `~reStart`, чтобы пересобирать и рестартить приложение при каждом изменении в коде).

Миграциями базы данных занимается [flyway](https://flywaydb.org), при запуске приложения он проверяет соответствие описанной схемы и того, что есть по факту, при необходимости накатывает нужные изменения. Файлы миграций лежат в папке [migrations](src/main/resources/db/migration).
 
Все настройки приложения описаны в файле [application.conf](src/main/resources/application.conf), в формате [HOCON](https://github.com/lightbend/config/blob/master/HOCON.md).
  
### Как задеплоить на heroku
Очевидно, сначала [зарегестрироваться](https://heroku.com) и [скачать](https://devcenter.heroku.com/articles/heroku-cli#download-and-install) клиент.
 
 ```bash
heroku login 
heroku create
heroku addons:create heroku-postgresql:hobby-dev
git push heroku master
```

### Как запустить локально

В качестве базы данных используется [PostgreSQL](https://www.postgresql.org/), так что сначала нужно как-то получить работающий инстанс СУБД.

Есть два варианта, как сделать:
- установить СУБД нативно
- развернуть контейнер (docker)

Вариант с контейнером примерно одинаков для любой ОС. 

- [Устанавливаем](https://docs.docker.com/install), собственно, Docker 
- Скачиваем и запускаем контейнер с СУБД: 
```bash
docker run -d \ 
    --name stocks-store-db \
    -p 5432:5432 \
    -e POSTGRES_DB=store \
    -e POSTGRES_USER=user \ 
    -e POSTGRES_PASSWORD=qwerty \
    postgres:11
```
- Заходим в консоль СУБД, чтобы проверить что все сделали правильно: `docker exec -it stock-store-db /bin/bash -c 'psql -U user store'`
- Все, контейнер с СУБД запущен. После перезагрузки компьютера он выключится, и снова может быть запущен командой `docker start stock-store-db`

#### Linux (Ubuntu)
[Официальная инструкция по установке](https://www.postgresql.org/download/linux/ubuntu/)

[От DigitalOcean, на русском](https://www.digitalocean.com/community/tutorials/postgresql-ubuntu-16-04-ru)

### Windows
[Официальная инструкция по установке](https://www.postgresql.org/download/windows/)

[И на русском](https://postgrespro.ru/windows)

### Mac OS
[Официальная инструкция по установке](https://www.postgresql.org/download/macosx/) 
