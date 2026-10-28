# TV Thuis

Startscherm voor de televisies in huis, gebaseerd op [Free My TV](https://github.com/simbasjungles/free-my-tv) (MIT). Zelfde app-plank, weer en nachtmodus. De posterrij komt uit de eigen [AIO Media Server Manager](https://github.com/Qballjos/aio-media-server-manager).

| Tab | Inhoud |
| --- | --- |
| Nieuw toegevoegd | Films en series die net in Jellyfin of Plex zijn gekomen |
| Films | Trending films van deze week |
| Series | Trending series van deze week |

OK op een titel die nog niet in de bibliotheek staat, vraagt hem aan via Seerr (de manager doet dat). OK op een titel die er al is, opent Jellyfin of Plex.

De schermbeveiliging toont de posters van de trendinglijst op een zwarte achtergrond, met klok en weer. Zwarte randen blijven uit, de klok is gedimd en schuift langzaam, zodat een OLED- of Mini-LED-paneel niet één helder vlak blijft branden.

## Instellingen op de tv

Home Settings → **AIO Media**:

| Veld | Voorbeeld |
| --- | --- |
| AIO-server | `https://aio.mediavangst.nl` of `http://192.168.1.20:8080` |
| Gebruikersnaam | de beheerder van de manager |
| Wachtwoord | hetzelfde wachtwoord |
| Zoeken en aanvragen | een titel die niet in de trendinglijst staat |
| Verbinding testen | moet trending- en recent-aantallen tonen |

De tv praat met `POST /api/auth/login` en daarna met `Authorization: Bearer` naar `/api/homepage`, `/api/homepage/search` en `/api/homepage/request`. Het wachtwoord blijft op die tv staan.

Stad en eenheid zet je in dezelfde Home Settings (weer en zonsondergang). Jellyfin of Plex moet op de tv geïnstalleerd zijn om een titel te openen.

`config.properties` kan het adres alvast meegeven bij het bouwen. Het wachtwoord hoort daar niet in: het komt dan in de app terecht. `config.properties` staat niet in git.

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

## Herkomst

De launcher, het tekenen en de installatie per platform komen uit Free My TV van simbasjungles, commit `fa47183`. Die code blijft MIT; zie [LICENSE](LICENSE). TV Thuis voegt de mediamanager, de aanvragen en de zwarte schermbeveiliging toe.
