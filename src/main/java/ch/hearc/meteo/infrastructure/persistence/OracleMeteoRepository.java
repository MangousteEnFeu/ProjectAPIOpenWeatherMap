package ch.hearc.meteo.infrastructure.persistence;

import ch.hearc.meteo.business.Meteo;
import ch.hearc.meteo.business.Pays;
import ch.hearc.meteo.business.StationMeteo;

import java.sql.*;
import java.text.SimpleDateFormat;

/**
 * Implémentation OJDBC simple de MeteoRepository.
 *
 * ATTENTION :
 * - Cette implémentation utilise DriverManager.getConnection.
 * - Adapte les chaînes SQL à ta structure de tables (ex: STATION_METEO, METEO, PAYS).
 * - Ajoute la dépendance Oracle (ojdbc8/ojdbc11) dans le pom.gradle.
 *
 * Exemple minimal de logique :
 * 1) Insérer / mettre à jour PAYS (si présent)
 * 2) Insérer / mettre à jour STATION_METEO (avec FK vers PAYS)
 * 3) Insérer METEO (avec FK vers STATION_METEO)
 *
 * Ici on utilise des inserts simples sans upsert — adapte selon besoin.
 */
public class OracleMeteoRepository implements MeteoRepository {

    private final String jdbcUrl;
    private final String username;
    private final String password;

    public OracleMeteoRepository(String jdbcUrl, String username, String password) {
        this.jdbcUrl = jdbcUrl;
        this.username = username;
        this.password = password;
    }

    @Override
    public void save(StationMeteo station) throws Exception {
        if (station == null) return;

        // Charger driver Oracle (souvent optionnel selon driver)
        try {
            Class.forName("oracle.jdbc.driver.OracleDriver");
        } catch (ClassNotFoundException ignored) {}

        try (Connection conn = DriverManager.getConnection(jdbcUrl, username, password)) {
            conn.setAutoCommit(false);
            try {
                // 1) gérer le pays si présent
                Integer paysId = null;
                if (station.getPays() != null && station.getPays().getCode() != null) {
                    Pays p = station.getPays();
                    // Inserer le pays (exemple) - adapter SQL
                    String sqlInsertPays = "MERGE INTO PAYS dest USING (SELECT ? AS CODE, ? AS NOM FROM DUAL) src " +
                            "ON (dest.CODE = src.CODE) " +
                            "WHEN MATCHED THEN UPDATE SET dest.NOM = src.NOM " +
                            "WHEN NOT MATCHED THEN INSERT (numero, code, nom) VALUES (PAYS_SEQ.NEXTVAL, src.CODE, src.NOM)";
                    try (PreparedStatement ps = conn.prepareStatement(sqlInsertPays)) {
                        ps.setString(1, p.getCode());
                        ps.setString(2, p.getNom());
                        ps.executeUpdate();
                    }
                    // Récupérer ID si nécessaire (ex: via select)
                    String sqlSelectPays = "SELECT numero FROM PAYS WHERE code = ?";
                    try (PreparedStatement ps = conn.prepareStatement(sqlSelectPays)) {
                        ps.setString(1, p.getCode());
                        try (ResultSet rs = ps.executeQuery()) {
                            if (rs.next()) paysId = rs.getInt(1);
                        }
                    }
                }

                // 2) Inserer la station (MERGE pour upsert)
                Integer stationId = null;
                String sqlMergeStation = "MERGE INTO STATION_METEO dest USING (SELECT ? AS OWM_ID, ? AS NOM FROM DUAL) src " +
                        "ON (dest.openweathermap_id = src.OWM_ID) " +
                        "WHEN MATCHED THEN UPDATE SET dest.nom = src.NOM, dest.latitude = ?, dest.longitude = ?, dest.pays_numero = ? " +
                        "WHEN NOT MATCHED THEN INSERT (numero, nom, latitude, longitude, openweathermap_id, pays_numero) " +
                        "VALUES (STATION_SEQ.NEXTVAL, src.NOM, ?, ?, src.OWM_ID, ?)";
                try (PreparedStatement ps = conn.prepareStatement(sqlMergeStation)) {
                    ps.setInt(1, station.getOpenWeatherMapId() != null ? station.getOpenWeatherMapId() : 0);
                    ps.setString(2, station.getNom());
                    // matched update params
                    ps.setObject(3, station.getLatitude());
                    ps.setObject(4, station.getLongitude());
                    ps.setObject(5, paysId);
                    // insert params
                    ps.setObject(6, station.getLatitude());
                    ps.setObject(7, station.getLongitude());
                    ps.setObject(8, paysId);
                    ps.executeUpdate();
                }
                // récupérer station.numero
                String sqlSelectStation = "SELECT numero FROM STATION_METEO WHERE openweathermap_id = ?";
                try (PreparedStatement ps = conn.prepareStatement(sqlSelectStation)) {
                    ps.setInt(1, station.getOpenWeatherMapId() != null ? station.getOpenWeatherMapId() : 0);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) stationId = rs.getInt(1);
                    }
                }

                // 3) Insérer les mesures météo (METEO)
                if (station.getDonneesMeteo() != null) {
                    String sqlInsertMeteo = "INSERT INTO METEO (numero, station_numero, date_mesure, temperature, description, pression, humidite, visibilite, precipitation) " +
                            "VALUES (METEO_SEQ.NEXTVAL, ?, ?, ?, ?, ?, ?, ?, ?)";
                    try (PreparedStatement ps = conn.prepareStatement(sqlInsertMeteo)) {
                        for (Meteo m : station.getDonneesMeteo()) {
                            ps.setObject(1, stationId);
                            // Oracle TIMESTAMP handling : setTimestamp using java.sql.Timestamp
                            if (m.getDateMesure() != null) {
                                ps.setTimestamp(2, new Timestamp(m.getDateMesure().getTime()));
                            } else {
                                ps.setTimestamp(2, null);
                            }
                            ps.setObject(3, m.getTemperature());
                            ps.setString(4, m.getDescription());
                            ps.setObject(5, m.getPression());
                            ps.setObject(6, m.getHumidite());
                            ps.setObject(7, m.getVisibilite());
                            ps.setObject(8, m.getPrecipitation());
                            ps.executeUpdate();
                        }
                    }
                }

                conn.commit();
            } catch (Exception ex) {
                conn.rollback();
                throw ex;
            }
        }
    }
}
