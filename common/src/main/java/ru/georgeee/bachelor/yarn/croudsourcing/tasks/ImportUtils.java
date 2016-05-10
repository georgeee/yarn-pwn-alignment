package ru.georgeee.bachelor.yarn.croudsourcing.tasks;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class ImportUtils {
    private static final Gson gson = new GsonBuilder().create();

    public static <T> Map<Integer, T> importFromJson(Path jsonPath, Class<? extends T> tClass) throws IOException {
        Map<Integer, T> results = new HashMap<>();
        try (BufferedReader br = Files.newBufferedReader(jsonPath)) {
            JsonObject jsonResults = gson.fromJson(br, JsonObject.class);
            jsonResults.entrySet().forEach(e -> {
                int taskId = Integer.parseInt(e.getKey());
                T jr = gson.fromJson(e.getValue(), tClass);
                results.put(taskId, jr);
            });
        }
        return results;
    }
}
