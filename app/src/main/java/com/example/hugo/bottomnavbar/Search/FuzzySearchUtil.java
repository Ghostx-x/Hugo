package com.example.hugo.bottomnavbar.Search;

public class FuzzySearchUtil {

    // Calculate Levenshtein distance between two strings
    public static int levenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];

        for (int i = 0; i <= s1.length(); i++) {
            for (int j = 0; j <= s2.length(); j++) {
                if (i == 0) {
                    dp[i][j] = j;
                } else if (j == 0) {
                    dp[i][j] = i;
                } else {
                    int cost = s1.charAt(i - 1) == s2.charAt(j - 1) ? 0 : 1;
                    dp[i][j] = Math.min(
                            Math.min(dp[i - 1][j] + 1,     // deletion
                                    dp[i][j - 1] + 1),    // insertion
                            dp[i - 1][j - 1] + cost);       // substitution
                }
            }
        }

        return dp[s1.length()][s2.length()];
    }

    // Determine if a fuzzy match is close enough (max 2 edits)
    public static boolean isFuzzyMatch(String input, String target) {
        if (input == null || target == null) return false;

        input = input.toLowerCase().trim();
        target = target.toLowerCase().trim();

        // Quick match shortcut
        if (target.contains(input) || input.contains(target)) return true;

        int distance = levenshteinDistance(input, target);
        return distance <= 2; // allow up to 2 character edits
    }

}

