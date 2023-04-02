# Кастомная вью аналоговых часов

Аналоговые часы с функцией ручного задания положения стрелок. Также возможно задать состояние в соответсвии с текущем временем. Часы поддерживают сохранение состояния при смене конфигурации. 

## Аттрибуты

| Аттрибут | Тип | Пример | 
| --- | --- | --- | 
| `app:second_hand_color` | color | @color/blue | 
| `app:minute_hand_color` | color | @color/red | 
| `app:hour_hand_color` | color | @color/black | 
| `app:clock_color` | color | @color/black | 
| `app:numbers_font_size` | dimension | 18sp | 
| `app:display_current_time` | boolean | true | 
| Лучше не использовать, так как не будут проводиться необходимые вычисления, но возможонсть поэксперементировать есть. | 
| `app:second_start_time` | integer | 10 | 
| `app:minute_start_time` | integer | 20 | 
| `app:hour_start_time` | integer | 4 | 

