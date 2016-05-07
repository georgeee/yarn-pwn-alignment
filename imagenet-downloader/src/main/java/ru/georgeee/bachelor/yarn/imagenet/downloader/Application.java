package ru.georgeee.bachelor.yarn.imagenet.downloader;


import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.config.SocketConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import ru.georgeee.bachelor.yarn.app.AppUtils;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.xml.bind.JAXBException;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@SpringBootApplication(scanBasePackages = {"ru.georgeee.bachelor.yarn"})
public class Application implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(Application.class);
    private static final Pattern WN_ID_PATTERN = Pattern.compile("^SID-([0-9]+)-([A-Za-z]+)$");
    private static final String IMAGENET_GET_URLS_BASE = "http://www.image-net.org/api/text/imagenet.synset.geturls.getmapping?wnid=";
    private CloseableHttpClient httpClient;
    @Value("${input}")
    private String inputFilePathString;
    @Value("${imagenet.imagesDir}")
    private String imagesDirPathString;
    @Value("${imagenet.downloader.soTimeout:1000}")
    private int soTimeout;
    @Value("${imagenet.mapping_30_31}")
    private String imagenetIdMappingPathString;
    @Value("${limit:15}")
    private int limit;
    @Value("${imagenet.downloader.retryTimeout:1000}")
    private int retryTimeout;
    @Value("${imagenet.downloader.threads:8}")
    private int threadCount;
    @Value("${imagenet.downloader.mainExecutorCount:2}")
    private int mainExecutorCount;

    private Map<String, String> imagenetIdMapping;
    private Path imagesDirPath;
    private Path inputFilePath;
    private ScheduledExecutorService executorService;

    private Queue<String> idQueue = new ConcurrentLinkedQueue<>();

    public static void main(String[] args) throws Exception {
        SpringApplication application = new SpringApplication(Application.class);
        application.setApplicationContextClass(AnnotationConfigApplicationContext.class);
        SpringApplication.run(Application.class, args);
    }

    @PostConstruct
    private void init() throws IOException, JAXBException {
        imagesDirPath = Paths.get(this.imagesDirPathString);
        inputFilePath = Paths.get(this.inputFilePathString);
        Files.createDirectories(imagesDirPath);
        if (!Files.isRegularFile(inputFilePath)) {
            throw new IllegalArgumentException("File " + inputFilePathString + " is not a regular file");
        }
        imagenetIdMapping = Collections.unmodifiableMap(AppUtils.loadIdMapping(Paths.get(imagenetIdMappingPathString), true));
        PoolingHttpClientConnectionManager connManager = new PoolingHttpClientConnectionManager();
        connManager.setMaxTotal(threadCount * 4);
        connManager.setDefaultSocketConfig(SocketConfig.custom().setSoTimeout(soTimeout).build());
        httpClient = HttpClientBuilder.create()
                .setRedirectStrategy(new ImagenetDownloaderRedirectStrategy())
                .setConnectionManager(connManager)
                .build();
        executorService = Executors.newScheduledThreadPool(threadCount);
        latch = new CountDownLatch(mainExecutorCount);
    }

    //    @PreDestroy
