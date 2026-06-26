package com.infotech.wishmaplus.reels.bottomsheet;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Drafts Bottom Sheet
 * Shows saved drafts stored in SharedPreferences as JSON
 */
public class DraftsBottomSheet extends BottomSheetDialogFragment {

    private static final String PREFS_KEY = "reel_drafts";
    private static final String DRAFTS_JSON_KEY = "drafts_json";

    public interface DraftSelectedListener {
        void onDraftSelected(DraftModel draft);
        void onDraftDeleted(DraftModel draft);
    }

    public static class DraftModel {
        public String id;
        public String title;
        public List<String> mediaPaths;
        public long createdAt;
        public String thumbnailPath;
        public int mediaCount;

        public DraftModel() {
            mediaPaths = new ArrayList<>();
        }

        public JSONObject toJson() throws JSONException {
            JSONObject obj = new JSONObject();
            obj.put("id", id);
            obj.put("title", title);
            obj.put("created_at", createdAt);
            obj.put("thumbnail", thumbnailPath != null ? thumbnailPath : "");
            obj.put("media_count", mediaCount);
            JSONArray arr = new JSONArray();
            for (String p : mediaPaths) arr.put(p);
            obj.put("media_paths", arr);
            return obj;
        }

        public static DraftModel fromJson(JSONObject obj) throws JSONException {
            DraftModel d = new DraftModel();
            d.id = obj.getString("id");
            d.title = obj.optString("title", "Untitled Draft");
            d.createdAt = obj.optLong("created_at", 0);
            d.thumbnailPath = obj.optString("thumbnail", null);
            d.mediaCount = obj.optInt("media_count", 0);
            JSONArray arr = obj.optJSONArray("media_paths");
            if (arr != null) {
                for (int i = 0; i < arr.length(); i++) {
                    d.mediaPaths.add(arr.getString(i));
                }
            }
            return d;
        }
    }

    // ── Static helpers to save/load drafts ───────────────────────────────────

    public static void saveDraft(Context ctx, DraftModel draft) {
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS_KEY, Context.MODE_PRIVATE);
        List<DraftModel> existing = loadDrafts(ctx);

        // Update if same id, else prepend
        boolean found = false;
        for (int i = 0; i < existing.size(); i++) {
            if (existing.get(i).id.equals(draft.id)) {
                existing.set(i, draft);
                found = true;
                break;
            }
        }
        if (!found) existing.add(0, draft);

