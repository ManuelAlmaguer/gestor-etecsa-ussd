package com.manu.etecsaussd.telephony;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class EtecsaUssdCodes {
    public static final String MAIN_BALANCE = "*222#";
    public static final String MOBILE_DATA = "*222*328#";
    public static final String VOICE_SMS = "*222*869#";
    public static final String RECHARGE_STATUS = "*222*732#";

    private EtecsaUssdCodes() {
    }

    public static List<String> dashboardCodes() {
        return Collections.unmodifiableList(Arrays.asList(
                MAIN_BALANCE,
                MOBILE_DATA,
                VOICE_SMS,
                RECHARGE_STATUS
        ));
    }
}

