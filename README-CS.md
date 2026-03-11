<p align="center">
  <svg width="64" height="64" viewBox="0 0 64 64" fill="none" xmlns="http://www.w3.org/2000/svg">
    <rect width="64" height="64" rx="16" fill="#111113"/>
    <rect x="12" y="16" width="40" height="26" rx="3" stroke="#F4F4F5" stroke-width="4"/>
    <path d="M32 42V50M24 50H40" stroke="#F4F4F5" stroke-width="4" stroke-linecap="round"/>
    <path d="M18 31H24L28 23L34 37L38 31H46" stroke="#F4F4F5" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"/>
  </svg>

  <h1 align="center">Monitorovací systém — Backend</h1>
  <p align="center">
    Spring Boot REST API pro sběr systémových metrik, agregaci historických dat a odesílání upozornění.
    <br />
    Součást projektu <strong>Aplikace pro sběr, monitoring a vizualizaci dat počítačů a serverů</strong>.
  </p>
</p>

<p align="center">
  <a href="README.md">🇪🇳 English Version</a>
</p>

---

## O projektu

**Backend monitorovacího systému** zajišťuje serverovou část platformy pro sledování zařízení. Přijímá metriky z distribuovaných linuxových agentů, ukládá je do PostgreSQL, agreguje do hodinových a denních souhrnů a spouští upozornění při překročení definovaných prahových hodnot.

### Co systém dělá

- **Příjem metrik** — REST endpointy pro příjem systémových metrik (CPU, RAM, disk, síť atd.) od agentů autentizovaných pomocí API klíčů.
- **Agregace dat** — Úlohy na pozadí (přes JobRunr) automaticky sumarizují surové metriky do hodinových a denních přehledů pro efektivní dotazování nad historickými daty.
- **Upozornění (Alerting)** — Sleduje příchozí metriky oproti uživatelem definovaným prahovým hodnotám. Notifikace odesílá e-mailem (SMTP) a v reálném čase přes WebSocket.
- **Správa uživatelů a zařízení** — Řízení přístupu dle rolí (Admin / Uživatel) s oprávněními na úrovni jednotlivých zařízení. Uživatelé vidí pouze zařízení, ke kterým mají přístup.
- **Životní cyklus zařízení** — Registrace, sledování stavu online/offline, detekce SSH, profilové obrázky a správa API klíčů.

---

## Technologie

| Vrstva | Technologie |
|---|---|
| Runtime | Java 21 / Spring Boot 3.5 |
| Databáze | PostgreSQL |
| Zabezpečení | Spring Security + JWT (bezstavová autentizace) |
| Přístup k datům | Spring Data JPA + Hibernate |
| Cache | Caffeine (in-memory) |
| Úlohy na pozadí | JobRunr |
| Reálný čas | WebSocket (STOMP) |
| E-mail | Spring Mail (SMTP) |
| Dokumentace API | SpringDoc OpenAPI 3 / Swagger UI |
| Validace | Jakarta Validation + vlastní validátory |

---

## Související části projektu

### Frontend
Aplikace v **React 19** (Vite + TypeScript + Tailwind CSS 4) pro vizualizaci metrik, správu zařízení a konfiguraci upozornění.
- Interaktivní grafy přes **Recharts**
- Mapy polohy zařízení přes **Leaflet**
- Správa stavu na klientu pomocí **Zustand**
- Aktualizace v reálném čase přes **STOMP** WebSocket

### Klientský agent
Lehký daemon pro Linux v **.NET 9**, který sbírá systémové metriky a odesílá je na tento backend. Podporuje automatickou detekci hardwaru, nastavitelný interval sběru a self-signed certifikáty.

---

## Rychlý start

### Předpoklady

- **Java 21** (JDK)
- **Docker** a **Docker Compose** (pro PostgreSQL)
- **Maven** (nebo přiložený `mvnw` wrapper)

### Konfigurace

Vytvořte soubor `.env` v kořenovém adresáři projektu:

```env
# Databáze
DB_USERNAME=postgres
DB_PASSWORD=vase_heslo_k_databazi

# Zabezpečení
JWT_SECRET=vas_silny_jwt_secret
APP_ADMIN_EMAIL=admin@example.com
APP_ADMIN_PASSWORD=heslo_admina

# E-mailové notifikace (Gmail SMTP)
MAIL_USERNAME=vas_email@gmail.com
MAIL_PASSWORD=vase_gmail_app_heslo
```

