package com.example.skyblockusd;

public enum DisplayLayout {
    BRACKETS("Brackets", " [", " | ", "]"),
    PARENTHESES("Parentheses", " (", " | ", ")"),
    INLINE("Inline bars", " | ", " | ", ""),
    EQUALS("Equals", " = ", " = ", "");

    public final String label, firstSeparator, separator, end;
    DisplayLayout(String label, String firstSeparator, String separator, String end) {
        this.label = label; this.firstSeparator = firstSeparator; this.separator = separator; this.end = end;
    }
    public DisplayLayout next() { return values()[(ordinal() + 1) % values().length]; }
}
