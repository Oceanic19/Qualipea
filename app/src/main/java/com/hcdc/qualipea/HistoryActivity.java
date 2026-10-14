package com.hcdc.qualipea;

import android.animation.ObjectAnimator;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.content.res.ColorStateList;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * History: filter chips (All, Today, ••• sheet with more filters), numbered pagination
 * (10 per page), confirm-before-delete modal, skeleton loading, and cards that fade in
 * as they scroll into view.
 */
public class HistoryActivity extends AppCompatActivity {

    private static final int PAGE = 10;
    private static final long MIN_SKELETON_MS = 600;

    /** One filter (or sort) at a time. */
    private enum Filter { ALL, TODAY, WEEK, MONTH, OLDEST, EXTRA, CLASS1, CLASS2, REJECT }

    private final List<ScanRecord> records = new ArrayList<>();   // newest first (from Room)
    private Filter activeFilter = Filter.ALL;
    private String uid;

    private ScrollView scroll;
    private LinearLayout scanListContainer;
    private View emptyState;
    private TextView tvEmptyMessage;
    private MaterialButton btnEmptyAction;
    private TextView chipAll, chipToday, chipMore;

    private int currentPage = 1;
    private boolean animateNext = true;     // next render fades cards in on scroll

    private boolean skeletonShowing = false;
    private ObjectAnimator skeletonPulse;
    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private long startMillis;

    private final SimpleDateFormat dateFmt =
            new SimpleDateFormat("MMMM d, yyyy \u2022 h:mm a", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        uid = FirebaseAuth.getInstance().getUid();

        scroll = findViewById(R.id.historyScroll);
        scanListContainer = findViewById(R.id.scanListContainer);
        emptyState = findViewById(R.id.emptyState);
        tvEmptyMessage = findViewById(R.id.tvEmptyMessage);
        btnEmptyAction = findViewById(R.id.btnStartFirstScan);
        chipAll = findViewById(R.id.chipAll);
        chipToday = findViewById(R.id.chipToday);
        chipMore = findViewById(R.id.chipMore);

        chipAll.setOnClickListener(v -> selectFilter(Filter.ALL));
        chipToday.setOnClickListener(v -> selectFilter(Filter.TODAY));
        chipMore.setOnClickListener(v -> showFilterSheet());
        updateChips();

        // Cards (and the page bar) fade in as they scroll into view.
        scroll.setOnScrollChangeListener((v, x, y, ox, oy) -> revealVisible());

        NavHelper.setup(this, findViewById(R.id.bottomNav), NavHelper.Tab.HISTORY);

        startMillis = System.currentTimeMillis();
        showSkeleton();
        observeScans();
    }

    @Override
    protected void onResume() {
        super.onResume();
        SyncScheduler.syncNow(this);
    }

    @Override
    protected void onDestroy() {
        uiHandler.removeCallbacksAndMessages(null);
        if (skeletonPulse != null) skeletonPulse.cancel();
        super.onDestroy();
    }

    private float dp(int v) {
        return v * getResources().getDisplayMetrics().density;
    }

    // ------------------------------------------------------------------
    // Data
    // ------------------------------------------------------------------

    private void observeScans() {
        if (uid == null) {
            finishSkeletonThenRender(0);
            return;
        }

        AppDatabase.get(this).scanDao().observeForUser(uid).observe(this, entities -> {
            records.clear();
            for (ScanEntity e : entities) {
                records.add(new ScanRecord(e.id, e.scanNumber, e.total, e.good, e.defect, e.defectRate,
                        e.grade != null ? e.grade : "Unknown", e.inferenceTime, e.timestampMillis));
            }

            if (skeletonShowing) {
                uiHandler.removeCallbacksAndMessages(null);
                long elapsed = System.currentTimeMillis() - startMillis;
                finishSkeletonThenRender(Math.max(0, MIN_SKELETON_MS - elapsed));
            } else {
                animateNext = false;          // refresh (delete/sync): no re-animation
                renderList();
            }
        });
    }

    private String scanLabel(ScanRecord r) {
        return String.format(Locale.US, "Scan #%04d", r.scanNumber);
    }

    // ------------------------------------------------------------------
    // Filters
    // ------------------------------------------------------------------

    private String filterLabel(Filter f) {
        switch (f) {
            case TODAY:  return "Today";
            case WEEK:   return "This Week";
            case MONTH:  return "This Month";
            case OLDEST: return "Oldest to Newest";
            case EXTRA:  return "Extra Class";
            case CLASS1: return "Class I";
            case CLASS2: return "Class II";
            case REJECT: return "Reject";
            default:     return "All";
        }
    }

