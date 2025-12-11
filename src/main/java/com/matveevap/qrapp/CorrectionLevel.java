package com.matveevap.qrapp;

public enum CorrectionLevel {
    L("Low", 1),
    M("Medium", 2),
    Q("Quartile", 3),
    H("High", 4);

    private final String name;
    private final int levelCode;

    CorrectionLevel(String name, int levelCode) {
        this.name = name;
        this.levelCode = levelCode;
    }

    public static CorrectionLevel fromInt(int level) throws NumberFormatException{
        return switch (level) {
            case 1 -> L;
            case 2 -> M;
            case 3 -> Q;
            case 4 -> H;
            default -> throw new NumberFormatException(
                    "Correction level must be 1 (L), 2 (M), 3 (Q), or 4 (H), got: " + level);
        };
    }

    public static int toInt(CorrectionLevel level) {
        return level.levelCode;
    }
}
