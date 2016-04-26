package ru.georgeee.bachelor.yarn.imagenet.downloader;

import org.apache.http.Header;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolException;
import org.apache.http.impl.client.DefaultRedirectStrategy;
import org.apache.http.protocol.HttpContext;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

class ImagenetDownloaderRedirectStrategy extends DefaultRedirectStrategy {
    private static final List<Pattern> deniedUrlPatterns = Collections.singletonList(
            Pattern.compile("photo_unavailable\\.png$")
    );

    @Override
    public boolean isRedirected(HttpRequest request, HttpResponse response, HttpContext context) throws ProtocolException {
        if (!super.isRedirected(request, response, context)) {
            return false;
        }
        final Header locationHeader = response.getFirstHeader("location");
        String loc = locationHeader.getValue();
        for (Pattern pattern : deniedUrlPatterns) {
            if (pattern.matcher(loc).find()) {
                return false;
            }
        }
        return true;
    }
}