        JSONArray arr = new JSONArray();
        for (DraftModel d : existing) {
            try { arr.put(d.toJson()); } catch (JSONException ignored) {}
        }
        prefs.edit().putString(DRAFTS_JSON_KEY, arr.toString()).apply();
    }

    public static List<DraftModel> loadDrafts(Context ctx) {
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS_KEY, Context.MODE_PRIVATE);
        String json = prefs.getString(DRAFTS_JSON_KEY, "[]");
        List<DraftModel> list = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                list.add(DraftModel.fromJson(arr.getJSONObject(i)));
            }
        } catch (JSONException ignored) {}
        return list;
    }

    public static void deleteDraft(Context ctx, String draftId) {
        List<DraftModel> existing = loadDrafts(ctx);
        existing.removeIf(d -> d.id.equals(draftId));
        JSONArray arr = new JSONArray();
        for (DraftModel d : existing) {
            try { arr.put(d.toJson()); } catch (JSONException ignored) {}
        }
        ctx.getSharedPreferences(PREFS_KEY, Context.MODE_PRIVATE)
                .edit().putString(DRAFTS_JSON_KEY, arr.toString()).apply();
    }

    // ─────────────────────────────────────────────────────────────────────────

    private DraftSelectedListener listener;
    private DraftAdapter adapter;
    private List<DraftModel> drafts;

    public static DraftsBottomSheet newInstance() {
        return new DraftsBottomSheet();
    }

    public void setListener(DraftSelectedListener listener) {
        this.listener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NORMAL, com.google.android.material.R.style.Theme_Material3_Dark_BottomSheetDialog);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFF1A1A1A);
        root.setPadding(dp(16), dp(20), dp(16), dp(40));

        // Header row
        LinearLayout headerRow = new LinearLayout(requireContext());
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams hrLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        hrLp.bottomMargin = dp(16);
        headerRow.setLayoutParams(hrLp);

        TextView title = new TextView(requireContext());
        title.setText("Drafts");
        title.setTextColor(0xFFFFFFFF);
        title.setTextSize(18f);
        headerRow.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        root.addView(headerRow);

        // Load drafts
        drafts = loadDrafts(requireContext());

        if (drafts.isEmpty()) {
            // Empty state
            TextView empty = new TextView(requireContext());
            empty.setText("No drafts saved yet.\nYour in-progress reels will appear here.");
            empty.setTextColor(0x88FFFFFF);
            empty.setTextSize(14f);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, dp(40), 0, dp(40));
            root.addView(empty);
        } else {
            TextView countLabel = new TextView(requireContext());
            countLabel.setText(drafts.size() + " draft" + (drafts.size() != 1 ? "s" : ""));
            countLabel.setTextColor(0x66FFFFFF);
            countLabel.setTextSize(12f);
            LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            clp.bottomMargin = dp(12);
            countLabel.setLayoutParams(clp);
            root.addView(countLabel);

            RecyclerView recycler = new RecyclerView(requireContext());
            recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
            adapter = new DraftAdapter(drafts, (draft, delete) -> {
                if (delete) {
                    deleteDraft(requireContext(), draft.id);
                    drafts.remove(draft);
                    adapter.notifyDataSetChanged();
                    if (listener != null) listener.onDraftDeleted(draft);
                    Toast.makeText(requireContext(), "Draft deleted", Toast.LENGTH_SHORT).show();
                } else {
                    if (listener != null) listener.onDraftSelected(draft);
                    dismiss();
                }
            });
            recycler.setAdapter(adapter);
            root.addView(recycler);
        }

        return root;
    }

    private int dp(int val) {
        return (int) (val * requireContext().getResources().getDisplayMetrics().density);
    }

    // ── Adapter ───────────────────────────────────────────────────────────────

    private class DraftAdapter extends RecyclerView.Adapter<DraftAdapter.VH> {
        private final List<DraftModel> items;
        private final DraftActionCallback cb;

        DraftAdapter(List<DraftModel> items, DraftActionCallback cb) {
            this.items = items;
            this.cb = cb;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LinearLayout row = new LinearLayout(requireContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, dp(10), 0, dp(10));

            // Thumbnail
            ImageView thumb = new ImageView(requireContext());
            int size = dp(56);
            android.graphics.drawable.GradientDrawable thumbBg = new android.graphics.drawable.GradientDrawable();
            thumbBg.setColor(0xFF333333);
            thumbBg.setCornerRadius(dp(8));
            thumb.setBackground(thumbBg);
            thumb.setScaleType(ImageView.ScaleType.CENTER_CROP);
            thumb.setClipToOutline(true);
            thumb.setOutlineProvider(android.view.ViewOutlineProvider.BACKGROUND);
            LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(size, size);
            tlp.setMargins(0, 0, dp(12), 0);
            thumb.setLayoutParams(tlp);

            // Info column
            LinearLayout info = new LinearLayout(requireContext());
            info.setOrientation(LinearLayout.VERTICAL);

            TextView nameView = new TextView(requireContext());
            nameView.setTextColor(0xFFFFFFFF);
            nameView.setTextSize(14f);

            TextView timeView = new TextView(requireContext());
            timeView.setTextColor(0x66FFFFFF);
            timeView.setTextSize(11f);

            TextView countView = new TextView(requireContext());
            countView.setTextColor(0x88FFFFFF);
            countView.setTextSize(11f);

            info.addView(nameView);
            info.addView(timeView);
            info.addView(countView);

            // Delete button
            TextView deleteBtn = new TextView(requireContext());
            deleteBtn.setText("🗑");
            deleteBtn.setTextSize(18f);
            deleteBtn.setPadding(dp(12), dp(8), dp(4), dp(8));

            row.addView(thumb);
            row.addView(info, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            row.addView(deleteBtn);

            // Tag views for binding
            row.setTag(new Object[]{thumb, nameView, timeView, countView, deleteBtn});

            return new VH(row);
        }

        @SuppressLint("SetTextI18n")
        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            DraftModel d = items.get(position);
            Object[] tags = (Object[]) holder.itemView.getTag();
            ImageView thumb = (ImageView) tags[0];
            TextView nameView = (TextView) tags[1];
            TextView timeView = (TextView) tags[2];
            TextView countView = (TextView) tags[3];
            TextView deleteBtn = (TextView) tags[4];

            nameView.setText(d.title != null && !d.title.isEmpty() ? d.title : "Untitled Draft");
            timeView.setText(formatDate(d.createdAt));
            countView.setText(d.mediaCount + " clip" + (d.mediaCount != 1 ? "s" : ""));

            if (d.thumbnailPath != null && !d.thumbnailPath.isEmpty()) {
                Glide.with(requireContext()).load(d.thumbnailPath).centerCrop().into(thumb);
            } else {
                thumb.setImageDrawable(null);
            }

            holder.itemView.setOnClickListener(v -> cb.onAction(d, false));
            deleteBtn.setOnClickListener(v -> cb.onAction(d, true));
        }

        @Override
        public int getItemCount() { return items.size(); }

        class VH extends RecyclerView.ViewHolder {
            VH(View v) { super(v); }
        }
    }

    @SuppressLint("SimpleDateFormat")
    private String formatDate(long millis) {
        if (millis == 0) return "Just now";
        long diff = System.currentTimeMillis() - millis;
        if (diff < 60_000) return "Just now";
        if (diff < 3_600_000) return (diff / 60_000) + " min ago";
        if (diff < 86_400_000) return (diff / 3_600_000) + " hr ago";
        return new SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(new Date(millis));
    }

    interface DraftActionCallback {
        void onAction(DraftModel draft, boolean delete);
    }
}
