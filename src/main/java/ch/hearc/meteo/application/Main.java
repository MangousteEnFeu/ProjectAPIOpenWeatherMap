package ch.hearc.meteo.application;

import ch.hearc.meteo.business.Meteo;
import ch.hearc.meteo.business.StationMeteo;
import ch.hearc.meteo.infrastructure.http.CountryClient;
import ch.hearc.meteo.infrastructure.http.OpenWeatherMapClient;
import ch.hearc.meteo.infrastructure.persistence.MeteoRepository;
import ch.hearc.meteo.infrastructure.persistence.OracleMeteoRepository;
import ch.hearc.meteo.service.MeteoService;
import ch.hearc.meteo.service.MeteoServiceImpl;

import java.io.InputStream;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.*;

public class Main {

    private static final Scanner SCANNER = new Scanner(System.in);

    public static void main(String[] args) {

        // 1. Charger la configuration
        Properties props = new Properties();
        try (InputStream in = Main.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (in != null) props.load(in);
            else System.err.println("Fichier application.properties introuvable.");
        } catch (Exception e) {
            System.err.println("Erreur chargement configuration : " + e.getMessage());
        }

        String owmUrl     = props.getProperty("openweathermap.url", "https://api.openweathermap.org/data/2.5/weather");
        String owmKey     = props.getProperty("openweathermap.key", "");
        String countryUrl = props.getProperty("country.url", "https://db.ig.he-arc.ch/ens/scl/ws/country");

        String jdbcUrl    = props.getProperty("oracle.jdbc.url");
        String jdbcUser   = props.getProperty("oracle.jdbc.user");
        String jdbcPwd    = props.getProperty("oracle.jdbc.password");

        if (owmKey.isBlank()) {
            System.err.println("Clé API OpenWeatherMap manquante. Vérifie application.properties");
            return;
        }

        // 2. Instancier clients API
        OpenWeatherMapClient owmClient = new OpenWeatherMapClient(owmUrl, owmKey);
        CountryClient countryClient    = new CountryClient(countryUrl);

// 3. Instancier le repository Oracle SI les infos JDBC sont présentes et valides
        MeteoRepository repo = null;
        if (isNotBlank(jdbcUrl) && isNotBlank(jdbcUser)) {
            if (testConnexionOracle(jdbcUrl, jdbcUser, jdbcPwd)) {
                repo = new OracleMeteoRepository(jdbcUrl, jdbcUser, jdbcPwd);
                System.out.println("(Info) Connexion Oracle OK, la sauvegarde est activée.");
            } else {
                System.out.println("(Info) Connexion Oracle impossible. Mode sans sauvegarde.");
            }
        } else {
            System.out.println("(Info) Paramètres Oracle absents. Mode sans sauvegarde.");
        }

        // 4. Créer le service
        MeteoService service = new MeteoServiceImpl(owmClient, countryClient, repo);

        // 5. Boucler sur le menu principal
        boucleMenuPrincipal(service);
    }

    private static void boucleMenuPrincipal(MeteoService service) {
        boolean quitter = false;
        while (!quitter) {
            System.out.println();
            System.out.println("=== MENU PRINCIPAL ===");
            System.out.println("1. Météo HE-Arc Neuchâtel (afficher seulement)");
            System.out.println("2. Nouvelle station (saisir coordonnées et enregistrer)");
            System.out.println("3. Consulter historique enregistré");
            System.out.println("9. Quitter");
            System.out.print("Votre choix : ");

            String choix = SCANNER.nextLine().trim();

            switch (choix) {
                case "1":
                    // coordonnées école HE-Arc Neuchâtel (à ajuster si besoin)
                    afficherMeteoCourante(service, 46.9931, 6.9319, "fr", false);
                    break;
                case "2":
                    actionSaisirEtEnregistrer(service);
                    break;
                case "3":
                    sousMenuHistorique(service);
                    break;
                case "9":
                    quitter = true;
                    System.out.println("Fin du programme.");
                    break;
                default:
                    System.out.println("Choix invalide.");
                    break;
            }
        }
    }

    /**
     * Option 2 :
     * - On demande les coordonnées à l'utilisateur
     * - On appelle le service
     * - Le service va sauvegarder en base (si repo Oracle actif)
     * - On affiche le résultat
     */
    private static void actionSaisirEtEnregistrer(MeteoService service) {
        System.out.println();
        System.out.println("=== Nouvelle station météo ===");

        double lat = lireDouble("Latitude : ");
        double lon = lireDouble("Longitude : ");
        System.out.print("Langue (ex: fr) [fr par défaut] : ");
        String lang = SCANNER.nextLine().trim();
        if (lang.isBlank()) lang = "fr";

        StationMeteo station = service.obtenirMeteoEtTraiter(lat, lon, lang);

        if (station == null || station.getDonneesMeteo().isEmpty()) {
            System.out.println("Aucune donnée météo disponible.");
            return;
        }

        // Affichage
        afficherStation(station);
        System.out.println("(Les données ont été sauvegardées si la base est configurée)");
    }

