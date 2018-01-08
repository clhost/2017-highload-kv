Так как у меня всё ОЧЕНЬ плохо, для начала тестировалось следующее:

##### Put на 1/1
Сначала был рассмотрен внутренний протокол node - обращение напрямую к ноде:
    
    wrk --latency -c4 -d30s -s loadtesting/put2outof3.lua http://localhost:8080
    Running 30s test @ http://localhost:8080
      2 threads and 4 connections
      Thread Stats   Avg      Stdev     Max   +/- Stdev
        Latency     2.24ms    3.29ms  94.43ms   93.22%
        Req/Sec   518.24    104.67   670.00     67.17%
      Latency Distribution
         50%    1.63ms
         75%    1.91ms
         90%    3.98ms
         99%   13.14ms
      30946 requests in 30.02s, 2.95MB read
    Requests/sec:   1030.93
    Transfer/sec:    100.68KB


Здесь всё достаточно плохо, количество запросов - мало. Попробую повысить перформанс.

Далее протестирую 1/1 на entity протокол:

    wrk --latency -c4 -d30s -s loadtesting/put2outof3.lua http://localhost:8080
    Running 30s test @ http://localhost:8080
      2 threads and 4 connections
      Thread Stats   Avg      Stdev     Max   +/- Stdev
        Latency   122.46ms   92.16ms 632.91ms   81.89%
        Req/Sec    20.14     10.74    50.00     80.16%
      Latency Distribution
         50%   97.42ms
         75%  152.97ms
         90%  252.74ms
         99%  436.04ms
      1073 requests in 30.03s, 104.79KB read
    Requests/sec:     35.73
    Transfer/sec:      3.49KB

Вот здесь ужас, плюс ко всему Netty подливает масла в огонь:
    
    Dec 12, 2017 6:08:55 PM io.netty.util.ResourceLeakDetector reportUntracedLeak
    SEVERE: LEAK: ByteBuf.release() was not called before it's garbage-collected.
    
Что-ж, какие потенциальные проблемы могут возникнуть при тестировании 1/1:

В зависимости от ключа выбирается нода и на неё текущая нода пересылает данные. Здесь проблема может быть в том, что NodeService имеет плохую реализацию.Проверяю - делаю так, чтобы нода при 1/1 выбирала сама себя (временно):
   
    
    wrk --latency -c4 -d30s -s loadtesting/put2outof3.lua http://localhost:8080
    Running 30s test @ http://localhost:8080
      2 threads and 4 connections
      Thread Stats   Avg      Stdev     Max   +/- Stdev
        Latency     2.66ms    4.21ms 104.15ms   94.00%
        Req/Sec   476.55    107.64   670.00     65.77%
      Latency Distribution
         50%    1.66ms
         75%    2.16ms
         90%    5.37ms
         99%   16.19ms
      28442 requests in 30.04s, 2.71MB read
    Requests/sec:    946.88
    Transfer/sec:     92.47KB

Что это дает? Дает понимание того, что NodeService работает ужасно. В чем может быть проблема?
На каждый новый коннект между нодами создается новое подключение, где аллоцируются 
несколько потоков (около 8 штук), живут они не долго и уничтожаются. Следовательно, это сильно
тормозит мои ноды. Попробую переписать NodeService с реализации Netty на Apache, используя 
Apache fluent:

    wrk --latency -c4 -d30s -s loadtesting/put2outof3.lua http://localhost:8080
    Running 30s test @ http://localhost:8080
      2 threads and 4 connections
      Thread Stats   Avg      Stdev     Max   +/- Stdev
        Latency     5.11ms   12.06ms 253.05ms   96.18%
        Req/Sec   367.01    109.29   640.00     70.66%
      Latency Distribution
         50%    2.41ms
         75%    5.35ms
         90%    9.48ms
         99%   41.19ms
      21851 requests in 30.05s, 2.08MB read
    Requests/sec:    727.27
    Transfer/sec:     71.02KB

Ситуация заметно улучшилась, плюс ко всему пропали memory leak'и - возникали в 
NettyNodeServiceHandler'е.
 
##### Put на 2/3
Дописав ApacheNodeService, Scheduler с возможность сохранения всех заголовков запроса, 
результаты заметно улучшились:
    
     wrk --latency -c4 -d5m -s loadtesting/put2outof3.lua http://localhost:8080
     Running 5m test @ http://localhost:8080
       2 threads and 4 connections
       Thread Stats   Avg      Stdev     Max   +/- Stdev
         Latency     1.71ms    2.77ms 129.33ms   94.15%
         Req/Sec   689.23    113.08     1.03k    75.55%
       Latency Distribution
          50%    1.21ms
          75%    1.69ms
          90%    2.97ms
          99%   11.52ms
       411130 requests in 5.00m, 39.21MB read
     Requests/sec:   1370.35
     Transfer/sec:    133.82KB
 
    
