# Philips 58PUS7304

Android TV 9 (Pie), 2019, 4K, Ambilight. Android 9 is nieuw genoeg voor deze app (vanaf Android 8). Het geheugen is krap (16 GB flash). Ruim oude apps op als de installatie over opslag klaagt.

Dit is het klassieke Android TV-startscherm, geen Google TV. De stappen wijken daardoor af van de TCL’s.

## Op de tv

1. Home → tandwiel rechtsboven, of de Settings-knop op de afstandsbediening.
2. Apparaatvoorkeuren → Over.
3. Druk zeven keer op **Build** tot de melding komt dat je ontwikkelaar bent.
4. Apparaatvoorkeuren → **Ontwikkelaarsopties**.
5. Zet **USB-foutopsporing** aan.
6. Zet netwerk-debugging aan als die optie er is (ADB over het netwerk, poort 5555).
7. Noteer het IP-adres onder Netwerk.

Staat er geen netwerk-ADB, dan werkt een USB-kabel van de computer naar de USB-poort van de tv ook: `adb devices` moet de tv tonen. Daarna kun je `adb tcpip 5555` proberen en verder over wifi.

## Op de computer

```bash
adb connect <ip-van-de-tv>:5555
adb install -r FreeMyTV.apk
```

Op Android 9 kan de toestemmingsvraag op het scherm blijven hangen zolang de computer nog verbonden is. Verbreek dan eerst:

```bash
adb disconnect <ip-van-de-tv>:5555
```

Beantwoord de vraag op de tv, vink Altijd toestaan aan, en verbind daarna opnieuw als je nog iets wilt installeren.

## Op de tv, daarna

1. Open **TV Thuis** uit de app-rij.
2. Home Screen → **TV Thuis**.
3. Home Settings → **AIO Media** en **Stad**.
4. Installeer Jellyfin of Plex uit de Play Store.

Ambilight blijft van Philips. TV Thuis vervangt alleen het startscherm en, tenzij je de schermbeveiliging uitzet, de screensaver. Original zet het Philips-startscherm terug.

De set is uit 2019. Houd de apk klein en zet niet te veel zware apps ernaast. De posterrij haalt zijn plaatjes van de mediamanager; een TMDB-sleutel is niet nodig zolang AIO Media is ingevuld.
