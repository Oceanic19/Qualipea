package com.hcdc.qualipea;

/** One kind of defect a user can tick on the selection screen. */
public class DefectType {
    public final String id;
    public final String name;
    public final String summary;   // one-line description shown in the checklist
    public final String lookFor;   // "what to look for", shown in the example popup
    public final String[] steps;   // mini procedure shown in the recommendations
    public final int imageRes;     // example image

    public DefectType(String id, String name, String summary, String lookFor,
                       String[] steps, int imageRes) {
        this.id = id;
        this.name = name;
        this.summary = summary;
        this.lookFor = lookFor;
        this.steps = steps;
        this.imageRes = imageRes;
    }
}