    /**
     * Option 1 (ou utilitaire générique) :
     * Récupère la météo mais ne dit rien sur la sauvegarde.
     *
     * @param persistInfo si false, on n'affiche pas le message "sauvegardé ..."
     */
    private static void afficherMeteoCourante(MeteoService service,
                                              double lat,
                                              double lon,
                                              String lang,
                                              boolean persistInfo) {

        StationMeteo station = service.obtenirMeteoEtTraiter(lat, lon, lang);

        if (station == null || station.getDonneesMeteo().isEmpty()) {
            System.out.println("Aucune donnée météo disponible.");
            return;
        }

        afficherStation(station);

        if (persistInfo) {
            System.out.println("(Les données ont été sauvegardées si la base est configurée)");
        }
    }

    /**
     * Option 3 :
     * - affiche la liste des stations déjà enregistrées (ordre alphabétique)
     * - l'utilisateur en choisit une
     * - on affiche la liste des dates de mesures
     * - l'utilisateur en choisit une
     * - on affiche le relevé détaillé
     */
    private static void sousMenuHistorique(MeteoService service) {
        while (true) {
            System.out.println();
            System.out.println("=== Historique des mesures ===");
            System.out.println("8. Retour");
            List<String> stations = service.listerStationsEnregistrees();
            if (stations.isEmpty()) {
                System.out.println("(Aucune station enregistrée en base ou base non configurée)");
                return;
            }

            // Afficher les stations
            for (String s : stations) {
                System.out.println("- " + s);
            }

            System.out.print("Entrez un nom de station (ou 8 pour retour) : ");
            String choixStation = SCANNER.nextLine().trim();
            if ("8".equals(choixStation)) {
                return; // retour menu principal
            }
            if (!stations.contains(choixStation)) {
                System.out.println("Station inconnue.");
                continue; // redemande
            }

            // on a une station valide -> afficher dates
            List<Date> dates = service.listerDatesPourStation(choixStation);
            if (dates.isEmpty()) {
                System.out.println("(Aucune mesure historique trouvée pour cette station)");
                continue;
            }

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            System.out.println("Mesures disponibles pour " + choixStation + " :");
            for (Date d : dates) {
                System.out.println("- " + sdf.format(d));
            }
            System.out.print("Entrez une date exacte (format: yyyy-MM-dd HH:mm:ss) (copier-coller ci-dessus) ou 8 pour retour : ");
            String choixDate = SCANNER.nextLine().trim();
            if ("8".equals(choixDate)) {
                continue; // revient au choix de station
            }

            Date dateChoisie = null;
            try {
                dateChoisie = sdf.parse(choixDate);
            } catch (Exception e) {
                System.out.println("Format de date invalide.");
                continue;
            }

            Meteo relevé = service.obtenirMeteoHistorique(choixStation, dateChoisie);
            if (relevé == null) {
                System.out.println("Aucune donnée météo pour cette date précise.");
            } else {
                afficherMesureDetaillee(relevé);
            }
        }
    }

    /* ====== petites fonctions utilitaires d'affichage ===== */

    private static void afficherStation(StationMeteo station) {
        Meteo m = station.getDonneesMeteo().get(0);

        DecimalFormat df1 = new DecimalFormat("0.0");
        DecimalFormat df0 = new DecimalFormat("0");

        System.out.println();
        System.out.println("=== Météo actuelle ===");
        System.out.printf("Lieu : %s (%s)%n",
                (station.getNom() != null ? station.getNom() : "Inconnu"),
                (station.getPays() != null && station.getPays().getNom() != null)
                        ? station.getPays().getNom()
                        : (station.getPays() != null ? station.getPays().getCode() : "--")
        );
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

        System.out.println("========================");
    }

    private static void afficherMesureDetaillee(Meteo m) {
        DecimalFormat df1 = new DecimalFormat("0.0");
        DecimalFormat df0 = new DecimalFormat("0");
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        System.out.println();
        System.out.println("=== Détails relevé météo ===");
        if (m.getDateMesure() != null)
            System.out.println("Date du relevé : " + sdf.format(m.getDateMesure()));

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
        System.out.println("============================");
    }

    private static double lireDouble(String label) {
        while (true) {
            System.out.print(label);
            String s = SCANNER.nextLine().trim();
            try {
                return Double.parseDouble(s);
            } catch (NumberFormatException e) {
                System.out.println("Valeur invalide.");
            }
        }
    }

    private static boolean isNotBlank(String s) {
        return s != null && !s.isBlank();
    }

    private static String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    private static boolean testConnexionOracle(String url, String user, String pwd) {
        try (Connection cn = DriverManager.getConnection(url, user, pwd)) {
            return true;
        } catch (Exception e) {
            System.err.println("[WARN] Échec connexion Oracle : " + e.getMessage());
            return false;
        }
    }
}
