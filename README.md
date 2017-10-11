# 2017-highload-kv
Курсовой проект 2017 года [курса](https://polis.mail.ru/curriculum/program/discipline/50/) "Проектирование высоконагруженных систем" в [Технополис](https://polis.mail.ru).

## Этап 1. HTTP API + хранилище (deadline 2017-10-10)
### Fork
[Форкните проект](https://help.github.com/articles/fork-a-repo/), склонируйте и добавьте `upstream`:
```
$ git clone git@github.com:<username>/2017-highload-kv.git
Cloning into '2017-highload-kv'...
remote: Counting objects: 34, done.
remote: Compressing objects: 100% (24/24), done.
remote: Total 34 (delta 2), reused 34 (delta 2), pack-reused 0
Receiving objects: 100% (34/34), 11.43 KiB | 3.81 MiB/s, done.
Resolving deltas: 100% (2/2), done.
$ git remote add upstream git@github.com:polis-mail-ru/2017-highload-kv.git
$ git fetch upstream
From github.com:polis-mail-ru/2017-highload-kv
 * [new branch]      master     -> upstream/master
```

### Make
Так можно запустить тесты:
```
$ gradle test
```

А вот так -- сервер:
```
$ gradle run
```

### Develop
Откройте в IDE -- [IntelliJ IDEA Community Edition](https://www.jetbrains.com/idea/) нам будет достаточно.

**ВНИМАНИЕ!** При запуске тестов или сервера в IDE необходимо передавать Java опцию `-Xmx1g`. 

В своём Java package `ru.mail.polis.<username>` реализуйте интерфейс [`KVService`](src/main/java/ru/mail/polis/KVService.java) и поддержите следующий HTTP REST API протокол:
* HTTP `GET /v0/entity?id=<ID>` -- получить данные по ключу `<ID>`. Возвращает `200 OK` и данные или `404 Not Found`.
* HTTP `PUT /v0/entity?id=<ID>` -- создать/перезаписать (upsert) данные по ключу `<ID>`. Возвращает `201 Created`.
* HTTP `DELETE /v0/entity?id=<ID>` -- удалить данные по ключу `<ID>`. Возвращает `202 Accepted`.

Возвращайте реализацию интерфейса в [`KVServiceFactory`](src/main/java/ru/mail/polis/KVServiceFactory.java#L48).

Продолжайте запускать тесты и исправлять ошибки, не забывая [подтягивать новые тесты и фиксы из `upstream`](https://help.github.com/articles/syncing-a-fork/). Если заметите ошибку в `upstream`, заводите баг и присылайте pull request ;)

### Report
Когда всё будет готово, присылайте pull request со своей реализацией на review. Не забывайте **отвечать на комментарии в PR** и **исправлять замечания**!

## Этап 2. Кластер (deadline 2017-11-07)
Реализуем поддержку кластерных конфигураций, состоящих из нескольких узлов, взаимодействующих друг с другом через реализованный HTTP API.
Для этого в `KVServiceFactory` передаётся "топология", представленная в виде множества координат **всех** узлов кластера в формате `http://<host>:<port>`.

Кроме того, HTTP API расширяется query-параметром `replicas`, содержащим количество узлов, которые должны подтвердить операцию, чтобы она считалась выполненной успешно.
Таким образом, теперь узлы должны поддерживать расширенный протокол (совместимый с предыдущей версией):
* HTTP `GET /v0/entity?id=<ID>[&replicas=<RF>]` -- получить данные по ключу `<ID>`. Возвращает:
  * `200 OK` и данные
  * `404 Not Found`, если ни одна из реплик не содержит данные
  * `504 Not Enough Replicas`, если не получили `200`/`404` от `<RF>` реплик

* HTTP `PUT /v0/entity?id=<ID>[&replicas=<RF>]` -- создать/перезаписать (upsert) данные по ключу `<ID>`. Возвращает:
  * `201 Created`, если `<RF>` реплик подтвердили операцию
  * `504 Not Enough Replicas`

* HTTP `DELETE /v0/entity?id=<ID>[&replicas=<RF>]` -- удалить данные по ключу `<ID>`. Возвращает:
  * `202 Accepted`, если `<RF>` реплик подтвердили операцию
  * `504 Not Enough Replicas`

Если параметр `replicas` не указан, то используется значение по умолчанию, равное **кворуму** от количества узлов в кластере.

Так же как и на Этапе 1 присылайте pull request со своей реализацией поддержки кластерной конфигурации на review.
Набор тестов будет расширяться, поэтому не забывайте **подмёрдживать upstream** и **реагировать на замечания**.

## Этап 3. Нагрузочное тестирование и оптимизация (deadline 2017-12-05)
TBD

## Bonus (deadline 2017-12-19)
Фичи, которые позволяют получить дополнительные баллы:
* 10М ключей: нетривиальная реализация хранения данных
* [Consistent Hashing](https://en.wikipedia.org/wiki/Consistent_hashing): распределение данных между узлами устойчивое к сбоям
* Streaming: работоспособность при значениях больше 1 ГБ (и `-Xmx1g`)
* Conflict resolution: [отметки времени Лампорта](https://en.wikipedia.org/wiki/Lamport_timestamps) или [векторные часы](https://en.wikipedia.org/wiki/Vector_clock)
* Expire: возможность указания [времени жизни записей](https://en.wikipedia.org/wiki/Time_to_live)
* Server-side processing: трансформация данных с помощью скрипта, запускаемого на узлах кластера через API
* Предложите своё

Если решите реализовать что-то бонусное, обязательно сначала обсудите это с преподавателем.