> Při prvním spuštění se automaticky vytvoří administrátorský účet z hodnot `APP_ADMIN_EMAIL` a `APP_ADMIN_PASSWORD`.

### Spuštění lokálně

1. **Spusťte databázi:**
   ```bash
   docker-compose up -d
   ```
2. **Spusťte aplikaci:**
   ```bash
   ./mvnw spring-boot:run
   ```

Server se spustí na `http://localhost:8080`.

**Užitečné odkazy za běhu:**
- **Swagger UI:** `http://localhost:8080/swagger-ui/index.html`
- **JobRunr Dashboard:** `http://localhost:8080/jobrunr/dashboard`

---

## Nastavení Gmail SMTP

Pro funkční e-mailová upozornění potřebujete **App Password** (heslo aplikace) od Googlu. Běžné heslo k účtu nebude fungovat, pokud máte zapnuté dvoufázové ověření.

1. Přejděte na [Zabezpečení účtu Google](https://myaccount.google.com/security).
2. Ujistěte se, že je zapnuté **Ověření ve dvou krocích** (2-Step Verification).
3. Vyhledejte **„Hesla aplikací"** (App passwords) ve vyhledávacím poli Google účtu, nebo přejděte přímo na [Hesla aplikací](https://myaccount.google.com/apppasswords).
4. Klikněte na **Vytvořit nové heslo aplikace** a pojmenujte ho, např. `Monitoring App`.
5. Google vygeneruje **16znakový kód** — zkopírujte ho.
6. Vložte ho jako `MAIL_PASSWORD` do souboru `.env`:
   ```env
   MAIL_USERNAME=vas_email@gmail.com
   MAIL_PASSWORD=abcd efgh ijkl mnop
   ```

> **Poznámka:** Heslo aplikace vypadá jako `abcd efgh ijkl mnop` (s mezerami). 

---

## Nasazení

### Doporučená produkční konfigurace

Pro produkční nasazení je doporučeno použít **Docker Compose** ke spuštění všech součástí jako jednoho celku:

| Kontejner | Role |
|---|---|
| **PostgreSQL** | Databáze |
| **Spring Boot** | Backend API |
| **Nginx** | Reverzní proxy — servíruje sestavený React frontend, směruje API požadavky na backend a zajišťuje SSL/TLS terminaci |

Tato konfigurace umožňuje:
- **Jeden přístupový bod** — Nginx směruje `/api/*` na backend a ostatní požadavky obsluhuje frontendovým buildem.
- **SSL/TLS podpora** — Terminace HTTPS na úrovni Nginx (podpora certifikátů od CA i self-signed certifikátů pro privátní sítě).
- **WebSocket proxy** — Nginx přeposílá STOMP spojení na backend.

### Samostatné spuštění (vývoj)

Pro lokální vývoj spusťte backend přes `./mvnw spring-boot:run`, frontend přes `npm run dev` a PostgreSQL přes `docker-compose up -d` (viz sekce Rychlý start).

---

## Snímky obrazovky

<!-- TODO: Přidejte snímky obrazovky zobrazující následující:
  1. Frontend dashboard s grafy metrik v reálném čase
  2. Stránka se seznamem a správou zařízení
  3. Konfigurace pravidel prahových hodnot
  4. Swagger UI zobrazující dokumentaci API
-->

---

## API Endpointy

### Autentizace (`/api/auth`)

| Metoda | Endpoint | Popis |
|---|---|---|
| POST | `/api/auth/register` | Registrace nového uživatele |
| POST | `/api/auth/login` | Přihlášení a získání JWT tokenů |
| POST | `/api/auth/logout` | Odhlášení |
| POST | `/api/auth/refresh` | Obnovení vypršeného JWT tokenu |
| GET | `/api/auth/me` | Info o přihlášeném uživateli |
| POST | `/api/auth/change-password` | Změna hesla |

### Zařízení (`/api/devices`)

| Metoda | Endpoint | Popis |
|---|---|---|
| POST | `/api/devices` | Registrace nového zařízení |
| GET | `/api/devices` | Seznam zařízení (stránkovaně, s vyhledáváním) |
| GET | `/api/devices/{id}` | Detail zařízení |
| PUT | `/api/devices/{id}` | Aktualizace zařízení |
| DELETE | `/api/devices/{id}` | Smazání zařízení |
| POST | `/api/devices/{id}/image` | Nahrání obrázku |
| DELETE | `/api/devices/{id}/image` | Reset obrázku na výchozí |
| POST | `/api/devices/{id}/regenerate-api-key` | Regenerace API klíče |

### Metriky (`/api/devices/{deviceId}/metrics`)

| Metoda | Endpoint | Popis |
|---|---|---|
| POST | `/api/devices/{deviceId}/metrics` | Odeslání metrik (autentizace API klíčem) |
| GET | `/api/devices/{deviceId}/metrics/latest` | Poslední metriky |
| GET | `/api/devices/{deviceId}/metrics/history` | Historická data (dle typu a období) |

### Upozornění (`/api/alerts`)

| Metoda | Endpoint | Popis |
|---|---|---|
| GET | `/api/alerts` | Seznam upozornění (filtrovatelné) |
| POST | `/api/alerts/{id}/resolve` | Vyřešení upozornění |
| DELETE | `/api/alerts/{id}` | Smazání upozornění |

### Prahové hodnoty (`/api/devices/{deviceId}/thresholds`)

| Metoda | Endpoint | Popis |
|---|---|---|
| GET | `/api/devices/{deviceId}/thresholds` | Seznam pravidel |
| POST | `/api/devices/{deviceId}/thresholds` | Vytvoření pravidla |
| PUT | `/api/devices/{deviceId}/thresholds/{id}` | Aktualizace pravidla |
| DELETE | `/api/devices/{deviceId}/thresholds/{id}` | Smazání pravidla |

### Příjemci upozornění (`/api/devices/{deviceId}/recipients`)

| Metoda | Endpoint | Popis |
|---|---|---|
| POST | `/api/devices/{deviceId}/recipients` | Přidání příjemce |
| GET | `/api/devices/{deviceId}/recipients` | Seznam příjemců |
| PUT | `/api/devices/{deviceId}/recipients/{userId}` | Aktualizace nastavení |
| DELETE | `/api/devices/{deviceId}/recipients/{userId}` | Odebrání příjemce |

### Uživatelé (`/api/users`) — Pouze admin

| Metoda | Endpoint | Popis |
|---|---|---|
| POST | `/api/users` | Vytvoření uživatele |
| GET | `/api/users` | Seznam uživatelů (s vyhledáváním) |
| GET | `/api/users/{id}` | Detail uživatele |
| PUT | `/api/users/{id}` | Aktualizace uživatele |
| DELETE | `/api/users/{id}` | Smazání uživatele |
| POST | `/api/users/{userId}/devices/{deviceId}` | Udělení přístupu k zařízení |
| PUT | `/api/users/{userId}/devices/{deviceId}` | Aktualizace oprávnění |
| DELETE | `/api/users/{userId}/devices/{deviceId}` | Odebrání přístupu |

### Nastavení (`/api/settings`) — Pouze admin

| Metoda | Endpoint | Popis |
|---|---|---|
| GET | `/api/settings` | Systémová nastavení |
| PUT | `/api/settings` | Aktualizace nastavení |

> Kompletní interaktivní dokumentace API je dostupná na `/swagger-ui/index.html` při běžícím serveru.

---

## Struktura projektu

```
monitoring-system-backend/
├── src/main/java/cz/jirikfi/monitoringsystembackend/
│   ├── components/      # Specializované Spring komponenty
│   ├── configurations/  # Konfigurace zabezpečení, cache, WebSocket, OpenAPI
│   ├── controllers/     # REST API kontrolery
│   ├── entities/        # JPA entity (Metrics, Device, User, Alert atd.)
│   ├── enums/           # Výčtové typy (Role, MetricType, AlertSeverity atd.)
│   ├── exceptions/      # Vlastní třídy výjimek
│   ├── mappers/         # Mapování DTO <-> Entity
│   ├── middleware/      # JWT a API Key autentizační filtry
│   ├── models/          # DTOs a modely požadavků/odpovědí
│   ├── repositories/    # Spring Data JPA repozitáře
│   ├── services/        # Business logika (metriky, alerty, agregace atd.)
│   ├── utils/           # Pomocné třídy
│   └── validation/      # Vlastní validační anotace
├── src/main/resources/
│   └── application.yml  # Konfigurace aplikace
├── docker-compose.yml   # PostgreSQL kontejner
├── .env                 # Proměnné prostředí (mimo verzování)
└── uploads/             # Úložiště obrázků zařízení
```
