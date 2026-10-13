package com.hcdc.qualipea;

import java.util.Arrays;
import java.util.List;

/**
 * The list of defect types users can pick from, plus the mini
 * procedure for each. EDIT THIS FILE to match the defect categories in
 * your paper - names, descriptions and steps all live here.
 *
 * imageRes currently points at placeholder drawings (defect_*.xml).
 * To use real photos from your dataset: add e.g. defect_moldy.jpg to
 * res/drawable (delete the matching defect_moldy.xml first so the
 * names don't clash) - no code change needed.
 */
public final class DefectCatalog {

    public static final List<DefectType> ALL = Arrays.asList(
            new DefectType("moldy", "Moldy",
                    "Fuzzy green, gray or black growth",
                    "Powdery or fuzzy patches, often green, gray or black, sometimes with a musty smell.",
                    new String[]{
                            "Remove moldy kernels right away and keep them away from the good batch.",
                            "Do not eat, sell or feed them to animals. Mold can produce aflatoxin, which cooking does not remove.",
                            "Clean and dry the sorting area and containers, then re-dry the remaining batch before storing."
                    },
                    R.drawable.moldy_kernel),

            new DefectType("damaged", "Broken / Damaged",
                    "Split, cracked or crushed kernel",
                    "Kernels that are split in half, cracked, chipped or crushed.",
                    new String[]{
                            "Separate broken and split kernels from the whole ones.",
                            "Sell or use them separately (for example for peanut butter or oil) instead of mixing them into whole-kernel grades.",
                            "Use them sooner. Broken kernels spoil faster in storage."
                    },
                    R.drawable.broken_kernel),

            new DefectType("discolored", "Discolored",
                    "Dark, brown or stained kernel",
                    "Dark brown or stained patches on the skin or kernel that are not part of the normal color.",
                    new String[]{
                            "Set discolored kernels aside and check a few for a musty or rancid smell.",
                            "If they smell musty or rancid, discard them.",
                            "If they look and smell normal, sell them as a lower grade."
                    },
                    R.drawable.discolored_kernel),

            new DefectType("shriveled", "Shriveled",
                    "Thin, wrinkled, undersized kernel",
                    "Kernels that are noticeably thin, wrinkled or smaller than the rest of the batch.",
                    new String[]{
                            "Sieve or hand-sort to remove shriveled kernels.",
                            "Sell them separately as a lower grade or use them for processing.",
                            "Shriveling is often linked to early harvest or poor growing conditions, so note which field the batch came from."
                    },
                    R.drawable.shriveled_kernel),

            new DefectType("insect", "Insect-damaged",
                    "Small holes, tunnels or powdery dust",
                    "Tiny holes or tunnels in the kernel, sometimes with fine powdery dust (frass) around them.",
                    new String[]{
                            "Isolate damaged kernels and check the rest of the batch for live insects.",
                            "Clean the storage area and keep kernels in clean, sealed containers.",
                            "Store in a cool, dry place and inspect the batch regularly."
                    },
                    R.drawable.pestdamage_kernel)
    );

    public static DefectType byId(String id) {
        for (DefectType d : ALL) {
            if (d.id.equals(id)) return d;
        }
        return null;
    }

    private DefectCatalog() {}
}
