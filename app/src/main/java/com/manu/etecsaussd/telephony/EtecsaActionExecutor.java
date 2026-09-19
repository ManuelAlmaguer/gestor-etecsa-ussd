package com.manu.etecsaussd.telephony;

import android.annotation.SuppressLint;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Builds only the documented ETECSA action codes after validating user input. */
public final class EtecsaActionExecutor {
    private final UssdExecutor ussdExecutor;

    public EtecsaActionExecutor(UssdExecutor ussdExecutor) {
        this.ussdExecutor = ussdExecutor;
    }

    @SuppressLint("MissingPermission")
    public UssdResponse purchasePackage(String packageOption, Integer subscriptionId)
            throws UssdExecutionException {
        String code = buildPackageCode(packageOption);
        return ussdExecutor.executeBlocking(code, subscriptionId);
    }

    @SuppressLint("MissingPermission")
    public UssdResponse transferBalance(
            String phoneNumber,
            String password,
            String amount,
            Integer subscriptionId
    ) throws UssdExecutionException {
        String code = buildTransferCode(phoneNumber, password, amount);
        return ussdExecutor.executeBlocking(code, subscriptionId);
    }

    public static String buildPackageCode(String packageOption) {
        if (packageOption == null || !packageOption.matches("[1-9][0-9]{0,3}")) {
            throw new IllegalArgumentException("La opción del paquete debe ser un entero positivo.");
        }
        return "*133*" + packageOption + "#";
    }

    public static String buildTransferCode(String phoneNumber, String password, String amount) {
        String normalizedPhone = normalizeCubanMobile(phoneNumber);
        if (password == null || !password.matches("[0-9]{4,8}")) {
            throw new IllegalArgumentException("La contraseña de transferencia debe tener entre 4 y 8 dígitos.");
        }
        BigDecimal parsedAmount;
        try {
            parsedAmount = new BigDecimal(amount == null ? "" : amount.replace(',', '.'));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("El importe no es numérico.", exception);
        }
        if (parsedAmount.signum() <= 0 || parsedAmount.scale() > 2) {
            throw new IllegalArgumentException("El importe debe ser positivo y tener como máximo dos decimales.");
        }
        String normalizedAmount = parsedAmount.setScale(2, RoundingMode.UNNECESSARY)
                .stripTrailingZeros()
                .toPlainString();
        return "*234*1*" + normalizedPhone + "*" + password + "*" + normalizedAmount + "#";
    }

    private static String normalizeCubanMobile(String phoneNumber) {
        if (phoneNumber == null) {
            throw new IllegalArgumentException("El número de destino es obligatorio.");
        }
        String normalized = phoneNumber.replaceAll("[\\s()\\-]", "");
        if (normalized.startsWith("+53")) {
            normalized = normalized.substring(3);
        } else if (normalized.startsWith("53") && normalized.length() == 10) {
            normalized = normalized.substring(2);
        }
        if (!normalized.matches("5[0-9]{7}")) {
            throw new IllegalArgumentException("El número debe ser un móvil cubano de 8 dígitos.");
        }
        return normalized;
    }
}
