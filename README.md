# Шаблон проекта для курсовой

Подключены основные библиотеки, которые вам понадобятся. 

Так же, нужно будет где-то развернуть само приложение. В принципе, неважно где, можете использовать что угодно, от Google Cloud до старого ноутбука под кроватью, но проще всего это сделать с помощью [heroku](heroku.com).

### Как задеплоить на heroku
 ```bash
heroku login 
heroku create
heroku addons:create heroku-postgresql:hobby-dev
git push heroku master
```

