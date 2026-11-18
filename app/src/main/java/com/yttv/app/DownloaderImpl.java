package com.yttv.app;

import org.schabi.newpipe.extractor.downloader.Downloader;
import org.schabi.newpipe.extractor.downloader.Request;
import org.schabi.newpipe.extractor.downloader.Response;
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;

public class DownloaderImpl extends Downloader {
    static final String UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:128.0) Gecko/20100101 Firefox/128.0";
    private final OkHttpClient client = new OkHttpClient.Builder()
            .readTimeout(30, TimeUnit.SECONDS)
            .build();

    @Override
    public Response execute(Request request) throws IOException, ReCaptchaException {
        byte[] data = request.dataToSend();
        RequestBody body = data != null ? RequestBody.create(data, (MediaType) null) : null;

        okhttp3.Request.Builder b = new okhttp3.Request.Builder()
                .method(request.httpMethod(), body)
                .url(request.url())
                .addHeader("User-Agent", UA);

        for (Map.Entry<String, List<String>> e : request.headers().entrySet()) {
            List<String> vals = e.getValue();
            if (vals.size() > 1) {
                b.removeHeader(e.getKey());
                for (String v : vals) b.addHeader(e.getKey(), v);
            } else if (vals.size() == 1) {
                b.header(e.getKey(), vals.get(0));
            }
        }

        okhttp3.Response r = client.newCall(b.build()).execute();
        if (r.code() == 429) {
            r.close();
            throw new ReCaptchaException("reCaptcha", request.url());
        }
        ResponseBody rb = r.body();
        String text = rb != null ? rb.string() : null;
        return new Response(r.code(), r.message(), r.headers().toMultimap(),
                text, r.request().url().toString());
    }
}
