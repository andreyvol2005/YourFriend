<p align="center">
  <img src="docs/icon.png" width="96" alt="Иконка YourFriend">
</p>

<h1 align="center">YourFriend</h1>

<p align="center"><b>Тёплый собеседник, который всегда рядом</b></p>

| Выбор пола | Выбор имени | Тёплая беседа |
|:---:|:---:|:---:|
| [<img src="docs/screenshots/01-gender.png" width="260" alt="Экран выбора пола друга">](docs/screenshots/01-gender.png) | [<img src="docs/screenshots/02-name.png" width="260" alt="Экран выбора имени друга">](docs/screenshots/02-name.png) | [<img src="docs/screenshots/03-chat.png" width="260" alt="Экран тёплой беседы с другом">](docs/screenshots/03-chat.png) |

<p align="center">
  <img src="https://img.shields.io/badge/Android-7.0%2B-3DDC84?logo=android&logoColor=white" alt="Android 7.0+">
  <img src="https://img.shields.io/badge/Kotlin-7F52FF?logo=kotlin&logoColor=white" alt="Kotlin">
  <img src="https://img.shields.io/badge/AI-GigaChat-FF7AB6" alt="GigaChat">
  <img src="https://img.shields.io/badge/Material%203-757575?logo=materialdesign&logoColor=white" alt="Material 3">
</p>

**YourFriend** — Android-приложение для тёплого разговора с виртуальным другом. Вы выбираете, кто он — девушка или парень, — даёте ему имя, и он отвечает так, как отвечал бы близкий человек: без оценок, непрошеных советов и канцелярита.

Ответы генерирует **GigaChat**. Профиль и переписка остаются на телефоне: ни аккаунта, ни облака, ни ленты.

## Содержание

