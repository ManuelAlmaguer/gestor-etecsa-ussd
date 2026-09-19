package com.manu.etecsaussd.parser;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Defensive parsers for the Spanish responses currently returned by ETECSA USSD menus.
 * The raw response is always stored beside the parsed values so a new carrier format can
 * be supported without losing historical evidence.
 */
public final class EtecsaParsers {
    public static final String NUMBER_TOKEN =
            "(?:\\d{1,3}(?:[ .]\\d{3})*(?:[.,]\\d{1,2})?|\\d+(?:[.,]\\d{1,2})?)";

    /** *222#: "Saldo principal: 250.50 CUP" / "Su saldo es 250,50 CUP". */
    public static final Pattern MAIN_BALANCE_PATTERN = Pattern.compile(
            "(?iu)(?:saldo(?:\\s+principal)?|saldo\\s+disponible|balance)"
                    + "[^0-9]{0,45}(" + NUMBER_TOKEN + ")\\s*(?:CUP|MN|pesos?)?"
    );

    /** Fallback for currency values when the label is different. */
    public static final Pattern CURRENCY_AMOUNT_PATTERN = Pattern.compile(
            "(?iu)(" + NUMBER_TOKEN + ")\\s*(?:CUP|MN|pesos?)\\b"
    );

    /** *222*328#: values such as "2.4 GB LTE" and "500 MB todas las redes". */
    public static final Pattern DATA_VALUE_PATTERN = Pattern.compile(
            "(?iu)(" + NUMBER_TOKEN + ")\\s*(GB|GIB|MB|MBytes?|megabytes?|gigabytes?)\\b"
    );

    /** *222*869#: independent patterns tolerate either order of minutes and SMS. */
    public static final Pattern VOICE_MINUTES_PATTERN = Pattern.compile(
            "(?iu)(?<![0-9])([0-9]{1,6})\\s*(?:min(?:utos?)?)\\b"
    );
    public static final Pattern SMS_PATTERN = Pattern.compile(
            "(?iu)(?<![0-9])([0-9]{1,6})\\s*(?:SMS|mensajes?)\\b"
    );

    /** *222*732#: recharge/bonus amount. */
    public static final Pattern RECHARGE_AMOUNT_PATTERN = Pattern.compile(
            "(?iu)(?:bono|recarga|importe|monto|saldo)"
                    + "[^0-9]{0,50}(" + NUMBER_TOKEN + ")\\s*(?:CUP|MN|pesos?)?"
    );

    /** *222*732#: numeric dates and Spanish month names. */
    public static final Pattern EXPIRATION_DATE_PATTERN = Pattern.compile(
            "(?iu)(?:v[aá]lido\\s+(?:hasta|hasta el)|vence(?:\\s+el)?|"
                    + "expira(?:\\s+el)?|vencimiento|fecha(?:\\s+de)?\\s+vencimiento)?"
                    + "[^0-9]{0,30}("
                    + "\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4}"
                    + "|\\d{1,2}\\s+(?:ene(?:ro)?|feb(?:rero)?|mar(?:zo)?|abr(?:il)?|"
                    + "may(?:o)?|jun(?:io)?|jul(?:io)?|ago(?:sto)?|sep(?:tiembre)?|"
                    + "oct(?:ubre)?|nov(?:iembre)?|dic(?:iembre)?)\\s+\\d{4}"
                    + ")\\b"
    );

    private EtecsaParsers() {
    }

    public static BalanceData parseBalance(String rawResponse) {
        requireResponse(rawResponse);
        Matcher labelled = MAIN_BALANCE_PATTERN.matcher(rawResponse);
        if (labelled.find()) {
            Double amount = parseDecimal(labelled.group(1));
            if (amount != null) {
                return new BalanceData(amount);
            }
        }

        Matcher currency = CURRENCY_AMOUNT_PATTERN.matcher(rawResponse);
        if (currency.find()) {
            Double amount = parseDecimal(currency.group(1));
            if (amount != null) {
                return new BalanceData(amount);
            }
        }
        return null;
    }

