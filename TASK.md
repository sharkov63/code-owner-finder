# Тестовое задание для участников JetBrains Bootcamp

Это тестовое задание предназначено для отбора участников летней стажировки 
для младшекурсников JetBrains Bootcamp. Ожидаемое время выполнения — 6–8 часов.
По всем вопросам, связанным с выполнением задания, можно обращаться к Виталию Брагилевскому 
(vitaly.bragilevsky@jetbrains.com, @bravit111 в телеграме).

## Задание

При работе с большими кодовыми базами часто удобно знать, кто лучше всех разбирается 
в содержимом конкретного файла. С одной стороны, это может быть человек, который 
написал в этом файле больше всего кода. С другой, особенно если файл существует 
достаточно давно, может случиться так, что исходный автор долго этого кода не касался
и уже мог всё забыть. Наверняка тот, кто с этим кодом работал недавно, но при этом
сделал достаточно значительный вклад, может дать более актуальную информацию. 
Будем называть такого человека владельцем кода (code owner).

Реализуйте плагин к IntelliJ IDEA, который по данным системы
контроля версий оценивает вклад каждого коммиттера в выбранный файл и выдаёт рекомендацию, 
к кому стоит обращаться с вопросами. Заготовка для плагина уже есть – это действие (`AnAction`)
в классе `org.intellij.sdk.action.CodeOwnerFinderAction`. Его можно вызвать из выпадающего
меню для любого файла из Project View. Плагин может возвращать информацию в диалоговом окне, 
создаваемом с помощью `Messages.showMessageDialog`.

Центральный компонент задания — это алгоритм вычисления вклада отдельных коммиттеров
в заданный файл по данным системы контроля версий и определения владельца кода. Постарайтесь
сделать его максимально интеллектуальным. Результат работы алгоритма может, к примеру, не просто
возвращать владельца, но и оценивать вклад каждого разработчика в специальных баллах.
Не забудьте подробно описать и обосновать применимость своего алгоритма.

### Порядок выполнения и отправки на проверку 

Разработку следует вести в ветке `main`, а для отправки на проверку необходимо создать пулл-реквест 
на ветку `task`.

### Подсказки

При реализации плагина вам могут понадобиться следующие классы, свойства и методы IntelliJ Platform:

* `ProjectLevelVcsManager.getInstance` для доступа к данным системы контроля версий
* `VcsContextFactory.SERVICE.getInstance()` для работы с файлами и путями, находящимися 
   под управлением системы контроля версий
* `AbstractVcs.vcsHistoryProvider` для получения списка ревизий (в том числе для заданного файла)
* `VcsFileRevision`

Некоторые операции, связанные с системой контроля версий, нельзя выполнять в потоке UI (в котором
запускается метод `actionPerformed`). Такие операции следует запускать в специальном потоке 
для ввода-вывода. Для этого проще всего воспользоваться функцией `runBlocking`:

```kotlin
val smth = runBlocking(Dispatchers.IO) {
    // запрос к системе контроля версий
}
```

Правда, это приведёт к блокировке пользовательского интерфейса, но если операция не займёт
слишком много времени, то и нестрашно.


## Полезные ссылки по разработке плагинов к IntelliJ IDEA

* [IntelliJ Platform SDK](https://plugins.jetbrains.com/docs/intellij/welcome.html)
* [Running a Simple Gradle-Based IntelliJ Platform Plugin](https://plugins.jetbrains.com/docs/intellij/gradle-prerequisites.html#executing-the-plugin)
* [Creating Actions](https://plugins.jetbrains.com/docs/intellij/working-with-custom-actions.html)
* [Virtual Files](https://plugins.jetbrains.com/docs/intellij/virtual-file.html)
* [Version Control Systems](https://plugins.jetbrains.com/docs/intellij/vcs-integration-for-plugins.html)