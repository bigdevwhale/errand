# Google Play — всё для ручной публикации

Тексты лежат в `listing/<язык>/*.txt` (копируйте как есть), графика — в `graphics/`.
Ниже — что вписать в каждое поле Play Console и в каком порядке.

## 0. Перед стартом

- **Нужен AAB, не APK.** Релизный workflow кладёт в GitHub Release ещё и `Errand-v<версия>.aab`.
  Первая версия для Play — **v1.0.3**. Секреты `SIGNING_*` в репозитории заданы; без них сборка
  подписывается debug-ключом, а такой файл Play отклонит.
- **Языки.** AAB собирается без разбиения по языкам (`bundle.language.enableSplit = false`), иначе Play
  ставит только язык телефона и переключение языка в настройках приложения перестаёт работать.
  Не удаляйте этот блок.
- **Регистрация имени пакета.** Play Console при создании приложения пишет «You can't use this package
  name because it hasn't been registered». Порядок
  ([справка Google](https://support.google.com/googleplay/android-developer/answer/16761053)):
  1. Play Console → **Android developer verification** → **Package names** → **Register package name** → `app.errand`.
  2. **Select key** → ключ с SHA-256 `16254be1958bd5e247e84cf0c8a27a638dc8abef15198461acbe914b3d8abc41`
     (сертификат CN=Errand, O=Stillcraft — им подписаны все релизы).
  3. **Upload APK** → скопировать сниппет, собрать релизный APK с файлом `assets/adi-registration.properties`,
     в котором этот сниппет, подписать тем же ключом и загрузить. Ключ есть только в секретах GitHub,
     так что APK собирает workflow **Package registration proof** (Actions → Run workflow → вставить сниппет),
     APK — в артефактах запуска.
  4. Дождаться письма о регистрации, потом создавать приложение.
- **Подпись (Play App Signing).** При первой загрузке выберите **«Использовать свой ключ»** и загрузите ключ
  из того же keystore через PEPK (инструкция в самом диалоге). Так `app.errand` остаётся за одним
  зарегистрированным ключом, а APK из GitHub и версия из Play обновляют друг друга.
  Если оставить «Google создаст ключ подписи», версии из Play и GitHub друг на друга не встанут.
- **versionCode** берётся из номера запуска workflow (`github.run_number`), он только растёт — Play это устраивает.
- **Новый личный аккаунт разработчика** (создан после ноября 2023): перед выходом в production нужен
  закрытый тест — минимум 12 тестировщиков, которые не выходят из теста 14 дней подряд.
- **Политика конфиденциальности** — `PRIVACY.md` в корне репозитория. После push ссылка:
  `https://github.com/bigdevwhale/errand/blob/main/PRIVACY.md`
  (или перенесите текст на сайт Stillcraft).

## 1. Создание приложения

| Поле | Значение |
|---|---|
| App name | `Errand: Shopping List Runs` |
| Default language | English (United States) – en-US |
| App or game | **App** |
| Free or paid | Free |
| Declarations | обе галочки (Developer Program Policies, US export laws) |

## 2. Store listing → Main store listing

| Поле | en-US | ru-RU (Add translations → Russian) |
|---|---|---|
| App name (≤30) | `listing/en-US/title.txt` | `listing/ru-RU/title.txt` |
| Short description (≤80) | `listing/en-US/short_description.txt` | `listing/ru-RU/short_description.txt` |
| Full description (≤4000) | `listing/en-US/full_description.txt` | `listing/ru-RU/full_description.txt` |
| App icon 512×512 | `graphics/icon-512.png` | та же |
| Feature graphic 1024×500 | `graphics/feature-graphic-en.png` | `graphics/feature-graphic-ru.png` |
| Phone screenshots (2–8) | `graphics/screenshots-en/1…8.png` | `graphics/screenshots-ru/1…8.png` |
| Tablet screenshots | можно не загружать | — |
| Video | пусто | — |

Скриншоты 1080×1920 собраны `tools/play.py` из рендеров README (`docs/screenshots/<язык>`):
Play не берёт картинки длиннее двух ширин, а рендеры 1080×2341, поэтому экран уходит за нижний край
под подписью. Как пересобрать:

```bash
./gradlew testDebugUnitTest -Prender --tests '*RenderArt*' && python tools/shots.py
python tools/play.py   # office, waybill, run, payslip, depot, file, notebook, archive
```

## 3. Store settings

| Поле | Значение |
|---|---|
| App category | Application → **Shopping** |
| Tags (до 5) | Shopping, Productivity, Food & drink, House & home, Notebook |
| Email address | *ваш публичный email* — обязательно, его увидят все |
| Website | `https://bigdevwhale.github.io/stillcraft/apps/errand/` |
| Phone | пусто |
| External marketing | можно оставить включённым |

**Категория.** Посыльный — курьерская игра поверх списка покупок, но приходят в него за списком:
в Shopping его найдут по «список покупок», «shopping list», «grocery list» рядом с похожими приложениями.
Альтернатива — **Productivity**. Категорию можно сменить позже в Store settings.

**Имя разработчика** (Settings → Developer account) — **Stillcraft**, одинаковое для всех приложений:
тогда Play сам покажет под карточкой «Другие приложения этого разработчика».

## 4. App content (Policy → App content)

**Privacy policy** — URL выше.

**App access** — *All functionality is available without any access restrictions.*

**Ads** — *No, my app does not contain ads.*

**Content rating** (анкета IARC)
- Email — ваш; категория — **All Other App Types**.
- На все вопросы — **No**: насилия, страха, сексуального контента, ненормативной лексики нет; алкоголь
  и табак приложение само не показывает (каталог товаров в `Goods.kt` лишь узнаёт «пиво» или «вино»,
  если пользователь сам впишет их в накладную, чтобы посчитать вес — это пользовательский текст, а не
  контент приложения); азартных игр и покупок за деньги нет, жетоны нельзя купить; общения между пользователями и обмена
  контентом нет; местоположение не используется; браузера и поиска нет.
- Ожидаемый результат: Everyone / PEGI 3 / 0+.

**Target audience and content**
- Target age: **13–15, 16–17, 18 and over** (политика: приложение не для детей младше 13).
  Младше 13 не отмечать: иначе действует политика Families с отдельными требованиями.
- *Could your store listing unintentionally appeal to children?* → **No**: это список покупок для
  взрослых, которые ходят в магазин, без детских героев.

**News apps** — **No.**

**COVID-19 contact tracing and status apps** — My app is not a publicly available COVID-19 app.

**Data safety**
- Приложение не запрашивает ни одного разрешения, даже `INTERNET`: накладные, рейсы, цены и контора
  лежат в локальной базе и с устройства не уходят. Списки приходят через «Поделиться» от других
  приложений, обратно ничего не отправляется.
  - *Does your app collect or share any of the required user data types?* → **No**.
  - Итог в карточке: «No data collected», «No data shared with third parties».
- Системное резервное копирование Android делает система, а не приложение, его не декларируем.

**Government apps** — No.

**Financial features** — *My app doesn't provide any financial features.* (Бюджет на рейс и чеки —
это заметки в списке покупок, а не банковские или платёжные функции.)

**Health apps** — ничего не отмечать.

**Advertising ID** — *No* (рекламы и SDK аналитики нет, разрешения `AD_ID` в сборке нет).

**Разрешения и фоновые службы** — деклараций не нужно: разрешений нет, фоновых служб и точных
будильников нет.

## 5. Pricing & distribution

- Countries / regions: все доступные.
- Free, без встроенных покупок.

## 6. Релиз

1. Testing → **Internal testing** (или Closed testing, если аккаунт новый) → Create new release.
2. Загрузить `Errand-v1.0.3.aab` из GitHub Release.
3. Release name: `1.0.3`. Release notes:

```
<en-US>
(listing/en-US/release_notes.txt)
</en-US>
<ru-RU>
(listing/ru-RU/release_notes.txt)
</ru-RU>
```

4. Review release → Start rollout. Первая проверка Google обычно занимает от нескольких дней до недели.
5. После установки из Play проверьте: Настройки → Язык → Русский/English — экран должен
   переключиться сразу; список из мессенджера через «Поделиться» ложится в накладную; ссылки
   в разделе «Сообщество» открываются.

## Следующие версии

- `release_notes.txt` переписать под новую версию (для первой там «First release on Google Play»).
- Скриншоты пересобирать, только если поменялись показанные экраны.

## После публикации

Добавьте в README бейдж и ссылку `https://play.google.com/store/apps/details?id=app.errand`.
