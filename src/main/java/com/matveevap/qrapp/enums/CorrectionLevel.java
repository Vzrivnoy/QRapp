package com.matveevap.qrapp.enums;

public enum CorrectionLevel {
    L(0),
    M(1),
    Q(2),
    H(3);

    private final int index;

    CorrectionLevel(int levelIndex) {
        this.index = levelIndex;
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

    public static int getIndex(CorrectionLevel level) {
        return level.index;
    }
}
