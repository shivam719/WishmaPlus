package com.infotech.wishmaplus;
import android.annotation.SuppressLint;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.infotech.wishmaplus.R;
import com.infotech.wishmaplus.Api.Response.CategoryResponse;
import com.infotech.wishmaplus.Utils.ApiClient;
import com.infotech.wishmaplus.Utils.EndPointInterface;
import com.infotech.wishmaplus.Utils.PreferencesManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CategoryPickerBottomSheet extends BottomSheetDialogFragment {

    public interface OnCategoriesSelectedListener {
        // selectedIds  → comma-separated e.g. "1,5,12"
        // selectedNames → comma-separated e.g. "Local Business,Cafe,Gym"
        void onSelected(String selectedIds, String selectedNames);
    }

    private static final int MAX_SELECT = 3;

    private OnCategoriesSelectedListener listener;
    private PreferencesManager tokenManager;

    // already-selected ids passed in from caller
    private Set<Integer> preSelectedIds = new HashSet<>();

    private List<CategoryResponse> allCategories = new ArrayList<>();
    private List<CategoryResponse> filtered    = new ArrayList<>();
    private CategoryAdapter adapter;

    // ── factory ──────────────────────────────────────────────────────────
    public static CategoryPickerBottomSheet newInstance(String currentIds) {
        CategoryPickerBottomSheet f = new CategoryPickerBottomSheet();
        Bundle b = new Bundle();
        b.putString("currentIds", currentIds == null ? "" : currentIds);
        f.setArguments(b);
        return f;
    }

    public void setOnCategoriesSelectedListener(OnCategoriesSelectedListener l) {
        this.listener = l;
    }

    // ─────────────────────────────────────────────────────────────────────
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        tokenManager = new PreferencesManager(requireContext(), 1);

        // parse pre-selected ids
        if (getArguments() != null) {
            String raw = getArguments().getString("currentIds", "");
            if (raw != null && !raw.trim().isEmpty()) {
                for (String part : raw.split(",")) {
                    try { preSelectedIds.add(Integer.parseInt(part.trim())); }
                    catch (NumberFormatException ignored) {}
                }
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_category_picker, container, false);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        EditText etSearch   = view.findViewById(R.id.et_search_category);
        RecyclerView rv     = view.findViewById(R.id.rv_categories);
        TextView tvDone     = view.findViewById(R.id.tv_done);
        TextView tvCount    = view.findViewById(R.id.tv_selected_count);

        adapter = new CategoryAdapter(filtered, preSelectedIds, MAX_SELECT,
                count -> tvCount.setText(count + "/" + MAX_SELECT + " selected"));

        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        rv.setAdapter(adapter);

        // search filter
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s,int a,int b,int c) {}
            @Override public void onTextChanged(CharSequence s,int a,int b,int c) {
                filterList(s.toString().trim());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        // done button
        tvDone.setOnClickListener(v -> {
            Set<Integer> sel = adapter.getSelectedIds();
            if (sel.isEmpty()) {
                Toast.makeText(requireContext(), "Select at least 1 category",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            StringBuilder ids   = new StringBuilder();
            StringBuilder names = new StringBuilder();
            for (CategoryResponse cat : allCategories) {
                if (sel.contains(cat.getCategoryID())) {
                    if (ids.length() > 0) { ids.append(","); names.append(", "); }
                    ids.append(cat.getCategoryID());
                    names.append(cat.getCategoryName());
                }
            }
            if (listener != null) listener.onSelected(ids.toString(), names.toString());
            dismiss();
        });

        fetchCategories(tvCount);
    }

    // ── API call ─────────────────────────────────────────────────────────
    private void fetchCategories(TextView tvCount) {
        EndPointInterface api = ApiClient.getClient().create(EndPointInterface.class);
        api.getPageCategories("Bearer " + tokenManager.getAccessToken())
                .enqueue(new Callback<List<CategoryResponse>>() {
                    @Override
                    public void onResponse(@NonNull Call<List<CategoryResponse>> call,
                                           @NonNull Response<List<CategoryResponse>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            allCategories.clear();
                            allCategories.addAll(response.body());
                            filtered.clear();
                            filtered.addAll(allCategories);
                            if (adapter != null) adapter.notifyDataSetChanged();

                            // reflect pre-selected count
                            int count = adapter.getSelectedIds().size();
                            tvCount.setText(count + "/" + MAX_SELECT + " selected");
                        }
                    }
                    @Override
                    public void onFailure(@NonNull Call<List<CategoryResponse>> call,
                                          @NonNull Throwable t) {
                        Toast.makeText(requireContext(),
                                "Failed to load categories", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void filterList(String query) {
        filtered.clear();
        if (query.isEmpty()) {
            filtered.addAll(allCategories);
        } else {
            String lower = query.toLowerCase();
            for (CategoryResponse c : allCategories) {
                if (c.getCategoryName().toLowerCase().contains(lower))
                    filtered.add(c);
            }
        }
        if (adapter != null) adapter.notifyDataSetChanged();
    }

    // ══════════════════════════════════════════════════════════════════════
    // Inner Adapter
    // ══════════════════════════════════════════════════════════════════════
    static class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.VH> {

        interface CountCallback { void onCountChanged(int count); }

        private final List<CategoryResponse> list;
        private final Set<Integer> selectedIds;
        private final int maxSelect;
        private final CountCallback cb;

        CategoryAdapter(List<CategoryResponse> list, Set<Integer> preSelected,
                        int maxSelect, CountCallback cb) {
            this.list        = list;
            this.selectedIds = new HashSet<>(preSelected);
            this.maxSelect   = maxSelect;
            this.cb          = cb;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_category_picker, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            CategoryResponse cat = list.get(pos);
            h.tvName.setText(cat.getCategoryName());

            boolean checked = selectedIds.contains(cat.getCategoryID());
            h.tvCheck.setVisibility(checked ? View.VISIBLE : View.INVISIBLE);
            h.itemView.setAlpha(checked ? 1f :
                    (selectedIds.size() >= maxSelect ? 0.45f : 1f));

            h.itemView.setOnClickListener(v -> {
                if (selectedIds.contains(cat.getCategoryID())) {
                    selectedIds.remove(cat.getCategoryID());
                } else {
                    if (selectedIds.size() >= maxSelect) {
                        Toast.makeText(v.getContext(),
                                "You can select max " + maxSelect + " categories",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    selectedIds.add(cat.getCategoryID());
                }
                notifyDataSetChanged();
                cb.onCountChanged(selectedIds.size());
            });
        }

        @Override public int getItemCount() { return list.size(); }

        Set<Integer> getSelectedIds() { return selectedIds; }

        static class VH extends RecyclerView.ViewHolder {
            TextView tvName, tvCheck;
            VH(@NonNull View v) {
                super(v);
                tvName  = v.findViewById(R.id.tv_category_name);
                tvCheck = v.findViewById(R.id.tv_check);
            }
        }
    }
}