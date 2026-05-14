# DiplomovaPraca_SamuelBolibruch

Mobilná aplikácia (Android, Kotlin) systému behaviorálnej biometrickej
autentifikácie pre smartfónovú platformu. Projekt je súčasťou diplomovej
práce na FEI STU v Bratislave (2026).

Aplikácia zabezpečuje zber behaviorálnych údajov používateľa – dynamiky
písania na dotykovom displeji a údajov z pohybových senzorov
(akcelerometer, gyroskop) – ako druhý faktor overenia identity
po štandardnej autentifikácii e-mailom a heslom.

**Súvisiace repozitáre:**

- [Serverová časť (Python, FastAPI)](https://github.com/SamuelBolibruch/DiplomovaPraca_Backend)


## Inštalácia a spustenie

> Pred spustením mobilnej aplikácie sa uistite, že máte spustený a dostupný backend server (viď inštalačná príručka serverovej časti).

### Požiadavky

- **Android Studio** (Ladybug 2024.2.1 alebo novšia) – [stiahnuť tu](https://developer.android.com/studio)

---

### 1. Klonovanie repozitára

```bash
git clone git@github.com:SamuelBolibruch/DiplomovaPraca_SamuelBolibruch.git
cd DiplomovaPraca_SamuelBolibruch
```

### 2. Otvorenie projektu v Android Studio

1. Spusti **Android Studio**.
2. Zvol **File → Open** a vyber priečinok projektu.
3. Počkaj na dokončenie **Gradle sync** – závislosti sa stiahnu automaticky.

### 3. Firebase – `google-services.json`

Bez tohto súboru **projekt neskompiluje**.

> Mobilná aplikácia musí byť pripojená k **rovnakému Firebase projektu** ako serverová časť – inak autentifikácia nebude fungovať.

> Prístup k pôvodnému Firebase projektu je možné získať na požiadanie od autora práce.

**Možnosť A – prístup k pôvodnému Firebase projektu**

1. V [Firebase Console](https://console.firebase.google.com) otvor pôvodný projekt.
2. Prejdi do **Project settings → General**.
3. Stiahni `google-services.json` a umiestni ho do priečinka `app/`.

**Možnosť B – vlastný Firebase projekt**

> ⚠️ V tomto prípade budú môcť aplikáciu používať iba **novo zaregistrovaní používatelia**. Používatelia z pôvodného projektu sa nebudú vedieť prihlásiť. Ich surové dáta však zostávajú dostupné na backende, a teda pri pretrénovaní modelov sa pracuje aj s nimi.

1. V [Firebase Console](https://console.firebase.google.com) vytvor nový projekt.
2. Aktivuj služby **Authentication**, **Firestore** a **Storage**.
3. Prejdi do **Project settings → General**.
4. Stiahni `google-services.json` a umiestni ho do priečinka `app/`.

> **Nepridávaj tento súbor do gitu.**

### 4. Nastavenie adresy backend servera

Serverová časť tejto aplikácie sa nachádza na: https://github.com/SamuelBolibruch/DiplomovaPraca_Backend

Aplikácia komunikuje s autentifikačným serverom (napr. cez [ngrok](https://ngrok.com)). Po spustení backendu a jeho sprístupnení cez ngrok (alebo iný tunel/server) je potrebné aktualizovať URL na jednom mieste v kóde:

**`app/src/main/java/eu/mcomputing/mobv/diplomovapraca/AppContainer.kt`**

```kotlin
.baseUrl("https://xxxx.ngrok-free.app/")
```

> Zmeň URL na aktuálnu adresu tvojho bežiaceho backendu. Toto je jediné miesto, ktoré treba upraviť – všetky HTTP volania v aplikácii idú cez tento Retrofit klient.

### 5. Spustenie

1. Vyber zariadenie alebo emulátor (min. Android 9.0 – API 28).
2. Klikni **Run ▶** (`Shift + F10`).