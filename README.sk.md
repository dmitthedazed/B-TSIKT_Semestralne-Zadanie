<div align="center">

# 📚 TaskBoard

### Kolaboratívna platforma pre úlohy

*Moderný JavaFX desktopový klient + Spring Boot backend pre študijné skupiny*

[![CI](https://github.com/dmitthedazed/B-TSIKT_Semestralne-Zadanie/actions/workflows/ci.yml/badge.svg)](https://github.com/dmitthedazed/B-TSIKT_Semestralne-Zadanie/actions/workflows/ci.yml)

[![Java](https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.1-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![JavaFX](https://img.shields.io/badge/JavaFX-21-007396?style=for-the-badge&logo=java&logoColor=white)](https://openjfx.io/)
[![SQLite](https://img.shields.io/badge/SQLite-003B57?style=for-the-badge&logo=sqlite&logoColor=white)](https://www.sqlite.org/)
[![Google OAuth](https://img.shields.io/badge/Google%20OAuth-4285F4?style=for-the-badge&logo=google&logoColor=white)](https://developers.google.com/identity)
[![License](https://img.shields.io/badge/Licencia-MIT-blue?style=for-the-badge)](LICENSE)

🇬🇧 [English](README.md) | 🇸🇰 **Slovenčina**

[Funkcie](#-funkcie) •
[Snímky obrazovky](#-snímky-obrazovky) •
[Technológie](#️-technológie) •
[Inštalácia](#-inštalácia) •
[Použitie](#-použitie) •
[Architektúra](#️-architektúra) •
[API](#-api-referencia) •
[WebSocket](#-websocket-udalosti) •
[Štruktúra projektu](#-štruktúra-projektu) •
[Bezpečnosť](#-bezpečnosť) •
[Testovanie](#-testovanie) •
[Použitie AI](#-použitie-ai) •
[Autor](#-autor)

</div>

---

## ✨ Funkcie

<table>
<tr>
<td width="50%">

### 👥 Skupinová spolupráca
- Vytváranie a správa študijných skupín
- Prístup podľa rolí (Tvorca, Admin, Člen)
- Pozývanie členov e-mailom
- Aktivita členov v reálnom čase

</td>
<td width="50%">

### ✅ Správa úloh
- Vytváranie, priraďovanie a sledovanie úloh
- Sledovanie stavu (Otvorená → Prebieha → Hotovo)
- Správa termínov
- Osobný prehľad úloh

</td>
</tr>
<tr>
<td width="50%">

### 📁 Zdieľanie zdrojov
- Nahrávanie súborov a zdieľanie odkazov
- Organizácia podľa študijných skupín
- Sťahovanie a správa materiálov
- Podpora rôznych typov súborov

</td>
<td width="50%">

### 📊 Analytika a štatistiky
- Štatistiky aktivity skupiny
- Grafy dokončenia úloh
- 90-dňová história aktivít
- Vizuálne sledovanie pokroku

</td>
</tr>
<tr>
<td width="50%">

### 🔐 Bezpečnosť
- JWT autentifikácia
- **Google OAuth 2.0** prihlásenie
- Bezpečné hashovanie hesiel
- Oprávnenia podľa rolí
- Chránené API endpointy

</td>
</tr>
<tr>
<td width="50%">

### 🚀 CI/CD Pipeline
- Automatizované testovanie cez GitHub Actions
- Overenie buildu pri každom pushu
- Kontrola kvality kódu
- Kontinuálna integrácia

</td>
<td width="50%">

### ⚡ Aktualizácie v reálnom čase
- WebSocket notifikácie
- Živé aktualizácie úloh
- Upozornenia na aktivitu členov
- Okamžitá synchronizácia

</td>
</tr>
</table>

---

## 📸 Snímky obrazovky

<div align="center">

| Prihlásenie | Hlavný panel | Detail skupiny |
|:---:|:---:|:---:|
| ![Login](docs/screenshots/login.png) | ![Dashboard](docs/screenshots/dashboard.png) | ![Group](docs/screenshots/group.png) |

| Zobrazenie úloh | Štatistiky | Profil |
|:---:|:---:|:---:|
| ![Tasks](docs/screenshots/tasks.png) | ![Stats](docs/screenshots/stats.png) | ![Profile](docs/screenshots/profile.png) |

</div>

---

## 🛠️ Technológie

<div align="center">

| Vrstva | Technológia |
|:---:|:---|
| **Frontend** | JavaFX 21, FXML, CSS |
| **Backend** | Spring Boot 3.3.1, Spring Security |
| **Databáza** | SQLite (prod), H2 (demo) |
| **Autentifikácia** | JWT (JSON Web Tokens) |
| **Reálny čas** | WebSocket (STOMP) |
| **Build** | Maven (multi-modul) |

</div>

---

## 📦 Inštalácia

### Požiadavky

- **Java 21** alebo vyššia ([Stiahnuť](https://adoptium.net/))
- **Maven 3.8+** ([Stiahnuť](https://maven.apache.org/download.cgi))
- Git

### Rýchly štart

```bash
# Klonovanie repozitára
git clone https://github.com/dmitthedazed/B-TSIKT_Semestralne-Zadanie.git
cd B-TSIKT_Semestralne-Zadanie

# Zostavenie projektu
mvn clean install

# Spustenie backendu (Terminál 1)
mvn -pl backend spring-boot:run

# Spustenie frontendu (Terminál 2)
mvn -pl frontend javafx:run
```

> 📝 **Poznámka:** SQLite databázový súbor sa vytvorí automaticky pri prvom spustení.

---

## 🚀 Použitie

### 🎮 Demo režim

Vyskúšajte aplikáciu okamžite s predvyplnenými dátami:

```bash
# Spustenie demo backendu (H2 in-memory, port 8081)
cd backend
mvn spring-boot:run "-Dspring-boot.run.profiles=demo"

# Spustenie frontendu smerujúceho na demo
mvn -pl frontend javafx:run -DAPI_BASE_URL=http://localhost:8081/api
```

Alebo jednoducho kliknite na **"Vyskúšať demo"** na prihlasovacej obrazovke!

| Demo prihlasovacie údaje |  |
|:---|:---|
| 📧 Email | `demo@example.com` |
| 🔑 Heslo | `demo123` |

### Predvolené porty

| Služba | Port |
|:---:|:---:|
| Backend (Produkcia) | `8080` |
| Backend (Demo) | `8081` |

---

## 🏗️ Architektúra

```
┌─────────────────────────────────────────────────────────────┐
│                        KLIENT (JavaFX)                       │
├─────────────────────────────────────────────────────────────┤
│  Kontroléry (FXML)   │  ApiService  │  WebSocket Klient     │
│         ↓            │      ↓       │         ↓             │
│       Pohľady        │  HTTP/JWT    │    STOMP Správy       │
└──────────────────────┴──────────────┴───────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                      SERVER (Spring Boot)                    │
├─────────────────────────────────────────────────────────────┤
│  REST Kontroléry   │  WebSocket Handler  │  Security Filter │
│         ↓          │         ↓           │        ↓         │
│     Služby         │    Notifikačná      │   JWT Validátor  │
│         ↓          │      služba         │                  │
│   Repozitáre       │                     │                  │
│         ↓          │                     │                  │
│    JPA/Hibernate   │                     │                  │
│         ↓          │                     │                  │
│      SQLite        │                     │                  │
└─────────────────────────────────────────────────────────────┘
```

### 📊 Dátový model

```mermaid
erDiagram
    USERS ||--o{ MEMBERSHIPS : má
    USERS ||--o{ TASKS : vytvára
    USERS ||--o{ RESOURCES : nahráva
    USERS ||--o{ ACTIVITY_LOG : generuje
    
    GROUPS ||--o{ MEMBERSHIPS : obsahuje
    GROUPS ||--o{ TASKS : má
    GROUPS ||--o{ RESOURCES : ukladá
    GROUPS ||--o{ ACTIVITY_LOG : sleduje
    
    USERS {
        int user_id PK
        string nickname
        string email UK
        string password_hash
        datetime created_at
    }
    
    GROUPS {
        int group_id PK
        string name
        string description
        int created_by FK
        datetime created_at
    }
    
    MEMBERSHIPS {
        int id PK
        int user_id FK
        int group_id FK
        enum role "CREATOR|ADMIN|MEMBER"
        datetime joined_at
    }
    
    TASKS {
        int task_id PK
        int group_id FK
        int created_by FK
        int assigned_to FK
        string title
        string description
        enum status "OPEN|IN_PROGRESS|DONE"
        datetime deadline
        datetime created_at
    }
    
    RESOURCES {
        int resource_id PK
        int group_id FK
        int uploaded_by FK
        string title
        enum type "FILE|LINK"
        string path_or_url
        string file_extension
        string description
        datetime uploaded_at
    }
    
    ACTIVITY_LOG {
        int id PK
        int user_id FK
        int group_id FK
        int task_id FK
        string activity_type
        string description
        datetime timestamp
    }
```

---

## 📡 API Referencia

<details>
<summary><b>🔐 Autentifikácia</b></summary>

| Metóda | Endpoint | Popis |
|:---:|:---|:---|
| `POST` | `/api/auth/register` | Registrácia nového používateľa |
| `POST` | `/api/auth/login` | Prihlásenie a získanie JWT |

</details>

<details>
<summary><b>👤 Používatelia</b></summary>

| Metóda | Endpoint | Popis |
|:---:|:---|:---|
| `GET` | `/api/users/me` | Získať aktuálneho používateľa |
| `PUT` | `/api/users/me` | Aktualizovať profil |
| `POST` | `/api/users/me/change-password` | Zmena hesla |

</details>

<details>
<summary><b>👥 Skupiny</b></summary>

| Metóda | Endpoint | Popis |
|:---:|:---|:---|
| `GET` | `/api/groups` | Zoznam všetkých skupín |
| `GET` | `/api/groups/my` | Zoznam mojich skupín |
| `GET` | `/api/groups/available` | Zoznam dostupných skupín |
| `POST` | `/api/groups` | Vytvoriť skupinu |
| `GET` | `/api/groups/{id}` | Detail skupiny |

</details>

<details>
<summary><b>🤝 Členstvá</b></summary>

| Metóda | Endpoint | Popis |
|:---:|:---|:---|
| `POST` | `/api/memberships/join/{groupId}` | Pridať sa do skupiny |
| `POST` | `/api/memberships/leave/{groupId}` | Opustiť skupinu |
| `GET` | `/api/memberships/group/{groupId}/members` | Zoznam členov |
| `POST` | `/api/memberships/group/{groupId}/add-member` | Pridať člena |
| `POST` | `/api/memberships/group/{groupId}/add-member-by-email` | Pridať podľa emailu |
| `PUT` | `/api/memberships/group/{groupId}/update-role` | Zmeniť rolu |
| `DELETE` | `/api/memberships/group/{groupId}/remove-member` | Odstrániť člena |

</details>

<details>
<summary><b>✅ Úlohy</b></summary>

| Metóda | Endpoint | Popis |
|:---:|:---|:---|
| `POST` | `/api/tasks` | Vytvoriť úlohu |
| `GET` | `/api/tasks/group/{id}` | Zoznam úloh skupiny |
| `GET` | `/api/tasks/my` | Zoznam mojich úloh |
| `PUT` | `/api/tasks/{id}` | Aktualizovať úlohu |
| `DELETE` | `/api/tasks/{id}` | Odstrániť úlohu |

</details>

<details>
<summary><b>📁 Zdroje</b></summary>

| Metóda | Endpoint | Popis |
|:---:|:---|:---|
| `GET` | `/api/resources/group/{id}` | Zoznam zdrojov |
| `POST` | `/api/resources` | Vytvoriť zdroj (odkaz) |
| `POST` | `/api/resources/upload` | Nahrať súbor |
| `PUT` | `/api/resources/{id}` | Aktualizovať zdroj |
| `DELETE` | `/api/resources/{id}` | Odstrániť zdroj |
| `GET` | `/api/resources/{id}/download` | Stiahnuť súbor |

</details>

<details>
<summary><b>📊 Štatistiky a aktivita</b></summary>

| Metóda | Endpoint | Popis |
|:---:|:---|:---|
| `GET` | `/api/group-stats/{groupId}` | Štatistiky skupiny |
| `GET` | `/api/activity-logs/user/me` | Moja aktivita |
| `GET` | `/api/activity-logs/group/{groupId}` | Aktivita skupiny |

</details>

---

## 🔌 WebSocket udalosti

Notifikácie v reálnom čase cez STOMP nad WebSocket:

| Udalosť | Popis |
|:---|:---|
| `task.created` | Nová úloha pridaná |
| `task.updated` | Úloha upravená |
| `task.deleted` | Úloha odstránená |
| `task.status` | Stav zmenený |
| `resource.added` | Nový materiál nahraný |
| `member.joined` | Nový člen sa pridal |
| `member.role` | Rola zmenená |

---

## 📁 Štruktúra projektu

```
B-TSIKT_Semestralne-Zadanie
├──  backend/                    # Spring Boot backend
│   ├──  src/main/java/com/combined/backend/
│   │   ├──  config/             # Konfigurácia Security, WebSocket
│   │   ├──  controller/         # REST endpointy
│   │   ├──  dto/                # Data transfer objekty
│   │   ├──  model/              # JPA entity
│   │   ├──  repository/         # Dátová vrstva
│   │   ├──  service/            # Biznis logika
│   │   └──  BackendApplication.java
│   └──  src/main/resources/
│       ├──  application.properties
│       └──  application-demo.properties
├──  frontend/                   # JavaFX klient
│   ├──  src/main/java/          # Kontroléry a služby
│   └──  src/main/resources/
│       ├──  fxml/               # UI rozloženia
│       ├──  css/                # Štýly
│       └──  img/                # Obrázky
├──  pom.xml                     # Rodičovský POM
└──  README.md
```

---

## 🔒 Bezpečnosť

| Funkcia | Implementácia |
|:---|:---|
| **Autentifikácia** | JWT tokeny s konfigurovateľnou expiráciou |
| **Ukladanie hesiel** | BCrypt hashovanie |
| **Autorizácia** | Podľa rolí (CREATOR > ADMIN > MEMBER) |
| **Ochrana API** | Všetky endpointy vyžadujú platný JWT |
| **CSRF** | Vypnuté (stateless API) |
| **Identita používateľa** | `@AuthenticationPrincipal` zabraňuje podvrhnutiu |

### Pravidlá prístupu

- ✅ **Tvorca**: Plná kontrola, nemôže byť odstránený/degradovaný
- ✅ **Admin**: Správa členov (okrem tvorcu), správa zdrojov/úloh
- ✅ **Člen**: Zobrazenie zdrojov, správa vlastných úloh

---

## 🧪 Testovanie

```bash
# Spustenie všetkých testov
mvn test

# Spustenie iba backend testov
mvn -pl backend test

# Spustenie s pokrytím kódu
mvn -pl backend test jacoco:report
```

---

## 🤖 Použitie AI

Tento projekt bol vyvinutý s pomocou AI nástrojov, konkrétne **GitHub Copilot** a **Claude** (Anthropic). AI bola použitá na:

| Oblasť | Ako AI pomohla |
|:---|:---|
| **Generovanie kódu** | Zrýchlenie písania boilerplate kódu, DTO a opakujúcich sa vzorov |
| **Ladenie** | Identifikácia problémov a návrhy opráv pre komplexné chyby |
| **Revízia kódu** | Návrhy na zlepšenie kvality kódu a osvedčených postupov |
| **Testovanie** | Pomoc pri písaní unit a integračných testov |
| **Architektúra** | Diskusia o návrhových vzoroch a architektonických rozhodnutiach |

> ⚠️ **Dôležité:** Všetok kód generovaný AI bol starostlivo skontrolovaný, otestovaný a upravený. AI slúžila ako nástroj produktivity, nie ako náhrada za porozumenie a kritické myslenie.

---

## 👨‍💻 Autor

<div align="center">

**Dmitrii Savin**

[![GitHub](https://img.shields.io/badge/GitHub-dmitthedazed-181717?style=for-the-badge&logo=github)](https://github.com/dmitthedazed)

*Semestrálny projekt — B-TSIKT*

</div>

---

## 📄 Licencia

Tento projekt je licencovaný pod MIT licenciou - pozrite súbor [LICENSE](LICENSE) pre podrobnosti.

---

<div align="center">

**⭐ Dajte hviezdu tomuto repozitáru, ak vám pomohol!**

Vytvorené s ❤️ a ☕

</div>