//    private void close() throws IOException {
//        httpClient.close();
//    }
    private CountDownLatch latch;

    @Override
    public void run(String... args) throws IOException {
        Files.lines(inputFilePath)
                .map(String::trim)
                .filter(StringUtils::isNotEmpty)
                .filter(imagenetIdMapping::containsKey)
                .forEach(idQueue::add);

        for (int i = 0; i < mainExecutorCount; ++i) {
            executorService.submit(this::processIds);
        }
        try {
            latch.await();
        } catch (InterruptedException e) {
            log.warn("Main thread interrupted", e);
        }
        log.info("All url lists processed");
    }

    private void processIds() {
        while (!idQueue.isEmpty()) {
            String id;
            try {
                id = idQueue.remove();
            } catch (NoSuchElementException e) {
                break;
            }
            log.info("Getting url list for {}", id);
            String wn30Id = imagenetIdMapping.get(id);
            List<String> urls;
            try {
                urls = getUrlList(wn30Id);
            } catch (IOException | IllegalArgumentException e) {
                log.warn("Error loading urls for id {} ({})", id, wn30Id, e);
                idQueue.add(id);
                executorService.schedule(this::processIds, retryTimeout, TimeUnit.MILLISECONDS);
                return;
            }
            try {
                new ImageDownloader(id, urls).download();
            } catch (Exception e) {
                log.warn("Error processing id {}", id, e);
            }
        }
        latch.countDown();
    }

    private boolean tryDownload(Path idImagesDirPath, String l) {
        log.info("Downloading {} to {}", l, idImagesDirPath);
        boolean res = tryDownloadImpl(idImagesDirPath, l);
        log.info("Downloading {} to {}: result {}", l, idImagesDirPath, res);
        return res;
    }

    private boolean tryDownloadImpl(Path idImagesDirPath, String l) {
        String[] parts = l.trim().split("\\s+", 2);
        if (parts.length < 2) {
            return false;
        }
        String url = parts[1];
        String fname = Util.addExtensionFromUrl(parts[0], url);
        Path filePath = idImagesDirPath.resolve(fname);
        Path tmpFilePath = idImagesDirPath.resolve(fname + ".tmp");
        if (Files.exists(filePath)) {
            return true;
        }
        HttpGet httpGet = new HttpGet(url);
        try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode < 200 || statusCode >= 300) {
                return false;
            }
            try (BufferedOutputStream bis = new BufferedOutputStream(new FileOutputStream(tmpFilePath.toFile()))) {
                response.getEntity().writeTo(bis);
            } catch (IOException e) {
                log.error("Error saving {} to {}", url, tmpFilePath, e);
                return false;
            }
        } catch (IllegalArgumentException | IOException e) {
            log.debug("Error getting url {}", url, e);
            return false;
        }
        try {
            Files.move(tmpFilePath, filePath);
        } catch (IOException e) {
            log.error("Error moving {} to {}", tmpFilePath, filePath, e);
        }
        return true;
    }


    private List<String> getUrlList(String wn30Id) throws IOException {
        Matcher m = WN_ID_PATTERN.matcher(wn30Id);
        if (m.find()) {
            String nums = m.group(1);
            String pos = m.group(2);
            String formattedId = pos.toLowerCase() + nums;
            HttpGet httpGet = new HttpGet(IMAGENET_GET_URLS_BASE + formattedId);
            InputStream is = httpClient.execute(httpGet).getEntity().getContent();
            List<String> urls = IOUtils.readLines(is);
            if (urls.isEmpty() || (urls.size() == 1 && urls.get(0).equals("Invalid url!"))) {
                throw new IllegalArgumentException("Id not found in imagenet: " + wn30Id);
            }
            return urls;
        } else {
            throw new IllegalArgumentException("Wrong wn3.0 id passed: " + wn30Id);
        }
    }

    private class ImageDownloader {
        final String id;
        final List<String> urlDescs;
        final Object monitor = new Object();
        final int limit_;
        int urlsTaken;

        ImageDownloader(String id, List<String> urlDescs) {
            this.id = id;
            this.urlDescs = urlDescs;
            limit_ = Math.min(urlDescs.size(), limit);
            urlsTaken = limit_;
        }

        void download() throws IOException {
            Path idImagesDirPath = imagesDirPath.resolve(id);
            Files.createDirectories(idImagesDirPath);
            for (int i = 0; i < limit_; ++i) {
                int i_ = i;
                executorService.submit(() -> {
                    boolean success = tryDownload(idImagesDirPath, urlDescs.get(i_));
                    while (!success) {
                        String ud;
                        int i2;
                        synchronized (monitor) {
                            i2 = urlsTaken++;
                        }
                        if (i2 >= urlDescs.size()) {
                            break;
                        }
                        ud = urlDescs.get(i2);
                        success = tryDownload(idImagesDirPath, ud);
                    }
                    log.debug("Download task for {}: exiting...", id);
                });
            }
        }

    }
}