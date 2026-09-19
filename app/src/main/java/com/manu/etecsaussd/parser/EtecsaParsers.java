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
    public static final double MONTHLY_RECHARGE_LIMIT_CUP = 360.0d;
    public static final String NUMBER_TOKEN =
            "(?:\\d{1,3}(?:[ .]\\d{3})*(?:[.,]\\d{1,2})?|\\d+(?:[.,]\\d{1,2})?)";
    private static final String DATE_TOKEN =
            "(?:\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4}|\\d{1,2}\\s+(?:ene(?:ro)?|feb(?:rero)?|mar(?:zo)?|abr(?:il)?|"
                    + "may(?:o)?|jun(?:io)?|jul(?:io)?|ago(?:sto)?|sep(?:tiembre)?|oct(?:ubre)?|"
                    + "nov(?:iembre)?|dic(?:iembre)?)\\s+\\d{4})";

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
    public static final Pattern VOICE_DURATION_PATTERN = Pattern.compile(
            "(?iu)\\b([0-9]{1,3}):([0-5][0-9]):([0-5][0-9])\\b"
    );
    public static final Pattern SMS_PATTERN = Pattern.compile(
            "(?iu)(?<![0-9])([0-9]{1,6})\\s*(?:SMS|mensajes?)\\b"
    );
    public static final Pattern SMS_LABEL_PATTERN = Pattern.compile(
            "(?iu)\\b(?:SMS|mensajes?)\\s*[:=-]?\\s*([0-9]{1,6})\\b"
    );

    /** *222*732#: recharge/bonus amount. */
    public static final Pattern RECHARGE_AMOUNT_PATTERN = Pattern.compile(
            "(?iu)(?:bono|recarga|importe|monto|saldo)"
                    + "[^0-9]{0,50}(" + NUMBER_TOKEN + ")\\s*(?:CUP|MN|pesos?)?"
    );
    private static final Pattern RECHARGED_AMOUNT_PATTERN = Pattern.compile(
            "(?iu)(?:bono|recargad[oa]|ha\\s+recargado|importe|monto)"
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

    public static final Pattern LINE_ACTIVE_UNTIL_PATTERN = Pattern.compile(
            "(?iu)(?:l[ií]nea\\s+activa|linea\\s+activa)\\s+(?:hasta|hasta\\s+el)\\s+(" + DATE_TOKEN + ")"
    );

    public static final Pattern PACKAGE_EXPIRATION_PATTERN = Pattern.compile(
            "(?iu)(?:vence|vencimiento|expira|paquete\\s+(?:vence|expira))[^0-9]{0,24}(" + DATE_TOKEN + ")"
    );

    public static final Pattern RECHARGE_LIMIT_DATE_PATTERN = Pattern.compile(
            "(?iu)(?:posterior\\s+al\\s+(?:d[ií]a)?|despu[eé]s\\s+del\\s+d[ií]a|"
                    + "puede\\s+recargar[^0-9]{0,24})(" + DATE_TOKEN + ")"
    );

    public static final Pattern RECHARGE_REMAINING_PATTERN = Pattern.compile(
            "(?iu)(?:puede\\s+recargar|restan|restante|disponible|a[uú]n\\s+puede\\s+recargar)"
                    + "[^0-9]{0,35}(" + NUMBER_TOKEN + ")\\s*(?:CUP|MN|pesos?)?"
    );

    public static final Pattern RECHARGE_LIMIT_REACHED_PATTERN = Pattern.compile(
            "(?iu)(?:ha\\s+alcanzado|alcanzado|l[ií]mite|limite)[^0-9]{0,45}"
                    + "(?:360|trescientos\\s+sesenta)"
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

    public static MainBalanceData parseMainBalance(String rawResponse) {
        requireResponse(rawResponse);
        BalanceData balance = parseBalance(rawResponse);
        DataUsageData dataUsage = parseDataUsageOrNull(rawResponse);
        VoiceSmsData voiceSms = parseVoiceSmsOrNull(rawResponse);
        String lineActiveUntilIso = extractDateIso(LINE_ACTIVE_UNTIL_PATTERN, rawResponse);
        String packageExpirationIso = extractDateIso(PACKAGE_EXPIRATION_PATTERN, rawResponse);
        if (balance == null && dataUsage == null && voiceSms == null
                && lineActiveUntilIso == null && packageExpirationIso == null) {
            return null;
        }
        return new MainBalanceData(
                balance,
                dataUsage,
                voiceSms,
                lineActiveUntilIso,
                packageExpirationIso
        );
    }

    public static DataUsageData parseDataUsage(String rawResponse) {
        requireResponse(rawResponse);
        DataUsageData data = parseDataUsageOrNull(rawResponse);
        if (data == null) {
            return null;
        }
        return data;
    }

    private static DataUsageData parseDataUsageOrNull(String rawResponse) {
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
        return parseVoiceSmsOrNull(rawResponse);
    }

    private static VoiceSmsData parseVoiceSmsOrNull(String rawResponse) {
        Matcher durationMatcher = VOICE_DURATION_PATTERN.matcher(rawResponse);
        Long voiceSeconds = null;
        Long minutes = null;
        if (durationMatcher.find()) {
            long hours = Long.parseLong(durationMatcher.group(1));
            long durationMinutes = Long.parseLong(durationMatcher.group(2));
            long seconds = Long.parseLong(durationMatcher.group(3));
            voiceSeconds = hours * 3600L + durationMinutes * 60L + seconds;
            minutes = voiceSeconds / 60L;
        } else {
            minutes = findInteger(VOICE_MINUTES_PATTERN, rawResponse);
            if (minutes != null) {
                voiceSeconds = minutes * 60L;
            }
        }
        Long sms = findInteger(SMS_PATTERN, rawResponse);
        if (sms == null) {
            sms = findInteger(SMS_LABEL_PATTERN, rawResponse);
        }
        if (voiceSeconds == null && sms == null) {
            return null;
        }
        return new VoiceSmsData(minutes, voiceSeconds, sms);
    }

    public static RechargeData parseRechargeStatus(String rawResponse) {
        requireResponse(rawResponse);
        boolean limitReached = RECHARGE_LIMIT_REACHED_PATTERN.matcher(rawResponse).find();
        Double explicitAmount = null;
        Matcher amountMatcher = RECHARGED_AMOUNT_PATTERN.matcher(rawResponse);
        if (amountMatcher.find()) {
            explicitAmount = parseDecimal(amountMatcher.group(1));
        }
        Double remainingRechargeCup = null;
        if (limitReached) {
            remainingRechargeCup = 0.0d;
        } else {
            Matcher remainingMatcher = RECHARGE_REMAINING_PATTERN.matcher(rawResponse);
            if (remainingMatcher.find()) {
                remainingRechargeCup = parseDecimal(remainingMatcher.group(1));
            }
        }

        if (remainingRechargeCup == null && explicitAmount != null) {
            remainingRechargeCup = MONTHLY_RECHARGE_LIMIT_CUP - explicitAmount;
        }

        String limitDateIso = extractDateIso(RECHARGE_LIMIT_DATE_PATTERN, rawResponse);
        if (limitDateIso == null) {
            limitDateIso = extractDateIso(EXPIRATION_DATE_PATTERN, rawResponse);
        }
        if (remainingRechargeCup == null && limitDateIso == null) {
            return null;
        }
        if (remainingRechargeCup != null) {
            remainingRechargeCup = Math.max(0.0d, Math.min(MONTHLY_RECHARGE_LIMIT_CUP, remainingRechargeCup));
        }
        Double rechargedThisCycleCup = explicitAmount != null
                ? explicitAmount
                : remainingRechargeCup == null
                ? null
                : MONTHLY_RECHARGE_LIMIT_CUP - remainingRechargeCup;
        String rechargeAvailableDateIso = addOneDay(limitDateIso);
        return new RechargeData(
                rechargedThisCycleCup,
                remainingRechargeCup,
                MONTHLY_RECHARGE_LIMIT_CUP,
                limitDateIso,
                rechargeAvailableDateIso,
                limitReached
        );
    }

    private static String extractDateIso(Pattern pattern, String rawResponse) {
        Matcher matcher = pattern.matcher(rawResponse);
        return matcher.find() ? parseDateToIso(matcher.group(1)) : null;
    }

    private static String addOneDay(String isoDate) {
        if (isoDate == null) {
            return null;
        }
        try {
            return LocalDate.parse(isoDate).plusDays(1L).toString();
        } catch (DateTimeParseException exception) {
            return null;
        }
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

    public static final class MainBalanceData {
        public final BalanceData balance;
        public final DataUsageData dataUsage;
        public final VoiceSmsData voiceSms;
        public final String lineActiveUntilIso;
        public final String packageExpirationIso;

        public MainBalanceData(
                BalanceData balance,
                DataUsageData dataUsage,
                VoiceSmsData voiceSms,
                String lineActiveUntilIso,
                String packageExpirationIso
        ) {
            this.balance = balance;
            this.dataUsage = dataUsage;
            this.voiceSms = voiceSms;
            this.lineActiveUntilIso = lineActiveUntilIso;
            this.packageExpirationIso = packageExpirationIso;
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
        public final Long voiceSeconds;
        public final Long smsMessages;

        public VoiceSmsData(Long voiceMinutes, Long voiceSeconds, Long smsMessages) {
            this.voiceMinutes = voiceMinutes;
            this.voiceSeconds = voiceSeconds;
            this.smsMessages = smsMessages;
        }
    }

    public static final class RechargeData {
        public final Double rechargedThisCycleCup;
        public final Double remainingRechargeCup;
        public final double limitCup;
        public final String limitDateIso;
        public final String rechargeAvailableDateIso;
        public final boolean limitReached;

        /** Backward-compatible aliases for callers that used the original model. */
        public final Double amountCup;
        public final String expirationDateIso;

        public RechargeData(
                Double rechargedThisCycleCup,
                Double remainingRechargeCup,
                double limitCup,
                String limitDateIso,
                String rechargeAvailableDateIso,
                boolean limitReached
        ) {
            this.rechargedThisCycleCup = rechargedThisCycleCup;
            this.remainingRechargeCup = remainingRechargeCup;
            this.limitCup = limitCup;
            this.limitDateIso = limitDateIso;
            this.rechargeAvailableDateIso = rechargeAvailableDateIso;
            this.limitReached = limitReached;
            this.amountCup = rechargedThisCycleCup;
            this.expirationDateIso = limitDateIso;
        }
    }
}
