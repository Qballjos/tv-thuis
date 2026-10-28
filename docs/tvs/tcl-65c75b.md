# TCL 65C75B (2024)

Google TV op Android 12. QLED, 4K. De app wordt het startscherm.

## Op de tv

1. Instellingen → Systeem → Over.
2. Druk zeven keer op **Android TV OS-build** tot ontwikkelaaropties aan staan.
3. Instellingen → Systeem → Ontwikkelaarsopties → **USB-foutopsporing** aan.
4. Noteer het IP-adres: Instellingen → Netwerk en internet.

Op Android 12 blijft poort 5555 meestal open zolang USB-foutopsporing aan staat.

## Op de computer

De computer en de tv zitten op hetzelfde wifi-netwerk. Bouw eerst zoals in de [README](../../README.md), daarna:

```bash
adb connect <ip-van-de-tv>:5555
adb install -r FreeMyTV.apk
adb shell "appops set io.freemytv AUTO_START allow || appops set io.freemytv APP_AUTO_START allow"
```

TCL blokkeert anders de achtergrondstart, en de omschakeling naar TV Thuis komt dan niet op gang. Staat Play Protect in de weg met *Risky app blocked*, kies Meer details → Toch installeren.

## Op de tv, daarna

1. Open **TV Thuis**.
2. Home Screen → **TV Thuis**. De tv vraagt *USB-foutopsporing toestaan?* nog een keer, nu voor de app zelf. Vink Altijd toestaan aan.
3. Home Settings → **AIO Media**: server, gebruikersnaam, wachtwoord, Verbinding testen.
4. Home Settings → **Stad**: jullie plaats, voor weer en de nachtstand.
5. Installeer Jellyfin of Plex uit de Play Store als die er nog niet op staat.

De Home-knop opent daarna TV Thuis. Original in hetzelfde menu zet het Google TV-startscherm terug.

De 65C75B staat niet onder die winkelnaam in de opschoonlijsten. Hij valt in de G08-familie (de C755 en de Europese Smart TV Pro op Android 12). Bij de omschakeling gaan TCL Channel, de winkeldemo, logkit, de TCL-analytics, Samba TV, T-Cast en de Google-reclamediensten uit. Het zijpaneel met meldingen gaat mee uit. Samba TV en T-Cast zitten in `com.tcl.ttvs`. Op deze generatie zit de TCL-firmware-update daar ook in, dus die updater gaat mee uit. TGuard, AirPlay, Chromecast en de spelmodus blijven aan.

Netflix-, YouTube- en Prime-knoppen op de afstandsbediening stel je in via Remote Buttons.
