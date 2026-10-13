package com.hcdc.qualipea;

/**
 * Shared Intent extra keys for passing scan-result data between
 * screens.
 */
public final class ScanKeys {
    public static final String TITLE = "extra_title";
    public static final String SELECTED_DEFECTS = "extra_selected_defects";
    public static final String DATE = "extra_date";
    public static final String TOTAL = "extra_total";
    public static final String GOOD = "extra_good";          // = "normal" from Pi JSON
    public static final String DAMAGED = "extra_damaged";    // legacy mock field (Recommendations screen)
    public static final String MOLDY = "extra_moldy";        // legacy mock field (Recommendations screen)
    public static final String PRICE = "extra_price";        // legacy mock field, no formula yet

    // Real Pi /scan response fields
    public static final String DEFECT = "extra_defect";
    public static final String DEFECT_RATE = "extra_defect_rate";
    public static final String GRADE = "extra_grade";
    public static final String INFERENCE_TIME = "extra_inference_time";

    // Firestore-related
    public static final String ALREADY_SAVED = "extra_already_saved";
    public static final String DOC_ID = "extra_doc_id";

    public static final String SCAN_NUMBER = "extra_scan_number";

    private ScanKeys() {}
}
