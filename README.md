# Шаблон проекта для курсовой

Подключены основные библиотеки, которые вам понадобятся. 

Так же, нужно будет где-то развернуть само приложение. В принципе, неважно где, можете использовать что угодно, от Google Cloud до старого ноутбука под кроватью, но проще всего это сделать с помощью [heroku](https://heroku.com).

Еще подключен плагин [`sbt-revolver`](https://github.com/spray/sbt-revolver), который позволяет удобно перезапускать приложение при разработке. Можете запустить `reStart` и `reStop` соответственно (и `~reStart`, чтобы пересобирать и рестартить приложение при каждом изменении в коде).

Миграциями базы данных занимается [flyway](https://flywaydb.org), при запуске приложения он проверяет соответствие описанной схемы и того, что есть по факту, при необходимости накатывает нужные изменения. Файлы миграций лежат в папке [migrations](src/main/resources/db/migration).
 
### Как задеплоить на heroku
Очевидно, сначала [зарегестрироваться](https://heroku.com) и [скачать](https://devcenter.heroku.com/articles/heroku-cli#download-and-install) клиент.
 
 ```bash
heroku login 
heroku create
heroku addons:create heroku-postgresql:hobby-dev
git push heroku master
```

