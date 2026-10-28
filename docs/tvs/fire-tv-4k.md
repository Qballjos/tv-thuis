# Fire TV 4K

Er is geen toestel dat officieel *Fire TV 4K Pro* heet. Dit geldt voor een Fire TV Stick 4K, 4K Max of 4K Plus op **Fire OS**. Die kunnen de apk installeren. TV Thuis opent als app. Amazon houdt de Home-knop zelf: die blijft het Fire TV-startscherm openen. Je start TV Thuis vanuit Apps.

## Eerst kijken welk systeem het is

Instellingen → My Fire TV → Over.

- Staat er **Fire OS**, dan kan de apk erop.
- Staat er **Vega OS** (de nieuwe Fire TV Stick 4K en de 4K Select), dan draait dit project daar niet. Vega installeert geen Android-apk. Gebruik voor dat scherm de [Google TV Streamer](google-tv-streamer.md).

## Op Fire OS

1. Instellingen → My Fire TV → Over.
2. Druk zeven keer op de naam van de stick tot Ontwikkelaarsopties verschijnt.
3. Ontwikkelaarsopties → **ADB-foutopsporing** aan.
4. **Apps van onbekende bronnen** aan.
5. Noteer het IP-adres onder Netwerk.

## Op de computer

```bash
adb connect <ip-van-de-stick>:5555
adb install -r FreeMyTV.apk
```

De stick vraagt om de verbinding toe te staan. Vink het vakje aan.

## Op de stick, daarna

1. Apps → **TV Thuis**.
2. Home Settings → **AIO Media**: server, gebruikersnaam, wachtwoord, Verbinding testen.
3. Home Settings → **Stad**.
4. Installeer Jellyfin of Plex uit de Amazon Appstore als je een titel wilt openen. Jellyfin staat daar niet altijd in; een apk van Jellyfin voor Fire TV installeer je met dezelfde `adb install`.

De schermbeveiliging van TV Thuis geldt zolang de app open is. De Home-knop gaat terug naar Fire TV. Original in het menu van de app zet niets van Fire OS uit, omdat de Home-knop nooit is overgenomen.

CleanYourTV heeft wel Fire TV-lijsten. Die worden hier niet toegepast. De omschakeling naar TV Thuis als startscherm weigert op Fire OS, en geen enkel `com.amazon.*`-pakket gaat uit.
