package be.ugent.devops.services.logic.utils;

public interface Constants {
    String HTTP_PORT_PROPERTY = "HTTP_PORT";
    String SECURE_ENDPOINTS_PROPERTY = "SECURE_ENDPOINTS";
    String SECURE_KEY_PROPERTY = "SECURE_KEY";
    int DEFAULT_HTTP_PORT = 8081;
    boolean DEFAULT_SECURE_ENDPOINTS = false;
    //String BONUS_CODE_PROPERTY = "BONUS_CODE";

    String BASEMOVE_ENDPOINT = "/moves/base";
    String UNITMOVE_ENDPOINT = "/moves/unit";
    String STATS_ENDPOINT = "/stats";

    String SECURE_KEY_HEADER = "X-SECURE-KEY";

    String HINTS_POIS_ENDPOINT = "/hints/pois";
}
