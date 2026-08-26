package com.minecraft.launcher.model.rule;

public class OSVersionComparator {

    public static int compare(String v1, String v2) {
        int[] va = parse(v1);
        int[] vb = parse(v2);

        for(int i = 0 ; i < va.length ; i++) {
            int result = Integer.compare(va[i], vb[i]);
            if(result != 0) {
                return result;
            }
        }
        return 0;
    }

    private static int[] parse(String version) {
        String[] parts = version.split("\\.");

        int[] result = new int[3];

        // Compare only major.minor.build.
        // Extra version components are ignored.
        for (int i = 0 ; i < Math.min(parts.length, 3) ; i++) {
            result[i] = Integer.parseInt(parts[i]);
        }
        return result;
    }
}
