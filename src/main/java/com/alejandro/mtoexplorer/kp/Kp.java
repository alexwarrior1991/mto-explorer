package com.alejandro.mtoexplorer.kp;

import java.math.BigDecimal;
import java.util.Locale;

/**
 * Punto kilométrico. Se guarda en metros (17858.5) y se escribe como en los planos (17+858.5).
 */
public final class Kp {

    private Kp() {
    }

    /**
     * 17858.5 → "17+858.5"; 17858 → "17+858"; -77.96 → "-0+077.96".
     */
    public static String format(Double meters) {
        if (meters == null) {
            return null;
        }

        String sign = meters < 0 ? "-" : "";
        BigDecimal abs = BigDecimal.valueOf(Math.abs(meters)).stripTrailingZeros();
        BigDecimal[] kmAndRest = abs.divideAndRemainder(BigDecimal.valueOf(1000));
        String rest = kmAndRest[1].toPlainString();
        int dot = rest.indexOf('.');
        String whole = dot < 0 ? rest : rest.substring(0, dot);
        String decimals = dot < 0 ? "" : rest.substring(dot);
        return String.format(Locale.ROOT, "%s%d+%03d%s", sign, kmAndRest[0].intValue(), Integer.parseInt(whole), decimals);
    }

    /**
     * Acepta "17+858.5", "17+858" o metros a secas ("17858.5").
     */
    public static double parse(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("PK vacío");
        }
        String value = text.trim().replace(',', '.');
        try {
            int plus = value.indexOf('+');
            if (plus < 0) {
                return Double.parseDouble(value);
            }
            String km = value.substring(0, plus);
            boolean negative = km.startsWith("-");
            double meters = Math.abs(Integer.parseInt(km)) * 1000d + Double.parseDouble(value.substring(plus + 1));
            return negative ? -meters : meters;

        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("PK no válido: '" + text + "'. Usa 12+345 o metros (12345)", e);
        }
    }
}
