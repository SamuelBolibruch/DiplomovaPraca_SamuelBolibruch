## Inštalácia a spustenie

### Požiadavky

- **Android Studio** (Ladybug 2024.2.1 alebo novšia) – [stiahnuť tu](https://developer.android.com/studio)


---

### 1. Klonovanie repozitára

```bash
git clone git@github.com:SamuelBolibruch/DiplomovaPraca_SamuelBolibruch.git
cd DiplomovaPraca
```

### 2. Otvorenie projektu v Android Studio

1. Spusti **Android Studio**.
2. Zvol **File → Open** a vyber priečinok projektu.
3. Počkaj na dokončenie **Gradle sync** – závislosti sa stiahnu automaticky.

### 3. Firebase – `google-services.json`

Bez tohto súboru **projekt neskompiluje**.

> Mobilná aplikácia musí byť pripojená k **rovnakému Firebase projektu** ako serverová časť – inak autentifikácia nebude fungovať.

> ⚠️ Pri použití vlastného Firebase projektu budú môcť aplikáciu používať iba **novo zaregistrovaní používatelia**. Používatelia z pôvodného projektu sa nebudú vedieť prihlásiť – ich účty v novom Firebase neexistujú, aj keď ich autentifikačné modely na BE ostávajú zachované.

1. V [Firebase Console](https://console.firebase.google.com) otvor projekt → **Project settings → General**.
2. Klikni na **Download google-services.json**.
3. Súbor umiestni do priečinka `app/`.

> **Nepridávaj tento súbor do gitu.**

### 4. Spustenie

1. Vyber zariadenie alebo emulátor (min. Android 9.0 – API 28).
2. Klikni **Run ▶** (`Shift + F10`).

### 5. Backend

Serverová časť tejto aplikácie sa nachádza na: https://github.com/SamuelBolibruch/DiplomovaPraca_Backend

Aplikácia komunikuje s autentifikačným serverom. URL servera nastav v kóde – napr. ngrok URL vo formáte `https://xxxx.ngrok-free.app`.
