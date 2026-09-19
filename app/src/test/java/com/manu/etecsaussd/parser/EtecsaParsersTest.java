package com.manu.etecsaussd.parser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

public class EtecsaParsersTest {
    @Test
    public void parsesMainBalanceInCup() {
        EtecsaParsers.BalanceData result = EtecsaParsers.parseBalance(
                "Saldo principal: 250.50 CUP"
        );

        assertNotNull(result);
        assertEquals(250.50d, result.amountCup, 0.001d);
    }

    @Test
    public void parsesDataWithLteAndAllNetworks() {
        EtecsaParsers.DataUsageData result = EtecsaParsers.parseDataUsage(
                "Datos disponibles: 2.4 GB LTE y 500 MB (todas las redes)."
        );

        assertNotNull(result);
        assertEquals(Long.valueOf(2458L), result.lteMegabytes);
        assertEquals(Long.valueOf(500L), result.allNetworksMegabytes);
    }

    @Test
    public void parsesDataWhenAllNetworksAppearsBeforeLte() {
        EtecsaParsers.DataUsageData result = EtecsaParsers.parseDataUsage(
                "500 MB todas las redes; 2.4 GB LTE"
        );

        assertNotNull(result);
        assertEquals(Long.valueOf(2458L), result.lteMegabytes);
        assertEquals(Long.valueOf(500L), result.allNetworksMegabytes);
    }

    @Test
    public void parsesVoiceAndSmsIndependently() {
        EtecsaParsers.VoiceSmsData result = EtecsaParsers.parseVoiceSms(
                "Recursos restantes: 45 minutos y 120 SMS"
        );

        assertNotNull(result);
        assertEquals(Long.valueOf(45L), result.voiceMinutes);
        assertEquals(Long.valueOf(120L), result.smsMessages);
    }

    @Test
    public void parsesRechargeAmountAndSpanishExpirationDate() {
        EtecsaParsers.RechargeData result = EtecsaParsers.parseRechargeStatus(
                "Bono: 120.00 CUP. Válido hasta: 15 Oct 2026"
        );

        assertNotNull(result);
        assertEquals(Double.valueOf(120.00d), result.amountCup);
        assertEquals("2026-10-15", result.expirationDateIso);
    }

    @Test
    public void parsesTheRealMainBalanceResponse() {
        EtecsaParsers.MainBalanceData result = EtecsaParsers.parseMainBalance(
                "Saldo: 245.16 CUP. Datos: 1.38 GB. Voz: 00:34:15. SMS: 73. "
                        + "Linea activa hasta 23-07-27 vence 19-01-28."
        );

        assertNotNull(result);
        assertEquals(245.16d, result.balance.amountCup, 0.001d);
        assertEquals(Long.valueOf(1413L), result.dataUsage.totalMegabytes);
        assertEquals(Long.valueOf(2055L), result.voiceSms.voiceSeconds);
        assertEquals(Long.valueOf(73L), result.voiceSms.smsMessages);
        assertEquals("2027-07-23", result.lineActiveUntilIso);
        assertEquals("2028-01-19", result.packageExpirationIso);
    }

    @Test
    public void parsesTheMonthlyRechargeLimitAndNextAvailableDate() {
        EtecsaParsers.RechargeData result = EtecsaParsers.parseRechargeStatus(
                "Ud ha alcanzado el monto de recarga permitido de 360 CUP. "
                        + "Puede recargar posterior al dia 30-09-26"
        );

        assertNotNull(result);
        assertEquals(Double.valueOf(360.0d), result.rechargedThisCycleCup);
        assertEquals(Double.valueOf(0.0d), result.remainingRechargeCup);
        assertEquals("2026-09-30", result.limitDateIso);
        assertEquals("2026-10-01", result.rechargeAvailableDateIso);
        assertEquals(true, result.limitReached);
    }
}
