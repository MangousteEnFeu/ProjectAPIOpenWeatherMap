# Projet Météo – OpenWeatherMap + Oracle (Java / Maven)

Application console Java qui :

- interroge l’API OpenWeatherMap pour récupérer la météo actuelle à une latitude/longitude,
- enrichit les infos pays,
- affiche les données à l’utilisateur,
- enregistre chaque relevé météo dans une base Oracle,
- permet de consulter l’historique des relevés enregistrés.

Projet réalisé dans le cadre du module Services et Composants Logiciels – HE-Arc.

## Sommaire

1. [Fonctionnalités](#fonctionnalit%C3%A9s)
2. [Utilisation de l'application (menus)](#utilisation-de-lapplication-menus)
3. [Architecture logicielle](#architecture-logicielle)
4. [Base de données Oracle](#base-de-donn%C3%A9es-oracle)
5. [Gestion des secrets / configuration](#gestion-des-secrets--configuration)
6. [Prérequis techniques](#pr%C3%A9requis-techniques)
7. [Lancer l’application](#lancer-lapplication)
8. [Dépendances Maven](#d%C3%A9pendances-maven)
9. [Détails techniques utiles](#d%C3%A9tails-techniques-utiles)
10. [Auteurs / rôles](#auteurs--r%C3%B4les)
11. [Améliorations possibles](#am%C3%A9liorations-possibles)
12. [Résumé rapide](#r%C3%A9sum%C3%A9-rapide)

---

## Fonctionnalités

### Météo actuelle

L’application peut récupérer la météo actuelle pour :

- une position fixe (HE-Arc Neuchâtel),
- une station météo déjà enregistrée en base,
- une nouvelle position entrée manuellement (latitude / longitude).

Les informations affichées incluent :

- nom du lieu (ville + pays),
- coordonnées,
- température,
- humidité,
- pression,
- visibilité,
- précipitations,
- description (ex: "ciel dégagé").

À chaque récupération météo réussie :

- si la base Oracle est configurée et accessible, on enregistre automatiquement le relevé (station + pays + mesures météo).

### Historique

L’application permet :

- de lister les stations météo connues en base (ordre alphabétique),
- d’afficher les dates de relevé météo disponibles pour une station (triées du plus récent au plus ancien),
- de sélectionner une date précise pour rejouer l’état météo de ce moment-là.

### Mode sans base

- Si la connexion Oracle échoue au démarrage, l’application continue de fonctionner.
- Les relevés météo sont affichés mais pas stockés.
- L’historique n’est pas disponible dans ce mode.

---

## Utilisation de l'application (menus)

Au lancement de l'application (`Main`), l’utilisateur voit :

```
=== MENU PRINCIPAL ===
1. Météo HE-Arc Neuchâtel (afficher seulement)
2. Météo actuelle (saisie du lieu puis enregistrement)
3. Consulter historique enregistré
9. Quitter
Votre choix :
```

### Option 1 : Météo HE-Arc Neuchâtel

- Utilise des coordonnées fixes (par exemple coordonnées HE-Arc Neuchâtel).
- Récupère et affiche la météo actuelle.
- Enregistre le relevé en base si Oracle est configurée.

### Option 2 : Météo actuelle (saisie du lieu puis enregistrement)

Ouvre un sous-menu :

```
=== Météo actuelle / Enregistrement ===
1. Depuis une station météo déjà enregistrée
2. Nouvelle station (saisir latitude / longitude)
8. Retour
Votre choix :
```

#### 2.1 Depuis une station météo déjà enregistrée

- Affiche la liste des stations présentes dans la base Oracle.
- L’utilisateur entre le nom exact d’une station.
- Le programme :

- récupère les coordonnées (latitude / longitude) de cette station depuis la base,
- interroge l’API OpenWeatherMap pour la météo actuelle de cet endroit,
- affiche les résultats,
- enregistre en base un NOUVEAU relevé météo pour cette station (historique).

#### 2.2 Nouvelle station (saisir latitude / longitude)

- L’utilisateur entre manuellement latitude et longitude.
- Le programme interroge OpenWeatherMap pour ces coordonnées.
- Le programme affiche les résultats météo.
- Puis enregistre en base :

- le pays (si nouveau),
- la station météo (si nouvelle),
- le relevé météo courant.

### Option 3 : Consulter historique enregistré

1. Le programme affiche les stations enregistrées en base (ordre alphabétique).
2. L’utilisateur choisit une station.
3. Le programme affiche toutes les dates où un relevé météo a été stocké pour cette station (les dates les plus récentes d’abord).
4. L’utilisateur choisit une des dates affichées, par exemple :

```
2025-10-26 17:42:23
```
5. Le programme affiche les valeurs météo pour CET instant :

- Date du relevé
- Température
- Humidité
- Pression
- Visibilité
- Précipitations
- Description météo

Remarque : pour retrouver un relevé, la date doit être copiée-collée au format exact `yyyy-MM-dd HH:mm:ss`.

Le repository recherche dans la même seconde (ex: entre `17:42:23.000` et `<17:42:24.000` en base Oracle).

---

## Architecture logicielle

Le projet est organisé par couches :

### 1. `ch.hearc.meteo.application`

- Contient `Main`
- Rôle : interaction utilisateur (menus console, saisie Scanner, affichage du texte)

### 2. `ch.hearc.meteo.service`

- Contient :

- `MeteoService` (interface)
- `MeteoServiceImpl` (implémentation)
- Rôle :

- appeler l’API OpenWeatherMap via un client HTTP dédié,
- enrichir les informations pays,
- stocker (via le repository Oracle) les relevés météo,
- fournir des méthodes haut niveau utilisées par `Main`, comme :

- `obtenirMeteoEtTraiter(lat, lon, langue)`
- `capturerMeteoPourStationEnregistree(nomStation, langue)`
- `listerStationsEnregistrees()`
- `listerDatesPourStation(station)`
- `obtenirMeteoHistorique(station, date)`

### 3. `ch.hearc.meteo.infrastructure.http`

- `OpenWeatherMapClient`

- Contacte l’API OpenWeatherMap `/data/2.5/weather`
- Convertit la réponse JSON (via Gson) en objets métier `StationMeteo` et `Meteo`
- `CountryClient`

- Récupère le nom lisible du pays depuis le code pays (ex: `CH` → `Suisse`)
- En cas d’erreur de l’API pays, un fallback Java `Locale` est utilisé

### 4. `ch.hearc.meteo.infrastructure.persistence`

- `MeteoRepository` (interface)
- `OracleMeteoRepository` (implémentation JDBC Oracle)

- Connexion à Oracle avec OJDBC
- Méthodes principales :

- `save(StationMeteo station)`

Enregistre le pays, la station météo, et les relevés météo.
- `findAllStationNames()`

Liste les noms des stations (triés alphabétiquement).
- `findMeasurementDatesForStation(stationName)`

Liste les dates des relevés météo pour une station (ordre : du plus récent au plus ancien).
- `findMeteoForStationAtDate(stationName, date)`

Récupère le relevé météo stocké pour cette station à cette date (avec tolérance sur les millisecondes).
- `findStationByName(stationName)`

Permet de retrouver les coordonnées d’une station enregistrée, pour relancer un relevé actuel.

### 5. `ch.hearc.meteo.business`

Objets métier utilisés dans tout le projet :

```
public class StationMeteo {
    private Integer numero;            // identifiant interne
    private String nom;                // ex: "Neuchâtel"
    private Pays pays;                 // pays de la station
    private Double latitude;
    private Double longitude;
    private Integer openWeatherMapId;  // id OpenWeatherMap
    private List<Meteo> donneesMeteo;  // liste des relevés météo
}

public class Meteo {
    private Date dateMesure;
    private Double temperature;
    private Double humidite;
    private Double pression;
    private Integer visibilite;        // mètres
    private Double precipitation;      // mm
    private String description;        // ex: "ciel dégagé"
}

public class Pays {
    private Integer numero;
    private String nom;                // ex "Suisse"
    private String code;               // ex "CH"
}
```

---

## Base de données Oracle

L’application utilise 3 tables principales, avec des séquences et des triggers pour générer automatiquement les clés primaires.

### Schéma simplifié

```
CREATE SEQUENCE seq_pays START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE seq_stations_meteo START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE seq_meteo START WITH 1 INCREMENT BY 1;

CREATE TABLE pays (
  pays_id    NUMBER         NOT NULL,
  nom        VARCHAR2(255)  NOT NULL,
  code_pays  VARCHAR2(10)   UNIQUE,
  CONSTRAINT pk_pays PRIMARY KEY(pays_id)
);

CREATE TABLE stations_meteo (
  station_id      NUMBER        NOT NULL,
  pays_id         NUMBER,
  nom             VARCHAR2(255) NOT NULL,
  latitude        NUMBER        NOT NULL,
  longitude       NUMBER        NOT NULL,
  openweather_id  NUMBER,
  CONSTRAINT pk_stations_meteo PRIMARY KEY(station_id),
  CONSTRAINT fk_stations_meteo_pays FOREIGN KEY(pays_id)
    REFERENCES pays(pays_id)
);

CREATE TABLE meteo (
  meteo_id        NUMBER        NOT NULL,
  station_id      NUMBER,
  date_releve     TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
  temperature     NUMBER,
  humidite        NUMBER,
  pression        NUMBER,
  visibilite      NUMBER,
  precipitation   NUMBER,
  description     VARCHAR2(255),
  CONSTRAINT pk_meteo PRIMARY KEY(meteo_id),
  CONSTRAINT fk_meteo_station FOREIGN KEY(station_id)
    REFERENCES stations_meteo(station_id)
);

CREATE OR REPLACE TRIGGER trg_pays_pk
BEFORE INSERT ON pays
FOR EACH ROW
BEGIN
  IF :NEW.pays_id IS NULL THEN
    :NEW.pays_id := seq_pays.NEXTVAL;
  END IF;
END;
/

CREATE OR REPLACE TRIGGER trg_stations_meteo_pk
BEFORE INSERT ON stations_meteo
FOR EACH ROW
BEGIN
  IF :NEW.station_id IS NULL THEN
    :NEW.station_id := seq_stations_meteo.NEXTVAL;
  END IF;
END;
/

CREATE OR REPLACE TRIGGER trg_meteo_pk
BEFORE INSERT ON meteo
FOR EACH ROW
BEGIN
  IF :NEW.meteo_id IS NULL THEN
    :NEW.meteo_id := seq_meteo.NEXTVAL;
  END IF;
END;
/
```

### Détails importants

- `pays` contient nom + code ISO du pays (`code_pays`).
- `stations_meteo` contient :

- nom de la station,
- latitude / longitude,
- lien vers le pays,
- `openweather_id` de la ville si fourni par l’API.
- `meteo` contient les mesures météo individuelles (horodatées).
- Les PK sont auto-générées par Oracle via séquences / triggers.

---

## Gestion des secrets / configuration

L’application lit sa configuration dans `src/main/resources/application.properties`.

Ce fichier contient :

- la clé API OpenWeatherMap,
- les informations de connexion Oracle (URL, utilisateur, mot de passe).

Ce fichier ne doit PAS être committé dans GitHub.

### Fichier d'exemple public

Le dépôt inclut un fichier modèle commitable :

`src/main/resources/application-example.properties`

Ce fichier sert de template pour les autres développeurs, et ressemble à ceci :

```
# --- OpenWeatherMap API 2.5 ---
openweathermap.url=https://api.openweathermap.org/data/2.5/weather
openweathermap.key=YOUR_OPENWEATHER_API_KEY_HERE

# --- API Pays ---
country.url=https://db.ig.he-arc.ch/ens/scl/ws/country

# --- JDBC / Oracle ---
oracle.jdbc.url=jdbc:oracle:thin:@HOST:PORT:SID
oracle.jdbc.user=ORACLE_USERNAME
oracle.jdbc.password=ORACLE_PASSWORD
```

### Fichier réel local (non commité)

Chaque développeur doit créer son propre fichier local :

`src/main/resources/application.properties`

en copiant le modèle :

```
cp src/main/resources/application-example.properties src/main/resources/application.properties
```

Puis il remplit ses vraies valeurs :

- `openweathermap.key=...`
- `oracle.jdbc.url=...`
- `oracle.jdbc.user=...`
- `oracle.jdbc.password=...`

### .gitignore

Le `.gitignore` du projet contient :

```
src/main/resources/application.properties
```

Ce qui garantit que `application.properties` (avec tes secrets) n’est jamais poussé dans le dépôt.

---

## Prérequis techniques

- Java 11
- Maven
- Accès internet (OpenWeatherMap + API pays)
- Accès à une base Oracle avec :

- URL JDBC,
- utilisateur Oracle,
- mot de passe Oracle,
- accès réseau (selon l’école : parfois VPN requis)

Comportement en cas d’échec de la connexion Oracle :

- Au démarrage, l’application teste la connexion JDBC.
- Si la connexion échoue, le programme signale qu’il fonctionnera "en mode sans sauvegarde".
- Les options d’historique ne fonctionneront pas dans ce mode.

---

## Lancer l’application

### 1. Configurer le fichier de propriétés

Créer / éditer `src/main/resources/application.properties` avec vos vraies valeurs.

Exemple (ne pas commiter) :

```
openweathermap.url=https://api.openweathermap.org/data/2.5/weather
openweathermap.key=MA_CLE_API

country.url=https://db.ig.he-arc.ch/ens/scl/ws/country

oracle.jdbc.url=jdbc:oracle:thin:@db.ig.he-arc.ch:1521:ens
oracle.jdbc.user=MON_UTILISATEUR
oracle.jdbc.password=MON_MOT_DE_PASSE
```

### 2. Builder le projet

```
mvn clean install
```

### 3. Exécuter

Dans l’IDE : lancer la classe `ch.hearc.meteo.application.Main`.

Ou via `java -cp ...` si vous assemblez un jar exécutable.

---

## Dépendances Maven

Extrait du `pom.xml` :

```
<properties>
    <maven.compiler.source>11</maven.compiler.source>
    <maven.compiler.target>11</maven.compiler.target>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
</properties>

<dependencies>
    <!-- JSON / parsing API -->
    <dependency>
        <groupId>com.google.code.gson</groupId>
        <artifactId>gson</artifactId>
        <version>2.10.1</version>
    </dependency>

    <!-- Driver JDBC Oracle -->
    <dependency>
        <groupId>com.oracle.database.jdbc</groupId>
        <artifactId>ojdbc11</artifactId>
        <version>23.3.0.23.09</version>
    </dependency>

    <!-- Logging -->
    <dependency>
        <groupId>org.slf4j</groupId>
        <artifactId>slf4j-api</artifactId>
        <version>2.0.9</version>
    </dependency>
    <dependency>
        <groupId>ch.qos.logback</groupId>
        <artifactId>logback-classic</artifactId>
        <version>1.4.11</version>
    </dependency>
</dependencies>
```

Rôles :

- `gson` : parse la réponse JSON d’OpenWeatherMap et de l’API pays.
- `ojdbc11` : driver Oracle compatible Java 11 pour `DriverManager.getConnection(...)`.
- `slf4j` + `logback` : logs techniques, messages d’info / avertissements.

---

## Détails techniques utiles

- Lorsqu’on choisit "station météo déjà enregistrée", l’application :

- lit les coordonnées (latitude/longitude) de la station en base Oracle,
- interroge l’API OpenWeatherMap pour ces coordonnées,
- affiche les données météo actuelles,
- insère un nouveau relevé météo dans la table `meteo`.
- Chaque relevé `meteo` est stocké avec un `TIMESTAMP`, parfois avec millisecondes.

Pour rejouer un relevé précis dans l’historique, l’utilisateur tape la date affichée au format `yyyy-MM-dd HH:mm:ss`, et la requête en base recherche dans la fenêtre `[date, date+1 sec)`.
- Lors de l’affichage de l’historique :

- les dates sont triées de la plus récente à la plus ancienne.

---

## Améliorations possibles

- Générer un jar exécutable autonome (fat jar) avec `maven-shade-plugin`.
- Exporter l’historique d’une station en CSV.
- Support de la recherche par "nom de ville" plutôt que lat/lon.
- Interface graphique (Swing ou Web) à la place de la console.

---

## Résumé rapide

- Application console Java/Maven.
- Appelle OpenWeatherMap pour obtenir la météo actuelle.
- Enrichit les données pays.
- Sauvegarde les relevés météo dans une base Oracle via JDBC.
- Permet d’explorer l’historique par station et par date.
- Gère un mode offline si Oracle n’est pas joignable.
- Les secrets (clé API, mot de passe BD) restent en local et ne sont pas committés grâce à `.gitignore` et `application-example.properties`.
