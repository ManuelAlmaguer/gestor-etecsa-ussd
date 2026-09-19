package com.manu.etecsaussd;

import android.Manifest;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.telephony.SubscriptionInfo;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.ComponentActivity;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.appbar.MaterialToolbar;

import com.manu.etecsaussd.data.AppPreferences;
import com.manu.etecsaussd.data.entity.BalanceSnapshotEntity;
import com.manu.etecsaussd.data.entity.DataUsageSnapshotEntity;
import com.manu.etecsaussd.data.entity.PackageStatusEntity;
import com.manu.etecsaussd.data.entity.RechargeStatusEntity;
import com.manu.etecsaussd.data.entity.VoiceSmsSnapshotEntity;
import com.manu.etecsaussd.data.model.DashboardSnapshot;
import com.manu.etecsaussd.domain.EtecsaRepository;
import com.manu.etecsaussd.domain.SyncReport;
import com.manu.etecsaussd.sync.EtecsaNotificationScheduler;
import com.manu.etecsaussd.sync.EtecsaSyncWorker;
import com.manu.etecsaussd.telephony.EtecsaActionExecutor;
import com.manu.etecsaussd.telephony.UssdExecutionException;
import com.manu.etecsaussd.telephony.UssdExecutor;

import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Native Android shell for Manu ETECSA. The original HTML mockup is kept as design reference only. */
public final class MainActivity extends ComponentActivity {
    private static final int PERMISSION_REQUEST = 7001;
    private static final int BG = Color.rgb(11, 18, 32);
    private static final int SURFACE = Color.rgb(23, 34, 55);
    private static final int SURFACE_HIGH = Color.rgb(31, 46, 73);
    private static final int TEXT = Color.rgb(239, 244, 255);
    private static final int MUTED = Color.rgb(164, 178, 205);
    private static final int BLUE = Color.rgb(59, 130, 246);
    private static final int CYAN = Color.rgb(34, 211, 238);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.ROOT);

    private enum Page { DASHBOARD, PACKAGES, TRANSFER, SETTINGS, ABOUT }

    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private EtecsaApplication application;
    private EtecsaRepository repository;
    private UssdExecutor ussdExecutor;
    private EtecsaActionExecutor actionExecutor;
    private AppPreferences preferences;
    private DrawerLayout drawerLayout;
    private LinearLayout pageHost;
    private MaterialToolbar toolbar;
    private Page currentPage = Page.DASHBOARD;
    private DashboardSnapshot dashboard;
    private List<SubscriptionInfo> activeSubscriptions = Collections.emptyList();
    private AutoCompleteTextView displaySimSelector;
    private AutoCompleteTextView actionSimSelector;
    private MaterialSwitch notificationsSwitch;
    private MaterialSwitch reminder5Switch;
    private MaterialSwitch reminder3Switch;
    private MaterialSwitch reminder1Switch;
    private MaterialSwitch rechargeLimitSwitch;
    private MaterialSwitch rechargeAvailableSwitch;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        application = (EtecsaApplication) getApplication();
        repository = application.getRepository();
        ussdExecutor = application.getUssdExecutor();
        actionExecutor = new EtecsaActionExecutor(ussdExecutor);
        preferences = AppPreferences.from(this);
        applyThemeChrome();
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        buildShell();
        renderPage(Page.DASHBOARD);
        requestPermissionsIfNeeded();
        EtecsaSyncWorker.enqueueDailySync(this);
        loadSimOptions();
    }

    private void buildShell() {
        drawerLayout = new DrawerLayout(this);
        drawerLayout.setBackgroundColor(BG);

        LinearLayout main = column();
        toolbar = new MaterialToolbar(this);
        toolbar.setTitleTextColor(TEXT);
        toolbar.setTitle("Inicio");
        toolbar.setNavigationIcon(android.R.drawable.ic_menu_sort_by_size);
        toolbar.setNavigationIconTint(TEXT);
        toolbar.setNavigationContentDescription("Abrir menú");
        toolbar.setNavigationOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));
        toolbar.setBackgroundColor(BG);
        main.addView(toolbar, new LinearLayout.LayoutParams(-1, dp(64)));

        pageHost = column();
        main.addView(pageHost, new LinearLayout.LayoutParams(-1, 0, 1));
        drawerLayout.addView(main, new DrawerLayout.LayoutParams(-1, -1));
        drawerLayout.addView(buildDrawer(), drawerParams());
        setContentView(drawerLayout);
    }

    private DrawerLayout.LayoutParams drawerParams() {
        DrawerLayout.LayoutParams params = new DrawerLayout.LayoutParams(dp(300), -1);
        params.gravity = GravityCompat.START;
        return params;
    }

    private View buildDrawer() {
        LinearLayout drawer = column();
        drawer.setPadding(dp(22), dp(34), dp(18), dp(20));
        drawer.setBackgroundColor(Color.rgb(15, 25, 43));
        TextView brand = text("MANU ETECSA\nGestor personal de líneas", 22, TEXT, true);
        brand.setPadding(0, 0, 0, dp(28));
        drawer.addView(brand);
        drawer.addView(drawerItem("⌂  Inicio", Page.DASHBOARD));
        drawer.addView(drawerItem("▣  Comprar paquetes", Page.PACKAGES));
        drawer.addView(drawerItem("⇄  Transferir saldo", Page.TRANSFER));
        drawer.addView(drawerItem("⚙  Configuración", Page.SETTINGS));
        drawer.addView(drawerItem("ⓘ  Acerca de", Page.ABOUT));
        TextView offline = text("Diseñada para funcionar sin Internet", 12, MUTED, false);
        offline.setPadding(0, dp(30), 0, 0);
        drawer.addView(offline);
        return drawer;
    }

    private TextView drawerItem(String label, Page page) {
        TextView item = text(label, 16, TEXT, true);
        item.setGravity(Gravity.CENTER_VERTICAL);
        item.setPadding(dp(12), 0, dp(8), 0);
        item.setBackground(roundDrawable(SURFACE, 18));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(52));
        params.setMargins(0, 0, 0, dp(10));
        item.setLayoutParams(params);
        item.setOnClickListener(v -> {
            drawerLayout.closeDrawer(GravityCompat.START);
            renderPage(page);
        });
        return item;
    }

    private void renderPage(Page page) {
        currentPage = page;
        pageHost.removeAllViews();
        toolbar.setTitle(pageTitle(page));
        switch (page) {
            case DASHBOARD:
                pageHost.addView(scroll(dashboardPage()));
                break;
            case PACKAGES:
                pageHost.addView(scroll(packagesPage()));
                break;
            case TRANSFER:
                pageHost.addView(scroll(transferPage()));
                break;
            case SETTINGS:
                pageHost.addView(scroll(settingsPage()));
                break;
            case ABOUT:
                pageHost.addView(scroll(aboutPage()));
                break;
        }
    }

    private String pageTitle(Page page) {
        switch (page) {
            case PACKAGES: return "Comprar paquetes";
            case TRANSFER: return "Transferir saldo";
            case SETTINGS: return "Configuración";
            case ABOUT: return "Acerca de";
            default: return "Inicio";
        }
    }

    private View dashboardPage() {
        LinearLayout body = contentColumn();
        LinearLayout heading = row();
        LinearLayout headingText = column();
        headingText.addView(text("Tu línea, bajo control", 25, TEXT, true));
        headingText.addView(text("Datos guardados localmente y sincronización USSD", 13, MUTED, false));
        heading.addView(headingText, new LinearLayout.LayoutParams(0, -2, 1));
        MaterialButton refresh = button("Actualizar", BLUE);
        refresh.setOnClickListener(v -> syncDisplayLine());
        heading.addView(refresh, new LinearLayout.LayoutParams(-2, dp(46)));
        body.addView(heading, margins(0, 0, 0, 16));

        String displayLabel = selectedSimLabel(false);
        body.addView(chip("MOSTRANDO EN INICIO  ·  " + displayLabel, CYAN), margins(0, 0, 0, 14));

        if (dashboard == null) {
            body.addView(cardMessage("Aún no hay datos guardados", "Concede los permisos de teléfono y pulsa Actualizar para consultar la SIM mostrada."));
        } else {
            BalanceSnapshotEntity balance = dashboard.balance;
            DataUsageSnapshotEntity data = dashboard.dataUsage;
            VoiceSmsSnapshotEntity voice = dashboard.voiceSms;
            RechargeStatusEntity recharge = dashboard.rechargeStatus;
            PackageStatusEntity pkg = dashboard.packageStatus;
            body.addView(metricCard("Saldo principal", balance == null ? "—" : money(balance.amountCup),
                    balance == null ? "Sin consulta todavía" : "CUP · ETECSA", BLUE));
            body.addView(metricCard("Datos · todas las redes", data == null ? "—" : formatData(data.allNetworksMegabytes),
                    "Se muestra el total disponible en cualquier red", CYAN));
            body.addView(metricCard("Voz y SMS", voiceSummary(voice), voice == null ? "Sin consulta todavía" : "Recursos de la línea", Color.rgb(167, 139, 250)));
            body.addView(rechargeCard(recharge));
            body.addView(packageCard(pkg, balance));
        }
        MaterialButton permissions = button("Revisar permisos", SURFACE_HIGH);
        permissions.setOnClickListener(v -> requestPermissionsIfNeeded());
        body.addView(permissions, margins(0, 4, 0, 26));
        return body;
    }

    private View packagesPage() {
        LinearLayout body = contentColumn();
        body.addView(text("Compra por el menú ETECSA", 24, TEXT, true));
        body.addView(text("La app envía el recorrido completo: *133# → 1 Datos → 4 Planes.", 14, MUTED, false), margins(0, 6, 0, 18));
        body.addView(chip("LÍNEA PARA ACCIONES  ·  " + selectedSimLabel(true), BLUE), margins(0, 0, 0, 14));
        addPackageOption(body, "1", "4.5 GB", "Paquete de datos", "240 CUP");
        addPackageOption(body, "2", "2 GB  +  15 min  +  20 SMS", "Datos, voz y mensajería", "120 CUP");
        addPackageOption(body, "3", "4 GB  +  35 min  +  40 SMS", "Datos, voz y mensajería", "240 CUP");
        addPackageOption(body, "4", "6 GB  +  60 min  +  70 SMS", "Datos, voz y mensajería", "360 CUP");
        return body;
    }

    private void addPackageOption(LinearLayout body, String option, String name, String detail, String price) {
        MaterialCardView card = card();
        LinearLayout content = row();
        content.setPadding(dp(16), dp(15), dp(12), dp(15));
        TextView number = text(option, 24, CYAN, true);
        number.setGravity(Gravity.CENTER);
        content.addView(number, new LinearLayout.LayoutParams(dp(40), -1));
        LinearLayout copy = column();
        copy.addView(text(name, 16, TEXT, true));
        copy.addView(text(detail, 12, MUTED, false));
        content.addView(copy, new LinearLayout.LayoutParams(0, -2, 1));
        LinearLayout purchase = column();
        TextView amount = text(price, 14, TEXT, true);
        amount.setGravity(Gravity.CENTER);
        purchase.addView(amount);
        MaterialButton buy = button("Comprar", BLUE);
        buy.setOnClickListener(v -> purchasePackage(option));
        purchase.addView(buy, new LinearLayout.LayoutParams(-2, dp(40)));
        content.addView(purchase);
        card.addView(content);
        body.addView(card, margins(0, 0, 0, 12));
    }

    private View transferPage() {
        LinearLayout body = contentColumn();
        body.addView(text("Envía saldo a otro móvil", 24, TEXT, true));
        body.addView(text("Se usará la línea predeterminada para acciones. Verifícala en Configuración.", 14, MUTED, false), margins(0, 6, 0, 18));
        body.addView(chip("LÍNEA DE ACCIONES  ·  " + selectedSimLabel(true), BLUE), margins(0, 0, 0, 18));
        TextInputLayout phone = input("Número móvil cubano");
        phone.getEditText().setInputType(android.text.InputType.TYPE_CLASS_PHONE);
        body.addView(phone, margins(0, 0, 0, 12));
        TextInputLayout password = input("Contraseña de transferencia");
        password.getEditText().setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        body.addView(password, margins(0, 0, 0, 12));
        TextInputLayout amount = input("Importe en CUP");
        amount.getEditText().setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        body.addView(amount, margins(0, 0, 0, 18));
        MaterialButton transfer = button("Transferir saldo", BLUE);
        transfer.setOnClickListener(v -> transferBalance(phone, password, amount, transfer));
        body.addView(transfer);
        body.addView(cardMessage("Código utilizado", "*234*1*Número*Contraseña*Importe#"), margins(0, 18, 0, 0));
        return body;
    }

    private View settingsPage() {
        LinearLayout body = contentColumn();
        body.addView(text("Preferencias", 24, TEXT, true));
        body.addView(text("La SIM visible y la SIM usada para acciones son independientes.", 14, MUTED, false), margins(0, 6, 0, 18));
        body.addView(selectionCard("SIM mostrada en Inicio", "Solo cambia los datos del panel principal.", false));
        body.addView(selectionCard("SIM predeterminada para acciones", "Se usa para compras y transferencias.", true), margins(0, 12, 0, 0));
        body.addView(themeCard(), margins(0, 18, 0, 0));

        MaterialCardView notifications = card();
        LinearLayout n = paddedColumn();
        n.addView(text("Notificaciones", 18, TEXT, true));
        n.addView(text("Avisos independientes para cada SIM, sin Internet.", 13, MUTED, false), margins(0, 4, 0, 8));
        notificationsSwitch = settingSwitch(n, "Permitir notificaciones", preferences.notificationsEnabled());
        reminder5Switch = settingSwitch(n, "Paquete: 5 días antes", preferences.packageReminder5());
        reminder3Switch = settingSwitch(n, "Paquete: 3 días antes", preferences.packageReminder3());
        reminder1Switch = settingSwitch(n, "Paquete: 1 día antes", preferences.packageReminder1());
        rechargeLimitSwitch = settingSwitch(n, "Día del límite mensual (360 CUP)", preferences.rechargeLimitReminder());
        rechargeAvailableSwitch = settingSwitch(n, "Día en que vuelve a estar disponible", preferences.rechargeAvailableReminder());
        notifications.addView(n);
        body.addView(notifications, margins(0, 18, 0, 12));

        MaterialCardView permissions = card();
        LinearLayout p = paddedColumn();
        p.addView(text("Permisos del sistema", 18, TEXT, true));
        p.addView(text(permissionSummary(), 13, MUTED, false), margins(0, 5, 0, 10));
        MaterialButton request = button("Solicitar permisos", SURFACE_HIGH);
        request.setOnClickListener(v -> requestPermissionsIfNeeded());
        p.addView(request);
        permissions.addView(p);
        body.addView(permissions);
        updateSimSelectors();
        return body;
    }

    private View themeCard() {
        MaterialCardView card = card();
        LinearLayout content = paddedColumn();
        content.addView(text("Tema visual", 17, TEXT, true));
        content.addView(text("Se guarda en el dispositivo y no depende de Internet.", 13, MUTED, false), margins(0, 3, 0, 8));
        AutoCompleteTextView selector = new AutoCompleteTextView(this);
        selector.setTextSize(15);
        selector.setTextColor(TEXT);
        selector.setSingleLine(true);
        selector.setPadding(dp(14), 0, dp(14), 0);
        selector.setBackground(roundDrawable(SURFACE_HIGH, 14));
        String[] labels = {"Azul ETECSA", "Cian", "Alto contraste"};
        selector.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, labels));
        selector.setText(themeLabel(), false);
        selector.setOnItemClickListener((parent, view, position, id) -> {
            preferences.setTheme(position == 1 ? "cyan" : position == 2 ? "contrast" : "blue");
            applyThemeChrome();
            renderPage(Page.SETTINGS);
        });
        content.addView(selector, new LinearLayout.LayoutParams(-1, dp(52)));
        card.addView(content);
        return card;
    }

    private View selectionCard(String title, String subtitle, boolean actions) {
        MaterialCardView card = card();
        LinearLayout content = paddedColumn();
        content.addView(text(title, 17, TEXT, true));
        content.addView(text(subtitle, 13, MUTED, false), margins(0, 3, 0, 8));
        AutoCompleteTextView selector = new AutoCompleteTextView(this);
        selector.setTextSize(15);
        selector.setTextColor(TEXT);
        selector.setHintTextColor(MUTED);
        selector.setHint("Selecciona una línea");
        selector.setSingleLine(true);
        selector.setPadding(dp(14), 0, dp(14), 0);
        selector.setBackground(roundDrawable(SURFACE_HIGH, 14));
        if (actions) actionSimSelector = selector; else displaySimSelector = selector;
        content.addView(selector, new LinearLayout.LayoutParams(-1, dp(52)));
        selector.setOnItemClickListener((parent, view, position, id) -> {
            if (position >= activeSubscriptions.size()) return;
            int subscriptionId = activeSubscriptions.get(position).getSubscriptionId();
            worker.execute(() -> {
                try {
                    if (actions) ussdExecutor.setActionSubscriptionId(subscriptionId);
                    else ussdExecutor.setDisplaySubscriptionId(subscriptionId);
                    runOnUiThread(() -> {
                        toast(actions ? "SIM predeterminada para acciones guardada" : "SIM del inicio guardada");
                        if (!actions) renderPage(Page.DASHBOARD);
                    });
                } catch (UssdExecutionException exception) {
                    runOnUiThread(() -> toast(exception.getMessage()));
                }
            });
        });
        return card;
    }

    private View aboutPage() {
        LinearLayout body = contentColumn();
        MaterialCardView card = card();
        LinearLayout content = paddedColumn();
        TextView logo = text("M", 54, CYAN, true);
        logo.setGravity(Gravity.CENTER);
        logo.setBackground(roundDrawable(Color.rgb(9, 35, 73), 24));
        content.addView(logo, new LinearLayout.LayoutParams(-1, dp(110)));
        content.addView(text("Manu ETECSA", 27, TEXT, true), margins(0, 20, 0, 2));
        content.addView(text("Gestor personal de líneas ETECSA", 15, CYAN, false));
        content.addView(text("Creada para Manuel Almaguer Sosa", 15, TEXT, true), margins(0, 25, 0, 2));
        content.addView(text("Consulta saldo, datos, voz, SMS, límite mensual de recargas y vigencia de paquetes desde una aplicación Android nativa. Los datos históricos se guardan en Room.", 14, MUTED, false));
        content.addView(text("Versión 1.0.0  ·  Java  ·  Android 28–34", 12, MUTED, false), margins(0, 22, 0, 0));
        card.addView(content);
        body.addView(card);
        return body;
    }

    private View rechargeCard(RechargeStatusEntity recharge) {
        MaterialCardView card = card();
        LinearLayout content = paddedColumn();
        content.addView(text("Límite de recarga", 18, TEXT, true));
        if (recharge == null) {
            content.addView(text("Sin consulta todavía", 14, MUTED, false), margins(0, 6, 0, 0));
        } else {
            content.addView(text(recharge.remainingRechargeCup == null ? "—" : money(recharge.remainingRechargeCup) + " disponibles", 25, CYAN, true), margins(0, 5, 0, 0));
            content.addView(text("de " + money(recharge.limitCup) + " CUP para este ciclo", 13, MUTED, false));
            content.addView(text("Recargado este ciclo: " + money(recharge.rechargedThisCycleCup) + " CUP", 14, TEXT, false), margins(0, 13, 0, 0));
            content.addView(text("Fecha del límite: " + date(recharge.limitDateIso), 13, MUTED, false));
            content.addView(text("Puedes volver a recargar desde: " + date(recharge.rechargeAvailableDateIso), 13, MUTED, false));
            content.addView(text("La fecha del límite se restablece al día siguiente.", 12, CYAN, false), margins(0, 9, 0, 0));
        }
        card.addView(content);
        return card;
    }

    private View packageCard(PackageStatusEntity pkg, BalanceSnapshotEntity balance) {
        MaterialCardView card = card();
        LinearLayout content = paddedColumn();
        content.addView(text("Vigencia", 18, TEXT, true));
        String expiration = pkg == null ? (balance == null ? null : balance.packageExpirationIso) : pkg.expirationDateIso;
        content.addView(text(expiration == null ? "Sin fecha de paquete" : "Paquete activo hasta " + date(expiration), 16, TEXT, false), margins(0, 8, 0, 0));
        if (balance != null && balance.lineActiveUntilIso != null) {
            content.addView(text("Línea activa hasta " + date(balance.lineActiveUntilIso), 13, MUTED, false), margins(0, 4, 0, 0));
        }
        card.addView(content);
        return card;
    }

    private View metricCard(String title, String value, String detail, int accent) {
        MaterialCardView card = card();
        LinearLayout content = paddedColumn();
        content.addView(text(title, 15, MUTED, true));
        content.addView(text(value, 28, TEXT, true), margins(0, 8, 0, 0));
        content.addView(text(detail, 12, paletteColor(accent), false), margins(0, 3, 0, 0));
        card.addView(content);
        return card;
    }

    private View cardMessage(String title, String message) {
        MaterialCardView card = card();
        LinearLayout content = paddedColumn();
        content.addView(text(title, 18, TEXT, true));
        content.addView(text(message, 14, MUTED, false), margins(0, 8, 0, 0));
        card.addView(content);
        return card;
    }

    private MaterialSwitch settingSwitch(LinearLayout parent, String label, boolean checked) {
        MaterialSwitch toggle = new MaterialSwitch(this);
        toggle.setText(label);
        toggle.setTextColor(TEXT);
        toggle.setTextSize(14);
        toggle.setChecked(checked);
        toggle.setOnCheckedChangeListener((button, value) -> saveNotificationSettings());
        parent.addView(toggle, new LinearLayout.LayoutParams(-1, dp(50)));
        return toggle;
    }

    private void saveNotificationSettings() {
        if (notificationsSwitch == null || rechargeAvailableSwitch == null) return;
        preferences.setNotificationSettings(
                notificationsSwitch.isChecked(),
                reminder5Switch.isChecked(),
                reminder3Switch.isChecked(),
                reminder1Switch.isChecked(),
                rechargeLimitSwitch.isChecked(),
                rechargeAvailableSwitch.isChecked()
        );
    }

    private TextInputLayout input(String hint) {
        TextInputLayout layout = new TextInputLayout(this);
        layout.setHint(hint);
        layout.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE);
        layout.setBoxStrokeColor(BLUE);
        layout.setHintTextColor(ColorStateList.valueOf(MUTED));
        EditText edit = new EditText(this);
        edit.setTextColor(TEXT);
        edit.setHintTextColor(MUTED);
        edit.setSingleLine(true);
        layout.addView(edit, new ViewGroup.LayoutParams(-1, -2));
        return layout;
    }

    private void syncDisplayLine() {
        if (!ussdExecutor.hasRequiredPermissions()) {
            requestPermissionsIfNeeded();
            toast("Concede los permisos de teléfono para actualizar");
            return;
        }
        setBusy(true);
        worker.execute(() -> {
            try {
                int subscriptionId = ussdExecutor.resolveSubscriptionIdForDisplay(preferences.getDisplaySubscriptionId());
                SyncReport report = repository.syncAll(ussdExecutor, subscriptionId);
                dashboard = repository.getLatestDashboard(subscriptionId);
                EtecsaNotificationScheduler.scheduleForSubscription(this, repository, subscriptionId);
                runOnUiThread(() -> {
                    setBusy(false);
                    if (report.successCount > 0) {
                        toast(report.failureCount == 0 ? "Datos actualizados" : "Datos actualizados parcialmente");
                        renderPage(Page.DASHBOARD);
                    } else {
                        toast(report.errors.isEmpty() ? "No se pudo consultar la SIM" : report.errors.get(0));
                    }
                });
            } catch (Exception exception) {
                runOnUiThread(() -> {
                    setBusy(false);
                    toast(exception.getMessage() == null ? "No se pudo actualizar" : exception.getMessage());
                });
            }
        });
    }

    private void purchasePackage(String option) {
        if (!ussdExecutor.hasRequiredPermissions()) {
            requestPermissionsIfNeeded();
            toast("Concede los permisos de teléfono para comprar");
            return;
        }
        worker.execute(() -> {
            try {
                actionExecutor.purchasePackage(option, preferences.getActionSubscriptionId());
                runOnUiThread(() -> toast("Solicitud del paquete enviada a ETECSA"));
            } catch (Exception exception) {
                runOnUiThread(() -> toast(exception.getMessage() == null ? "No se pudo comprar el paquete" : exception.getMessage()));
            }
        });
    }

    private void transferBalance(TextInputLayout phone, TextInputLayout password, TextInputLayout amount, Button button) {
        String phoneValue = phone.getEditText().getText().toString().trim();
        String passwordValue = password.getEditText().getText().toString().trim();
        String amountValue = amount.getEditText().getText().toString().trim();
        if (phoneValue.isEmpty() || passwordValue.isEmpty() || amountValue.isEmpty()) {
            toast("Completa todos los campos");
            return;
        }
        if (!ussdExecutor.hasRequiredPermissions()) {
            requestPermissionsIfNeeded();
            toast("Concede los permisos de teléfono para transferir");
            return;
        }
        button.setEnabled(false);
        worker.execute(() -> {
            try {
                actionExecutor.transferBalance(phoneValue, passwordValue, amountValue, preferences.getActionSubscriptionId());
                runOnUiThread(() -> {
                    button.setEnabled(true);
                    toast("Solicitud de transferencia enviada a ETECSA");
                });
            } catch (Exception exception) {
                runOnUiThread(() -> {
                    button.setEnabled(true);
                    toast(exception.getMessage() == null ? "No se pudo transferir" : exception.getMessage());
                });
            }
        });
    }

    private void loadSimOptions() {
        if (!ussdExecutor.hasRequiredPermissions()) return;
        worker.execute(() -> {
            try {
                activeSubscriptions = ussdExecutor.getActiveSubscriptions();
                runOnUiThread(this::updateSimSelectors);
            } catch (Exception exception) {
                activeSubscriptions = Collections.emptyList();
            }
        });
    }

    private void updateSimSelectors() {
        String[] labels = new String[activeSubscriptions.size()];
        for (int i = 0; i < activeSubscriptions.size(); i++) labels[i] = simLabel(activeSubscriptions.get(i));
        if (displaySimSelector != null) {
            displaySimSelector.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, labels));
            setSelectorValue(displaySimSelector, preferences.getDisplaySubscriptionId());
        }
        if (actionSimSelector != null) {
            actionSimSelector.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, labels));
            setSelectorValue(actionSimSelector, preferences.getActionSubscriptionId());
        }
    }

    private void setSelectorValue(AutoCompleteTextView selector, Integer subscriptionId) {
        if (subscriptionId == null) return;
        for (int i = 0; i < activeSubscriptions.size(); i++) {
            if (activeSubscriptions.get(i).getSubscriptionId() == subscriptionId) {
                selector.setText(simLabel(activeSubscriptions.get(i)), false);
                return;
            }
        }
    }

    private String selectedSimLabel(boolean action) {
        Integer selected = action ? preferences.getActionSubscriptionId() : preferences.getDisplaySubscriptionId();
        if (selected != null) {
            for (SubscriptionInfo info : activeSubscriptions) if (info.getSubscriptionId() == selected) return simLabel(info);
            return "SIM guardada (" + selected + ")";
        }
        if (activeSubscriptions.isEmpty()) return "SIM predeterminada del teléfono";
        return simLabel(activeSubscriptions.get(0)) + " · predeterminada";
    }

    private String simLabel(SubscriptionInfo info) {
        CharSequence carrier = info.getCarrierName();
        String name = carrier == null || carrier.toString().trim().isEmpty() ? "Línea" : carrier.toString();
        return name + " · SIM " + info.getSimSlotIndex() + " · id " + info.getSubscriptionId();
    }

    private String permissionSummary() {
        boolean call = ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED;
        boolean phone = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED;
        boolean notifications = Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        return "Teléfono: " + (call && phone ? "concedido" : "pendiente") + "  ·  Notificaciones: " + (notifications ? "concedidas" : "pendientes") + "\nAlmacenamiento: no requerido; Room usa el almacenamiento privado de la app.";
    }

    private void requestPermissionsIfNeeded() {
        ArrayList<String> missing = new ArrayList<>();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) missing.add(Manifest.permission.CALL_PHONE);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) missing.add(Manifest.permission.READ_PHONE_STATE);
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) missing.add(Manifest.permission.POST_NOTIFICATIONS);
        if (!missing.isEmpty()) requestPermissions(missing.toArray(new String[0]), PERMISSION_REQUEST);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST) {
            loadSimOptions();
            if (currentPage == Page.SETTINGS) renderPage(Page.SETTINGS);
            toast("Permisos actualizados");
        }
    }

    private void setBusy(boolean busy) {
        if (toolbar != null) toolbar.setSubtitle(busy ? "Consultando ETECSA…" : null);
    }

    private void applyThemeChrome() {
        int primary = primaryColor();
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        if (toolbar != null) toolbar.setTitleTextColor(TEXT);
        if (drawerLayout != null) drawerLayout.setBackgroundColor(BG);
        if (primary == Color.rgb(245, 158, 11)) {
            getWindow().setStatusBarColor(Color.rgb(35, 25, 8));
        }
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private LinearLayout column() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        return layout;
    }

    private LinearLayout row() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setGravity(Gravity.CENTER_VERTICAL);
        return layout;
    }

    private LinearLayout contentColumn() {
        LinearLayout body = column();
        body.setPadding(dp(18), dp(8), dp(18), dp(28));
        return body;
    }

    private LinearLayout paddedColumn() {
        LinearLayout layout = column();
        layout.setPadding(dp(16), dp(15), dp(16), dp(15));
        return layout;
    }

    private ScrollView scroll(View content) {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);
        scroll.addView(content, new ViewGroup.LayoutParams(-1, -2));
        return scroll;
    }

    private MaterialCardView card() {
        MaterialCardView card = new MaterialCardView(this);
        card.setCardBackgroundColor(SURFACE);
        card.setStrokeColor(primaryColor() == Color.rgb(245, 158, 11)
                ? Color.rgb(112, 80, 20) : Color.rgb(45, 63, 93));
        card.setStrokeWidth(dp(1));
        card.setRadius(dp(20));
        card.setClickable(false);
        return card;
    }

    private MaterialButton button(String label, int color) {
        MaterialButton button = new MaterialButton(this);
        button.setText(label);
        button.setTextColor(TEXT);
        button.setTextSize(13);
        button.setAllCaps(false);
        button.setMinHeight(0);
        button.setMinimumHeight(0);
        button.setPadding(dp(14), 0, dp(14), 0);
        button.setBackgroundTintList(ColorStateList.valueOf(paletteColor(color)));
        return button;
    }

    private TextView chip(String value, int color) {
        TextView chip = text(value, 11, paletteColor(color), true);
        chip.setPadding(dp(12), dp(8), dp(12), dp(8));
        chip.setBackground(roundDrawable(Color.rgb(17, 39, 70), 18));
        return chip;
    }

    private TextView text(String value, float size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setTypeface(Typeface.create("sans", bold ? Typeface.BOLD : Typeface.NORMAL));
        return view;
    }

    private GradientDrawable roundDrawable(int color, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radius));
        return drawable;
    }

    private LinearLayout.LayoutParams margins(int left, int top, int right, int bottom) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.setMargins(dp(left), dp(top), dp(right), dp(bottom));
        return params;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private String money(Double value) {
        return value == null ? "—" : new DecimalFormat("0.##", DecimalFormatSymbolsHolder.SYMBOLS).format(value) + " CUP";
    }

    private String money(double value) {
        return money(Double.valueOf(value));
    }

    private String formatData(Long megabytes) {
        if (megabytes == null) return "—";
        if (megabytes >= 1024L) return new DecimalFormat("0.##", DecimalFormatSymbolsHolder.SYMBOLS).format(megabytes / 1024d) + " GB";
        return megabytes + " MB";
    }

    private String voiceSummary(VoiceSmsSnapshotEntity voice) {
        if (voice == null) return "—";
        String duration = voice.voiceSeconds == null ? (voice.voiceMinutes == null ? "—" : voice.voiceMinutes + " min") : formatDuration(voice.voiceSeconds);
        String sms = voice.smsMessages == null ? "—" : voice.smsMessages + " SMS";
        return duration + "  ·  " + sms;
    }

    private String formatDuration(Long seconds) {
        long total = seconds == null ? 0L : seconds;
        return String.format(Locale.ROOT, "%02d:%02d:%02d", total / 3600L, (total % 3600L) / 60L, total % 60L);
    }

    private String date(String iso) {
        if (iso == null || iso.trim().isEmpty()) return "pendiente";
        try { return LocalDate.parse(iso).format(DATE_FORMAT); }
        catch (RuntimeException ignored) { return iso; }
    }

    private static final class DecimalFormatSymbolsHolder {
        private static final java.text.DecimalFormatSymbols SYMBOLS = java.text.DecimalFormatSymbols.getInstance(Locale.US);
    }

    private String themeLabel() {
        switch (preferences.getTheme()) {
            case "cyan": return "Cian";
            case "contrast": return "Alto contraste";
            default: return "Azul ETECSA";
        }
    }

    private int primaryColor() {
        switch (preferences.getTheme()) {
            case "cyan": return Color.rgb(6, 182, 212);
            case "contrast": return Color.rgb(245, 158, 11);
            default: return BLUE;
        }
    }

    private int secondaryColor() {
        switch (preferences.getTheme()) {
            case "cyan": return Color.rgb(103, 232, 249);
            case "contrast": return Color.rgb(253, 230, 138);
            default: return CYAN;
        }
    }

    private int paletteColor(int requested) {
        if (requested == BLUE) return primaryColor();
        if (requested == CYAN) return secondaryColor();
        return requested;
    }

    @Override
    protected void onDestroy() {
        worker.shutdownNow();
        super.onDestroy();
    }
}
