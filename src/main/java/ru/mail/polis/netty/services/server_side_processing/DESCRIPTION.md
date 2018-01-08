Минимальный скриптовый язык полуассемблерного вида. 
Запускается на любой ноде с помощью команды:
    
    /script file_path
    
где **file_path** - путь к файлу, содержащему скрипт.

Любая команда имеет вид:
    
    address command register/name/file_path, params...

В распоряжении имеется 5 зарезервированных переменных 
(их названия совпадают с 32 разрядными регистрами):
    
    eax, ebx, ecx, edx, res
    
Поддерживаемые команды (первым аргументом в каждой скриптовой строке
является адрес этой строки, здесь он опускается):

     1. arr name, arguments...         // создать массив с именем name и заполнить аргументами
     2. put name, val                  // добавить в массив с именем name значение val
     3. rem name, val                  // удалить из массива с именем name значение val
     4. get register, name, index      // достать элемент из массива с именем name по индексу index, положить в register
     5. soa register, name             // положить размер массива с именем name в регистр register
     
     6. pls register, value            // прибавить к значению в регистре register значение value
     7. add register, value            // положить в register значение value
    
     8. out register/name              // вывести значение/значения регистра/массива в консоль
    
     9. jmp address, condition_address // переместить instruction_ptr на адрес address, если условие по адресу 
                                         condition_address истинно
                                         
    10. eq register, condition         // результат выражения condition кладется в register, если true - кладется 1, 
                                          иначе 0. Condition имеет вид: (operand operator operand), слитно и обрамляя
                                          скобками, пример: (eax<10).
    11. eq register, cond, op, address // то же, что и обычный eq, только здесь op может принимать 2 значения: or или
                                          and, а address содержит адрес следующего логического выражения. Таким 
                                          образом можно сконструировать длинное логическое выражение.
                                          
    12. del register, file_path        // удалить файл по пути file_path, результат операции в register (1 или 0)
    13. delc register, file_path, 
             condition_address         // удалить файл по пути file_path, если логическое выражение по адресу 
                                          condition address истинно, результат операции в register (1 или 0)
    14. mov register, file_path, dest  // переместить файл file_path по пути dest, результат операции в register (1 
                                          или 0)
    15. cop register, file_path, dest  // скопировать файл file_path по пути dest (полный путь до файла), 
                                          результат операции в register
    16. fin register, file_path        // найти файл, результат операции в register (1 - если найден, 0 - если нет)
    17. ren register, file_path, name  // переименовать файл file_path на name, результат операции в register (1 или 0)
    18. rea file_path                  // считать содержимое файла file_path в консоль
    19. siz register, file_path        // получить размер файла file_path, результат - размер, в register
    20. wrt register, file_path, 
            str/register, parameter    // записать в файл file_path строку str или содержимое переменной register, 
                                          parameter может быть либо 1 (писать с новой строки), либо 0 (запись в начало)
                                          
                                          
Все пути к файлам, а так же значения в массивах, не должны содержать пробелов
или запятых, пути не должны содержать ковычек.

Примеры:

Удалить файл, если он существует

    0 add  eax, /tmp/file/1.txt
    1 fin  ebx, eax
    2 delc edx, eax, 3 
    3 eq   ecx, (ebx==1)
    4 out  edx
    
Записать в файл несколько строк из массива
    
    
    0 arr words, word1, word2, word3
    1 soa ebx, words
    2 add eax, 0
    3 get res, words, eax
    4 wrt edx, /tmp/file/1.txt, res, 1
    5 pls eax, 1
    6 eq  edx, (eax<ebx)
    7 jmp 3, 6 
    
    
 