- [Возможности](#возможности)
- [Как это работает](#как-это-работает)
- [Скачать APK](#скачать-apk)
- [Сборка и запуск](#сборка-и-запуск)
- [Приватность и безопасность](#приватность-и-безопасность)
- [Стек](#стек)
- [Структура проекта](#структура-проекта)
- [Тесты](#тесты)
- [Ограничения и идеи](#ограничения-и-идеи)
- [Автор](#автор)

## Возможности

- **Друг, которого вы создаёте.** Два характера на выбор, любое имя и обращение по имени в ответах.
- **Разговор, а не анкета.** GigaChat получает системный промпт и последние 16 сообщений, поэтому помнит контекст и не повторяется.
- **Тёплый тон.** Промпт просит сначала услышать чувство, говорить 1–3 предложениями и не засыпать вопросами.
- **Бережные границы.** Никаких диагнозов и непрошеных советов. В остром состоянии друг мягко предлагает обратиться к близкому человеку или специалисту.
- **Атмосфера.** Живой orb с дыханием и искрами, звёздный фон, пружинные анимации и мягкая хаптика.
- **Приватность по умолчанию.** Профиль и история лежат в `SharedPreferences` на устройстве; очистка — одним действием.
- **Тёмный интерфейс.** Material 3, edge-to-edge, русский язык.

## Как это работает

1. Вы выбираете пол и имя друга — приложение сохраняет профиль локально.
2. Пишете сообщение. Оно попадает в историю и отправляется в GigaChat вместе с системным промптом и последними 16 репликами.
3. GigaChat возвращает ответ, приложение очищает разметку и показывает его в чате.
4. История обрезается до 250 сообщений и остаётся на устройстве.

## Скачать APK

Готового APK в релизах пока нет. Причина практическая: ключ GigaChat встраивается в `BuildConfig` на этапе сборки, а из APK его можно достать за пару минут. Публиковать такой файл — значит отдать свой лимит токенов первому желающему.

Поэтому:

- в репозитории нет бинарников и ключей;
- APK собирается локально по [инструкции ниже](#сборка-и-запуск);
- когда у проекта появится бэкенд-прокси, ключ перестанет попадать в приложение, и готовый APK можно будет выложить в [Releases](https://github.com/andreyvol2005/YourFriend/releases).

Если APK уже собран: включите на телефоне установку из неизвестных источников, перекиньте `app-debug.apk` на устройство и откройте файл.

## Сборка и запуск

### Что понадобится

- Android Studio с JDK 21 (или отдельный JDK 21);
- Android SDK 36 (compileSdk 36.1);
- ключ GigaChat API.

### 1. Клонировать репозиторий

```bash
git clone https://github.com/andreyvol2005/YourFriend.git
cd YourFriend
```

### 2. Получить ключ GigaChat

Ключ авторизации выдаётся в личном кабинете [GigaChat Studio](https://developers.sber.ru/studio). Создайте приложение с доступом к GigaChat API и скопируйте Authorization key. У бесплатного тарифа есть лимит токенов — актуальные условия смотрите в кабинете.

### 3. Создать `gigachat.properties`

В корне проекта:

```properties
gigachat.authKey=ВАШ_КЛЮЧ
gigachat.scope=GIGACHAT_API_PERS
gigachat.model=GigaChat-3-Pro
```

Файл уже в `.gitignore` — в коммит он не попадёт. Не вставляйте ключ в код, README или Issues.

### 4. Запустить

Откройте проект в Android Studio, дождитесь Gradle Sync и нажмите **Run**. Или соберите APK из терминала:

```bash
# Windows
gradlew.bat :app:assembleDebug

# macOS / Linux
./gradlew :app:assembleDebug
```

Готовый файл: `app/build/outputs/apk/debug/app-debug.apk`.

Для release-сборки добавьте свой `signingConfig` — в проекте его нет, чтобы не хранить keystore в репозитории.

## Приватность и безопасность

- Профиль и переписка хранятся локально в `SharedPreferences`. Приложение не создаёт аккаунт и не синхронизирует историю с сервером.
- Для ответа текст последних сообщений отправляется в GigaChat. Без интернета друг не ответит.
- Ключ GigaChat не хранится в репозитории: он читается из `gigachat.properties`, который исключён через `.gitignore`.
- Ключ попадает в `BuildConfig` и, значит, в собранный APK. Не публикуйте такие APK и не передавайте их третьим лицам. Если ключ утёк — перевыпустите его в кабинете.
- Приложение не заменяет живого человека или специалиста. В кризисной ситуации промпт просит друга мягко направить к близким или в экстренную службу.

## Стек

| Слой | Технологии |
|---|---|
| Язык | Kotlin |
| UI | Android Views, Material 3, ConstraintLayout, RecyclerView |
| Анимации | DynamicAnimation, ValueAnimator, Canvas |
| Сеть | OkHttp, GigaChat API (OAuth 2.0 + chat completions) |
| Хранение | SharedPreferences + JSON |
| Сборка | Gradle 9.5, AGP 9.3.3, JDK 21 |
| Минимум | Android 7.0 (API 24) |

## Структура проекта

```text
app/src/main/java/com/example/yourfriend/
├── MainActivity.kt              # онбординг, чат, меню
├── chat/
│   ├── GigaChatClient.kt        # OAuth, запросы, ошибки
│   ├── GigaChatPrompt.kt        # системный промпт и история
│   ├── GigaChatTls.kt           # сертификаты Минцифры
│   ├── ChatAdapter.kt
│   ├── ChatItemAnimator.kt
│   ├── FriendPersona.kt
│   └── TextMagic.kt
├── data/
│   ├── ChatMessage.kt
│   ├── ChatStore.kt
│   └── FriendProfile.kt
└── ui/
    ├── AvatarOrbView.kt         # orb с дыханием и искрами
    ├── MagicBackgroundView.kt
    ├── SparkleBurstView.kt
    ├── TypingDotsView.kt
    └── ViewMagic.kt

app/src/main/res/                 # layout, drawable, themes, strings
docs/screenshots/                 # скриншоты для README
tools/generate_launcher_icons.py  # генератор растровых иконок
```

## Тесты

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebugAndroidTest
```

Инструментальный тест проверяет package name; для запуска нужен эмулятор или устройство.

## Ограничения и идеи

- Один друг на устройство; сменить его можно через «Начать заново».
- Нет облачной синхронизации и экспорта переписки.
- Нет стриминга ответа: сообщение появляется целиком.
- Публичного APK нет, пока ключ GigaChat встраивается в сборку.

Идеи на будущее: бэкенд-прокси, несколько профилей друзей, экспорт и резервное копирование переписки.

## Автор

[andreyvol2005](https://github.com/andreyvol2005)

Если проект пригодился — поставьте звезду репозиторию.
