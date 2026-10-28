# Google TV Streamer

De Google TV Streamer (2024) draait Android 14. Zelfde apk als de TCL’s. Dit is het toestel dat je op de HDMI van een tv prikt die zelf geen Android of webOS heeft, of naast een Fire TV-stick die de app niet kan draaien.

## Op de streamer

1. Instellingen → Systeem → Over.
2. Druk zeven keer op **Android TV OS-build**.
3. Instellingen → Systeem → Ontwikkelaarsopties.
4. Zet **USB-foutopsporing** aan.
5. Zet **Draadloos foutopsporing** aan. Android 14 houdt poort 5555 dicht tot je koppelt.

Noteer het IP-adres onder Netwerk en internet. Op het scherm Draadloos foutopsporing staan een koppeladres met poort en een code van zes cijfers.

## Op de computer

```bash
adb pair <ip>:<koppelpoort>
```

Typ de code van het scherm. Daarna, in dezelfde sessie:

```bash
adb connect <ip>:<verbindingspoort>
adb tcpip 5555
adb connect <ip>:5555
adb install -r FreeMyTV.apk
```

De verbindingspoort staat onder het koppeladres. Na een herstart van de streamer gaat poort 5555 weer dicht. Koppelen blijft bewaard. Voor een volgende installatie: draadloos foutopsporing openen en `adb connect` naar de nieuwe verbindingspoort, daarna opnieuw `adb tcpip 5555` als je de vaste poort wilt.

Play Protect op Android 14 kan *Risky app blocked* tonen. Meer details → Toch installeren.

## Op de streamer, daarna

1. Open **TV Thuis**.
2. Home Screen → **TV Thuis**. Beantwoord *USB-foutopsporing toestaan?* met Altijd toestaan.
3. Home Settings → **AIO Media**. De streamer moet het adres van de mediamanager kunnen bereiken (https, of http op het thuisnetwerk).
4. Home Settings → **Stad**.
5. Jellyfin of Plex uit de Play Store.

De Home-knop van de streamer opent daarna TV Thuis op de tv waar hij op HDMI zit. De schermbeveiliging is de trendingposter op zwart met klok en weer, ook als het paneel erachter OLED is.
