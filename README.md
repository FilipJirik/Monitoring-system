# Aplikace pro sběr, monitoring a vizualizaci dat počítačů a serverů

---

## Popis projektu
Tento projekt slouží k monitorování počítačů a serverů.  
Zařízení odesílají data o svém stavu (např. využití CPU, RAM, disku) pomocí REST API na server, kde jsou údaje uloženy, vyhodnoceny a následně vizualizovány na webovém rozhraní.  
Při překročení prahových hodnot jsou generována upozornění (např. e-mail).

## Architektura
Aplikace je rozdělena do tří částí:
- **Frontend (Vue 3)** – vizualizace dat (grafy, tabulky, přehled zařízení)
- **Backend (Spring Boot, Java)** – REST API pro příjem a zpracování dat
- **Databáze (PostgreSQL)** – ukládání zařízení, metrik a historie hodnot

## Použité technologie
- **Java 21** – hlavní programovací jazyk
- **Spring Boot** – implementace REST API
- **PostgreSQL** – relační databáze
- **Vue 3 + Chart.js** – webové rozhraní a vizualizace dat
- **OpenAPI (Swagger)** – dokumentace API
- **Maven** – buildovací nástroj

## Funkce
- Sběr metrik z klientských zařízení (CPU, RAM, disk)
- Přehled zařízení (název, popis, vlastník)
- Zobrazení metrik v reálném čase
- Vizualizace dat pomocí tabulek a grafů
- Upozornění při překročení prahových hodnot
- Uživatelsky přívětivé webové rozhraní

---