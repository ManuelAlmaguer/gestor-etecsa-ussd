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
}
