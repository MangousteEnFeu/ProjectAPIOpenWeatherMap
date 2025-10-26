package ch.hearc.meteo.infrastructure.persistence;

import ch.hearc.meteo.business.Meteo;
import ch.hearc.meteo.business.Pays;
import ch.hearc.meteo.business.StationMeteo;

import java.sql.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Implémentation Oracle du repository météo.
 * Gère :
 *  - insertion Pays / Station / Météo
 *  - lecture des stations / historique
 *
 * Tables attendues (avec triggers pour PK auto) :
 *
 *  PAYS(pays_id PK, nom, code_pays UNIQUE)
 *  STATIONS_METEO(station_id PK, pays_id FK, nom, latitude, longitude, openweather_id)
 *  METEO(meteo_id PK, station_id FK, date_releve, temperature, humidite, pression,
 *        visibilite, precipitation, vitesse_vent, direction_vent, description, icone)
 */
public class OracleMeteoRepository implements MeteoRepository {

    private final String jdbcUrl;
    private final String jdbcUser;
    private final String jdbcPassword;

    public OracleMeteoRepository(String jdbcUrl, String jdbcUser, String jdbcPassword) {
        this.jdbcUrl = jdbcUrl;
        this.jdbcUser = jdbcUser;
        this.jdbcPassword = jdbcPassword;
    }

    // --- Connexion Oracle ---
    private Connection getConnection() throws SQLException {
        // Assure-toi que ojdbc est bien dans les dépendances du projet
        return DriverManager.getConnection(jdbcUrl, jdbcUser, jdbcPassword);
    }

    // --- Méthode principale : sauvegarde d'une station + ses mesures météo ---

    @Override
    public void save(StationMeteo station) throws Exception {
        if (station == null) return;
        if (station.getDonneesMeteo() == null || station.getDonneesMeteo().isEmpty()) return;

        try (Connection cn = getConnection()) {
            cn.setAutoCommit(false);

            // 1. pays -> obtenir/insérer, récupérer pays_id
            Integer paysId = null;
            if (station.getPays() != null) {
                paysId = ensurePays(cn, station.getPays());
            }

            // 2. station -> obtenir/insérer, récupérer station_id
            Integer stationId = ensureStation(cn, station, paysId);

            // 3. relevés météo -> INSERT dans meteo
            for (Meteo m : station.getDonneesMeteo()) {
                insertMeteoRow(cn, stationId, m);
            }

            cn.commit();
        }
    }

