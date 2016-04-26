package ru.georgeee.bachelor.yarn.app;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AppUtils {
    private AppUtils() {
    }

    public static Map<String, String> loadIdMapping(Path mappingFile, boolean reverse) throws IOException {
        Map<String, String> mapping = new HashMap<>();
        Files.lines(mappingFile).forEach(s -> {
            s = s.trim();
            if (!s.isEmpty()) {
                String[] parts = s.split("\\s*:\\s*", 2);
                if (parts.length > 1) {
                    if (reverse) {
                        mapping.put(parts[1], parts[0]);
                    } else {
                        mapping.put(parts[0], parts[1]);
                    }
                }
            }
        });
        return mapping;
    }
}
