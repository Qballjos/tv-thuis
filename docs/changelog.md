# Wat er in TV Thuis is gedaan

Overzicht van wat deze fork ([Qballjos/tv-thuis](https://github.com/Qballjos/tv-thuis)) bovenop Free My TV toevoegt of veranderde. De Android-tv’s installeren een nieuwe versie via **Startscherm → Updates**. De LG krijgt die APK niet; daar blijft een aparte webOS-installatie nodig.

## Nog niet uitgebracht

- De AIO-startschermweergave heeft aparte tabbladen **Films** en **Series** voor de eigen bibliotheek, met doorladen
  tijdens het bladeren. **Trending** bundelt de aanbevelingen die je kunt aanvragen. Verder kijken bevat alleen
  begonnen titels; afzonderlijke films uit dezelfde franchise blijven in de bibliotheek zichtbaar.
- De onbedoelde wijziging aan de schermbeveiliging is teruggedraaid; ‘Voor jou’ blijft vooralsnog de streamingtips tonen.

- **Koppelen met AIO** via QR en een eenmalige koppellink uit het AIO-dashboard. De tv neemt het serveradres over
  en bewaart een eigen intrekbaar token; AIO-wachtwoorden en Jellyfin-sleutels hoeven niet meer op de tv.
- Verder kijken kan via de gekoppelde AIO-server; handmatig instellen blijft beschikbaar voor oudere AIO-versies.

- **Fotocollage** als extra schermbeveiliging op Android / Google TV: maximaal drie eigen foto's met witte lijsten,
  opnamedatums, zachte overgangen en klok en weer rechtsonder.
- Gebruikt de bestaande Immich-bron (alle foto's of één album), ook naast AIO. Nieuwe imports bewaren de volledige
  fotoverhouding en opnamedatum, zonder GPS-tags. De keuze voor de indeling is per profiel.
- Albumwissels gebruiken aparte fotocaches en verversingstijden. Foto's uit een vorige selectie blijven niet meer
  zichtbaar, ook niet als het nieuwe album leeg is of een eerdere download nog loopt.
- Een gekozen fotoalbum krijgt ook bij schermvullende weergave voorrang op AIO-afbeeldingen; een leeg of nog ladend
  album valt terug op de klok.

## 1.3.5

- **Profielen** op Android / Google TV: aanmaken, hernoemen, wisselen en verwijderen, met 25 ingebouwde afbeeldingen (inclusief een regenboog).
- Per profiel een eigen app-plank, app-zichtbaarheid en startscherminstellingen (inclusief Tips / Verder kijken / Klok / AIO).
- Verborgen apps blijven ook uit Verder kijken. Bestaande instellingen en de actieve profielkeuze blijven bij updates behouden.
- AIO-/Jellyfin-aanmeldgegevens en apparaatinstellingen blijven gedeeld; ze blijven ook staan als je een profiel verwijdert.
- Dit zijn startschermprofielen, geen ouderlijk toezicht: streaming-accounts en kijkgeschiedenis blijven gedeeld.

## 1.3.4

- **Verder kijken** en **Pas toegevoegd** onder AIO tonen weer plaatjes.
- Die plaatjes gaan via de AIO-manager over https. Rechtstreeks http naar Jellyfin werkt op de release-APK vaak niet, waardoor de kaarten leeg bleven.
- Staande en liggende kaarten vragen elk hun eigen formaat (via de manager of TMDB).

## 1.3.3

- Onder **Startscherm → Bovenaan het scherm** staan vier keuzes: **Tips**, **Verder kijken**, **Klok** en **AIO**.
- **Tips** blijft de filmtips van TMDB (Voor jou / Explore, Films, Series).
- **AIO** zet de mediaserver bovenaan, met de tabbladen Verder kijken, Pas toegevoegd, Films en Series.
- Tips en AIO blijven los van elkaar; de lijsten worden allebei opgehaald als beide zijn ingesteld.

## 1.3.2

- Schermbeveiliging: **links** en **rechts** bladeren door de foto’s zonder de beveiliging te sluiten.
- Nieuwe rij **Aanvraagknop**: kies zelf welke knop een titel aanvraagt of opent. Standaard is dat OK.
- Bij het kiezen van een aanvraagknop gaat “kijken vanaf de schermbeveiliging” mee aan.
- Posterformaat voor smalle (staande) en brede (liggende) kaarten zit in de app, zodat elke tv het met de update meekrijgt.

## 1.3.1

- Schermbeveiliging in Startscherm: **aan/uit**, **Start na** (5 min tot 2 uur) en **Bekijken**.
- Die timer schrijft de tv-instellingen via de eigen ADB van de tv (ModeJob), omdat de app dat zelf niet mag.
- **Rakuten TV** (`tv.wuaki.apptv`) en **TCL Channel** (`com.tcl.waterfall.overseas`) gaan uit bij Free My TV-modus en blijven uit na een update. Original zet ze weer aan.
- `com.tcl.channelplus` (de bron voor Live TV) blijft aan: uit betekent een zwart antennebeeld.

## Eerder in deze fork (tot 1.3.0)

- Posterrij en AIO-tabbladen via [AIO Media Server Manager](https://github.com/Qballjos/aio-media-server-manager): inloggen, verder kijken (Jellyfin), pas toegevoegd, trending, zoeken en aanvragen via Seerr.
- Jellyfin **Verder kijken** / next-up op het startscherm (met Jellyfin-server en API-sleutel in Startscherm).
- Updates van deze fork: wekelijkse check en **Startscherm → Updates**, alleen APK’s van `Qballjos/tv-thuis`, ondertekend met dezelfde sleutel als op de tv’s.
- Ingebouwde AIO-/Jellyfin-/stad-waarden uit `config.properties` blijven in de voorkeuren staan na een update (`keepBuiltIn`), zolang de app niet wordt verwijderd.
- Zwarte schermbeveiliging met posters, klok en weer (OLED-vriendelijk).
- Plank: Wholphin, SmartTube, Prime Video, UniFi Protect, Homey; YouTube-knop naar SmartTube.

## Toestellen in huis

Geen IP-adressen of servernamen in deze repo. De handleidingen per model staan onder [README](../README.md#handleiding-per-toestel).

| Toestel | Opmerkingen |
| --- | --- |
| TCL 58P635 (2023) | Android 11, G07. `APP_AUTO_START` na elke nieuwe installatie. TGuard kan de omschakeling even onderbreken. |
| TCL 65C75B (2024) | Android 12, G08. Gallery e.d. uit in Free My TV-modus. |
| Google TV Streamer | Android 14. Poort 5555 valt na herstart weg tot draadloze foutopsporing + `adb tcpip 5555`. |
| LG 43UP77006LB | webOS 6. Alleen de app; LG-home blijft. Nummertoets vasthouden om te pinnen. Geen APK-update. |

## Niet gedaan / bewust niet

- LG webOS vervangen of rooten: niet ondersteund; gebruik desnoods een Google TV-streamer op HDMI.
- Systeemapps verwijderen: alleen uitzetten via Free My TV → Original.
- Secrets (TMDB, AIO-wachtwoord, Jellyfin-sleutel, keystore) horen niet in git.