    /**
     * Vérifie si le pays existe déjà en base (par son code_pays).
     * Sinon, insère.
     * Retourne toujours le pays_id.
     */
    private Integer ensurePays(Connection cn, Pays pays) throws SQLException {
        // 1) chercher pays existant
        String selectSql =
                "SELECT pays_id " +
                        "FROM pays " +
                        "WHERE code_pays = ?";

        try (PreparedStatement ps = cn.prepareStatement(selectSql)) {
            ps.setString(1, pays.getCode());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("pays_id");
                }
            }
        }

        // 2) insérer si pas trouvé
        // grâce au trigger, pays_id est généré automatiquement
        String insertSql =
                "INSERT INTO pays (nom, code_pays) " +
                        "VALUES (?, ?)";

        try (PreparedStatement ps = cn.prepareStatement(insertSql)) {
            ps.setString(1, pays.getNom());
            ps.setString(2, pays.getCode());
            ps.executeUpdate();
        }

        // 3) relire l'id
        try (PreparedStatement ps = cn.prepareStatement(selectSql)) {
            ps.setString(1, pays.getCode());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("pays_id");
                }
            }
        }

        return null;
    }

    /**
     * Vérifie si la station existe déjà :
     * critère = même nom + latitude + longitude (pour éviter de dupliquer 36 fois la même).
     * Si pas trouvée => INSERT.
     * Retourne station_id.
     *
     * NOTE : Si vous voulez utiliser openweather_id comme clé d'unicité,
     * vous pouvez adapter ici.
     */
    private Integer ensureStation(Connection cn, StationMeteo station, Integer paysId) throws SQLException {

        String selectSql =
                "SELECT station_id " +
                        "FROM stations_meteo " +
                        "WHERE nom = ? " +
                        "AND ABS(latitude - ?) < 0.000001 " +
                        "AND ABS(longitude - ?) < 0.000001";

        try (PreparedStatement ps = cn.prepareStatement(selectSql)) {
            ps.setString(1, station.getNom());
            ps.setDouble(2, station.getLatitude() != null ? station.getLatitude() : 0.0);
            ps.setDouble(3, station.getLongitude() != null ? station.getLongitude() : 0.0);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("station_id");
                }
            }
        }

        // pas trouvé -> INSERT
        String insertSql =
                "INSERT INTO stations_meteo " +
                        "(pays_id, nom, latitude, longitude, openweather_id) " +
                        "VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement ps = cn.prepareStatement(insertSql)) {
            if (paysId != null) {
                ps.setInt(1, paysId);
            } else {
                ps.setNull(1, Types.INTEGER);
            }

            ps.setString(2, station.getNom());
            ps.setDouble(3, station.getLatitude() != null ? station.getLatitude() : 0.0);
            ps.setDouble(4, station.getLongitude() != null ? station.getLongitude() : 0.0);

            if (station.getOpenWeatherMapId() != null) {
                ps.setInt(5, station.getOpenWeatherMapId());
            } else {
                ps.setNull(5, Types.INTEGER);
            }

            ps.executeUpdate();
        }

        // relire l'id après insert
        try (PreparedStatement ps = cn.prepareStatement(selectSql)) {
            ps.setString(1, station.getNom());
            ps.setDouble(2, station.getLatitude() != null ? station.getLatitude() : 0.0);
            ps.setDouble(3, station.getLongitude() != null ? station.getLongitude() : 0.0);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("station_id");
                }
            }
        }

        return null;
    }

    /**
     * Insère UNE mesure météo dans la table METEO pour une station donnée.
     * Le trigger gère meteo_id.
     */
    private void insertMeteoRow(Connection cn, Integer stationId, Meteo m) throws SQLException {

        String insertSql =
                "INSERT INTO meteo (" +
                        "    station_id," +
                        "    date_releve," +
                        "    temperature," +
                        "    humidite," +
                        "    pression," +
                        "    visibilite," +
                        "    precipitation," +
                        "    description" +
                        ") VALUES (" +
                        "    ?, ?, ?, ?, ?, ?, ?, ?" +
                        ")";

        try (PreparedStatement ps = cn.prepareStatement(insertSql)) {

            // station_id
            if (stationId != null) {
                ps.setInt(1, stationId);
            } else {
                ps.setNull(1, Types.INTEGER);
            }

            // date_releve
            if (m.getDateMesure() != null) {
                ps.setTimestamp(2, new Timestamp(m.getDateMesure().getTime()));
            } else {
                ps.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
            }

            // temperature
            if (m.getTemperature() != null) {
                ps.setDouble(3, m.getTemperature());
            } else {
                ps.setNull(3, Types.DOUBLE);
            }

            // humidite
            if (m.getHumidite() != null) {
                ps.setDouble(4, m.getHumidite());
            } else {
                ps.setNull(4, Types.DOUBLE);
            }

            // pression
            if (m.getPression() != null) {
                ps.setDouble(5, m.getPression());
            } else {
                ps.setNull(5, Types.DOUBLE);
            }

            // visibilite
            if (m.getVisibilite() != null) {
                ps.setInt(6, m.getVisibilite());
            } else {
                ps.setNull(6, Types.INTEGER);
            }

            // precipitation
            if (m.getPrecipitation() != null) {
                ps.setDouble(7, m.getPrecipitation());
            } else {
                ps.setNull(7, Types.DOUBLE);
            }

            // description
            if (m.getDescription() != null) {
                ps.setString(8, m.getDescription());
            } else {
                ps.setNull(8, Types.VARCHAR);
            }

            ps.executeUpdate();
        }
    }


    // --- Lecture pour le menu historique ---

    /**
     * Retourne la liste des noms de stations en base, triés alphabétiquement.
     */
    @Override
    public List<String> findAllStationNames() throws Exception {
        List<String> result = new ArrayList<>();

        String sql =
                "SELECT DISTINCT nom " +
                        "FROM stations_meteo " +
                        "ORDER BY nom ASC";

        try (Connection cn = getConnection();
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                result.add(rs.getString("nom"));
            }
        }

        return result;
    }

    /**
     * Retourne toutes les dates de relevé météo pour une station donnée (par nom).
     * Triées chronologiquement.
     */
    @Override
    public List<Date> findMeasurementDatesForStation(String stationName) throws Exception {
        List<Date> result = new ArrayList<>();

        String sql =
                "SELECT m.date_releve " +
                        "FROM meteo m " +
                        "JOIN stations_meteo s ON s.station_id = m.station_id " +
                        "WHERE s.nom = ? " +
                        "ORDER BY m.date_releve ASC";

        try (Connection cn = getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setString(1, stationName);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Timestamp ts = rs.getTimestamp("date_releve");
                    if (ts != null) {
                        result.add(new Date(ts.getTime()));
                    }
                }
            }
        }

        return result;
    }

    /**
     * Retourne un relevé météo complet pour une station donnée à une date précise.
     * On matche sur l'égalité exacte station.nom + date_releve.
     */
    @Override
    public Meteo findMeteoForStationAtDate(String stationName, Date date) throws Exception {

        // On va chercher la mesure dont le timestamp est dans la même seconde.
        String sql =
                "SELECT m.date_releve, " +
                        "       m.temperature, " +
                        "       m.humidite, " +
                        "       m.pression, " +
                        "       m.visibilite, " +
                        "       m.precipitation, " +
                        "       m.description " +
                        "FROM meteo m " +
                        "JOIN stations_meteo s ON s.station_id = m.station_id " +
                        "WHERE s.nom = ? " +
                        "  AND m.date_releve >= ? " +
                        "  AND m.date_releve < ? " +
                        "ORDER BY m.date_releve DESC";

        // On définit la fenêtre [date, date+1sec)
        Timestamp startTs = new Timestamp(date.getTime());
        Timestamp endTs   = new Timestamp(date.getTime() + 1000); // +1 seconde

        try (Connection cn = getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setString(1, stationName);
            ps.setTimestamp(2, startTs);
            ps.setTimestamp(3, endTs);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Meteo m = new Meteo();

                    Timestamp ts = rs.getTimestamp("date_releve");
                    if (ts != null) {
                        m.setDateMesure(new Date(ts.getTime()));
                    }

                    double temp = rs.getDouble("temperature");
                    if (!rs.wasNull()) {
                        m.setTemperature(temp);
                    }

                    double hum = rs.getDouble("humidite");
                    if (!rs.wasNull()) {
                        m.setHumidite(hum);
                    }

                    double pres = rs.getDouble("pression");
                    if (!rs.wasNull()) {
                        m.setPression(pres);
                    }

                    int vis = rs.getInt("visibilite");
                    if (!rs.wasNull()) {
                        m.setVisibilite(vis);
                    }

                    double precip = rs.getDouble("precipitation");
                    if (!rs.wasNull()) {
                        m.setPrecipitation(precip);
                    }

                    String desc = rs.getString("description");
                    if (desc != null) {
                        m.setDescription(desc);
                    }

                    return m;
                }
            }
        }

        return null;
    }


}
