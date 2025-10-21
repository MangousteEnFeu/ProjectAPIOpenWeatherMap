package ch.hearc.meteo.dto;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * DTO correspondant à la réponse JSON du service OpenWeatherMap (endpoint /data/2.5/weather).
 * Sert uniquement au mapping JSON → objets Java via Gson.
 * Les noms de champs doivent correspondre exactement à ceux de l’API, sinon il faut annoter avec @SerializedName.
 */
public class OpenWeatherMapResponse {
    private Coord coord;          // coordonnées géographiques
    private List<Weather> weather; // liste (souvent 1 élément) contenant les conditions météo textuelles
    private Main main;             // section principale : température, pression, humidité
    private Integer visibility;    // en mètres
    private Sys sys;               // contient le code pays (et autres infos non utilisées ici)
    private String name;           // nom de la ville
    private Integer id;            // identifiant de la station dans OWM
    private Rain rain;             // précipitations (peut être null)

    // Getters uniquement : immuabilité logique du DTO
    public Coord getCoord() { return coord; }
    public List<Weather> getWeather() { return weather; }
    public Main getMain() { return main; }
    public Integer getVisibility() { return visibility; }
    public Sys getSys() { return sys; }
    public String getName() { return name; }
    public Integer getId() { return id; }
    public Rain getRain() { return rain; }

    /** Coordonnées (latitude / longitude) */
    public static class Coord {
        private Double lon;
        private Double lat;
        public Double getLon() { return lon; }
        public Double getLat() { return lat; }
    }

    /** Détails textuels de la météo (ex: "clear sky") */
    public static class Weather {
        private String main;
        private String description;
        private String icon;
        public String getMain() { return main; }
        public String getDescription() { return description; }
        public String getIcon() { return icon; }
    }

    /** Données principales (température, pression, humidité...) */
    public static class Main {
        private Double temp;
        @SerializedName("feels_like") private Double feelsLike; // nom différent dans le JSON
        private Double pressure;
        private Integer humidity;
        public Double getTemp() { return temp; }
        public Double getFeelsLike() { return feelsLike; }
        public Double getPressure() { return pressure; }
        public Integer getHumidity() { return humidity; }
    }

    /** Contient notamment le code du pays renvoyé par OWM (ex: "CH") */
    public static class Sys {
        private String country;
        public String getCountry() { return country; }
    }

    /** Section pluie, uniquement présente s’il y a des précipitations */
    public static class Rain {
        @SerializedName("1h") private Double h1; // précipitations sur la dernière heure (mm)
        public Double getH1() { return h1; }
    }
}
