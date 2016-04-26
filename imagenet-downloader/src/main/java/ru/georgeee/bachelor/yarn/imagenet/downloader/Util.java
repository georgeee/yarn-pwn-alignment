package ru.georgeee.bachelor.yarn.imagenet.downloader;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

class Util {
    private Util() {
    }

    private static final Pattern IMAGE_EXT_PATTERN = Pattern.compile("\\.(png|jpg|gif|jpeg)$");

    static String addExtensionFromUrl(String part, String url) {
        Matcher m = IMAGE_EXT_PATTERN.matcher(url);
        if (m.find()) {
            String ext = m.group(1);
            return part + "." + ext;
        }
        return part;
    }
}
