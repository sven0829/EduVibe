package com.eduvibe.util;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * com.eduvibe.util.ContentFilter  (NEW FILE)
 *
 * Simple language filter for posts and comments (English + Filipino).
 * Matches whole words (so ordinary words that merely contain a bad word are
 * not blocked), and also catches common disguises: capital letters, repeated
 * letters ("fuuuck") and number/symbol swaps ("sh1t", "@ss").
 *
 * To change what is blocked, edit EXACT_WORDS and STEMS below.
 */
public final class ContentFilter {

    public static final String MESSAGE =
            "Your text contains language that is not allowed on EduVibe. "
            + "Please edit it and try again.";

    /** Blocked whole words (lower case, letters only). */
    private static final Set<String> EXACT_WORDS = Set.of(
            "fuck", "fucks", "fucked", "fucker", "fucking", "motherfucker",
            "shit", "shits", "shitty", "bullshit",
            "bitch", "bitches", "asshole", "bastard", "dick", "cunt",
            "slut", "whore", "nigger", "nigga", "faggot", "retard",
            "puta", "putang", "putangina", "tangina", "tanginamo", "gago",
            "gaga", "ulol", "bobo", "tarantado", "pakyu", "pakshet",
            "pucha", "leche", "kantot", "tite", "puki", "pekpek", "burat",
            "hayop", "inutil", "siraulo", "bwisit", "buwisit"
    );

    /** Blocked word beginnings (catches variations such as "fuckwit"). */
    private static final String[] STEMS = {"fuck", "putang", "tangina", "motherf"};

    private ContentFilter() {
    }

    /** Returns the first blocked word found, or empty when the text is fine. */
    public static Optional<String> findBlocked(String text) {

        if (text == null || text.isBlank()) {
            return Optional.empty();
        }

        String normalized = normalize(text);

        for (String token : normalized.split("\\s+")) {

            if (token.isEmpty()) {
                continue;
            }

            for (String candidate : new String[]{token, squeeze(token)}) {
                if (EXACT_WORDS.contains(candidate)) {
                    return Optional.of(candidate);
                }
                for (String stem : STEMS) {
                    if (candidate.startsWith(stem)) {
                        return Optional.of(candidate);
                    }
                }
            }
        }

        return Optional.empty();
    }

    public static boolean isClean(String text) {
        return findBlocked(text).isEmpty();
    }

    /** Lower case, swap look-alike symbols for letters, everything else becomes a space. */
    static String normalize(String text) {

        StringBuilder sb = new StringBuilder(text.length());

        for (char c : text.toLowerCase(Locale.ROOT).toCharArray()) {
            switch (c) {
                case '0' -> sb.append('o');
                case '1' -> sb.append('i');
                case '3' -> sb.append('e');
                case '4', '@' -> sb.append('a');
                case '5', '$' -> sb.append('s');
                case '7' -> sb.append('t');
                default -> sb.append(Character.isLetter(c) ? c : ' ');
            }
        }
        return sb.toString();
    }

    /** "fuuuck" -> "fuck": collapses runs of the same letter into one. */
    static String squeeze(String token) {

        StringBuilder sb = new StringBuilder(token.length());
        char last = 0;

        for (char c : token.toCharArray()) {
            if (c != last) {
                sb.append(c);
                last = c;
            }
        }
        return sb.toString();
    }
}
