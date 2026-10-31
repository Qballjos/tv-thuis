# TV Thuis

Startscherm voor de televisies in huis, gebaseerd op [Free My TV](https://github.com/simbasjungles/free-my-tv) (MIT). Zelfde app-plank, weer en nachtmodus. De posterrij komt uit de eigen [AIO Media Server Manager](https://github.com/Qballjos/aio-media-server-manager) of uit TMDB-tips.

Wat er per versie is gedaan: [docs/changelog.md](docs/changelog.md).

## Bovenaan het scherm

In **Startscherm → Bovenaan het scherm** kies je wat de kaarten tonen:

| Keuze | Inhoud |
| --- | --- |
| Tips | Filmtips van TMDB (Voor jou): Explore, Films, Series |
| Verder kijken | Wat je in de streaming-apps liet liggen |
| Klok | Alleen klok, datum en weer |
| AIO | De mediaserver (zie tabellen hieronder) |

### Tabs bij AIO

| Tab | Inhoud |
| --- | --- |
| Verder kijken | In progress / next-up uit Jellyfin |
| Pas toegevoegd | Films en series die net in Jellyfin of Plex zijn gekomen |
| Films | Trending films van deze week (via de manager / Seerr) |
| Series | Trending series van deze week |

OK op een titel die nog niet in de bibliotheek staat, vraagt hem aan via Seerr. OK op een titel die er al is, opent Wholphin. Plaatjes voor Verder kijken en Pas toegevoegd gaan via de AIO-manager (https), niet rechtstreeks over http naar Jellyfin.

De plank toont, als ze geïnstalleerd zijn: **Wholphin**, **SmartTube**, **Prime Video**, **UniFi Protect** en **Homey**. De YouTube-knop opent SmartTube. Installeer daarbij de SmartTube-bridge (op Google TV en Android TV [ATV_SYTV_Bridge.apk](https://github.com/yuliskov/SmartTube/releases/download/latest/ATV_SYTV_Bridge.apk), op Fire TV [Amazon_SYTV_Bridge.apk](https://github.com/yuliskov/SmartTube/releases/download/latest/Amazon_SYTV_Bridge.apk)). Bij de omschakeling naar TV Thuis gaat de officiële YouTube-app uit, zodat de bridge de stemzoekopdracht kan opvangen. Original zet YouTube weer aan.

## Schermbeveiliging

De schermbeveiliging toont posters (of bij AIO de trendingfoto’s) op een zwarte achtergrond, met klok en weer. Zwarte randen blijven uit; de klok is gedimd en schuift langzaam, zodat een OLED- of Mini-LED-paneel niet één helder vlak blijft branden.

In **Startscherm → Schermbeveiliging**:

| Rij | Betekenis |
| --- | --- |
| Schermbeveiliging | Aan of uit |
| Start na | 5 min, 10 min, 15 min, 30 min, 1 uur of 2 uur |
| Aanvraagknop | Welke knop een titel aanvraagt of opent (standaard OK) |
| Bekijken | Direct de beveiliging tonen |

Tijdens de beveiliging: **links** / **rechts** voor de vorige of volgende foto; de gekozen aanvraagknop vraagt of opent. Andere knoppen sluiten de beveiliging. Volume blijft van de tv.

## Updates op de Android-tv’s

Nieuwe APK’s komen uit de releases van deze repo (`FreeMyTV.apk`), ondertekend met dezelfde sleutel als op de tv’s.

1. **Startscherm → Updates**
2. **OK** als er een nieuwere versie staat
3. Android vraagt of de update geïnstalleerd mag worden

Er is ook een wekelijkse check. De LG (webOS) krijgt deze APK niet.

## Instellingen op de tv

Home Settings → **AIO Media**:

| Veld | Voorbeeld |
| --- | --- |
| AIO-server | het https-adres van jullie manager (of http op het thuisnetwerk) |
| Gebruikersnaam | de beheerder van de manager |
| Wachtwoord | hetzelfde wachtwoord |
| Jellyfin-server | het adres van Jellyfin op het thuisnetwerk (voor Verder kijken) |
| Jellyfin API-sleutel | uit Jellyfin → Dashboard → API-sleutels |
| Zoeken en aanvragen | een titel die niet in de trendinglijst staat |
| Verbinding testen | moet trending- en recent-aantallen tonen |

De tv praat met `POST /api/auth/login` en daarna met `Authorization: Bearer` naar `/api/homepage`, `/api/homepage/search`, `/api/homepage/request` en `/api/homepage/art`. Het wachtwoord blijft op die tv staan.

Stad en eenheid zet je in dezelfde Home Settings (weer en zonsondergang). Wholphin of een andere Jellyfin-client moet op de tv staan om een titel te openen.

`config.properties` kan adres, stad en regio alvast meegeven bij het bouwen. Wachtwoorden en API-sleutels horen daar niet in (behalve wat je bewust inbouwt voor huisbuilds). `config.properties` staat niet in git. Na een update blijven waarden die al op de tv stonden behouden; een schone installatie begint opnieuw.

## Bouwen

Android / Google TV / Fire OS, op een Mac:

```bash
brew install --cask temurin android-commandlinetools android-platform-tools
sdkmanager "build-tools;34.0.0" "platforms;android-34"
./build.sh
```

De eerste run maakt `config.properties`. Daarna nog een keer `./build.sh`. Het resultaat is `FreeMyTV.apk`.

LG webOS (Python 3):

```bash
python3 webos/build.py
```

Dat maakt `FreeMyTV-webos.ipk`.

## Handleiding per toestel

| Toestel | Systeem | Handleiding |
| --- | --- | --- |
| TCL 65C75B (2024) | Google TV, Android 12 | [docs/tvs/tcl-65c75b.md](docs/tvs/tcl-65c75b.md) |
| TCL 58P635 (2023) | Google TV, Android 11 | [docs/tvs/tcl-58p635.md](docs/tvs/tcl-58p635.md) |
| LG 43UP77006LB | webOS 6.0 | [docs/tvs/lg-43up77006lb.md](docs/tvs/lg-43up77006lb.md) |
| Philips 58PUS7304 | Android TV 9 | [docs/tvs/philips-58pus7304.md](docs/tvs/philips-58pus7304.md) |
| Google TV Streamer | Google TV, Android 14 | [docs/tvs/google-tv-streamer.md](docs/tvs/google-tv-streamer.md) |
| Fire TV 4K | Fire OS, of Vega OS | [docs/tvs/fire-tv-4k.md](docs/tvs/fire-tv-4k.md) |

Terug naar het fabrieksscherm, op Android en Google TV: de app openen → Home Screen → Original.

Op TCL na elke nieuwe installatie (ook na Updates, als je opnieuw via adb zet):

```bash
adb shell "appops set io.freemytv APP_AUTO_START allow"
```

## Herkomst

De launcher, het tekenen en de installatie per platform komen uit Free My TV van simbasjungles. Die code blijft MIT; zie [LICENSE](LICENSE). TV Thuis voegt de mediamanager, de aanvragen, AIO bovenaan het scherm, de schermbeveiligingsopties en de updates van deze fork toe.
