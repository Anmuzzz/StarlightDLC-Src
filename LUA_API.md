# StarlightDLC Lua API

## Установка

1. Включи модуль **Lua** (категория Other)
2. Помести `.lua` файлы в папку: `.minecraft/isusdlc/scripts/`
3. Скрипты загружаются автоматически при включении модуля

## Глобальная таблица `client`

### Вывод информации

| Функция | Описание |
|---------|----------|
| `client.print(text)` | Вывести сообщение в чат (только для тебя) |
| `client.log(text)` | Вывести в консоль (логи) |
| `client.chat(text)` | Отправить сообщение в чат (как обычный игрок) |

### Модули

| Функция | Описание |
|---------|----------|
| `client.toggle(name)` | Включить/выключить модуль. Возвращает `true/false` |
| `client.isEnabled(name)` | Проверить, включён ли модуль |
| `client.getModules()` | Получить список всех модулей |

### Игрок

| Функция | Описание |
|---------|----------|
| `client.getPlayer()` | Возвращает таблицу с полями: `name`, `x`, `y`, `z`, `health`, `food`, `pitch`, `yaw`, `grounded` |
| `client.jump()` | Заставляет игрока прыгнуть |
| `client.isJumpKeyDown()` | Проверяет, зажата ли кнопка прыжка |

### События

| Функция | Описание |
|---------|----------|
| `client.on(eventName, callback)` | Подписаться на событие |
| `client.off(eventName)` | Отписаться от события |
| `client.clear()` | Отписаться от всех событий |

**Доступные события:**

| Событие | Тип | Описание |
|---------|-----|----------|
| `"tick"` | `ClientPlayerTickEvent` | Каждый тик (20 раз/сек) |
| `"gametick"` | `GameTickEvent` | Игровой тик |
| `"render"` | `HudRenderEvent` | Каждый кадр (рендер HUD) |
| `"render3d"` | `Render3DEvent` | 3D рендер |
| `"keypress"` | `KeyPressEvent` | Нажатие клавиши |

Чтобы отменить событие (если поддерживается), верни `false` из callback:
```lua
client.on("tick", function(e)
    -- не отменяем tick, он не cancellable
end)
```

## Примеры

### Авто-чат
```lua
local delay = 0
client.on("tick", function()
    delay = delay + 1
    if delay >= 200 then
        client.chat("/spawn")
        delay = 0
    end
end)
```

### Проверка здоровья
```lua
client.on("tick", function()
    local p = client.getPlayer()
    if p.health < 6 then
        client.print("§cМало здоровья! Ешь!")
    end
end)
```

### Тоггл модуля по условию
```lua
client.on("tick", function()
    local p = client.getPlayer()
    if p.y > 100 then
        client.toggle("Flight")
        client.off("tick")
    end
end)
```

## IDE (Редактор скриптов)

В модуле **Lua** есть кнопка **IDE** — она открывает редактор скриптов:
- **Слева** — список `.lua` файлов
- **Справа** — редактор кода (многострочный ввод)
- **New** — создать новый скрипт
- **Delete** — удалить выбранный скрипт
- **Reload** — перезагрузить все скрипты
- **Save** — сохранить изменения и перезагрузить скрипт

## Пример: NoJumpDelay

```lua
-- NoJumpDelay.lua
local running = true
client.on("tick", function()
    if not running then return end
    local p = client.getPlayer()
    if p.name == nil or not p.grounded then return end
    if client.isJumpKeyDown() then
        client.jump()
    end
end)
client.print("§aNoJumpDelay загружен!")
```

## Важно

- Скрипты загружаются при старте модуля **Lua**
- Для перезагрузки скриптов используй кнопку **Reload** в IDE или выключи/включи модуль
- Если в скрипте ошибка — он не загрузится, смотри лог (остальные скрипты продолжат загружаться)
- Стандартная библиотека Lua (`math`, `string`, `table`, `os.time`, `os.clock`, `os.date`) доступна
- Опасные функции отключены: `dofile`, `loadfile`, `load`, `loadstring`, `require`, а также `os.execute`, `os.exit`, `os.remove`, `os.rename`, `os.getenv`, `io.open`, `io.popen` и т.д.
- Каждый скрипт изолирован: у него свои глобальные переменные и свои подписки на события — при выгрузке/перезагрузке скрипта его подписки снимаются автоматически, остальные скрипты продолжают работать
- В IDE: **Ctrl+S** — сохранить, **Ctrl+V** — вставить
