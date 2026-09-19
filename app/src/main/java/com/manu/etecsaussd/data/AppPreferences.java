package com.manu.etecsaussd.data;

import android.content.Context;
import android.content.SharedPreferences;

/** Single source of truth for user-selectable SIM, theme and notification settings. */
public final class AppPreferences {
    public static final String PREFS_NAME = "etecsa_preferences";
    public static final String KEY_ACTION_SUBSCRIPTION_ID = "action_subscription_id";
    public static final String KEY_DISPLAY_SUBSCRIPTION_ID = "display_subscription_id";
    public static final String KEY_THEME = "theme";
    public static final String KEY_NOTIFICATIONS_ENABLED = "notifications_enabled";
    public static final String KEY_PACKAGE_REMINDER_5 = "package_reminder_5";
    public static final String KEY_PACKAGE_REMINDER_3 = "package_reminder_3";
    public static final String KEY_PACKAGE_REMINDER_1 = "package_reminder_1";
    public static final String KEY_RECHARGE_LIMIT_REMINDER = "recharge_limit_reminder";
    public static final String KEY_RECHARGE_AVAILABLE_REMINDER = "recharge_available_reminder";

    private final SharedPreferences preferences;

    private AppPreferences(Context context) {
        preferences = context.getApplicationContext().getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
        );
    }

    public static AppPreferences from(Context context) {
        return new AppPreferences(context);
    }

    public Integer getActionSubscriptionId() {
        return getOptionalInt(KEY_ACTION_SUBSCRIPTION_ID);
    }

    public void setActionSubscriptionId(int subscriptionId) {
        preferences.edit().putInt(KEY_ACTION_SUBSCRIPTION_ID, subscriptionId).apply();
    }

    public Integer getDisplaySubscriptionId() {
        return getOptionalInt(KEY_DISPLAY_SUBSCRIPTION_ID);
    }

    public void setDisplaySubscriptionId(int subscriptionId) {
        preferences.edit().putInt(KEY_DISPLAY_SUBSCRIPTION_ID, subscriptionId).apply();
    }

    public String getTheme() {
        return preferences.getString(KEY_THEME, "blue") == null
                ? "blue"
                : preferences.getString(KEY_THEME, "blue");
    }

    public void setTheme(String theme) {
        preferences.edit().putString(KEY_THEME, theme).apply();
    }

    public boolean notificationsEnabled() {
        return preferences.getBoolean(KEY_NOTIFICATIONS_ENABLED, true);
    }

    public boolean packageReminder5() {
        return preferences.getBoolean(KEY_PACKAGE_REMINDER_5, true);
    }

    public boolean packageReminder3() {
        return preferences.getBoolean(KEY_PACKAGE_REMINDER_3, true);
    }

    public boolean packageReminder1() {
        return preferences.getBoolean(KEY_PACKAGE_REMINDER_1, true);
    }

    public boolean rechargeLimitReminder() {
        return preferences.getBoolean(KEY_RECHARGE_LIMIT_REMINDER, true);
    }

    public boolean rechargeAvailableReminder() {
        return preferences.getBoolean(KEY_RECHARGE_AVAILABLE_REMINDER, true);
    }

    public void setNotificationSettings(
            boolean enabled,
            boolean reminder5,
            boolean reminder3,
            boolean reminder1,
            boolean rechargeLimit,
            boolean rechargeAvailable
    ) {
        preferences.edit()
                .putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled)
                .putBoolean(KEY_PACKAGE_REMINDER_5, reminder5)
                .putBoolean(KEY_PACKAGE_REMINDER_3, reminder3)
                .putBoolean(KEY_PACKAGE_REMINDER_1, reminder1)
                .putBoolean(KEY_RECHARGE_LIMIT_REMINDER, rechargeLimit)
                .putBoolean(KEY_RECHARGE_AVAILABLE_REMINDER, rechargeAvailable)
                .apply();
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        return preferences.getBoolean(key, defaultValue);
    }

    public void setNotificationEventSent(String eventKey, String date) {
        preferences.edit().putString("sent_" + eventKey, date).apply();
    }

    public String getNotificationEventSent(String eventKey) {
        return preferences.getString("sent_" + eventKey, "");
    }

    private Integer getOptionalInt(String key) {
        if (!preferences.contains(key)) {
            return null;
        }
        return preferences.getInt(key, -1);
    }
}
