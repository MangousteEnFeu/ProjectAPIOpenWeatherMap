package ch.hearc.meteo.application;

import ch.hearc.meteo.business.Meteo;
import ch.hearc.meteo.business.StationMeteo;
import ch.hearc.meteo.infrastructure.http.CountryClient;
import ch.hearc.meteo.infrastructure.http.OpenWeatherMapClient;
// --- Partie base de données (non utilisée par Théo) ---
// import ch.hearc.meteo.infrastructure.persistence.MeteoRepository;
// import ch.hearc.meteo.infrastructure.persistence.OracleMeteoRepository;
import ch.hearc.meteo.service.MeteoService;
import ch.hearc.meteo.service.MeteoServiceImpl;

import java.io.InputStream;
import java.text.DecimalFormat;
import java.util.Properties;

public class Main {
    public static void main(String[] args) {
        try {
            // Chargement du fichier de configuration
            Properties props = new Properties();
            try (InputStream in = Main.class.getClassLoader().getResourceAsStream("application.properties")) {
                if (in != null) props.load(in);
                else System.err.println("Fichier application.properties introuvable.");
            }

            String owmUrl     = props.getProperty("openweathermap.url", "https://api.openweathermap.org/data/2.5/weather");
            String owmKey     = props.getProperty("openweathermap.key", "");
            String countryUrl = props.getProperty("country.url", "https://db.ig.he-arc.ch/ens/scl/ws/country");

            // --- Partie base de données (non utilisée par Théo) ---
            // String jdbcUrl    = props.getProperty("oracle.jdbc.url");
            // String jdbcUser   = props.getProperty("oracle.jdbc.user");
            // String jdbcPwd    = props.getProperty("oracle.jdbc.password");

            if (owmKey.isBlank()) {
                System.err.println("Clé API OpenWeatherMap manquante.");
                return;
            }

            // Coordonnées par défaut ou fournies en arguments
            double lat = 46.2022; // Genève
            double lon = 6.1457;
            String lang = "fr";

            if (args.length >= 2) {
                try {
                    lat = Double.parseDouble(args[0]);
                    lon = Double.parseDouble(args[1]);
                    if (args.length >= 3) lang = args[2];
                } catch (NumberFormatException e) {
                    System.err.println("Coordonnées invalides, utilisation de Genève.");
                }
            }

            // Instanciation des composants nécessaires à ta partie
            OpenWeatherMapClient owmClient = new OpenWeatherMapClient(owmUrl, owmKey);
            CountryClient countryClient    = new CountryClient(countryUrl);

            // --- Partie base de données (non utilisée par Théo) ---
            // MeteoRepository repo = (jdbcUrl != null && !jdbcUrl.isBlank() && jdbcUser != null && !jdbcUser.isBlank())
            //         ? new OracleMeteoRepository(jdbcUrl, jdbcUser, jdbcPwd)
            //         : null;

            // --- Création du service (sans base de données) ---
            MeteoService service = new MeteoServiceImpl(owmClient, countryClient, null);

            // Exécution de la récupération et du traitement
            StationMeteo station = service.obtenirMeteoEtTraiter(lat, lon, lang);

            if (station == null || station.getDonneesMeteo().isEmpty()) {
                System.out.println("Aucune donnée météo disponible.");
                return;
            }

            Meteo m = station.getDonneesMeteo().get(0);
            DecimalFormat df1 = new DecimalFormat("0.0");
            DecimalFormat df0 = new DecimalFormat("0");

            // --- Affichage lisible avec unités ---
            System.out.println("\n=== Météo actuelle ===");
            System.out.printf("Lieu : %s (%s)%n",
                    station.getNom() != null ? station.getNom() : "Inconnu",
                    (station.getPays() != null && station.getPays().getNom() != null)
                            ? station.getPays().getNom()
                            : (station.getPays() != null ? station.getPays().getCode() : "--"));
            System.out.printf("Coordonnées : %.4f, %.4f%n", station.getLatitude(), station.getLongitude());
            System.out.println("------------------------");

            if (m.getTemperature() != null)
                System.out.printf("Température : %s °C%n", df1.format(m.getTemperature()));
            if (m.getHumidite() != null)
                System.out.printf("Humidité : %s %% %n", df0.format(m.getHumidite()));
            if (m.getPression() != null)
                System.out.printf("Pression : %s hPa%n", df0.format(m.getPression()));
            if (m.getVisibilite() != null)
                System.out.printf("Visibilité : %.1f km%n", m.getVisibilite() / 1000.0);
            if (m.getPrecipitation() != null)
                System.out.printf("Précipitations : %s mm%n", df1.format(m.getPrecipitation()));
            if (m.getDescription() != null)
                System.out.printf("Conditions : %s%n", capitalize(m.getDescription()));

            System.out.println("========================\n");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}
