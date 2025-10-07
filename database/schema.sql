/* CRÉATION DES SÉQUENCES
  Une séquence par table pour générer les clés primaires.
 */
CREATE SEQUENCE seq_pays START WITH 1 INCREMENT BY 1 ;
CREATE SEQUENCE seq_stations_meteo START WITH 1 INCREMENT BY 1 ;
CREATE SEQUENCE seq_meteo START WITH 1 INCREMENT BY 1 ;

/* CRÉATION DES TABLES
   Table pays
   Stocke les informations de base sur les pays.
   */
CREATE TABLE pays (
                      pays_id NUMBER NOT NULL,
                      nom VARCHAR2(255) NOT NULL,
                      code_pays VARCHAR2(10) UNIQUE,
                      CONSTRAINT pk_pays PRIMARY KEY(pays_id)
);

/* Table stations_meteo
   Stocke les informations sur les stations météo, liées à un pays.
 */
CREATE TABLE stations_meteo (
                                station_id NUMBER NOT NULL,
                                pays_id NUMBER,
                                nom VARCHAR2(255) NOT NULL,
                                latitude NUMBER NOT NULL,
                                longitude NUMBER NOT NULL,
                                CONSTRAINT pk_stations_meteo PRIMARY KEY(station_id),
                                CONSTRAINT fk_stations_meteo_pays FOREIGN KEY(pays_id) REFERENCES pays(pays_id)
);
/* Table meteo
   Contient les relevés météorologiques pour une station donnée à un moment T.
 */
CREATE TABLE meteo (
                       meteo_id NUMBER NOT NULL,
                       station_id NUMBER,
                       date_releve TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                       temperature NUMBER,
                       humidite NUMBER,
                       pression NUMBER,
                       vitesse_vent NUMBER,
                       direction_vent NUMBER,
                       description VARCHAR2(255),
                       icone VARCHAR2(50),
                       CONSTRAINT pk_meteo PRIMARY KEY(meteo_id),
                       CONSTRAINT fk_meteo_station FOREIGN KEY(station_id) REFERENCES stations_meteo(station_id)
);
/* CRÉATION DES TRIGGERS
   Un trigger par table pour remplir automatiquement la clé primaire avant l'insertion.
 */
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
-- Ne pas oublier de commit.
commit ;