    public static DataUsageData parseDataUsage(String rawResponse) {
        requireResponse(rawResponse);
        Matcher matcher = DATA_VALUE_PATTERN.matcher(rawResponse);
        Long lteMegabytes = null;
        Long allNetworksMegabytes = null;
        Long totalMegabytes = null;
        int matches = 0;

        while (matcher.find()) {
            Double quantity = parseDecimal(matcher.group(1));
            if (quantity == null) {
                continue;
            }
            long megabytes = toMegabytes(quantity, matcher.group(2));
            String before = nearestSegmentBefore(rawResponse, matcher.start());
            String after = nearestSegmentAfter(rawResponse, matcher.end());
            boolean lte = before.matches("(?is).*\\b(?:LTE|4G|5G)\\b.*")
                    || after.matches("(?is).*\\b(?:LTE|4G|5G)\\b.*");
            boolean allNetworks = before.matches("(?is).*\\b(?:todas?\\s+las\\s+redes|nacionales|cualquier\\s+red)\\b.*")
                    || after.matches("(?is).*\\b(?:todas?\\s+las\\s+redes|nacionales|cualquier\\s+red)\\b.*");

            if (lte && lteMegabytes == null) {
                lteMegabytes = megabytes;
            } else if (allNetworks && allNetworksMegabytes == null) {
                allNetworksMegabytes = megabytes;
            } else if (totalMegabytes == null) {
                totalMegabytes = megabytes;
            } else if (lteMegabytes == null) {
                lteMegabytes = megabytes;
            } else if (allNetworksMegabytes == null) {
                allNetworksMegabytes = megabytes;
            }
            matches++;
        }

        if (matches == 0) {
            return null;
        }
        return new DataUsageData(lteMegabytes, allNetworksMegabytes, totalMegabytes);
    }

    public static VoiceSmsData parseVoiceSms(String rawResponse) {
        requireResponse(rawResponse);
        Long minutes = findInteger(VOICE_MINUTES_PATTERN, rawResponse);
        Long sms = findInteger(SMS_PATTERN, rawResponse);
        if (minutes == null && sms == null) {
            return null;
        }
        return new VoiceSmsData(minutes, sms);
    }

    public static RechargeData parseRechargeStatus(String rawResponse) {
        requireResponse(rawResponse);
        Double amount = null;
        Matcher labelledAmount = RECHARGE_AMOUNT_PATTERN.matcher(rawResponse);
        if (labelledAmount.find()) {
            amount = parseDecimal(labelledAmount.group(1));
        }
        if (amount == null) {
            Matcher currency = CURRENCY_AMOUNT_PATTERN.matcher(rawResponse);
            if (currency.find()) {
                amount = parseDecimal(currency.group(1));
            }
        }

        String expirationDateIso = null;
        Matcher dateMatcher = EXPIRATION_DATE_PATTERN.matcher(rawResponse);
        if (dateMatcher.find()) {
            expirationDateIso = parseDateToIso(dateMatcher.group(1));
        }
        if (amount == null && expirationDateIso == null) {
            return null;
        }
        return new RechargeData(amount, expirationDateIso);
    }

    private static Long findInteger(Pattern pattern, String rawResponse) {
        Matcher matcher = pattern.matcher(rawResponse);
        return matcher.find() ? Long.parseLong(matcher.group(1)) : null;
    }

    private static long toMegabytes(double quantity, String unit) {
        String normalizedUnit = unit.toUpperCase(Locale.ROOT);
        return Math.round(normalizedUnit.startsWith("G") ? quantity * 1024d : quantity);
    }

    private static String nearestSegmentBefore(String value, int endExclusive) {
        int start = Math.max(
                Math.max(value.lastIndexOf(';', endExclusive - 1), value.lastIndexOf(',', endExclusive - 1)),
                value.lastIndexOf('\n', endExclusive - 1)
        );
        start = Math.max(start + 1, endExclusive - 28);
        return value.substring(start, endExclusive);
    }

