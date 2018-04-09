Task
====
- invoke specified Callable at specified LocalDateTime;
- on overloading execute tasks with ordering by specified LocalDateTime and scheduling sequence;
- invocation scheduling may be called from different Threads; 
- implement using Java language and Java platform API;
- don't use embedded schedulers (java.util.concurrent.ScheduledExecutorService and same);

#### Original text:
> На вход поступают пары (LocalDateTime, Callable). Нужно реализовать систему, которая будет выполнять Callable для 
> каждого пришедшего события в указанном LocalDateTime, либо как можно скорее в случае если система перегружена и не 
> успевает все выполнять (имеет беклог). Задачи должны выполняться в порядке согласно значению LocalDateTime либо в 
> порядке прихода события для равных LocalDateTime. События могут приходить в произвольном порядке и добавление новых 
> пар (LocalDateTime, Callable) может вызываться из разных потоков.
>
> Решение можно оформить в любом удобном виде: проект на гитхабе, архив с исходным кодом и т.п. Для решения использовать 
> можно любые встроенные средства. Выполнить необходимо на Java. 
>
> И, пожалуйста, не решайте через шедулер :)