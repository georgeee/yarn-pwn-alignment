package ru.georgeee.bachelor.yarn.dict.ya;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.config.SocketConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.georgeee.bachelor.yarn.core.POS;

import java.io.*;
import java.net.URLEncoder;
import java.util.*;

public class YandexDictEngine implements Closeable {

    public static final int FLAG_FAMILY = 0x0001;//family filter
    public static final int FLAG_SHORT_POS = 0x0002; //search short speech parts
    public static final int FLAG_MORPHO = 0x0004; //enable morphological preprocessing
    public static final int FLAG_POS_FILTER = 0x0008; //require pos of translation to be same as query's pos
    private static final Logger log = LoggerFactory.getLogger(YandexDictEngine.class);
    private static final int SO_TIMEOUT = 5000;
    private static final Gson gson = new GsonBuilder().create();
    private static final String GET_LANGS_URI = "https://dictionary.yandex.net/api/v1/dicservice.json/getLangs?key=%s";
    private static final String TRANSLATE_URI = "https://dictionary.yandex.net/api/v1/dicservice.json/lookup?key=%s&lang=%s&ui=en&flags=%d&text=%s";
    private static final Map<String, POS> POS_MAPPINGS;

    static {
        Map<String, POS> map = new HashMap<>();
        map.put("num", null);
        map.put("adv", POS.ADVERB);
        map.put("part", POS.ADJECTIVE);
        map.put("adj", POS.ADJECTIVE);
        map.put("v", POS.VERB);
        map.put("n", POS.NOUN);
        POS_MAPPINGS = Collections.unmodifiableMap(map);
    }

    private final CloseableHttpClient httpClient;
    private final String apiKey;
    private final int flags;

    public YandexDictEngine(String apiKey) {
        this(apiKey, FLAG_MORPHO | FLAG_SHORT_POS);
    }

    public YandexDictEngine(String apiKey, int flags) {
        this.apiKey = apiKey;
        BasicHttpClientConnectionManager connManager = new BasicHttpClientConnectionManager();
        connManager.setSocketConfig(SocketConfig.custom().setSoTimeout(SO_TIMEOUT).build());
        httpClient = HttpClientBuilder.create()
                .setConnectionManager(connManager)
                .build();
        this.flags = flags;
    }

    private String urlEncode(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

    public LookupResponse translate(String word, String dir) throws IOException {
        log.debug("YaDict: {} ({})", word, dir);
        String uri = String.format(TRANSLATE_URI, urlEncode(apiKey), dir, flags, urlEncode(word));
        HttpGet request = new HttpGet(uri);
        HttpResponse response = httpClient.execute(request);
        try (Reader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()))) {
            return gson.fromJson(reader, LookupResponse.class);
        }
    }

    @SuppressWarnings("unchecked")
    public List<String> getDirections() throws IOException {
        HttpGet request = new HttpGet(String.format(GET_LANGS_URI, apiKey));
        HttpResponse response = httpClient.execute(request);
        Reader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
        return gson.fromJson(reader, List.class);
    }

    static POS determinePOS(String yaPosLabel) {
        if (yaPosLabel == null) return null;
        if (!POS_MAPPINGS.containsKey(yaPosLabel)) {
            log.warn("Unknown yandex POS {}", yaPosLabel);
            return null;
        }
        return POS_MAPPINGS.get(yaPosLabel);
    }

    @Override
    public void close() throws IOException {
        httpClient.close();
    }
}