    private static String nearestSegmentAfter(String value, int startInclusive) {
        int semicolon = value.indexOf(';', startInclusive);
        int comma = value.indexOf(',', startInclusive);
        int newline = value.indexOf('\n', startInclusive);
        int end = value.length();
        if (semicolon >= 0) end = Math.min(end, semicolon);
        if (comma >= 0) end = Math.min(end, comma);
        if (newline >= 0) end = Math.min(end, newline);
        end = Math.min(end, startInclusive + 28);
        return value.substring(startInclusive, end);
    }

    private static Double parseDecimal(String rawNumber) {
        if (rawNumber == null) {
            return null;
        }
        String normalized = rawNumber
                .replace("\u00A0", "")
                .replace(" ", "");
        int comma = normalized.lastIndexOf(',');
        int dot = normalized.lastIndexOf('.');
        if (comma >= 0 && dot >= 0) {
            if (comma > dot) {
                normalized = normalized.replace(".", "").replace(',', '.');
            } else {
                normalized = normalized.replace(",", "");
            }
        } else if (comma >= 0) {
            normalized = normalized.replace(',', '.');
        } else if (dot >= 0 && normalized.indexOf('.') != dot) {
            normalized = normalized.replace(".", "");
        }
        try {
            return Double.parseDouble(normalized);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private static String parseDateToIso(String rawDate) {
        if (rawDate == null) {
            return null;
        }
        String normalized = stripAccents(rawDate.trim().toLowerCase(Locale.ROOT));
        try {
            if (normalized.matches("\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4}")) {
                String[] parts = normalized.split("[/-]");
                int day = Integer.parseInt(parts[0]);
                int month = Integer.parseInt(parts[1]);
                int year = Integer.parseInt(parts[2]);
                if (year < 100) {
                    year += 2000;
                }
                return LocalDate.of(year, month, day).toString();
            }

            String[] parts = normalized.replaceAll("\\s+", " ").split(" ");
            if (parts.length == 3) {
                int day = Integer.parseInt(parts[0]);
                int month = monthNumber(parts[1]);
                int year = Integer.parseInt(parts[2]);
                return LocalDate.of(year, month, day).toString();
            }
        } catch (DateTimeParseException | NumberFormatException exception) {
            return null;
        }
        return null;
    }

    private static int monthNumber(String month) {
        if (month.startsWith("ene")) return 1;
        if (month.startsWith("feb")) return 2;
        if (month.startsWith("mar")) return 3;
        if (month.startsWith("abr")) return 4;
        if (month.startsWith("may")) return 5;
        if (month.startsWith("jun")) return 6;
        if (month.startsWith("jul")) return 7;
        if (month.startsWith("ago")) return 8;
        if (month.startsWith("sep")) return 9;
        if (month.startsWith("oct")) return 10;
        if (month.startsWith("nov")) return 11;
        if (month.startsWith("dic")) return 12;
        throw new IllegalArgumentException("Mes no reconocido: " + month);
    }

    private static String stripAccents(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
    }

    private static void requireResponse(String rawResponse) {
        if (rawResponse == null || rawResponse.trim().isEmpty()) {
            throw new IllegalArgumentException("La respuesta USSD está vacía.");
        }
    }

    public static final class BalanceData {
        public final double amountCup;

        public BalanceData(double amountCup) {
            this.amountCup = amountCup;
        }
    }

    public static final class DataUsageData {
        public final Long lteMegabytes;
        public final Long allNetworksMegabytes;
        public final Long totalMegabytes;

        public DataUsageData(Long lteMegabytes, Long allNetworksMegabytes, Long totalMegabytes) {
            this.lteMegabytes = lteMegabytes;
            this.allNetworksMegabytes = allNetworksMegabytes;
            this.totalMegabytes = totalMegabytes;
        }
    }

    public static final class VoiceSmsData {
        public final Long voiceMinutes;
        public final Long smsMessages;

        public VoiceSmsData(Long voiceMinutes, Long smsMessages) {
            this.voiceMinutes = voiceMinutes;
            this.smsMessages = smsMessages;
        }
    }

    public static final class RechargeData {
        public final Double amountCup;
        public final String expirationDateIso;

        public RechargeData(Double amountCup, String expirationDateIso) {
            this.amountCup = amountCup;
            this.expirationDateIso = expirationDateIso;
        }
    }
}
