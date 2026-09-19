package com.manu.etecsaussd.telephony;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class EtecsaActionExecutorTest {
    @Test
    public void buildsPackagePurchaseCode() {
        assertEquals("*133*1#", EtecsaActionExecutor.buildPackageCode("1"));
    }

    @Test
    public void buildsBalanceTransferCode() {
        assertEquals(
                "*234*1*51234567*1234*25#",
                EtecsaActionExecutor.buildTransferCode("+53 51234567", "1234", "25.00")
        );
    }
}

