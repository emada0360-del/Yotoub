package com.yttv.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import org.schabi.newpipe.extractor.InfoItem;
import org.schabi.newpipe.extractor.NewPipe;
import org.schabi.newpipe.extractor.ServiceList;
import org.schabi.newpipe.extractor.StreamingService;
import org.schabi.newpipe.extractor.search.SearchInfo;
import org.schabi.newpipe.extractor.stream.StreamInfoItem;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {
    private final List<StreamInfoItem> results = new ArrayList<>();
    private ArrayAdapter<String> adapter;
    private TextView status;
    private EditText input;
    private ListView list;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        NewPipe.init(new DownloaderImpl());

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(48, 32, 48, 32);
        root.setBackgroundColor(0xFF121212);

        input = new EditText(this);
        input.setHint("ابحث في يوتيوب ثم اضغط بحث");
        input.setTextColor(0xFFFFFFFF);
        input.setHintTextColor(0xFF888888);
        input.setTextSize(24);
        input.setSingleLine(true);
        input.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        input.setOnEditorActionListener((v, actionId, e) -> {
            search(v.getText().toString());
            return true;
        });
        root.addView(input);

        status = new TextView(this);
        status.setTextColor(0xFFAAAAAA);
        status.setTextSize(18);
        root.addView(status);

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, new ArrayList<String>());
        list = new ListView(this);
        list.setAdapter(adapter);
        list.setOnItemClickListener((p, v, pos, id) -> {
            StreamInfoItem it = results.get(pos);
            Intent i = new Intent(this, PlayerActivity.class);
            i.putExtra("url", it.getUrl());
            startActivity(i);
        });
        root.addView(list, new LinearLayout.LayoutParams(-1, 0, 1f));

        setContentView(root);
        input.requestFocus();
    }

    private void search(String q) {
        if (q.trim().isEmpty()) return;
        status.setText("جاري البحث...");
        new Thread(() -> {
            try {
                StreamingService yt = ServiceList.YouTube;
                SearchInfo info = SearchInfo.getInfo(yt,
                        yt.getSearchQHFactory().fromQuery(q, new ArrayList<String>(), ""));
                List<StreamInfoItem> found = new ArrayList<>();
                for (InfoItem i : info.getRelatedItems()) {
                    if (i instanceof StreamInfoItem) found.add((StreamInfoItem) i);
                }
                runOnUiThread(() -> show(found));
            } catch (Exception e) {
                runOnUiThread(() -> status.setText("خطأ: " + e));
            }
        }).start();
    }

    private void show(List<StreamInfoItem> found) {
        results.clear();
        results.addAll(found);
        adapter.clear();
        for (StreamInfoItem s : found) {
            adapter.add(s.getName() + "\n" + s.getUploaderName());
        }
        status.setText(found.isEmpty() ? "ما فيه نتائج" : found.size() + " نتيجة");
        if (!found.isEmpty()) list.requestFocus();
    }
}