    /** Shorter text for the chip so the row never overflows. */
    private String chipLabel(Filter f) {
        return f == Filter.OLDEST ? "Oldest first" : filterLabel(f);
    }

    private boolean matches(ScanRecord r) {
        switch (activeFilter) {
            case TODAY:  return isToday(r.timestampMillis);
            case WEEK:   return isThisWeek(r.timestampMillis);
            case MONTH:  return isThisMonth(r.timestampMillis);
            case EXTRA:  return "Extra Class".equalsIgnoreCase(r.grade);
            case CLASS1: return "Class I".equalsIgnoreCase(r.grade);
            case CLASS2: return "Class II".equalsIgnoreCase(r.grade);
            case REJECT: return "Reject".equalsIgnoreCase(r.grade);
            default:     return true;   // ALL and OLDEST show everything
        }
    }

    private List<ScanRecord> getFiltered() {
        List<ScanRecord> out = new ArrayList<>();
        for (ScanRecord r : records) {
            if (matches(r)) out.add(r);
        }
        if (activeFilter == Filter.OLDEST) Collections.reverse(out);   // records are newest-first
        return out;
    }

    private boolean isToday(long millis) {
        Calendar a = Calendar.getInstance();
        Calendar b = Calendar.getInstance();
        b.setTimeInMillis(millis);
        return a.get(Calendar.YEAR) == b.get(Calendar.YEAR)
                && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR);
    }

    private boolean isThisWeek(long millis) {
        return System.currentTimeMillis() - millis <= 7L * 24 * 60 * 60 * 1000;
    }

    /** Current calendar month (e.g. everything in October while it is October). */
    private boolean isThisMonth(long millis) {
        Calendar a = Calendar.getInstance();
        Calendar b = Calendar.getInstance();
        b.setTimeInMillis(millis);
        return a.get(Calendar.YEAR) == b.get(Calendar.YEAR)
                && a.get(Calendar.MONTH) == b.get(Calendar.MONTH);
    }

    private void selectFilter(Filter f) {
        activeFilter = f;
        updateChips();
        currentPage = 1;
        animateNext = true;
        renderList();
        scroll.scrollTo(0, 0);
    }

    private void styleChip(TextView chip, boolean selected) {
        chip.setBackgroundResource(selected ? R.drawable.bg_chip_selected : R.drawable.bg_chip_unselected);
        chip.setTextColor(getResources().getColor(selected ? R.color.white : R.color.text_dark));
    }

    private void updateChips() {
        styleChip(chipAll, activeFilter == Filter.ALL);
        styleChip(chipToday, activeFilter == Filter.TODAY);

        boolean moreActive = activeFilter != Filter.ALL && activeFilter != Filter.TODAY;
        styleChip(chipMore, moreActive);
// Grade filters tint the chip with the grade's color. Every other state clears the
// tint, so the chip keeps its normal brand_primary / white look.
        if (moreActive && gradeOf(activeFilter) != null) {
            chipMore.setBackgroundTintList(ColorStateList.valueOf(filterColor(activeFilter)));
        } else {
            chipMore.setBackgroundTintList(null);
        }
        chipMore.setText(moreActive
                ? chipLabel(activeFilter) + "  \u2022\u2022\u2022"
                : "\u2022\u2022\u2022");
        chipMore.setContentDescription(moreActive
                ? "More filters, " + filterLabel(activeFilter) + " selected"
                : "More filters");
    }

    // ------------------------------------------------------------------
    // Filter bottom sheet (the ••• button)
    // ------------------------------------------------------------------

    private void showFilterSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = LayoutInflater.from(this).inflate(R.layout.sheet_history_filter, null);

        LinearLayout time = view.findViewById(R.id.optionsTime);
        LinearLayout sort = view.findViewById(R.id.optionsSort);
        LinearLayout grade = view.findViewById(R.id.optionsGrade);

        addFilterOption(time, dialog, Filter.WEEK, 0);
        addFilterOption(time, dialog, Filter.MONTH, 0);
        addFilterOption(sort, dialog, Filter.OLDEST, 0);
        addFilterOption(grade, dialog, Filter.EXTRA, R.drawable.dot_good);
        addFilterOption(grade, dialog, Filter.CLASS1, R.drawable.dot_good);
        addFilterOption(grade, dialog, Filter.CLASS2, R.drawable.dot_defect);
        addFilterOption(grade, dialog, Filter.REJECT, R.drawable.dot_reject);

        TextView reset = view.findViewById(R.id.tvReset);
        reset.setVisibility(activeFilter == Filter.ALL ? View.INVISIBLE : View.VISIBLE);
        reset.setOnClickListener(v -> {
            dialog.dismiss();
            selectFilter(Filter.ALL);
        });

        dialog.setContentView(view);
        // Let our own rounded background show instead of the default sheet background.
        View frame = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (frame != null) frame.setBackgroundColor(Color.TRANSPARENT);
        dialog.getBehavior().setSkipCollapsed(true);
        dialog.getBehavior().setState(BottomSheetBehavior.STATE_EXPANDED);
        dialog.show();
    }

    private void addFilterOption(LinearLayout parent, BottomSheetDialog dialog, Filter f, int dotRes) {
        View row = LayoutInflater.from(this).inflate(R.layout.item_filter_option, parent, false);
        TextView label = row.findViewById(R.id.tvOptionLabel);
        View dot = row.findViewById(R.id.viewOptionDot);
        ImageView check = row.findViewById(R.id.ivOptionCheck);

        label.setText(filterLabel(f));
        if (dotRes != 0) {
            dot.setBackgroundResource(dotRes);
            dot.setVisibility(View.VISIBLE);
            dot.setBackgroundTintList(ColorStateList.valueOf(filterColor(f)));
        }

        if (activeFilter == f) {
            int accent = filterColor(f);   // grade color for grades, brand_primary for the rest
            row.setBackgroundResource(R.drawable.bg_filter_option_selected);
            label.setTextColor(accent);
            label.setTypeface(label.getTypeface(), Typeface.BOLD);
            check.setImageTintList(ColorStateList.valueOf(accent));
            check.setVisibility(View.VISIBLE);
            row.setSelected(true);
        }

        // Tapping the selected option again clears the filter.
        row.setOnClickListener(v -> {
            dialog.dismiss();
            selectFilter(activeFilter == f ? Filter.ALL : f);
        });
        parent.addView(row);
    }

    // ------------------------------------------------------------------
    // Skeleton loading
    // ------------------------------------------------------------------

    private void showSkeleton() {
        skeletonShowing = true;
        emptyState.setVisibility(View.GONE);
        scanListContainer.setVisibility(View.VISIBLE);
        scanListContainer.removeAllViews();

        LayoutInflater inflater = LayoutInflater.from(this);
        for (int i = 0; i < 4; i++) {
            scanListContainer.addView(
                    inflater.inflate(R.layout.item_scan_skeleton, scanListContainer, false));
        }
        skeletonPulse = ObjectAnimator.ofFloat(scanListContainer, View.ALPHA, 1f, 0.45f);
        skeletonPulse.setDuration(700);
        skeletonPulse.setRepeatCount(ObjectAnimator.INFINITE);
        skeletonPulse.setRepeatMode(ObjectAnimator.REVERSE);
        skeletonPulse.start();
    }

    private void finishSkeletonThenRender(long delayMs) {
        uiHandler.postDelayed(() -> {
            skeletonShowing = false;
            if (skeletonPulse != null) skeletonPulse.cancel();
            scanListContainer.setAlpha(1f);
            currentPage = 1;
            animateNext = true;
            renderList();
        }, delayMs);
    }

    // ------------------------------------------------------------------
    // Rendering + numbered pagination
    // ------------------------------------------------------------------

    private void showEmptyState(boolean noScansAtAll) {
        scanListContainer.setVisibility(View.GONE);
        emptyState.setVisibility(View.VISIBLE);

        if (noScansAtAll) {
            tvEmptyMessage.setText("No grading history yet.");
            btnEmptyAction.setText("Start First Scan");
            btnEmptyAction.setOnClickListener(v ->
                    startActivity(new Intent(this, ScanActivity.class)));
        } else {
            tvEmptyMessage.setText("No scans match this filter.");
            btnEmptyAction.setText("Show all scans");
            btnEmptyAction.setOnClickListener(v -> selectFilter(Filter.ALL));
        }
    }

    private void renderList() {
        scanListContainer.removeAllViews();

        List<ScanRecord> filtered = getFiltered();

        // Nothing to show: no cards and no pagination bar / arrows.
        if (filtered.isEmpty()) {
            showEmptyState(records.isEmpty());
            return;
        }
        scanListContainer.setVisibility(View.VISIBLE);
        emptyState.setVisibility(View.GONE);

        int totalPages = (filtered.size() + PAGE - 1) / PAGE;
        if (currentPage > totalPages) currentPage = totalPages;   // e.g. deleted the last card on the last page
        if (currentPage < 1) currentPage = 1;

        int from = (currentPage - 1) * PAGE;
        int to = Math.min(from + PAGE, filtered.size());

        LayoutInflater inflater = LayoutInflater.from(this);
        for (int i = from; i < to; i++) {
            View item = inflater.inflate(R.layout.item_scan_history, scanListContainer, false);
            bindCard(item, filtered.get(i));
            prepareForReveal(item);
            scanListContainer.addView(item);
        }

        if (totalPages > 1) {
            View bar = buildPaginationBar(inflater, totalPages, filtered.size());
            prepareForReveal(bar);
            scanListContainer.addView(bar);
        }

        // Reveal whatever is already on screen once layout is done.
        scanListContainer.post(this::revealVisible);
        animateNext = false;
    }

    private View buildPaginationBar(LayoutInflater inflater, int totalPages, int totalItems) {
        View bar = inflater.inflate(R.layout.item_pagination, scanListContainer, false);

        TextView first = bar.findViewById(R.id.tvPageFirst);
        TextView prev = bar.findViewById(R.id.tvPagePrev);
        TextView current = bar.findViewById(R.id.tvPageCurrent);
        TextView next = bar.findViewById(R.id.tvPageNext);
        TextView last = bar.findViewById(R.id.tvPageLast);
        TextView info = bar.findViewById(R.id.tvPageInfo);

        current.setText(String.valueOf(currentPage));
        info.setText("Page " + currentPage + " of " + totalPages + " \u2022 " + totalItems + " scans");

        boolean hasPrev = currentPage > 1;
        boolean hasNext = currentPage < totalPages;
        first.setVisibility(hasPrev ? View.VISIBLE : View.GONE);
        prev.setVisibility(hasPrev ? View.VISIBLE : View.GONE);
        next.setVisibility(hasNext ? View.VISIBLE : View.GONE);
        last.setVisibility(hasNext ? View.VISIBLE : View.GONE);

        first.setOnClickListener(v -> goToPage(1));
        prev.setOnClickListener(v -> goToPage(currentPage - 1));
        next.setOnClickListener(v -> goToPage(currentPage + 1));
        last.setOnClickListener(v -> goToPage(totalPages));
        return bar;
    }

    private void goToPage(int page) {
        currentPage = page;
        animateNext = true;
        renderList();
        scroll.scrollTo(0, 0);
    }

    // ------------------------------------------------------------------
    // Fade-in as cards scroll into view
    // ------------------------------------------------------------------

    /** Hides a view until it scrolls into view (or shows it right away when not animating). */
    private void prepareForReveal(View v) {
        if (animateNext) {
            v.setAlpha(0f);
            v.setTranslationY(dp(28));
            v.setTag(null);
        } else {
            v.setTag("shown");
        }
    }

    /** Fades in every not-yet-shown card whose top has entered the visible area. */
    private void revealVisible() {
        if (scroll == null) return;
        int viewBottom = scroll.getScrollY() + scroll.getHeight() - (int) dp(24);
        int k = 0;
        for (int i = 0; i < scanListContainer.getChildCount(); i++) {
            View c = scanListContainer.getChildAt(i);
            if (c.getTag() != null) continue;
            int top = scanListContainer.getTop() + c.getTop();
            if (top < viewBottom) {
                c.setTag("shown");
                c.animate().alpha(1f).translationY(0f)
                        .setStartDelay(k * 80L)
                        .setDuration(450)
                        .setInterpolator(new DecelerateInterpolator())
                        .start();
                k++;
            }
        }
    }

    // ------------------------------------------------------------------
    // Card binding
    // ------------------------------------------------------------------

    private int gradeColor(String grade) {
        if ("Reject".equalsIgnoreCase(grade)) return getResources().getColor(R.color.delete_red);
        if ("Class II".equalsIgnoreCase(grade)) return getResources().getColor(R.color.brand_primary);
        return getResources().getColor(R.color.status_good);
    }

    /** The grade a filter stands for, or null if it isn't a grade filter. */
    private String gradeOf(Filter f) {
        switch (f) {
            case EXTRA:  return "Extra Class";
            case CLASS1: return "Class I";
            case CLASS2: return "Class II";
            case REJECT: return "Reject";
            default:     return null;
        }
    }

    /** Active color: the grade's own color for grade filters, brand_primary for everything else. */
    private int filterColor(Filter f) {
        String g = gradeOf(f);
        return g != null ? gradeColor(g) : getResources().getColor(R.color.brand_primary);
    }

    private int gradeTint(String grade) {
        if ("Reject".equalsIgnoreCase(grade)) return getResources().getColor(R.color.brand_primary);
        if ("Class II".equalsIgnoreCase(grade)) return getResources().getColor(R.color.brand_primary);
        return getResources().getColor(R.color.brand_primary);
    }

    private void bindCard(View item, ScanRecord record) {
        TextView tvTitle = item.findViewById(R.id.tvItemTitle);
        TextView tvDate = item.findViewById(R.id.tvItemDate);
        TextView tvKernels = item.findViewById(R.id.tvPillKernels);
        TextView tvGood = item.findViewById(R.id.tvPillGood);
        TextView tvDefect = item.findViewById(R.id.tvPillDefect);
        TextView tvDetails = item.findViewById(R.id.tvItemDetails);
        View accent = item.findViewById(R.id.viewGradeAccent);
        ImageButton btnDelete = item.findViewById(R.id.btnDelete);

        int color = gradeColor(record.grade);

        tvTitle.setText(record.grade);
        tvTitle.setTextColor(getColor(R.color.white));
        tvTitle.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(color)
        );
        accent.setBackgroundColor(gradeTint(record.grade));

        tvDate.setText(dateFmt.format(record.timestampMillis));
        tvKernels.setText(record.total + " kernels");
        tvGood.setText("Good " + record.good);
        tvDefect.setText("Defect " + record.defect);

        View.OnClickListener open = v -> openDetail(record);
        item.setOnClickListener(open);
        tvDetails.setOnClickListener(open);

        btnDelete.setOnClickListener(v -> confirmDelete(record, item));
        TextView tvScanNumber = item.findViewById(R.id.tvScanNumber);
        tvScanNumber.setText(scanLabel(record));
    }

    // ------------------------------------------------------------------
    // Delete with confirmation
    // ------------------------------------------------------------------

    private void confirmDelete(ScanRecord record, View itemView) {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_confirm_delete);
        dialog.setCanceledOnTouchOutside(true);

        TextView msg = dialog.findViewById(R.id.tvDeleteMessage);
        msg.setText(record.grade + " \u2022 " + record.total + " kernels\n"
                + dateFmt.format(record.timestampMillis)
                + "\n\nThis can't be undone.");

        dialog.findViewById(R.id.btnCancelDelete).setOnClickListener(v -> dialog.dismiss());
        dialog.findViewById(R.id.btnConfirmDelete).setOnClickListener(v -> {
            dialog.dismiss();
            itemView.animate()
                    .alpha(0f)
                    .translationX(dp(80))
                    .setDuration(260)
                    .withEndAction(() -> {
                        ScanRepository.delete(this, uid, record.id);
                        Toast.makeText(this, "Scan deleted", Toast.LENGTH_SHORT).show();
                        // Room notifies observeScans(), which refreshes the list.
                    })
                    .start();
        });

        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.88f);
            dialog.getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    // ------------------------------------------------------------------
    // Details
    // ------------------------------------------------------------------

    private void openDetail(ScanRecord record) {
        Intent intent = new Intent(this, ScanResultsActivity.class);
        intent.putExtra(ScanKeys.ALREADY_SAVED, true);
        intent.putExtra(ScanKeys.DOC_ID, record.id);
        intent.putExtra(ScanKeys.DATE, dateFmt.format(record.timestampMillis));
        intent.putExtra(ScanKeys.TOTAL, record.total);
        intent.putExtra(ScanKeys.GOOD, record.good);
        intent.putExtra(ScanKeys.DEFECT, record.defect);
        intent.putExtra(ScanKeys.DEFECT_RATE, record.defectRate);
        intent.putExtra(ScanKeys.GRADE, record.grade);
        intent.putExtra(ScanKeys.INFERENCE_TIME, record.inferenceTime);
        intent.putExtra(ScanKeys.SCAN_NUMBER, record.scanNumber);
        startActivity(intent);
    }
}