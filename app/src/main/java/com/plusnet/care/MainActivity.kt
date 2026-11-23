package com.plusnet.care

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Paint
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.*
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay
import org.json.JSONObject
import org.json.JSONTokener
import java.util.Locale

data class AppThemeConfig(
    val name: String,
    val bg: Color,
    val card: Color,
    val border: Color,
    val accent: Color,
    val secondary: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val heroStart: Color,
    val heroEnd: Color
)

val AppThemes = listOf(
    AppThemeConfig("Plusnet Glass (Default)", Color(0xFF0A0E17), Color(0xFF141B2D), Color(0xFF24304A), Color(0xFF059669), Color(0xFF38BDF8), Color.White, Color(0xFF94A3B8), Color(0xFF065F46), Color(0xFF1E3A8A)),
    AppThemeConfig("1. Windows XP (Luna Blue)", Color(0xFFECE9D8), Color(0xFFFFFFFF), Color(0xFF0054E3), Color(0xFF0054E3), Color(0xFF3B9919), Color(0xFF111827), Color(0xFF4B5563), Color(0xFF0058EE), Color(0xFF3888FF)),
    AppThemeConfig("2. Windows Vista (Aero)", Color(0xFF0B192C), Color(0xFF132A46), Color(0xFF38BDF8), Color(0xFF22D3EE), Color(0xFF34D399), Color.White, Color(0xFFBAE6FD), Color(0xFF0E7490), Color(0xFF1E3A8A)),
    AppThemeConfig("3. Windows 7 (Classic Aero)", Color(0xFF10253F), Color(0xFF1B3B64), Color(0xFF60A5FA), Color(0xFF38BDF8), Color(0xFF4ADE80), Color.White, Color(0xFFCBD5E1), Color(0xFF1D4ED8), Color(0xFF0284C7)),
    AppThemeConfig("4. Windows 8 (Metro Tile)", Color(0xFF1D0033), Color(0xFF2E0854), Color(0xFFA855F7), Color(0xFFD946EF), Color(0xFF06B6D4), Color.White, Color(0xFFE9D5FF), Color(0xFF7E22CE), Color(0xFFBE185D)),
    AppThemeConfig("5. Windows 10 (Fluent Dark)", Color(0xFF000000), Color(0xFF171717), Color(0xFF333333), Color(0xFF0078D7), Color(0xFF00B294), Color.White, Color(0xFFA3A3A3), Color(0xFF005A9E), Color(0xFF0078D7)),
    AppThemeConfig("6. Windows 10 (Clean Light)", Color(0xFFF3F4F6), Color(0xFFFFFFFF), Color(0xFFD1D5DB), Color(0xFF0078D7), Color(0xFF059669), Color(0xFF111827), Color(0xFF6B7280), Color(0xFF0078D7), Color(0xFF2563EB)),
    AppThemeConfig("7. Windows 11 (Mica Dark)", Color(0xFF1C1C1C), Color(0xFF282828), Color(0xFF3D3D3D), Color(0xFF60CDFF), Color(0xFFA78BFA), Color.White, Color(0xFFD4D4D4), Color(0xFF1E293B), Color(0xFF0F172A)),
    AppThemeConfig("8. Windows 11 (Mica Bloom)", Color(0xFF0F172A), Color(0xFF1E293B), Color(0xFF475569), Color(0xFF38BDF8), Color(0xFFF472B6), Color.White, Color(0xFFCBD5E1), Color(0xFF0284C7), Color(0xFF4F46E5)),
    AppThemeConfig("9. Windows 98 (Retro Teal)", Color(0xFF008080), Color(0xFFC0C0C0), Color(0xFF808080), Color(0xFF000080), Color(0xFF008000), Color(0xFF000000), Color(0xFF262626), Color(0xFF000080), Color(0xFF1084D0)),
    AppThemeConfig("10. Windows Terminal (Matrix)", Color(0xFF030704), Color(0xFF0A140D), Color(0xFF16A34A), Color(0xFF22C55E), Color(0xFF4ADE80), Color(0xFF4ADE80), Color(0xFF15803D), Color(0xFF052E16), Color(0xFF14532D))
)

data class PaymentItem(val date: String, val bill: String, val receive: String, val remarks: String)
data class DailyUsage(val label: String, val download: Float, val upload: Float)
data class SessionLogItem(val connDate: String, val discDate: String, val upload: String, val download: String, val duration: String)
data class TicketItem(val id: String, val status: String, val title: String, val description: String, val date: String, val assigned: String)
data class TicketCategory(val value: String, val label: String)

data class PlusnetState(
    val isLoggedIn: Boolean = false,
    val isSyncing: Boolean = true,
    val fullName: String = "Customer",
    val email: String = "",
    val userName: String = "",
    val mobile: String = "",
    val customerId: String = "",
    val packageName: String = "Basic+PIP",
    val accountStatus: String = "Active",
    val connectionStatus: String = "ONLINE",
    val expiryDate: String = "--",
    val planRate: String = "--",
    val payments: List<PaymentItem> = emptyList(),
    val dailyUsage: List<DailyUsage> = emptyList(),
    val monthlyDownGB: Float = 0f,
    val monthlyUpGB: Float = 0f,
    val downSpeed: String = "0.0 Mbps",
    val upSpeed: String = "0.0 Mbps",
    val liveSpeedHistory: List<Float> = listOf(2f, 5f, 4f, 8f, 6f, 10f),
    val sessionLogs: List<SessionLogItem> = emptyList(),
    val tickets: List<TicketItem> = emptyList(),
    val ticketCategories: List<TicketCategory> = listOf(
        TicketCategory("Link Down", "Link Down"),
        TicketCategory("Slow Speed", "Slow Speed"),
        TicketCategory("Billing Issue", "Billing Issue"),
        TicketCategory("Router Configuration", "Router Configuration")
    )
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PlusnetNativeApp()
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun PlusnetNativeApp() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("plusnet_prefs_v5", Context.MODE_PRIVATE) }
    var themeIndex by remember { mutableIntStateOf(prefs.getInt("theme_idx", 0).coerceIn(0, AppThemes.lastIndex)) }
    val theme = AppThemes[themeIndex]

    var state by remember { mutableStateOf(PlusnetState()) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showNewTicketDialog by remember { mutableStateOf(false) }

    LaunchedEffect(state.isLoggedIn) {
        while (state.isLoggedIn) {
            delay(14000)
            val target = when (selectedTab) {
                0 -> "dashboard"
                1 -> "report"
                2 -> "complainlist"
                else -> "dashboard"
            }
            webViewRef?.loadUrl("https://care.plus.net.bd/customer/$target")
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(theme.bg)) {
        if (state.isLoggedIn) {
            Scaffold(
                containerColor = theme.bg,
                topBar = {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(theme.card)
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            PlusnetGreenLogo(size = 38.dp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("PLUSNET CARE", color = theme.textPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text(
                                    text = if (state.isSyncing) "Syncing live data..." else "Online • ${state.userName.ifEmpty { "PPG092" }}",
                                    color = if (state.isSyncing) Color(0xFFF59E0B) else Color(0xFF10B981),
                                    fontSize = 11.sp
                                )
                            }
                        }
                        Row {
                            IconButton(onClick = { showThemeDialog = true }) {
                                Icon(Icons.Default.Palette, contentDescription = "Themes", tint = theme.accent)
                            }
                            IconButton(onClick = {
                                state = state.copy(isSyncing = true)
                                val p = when (selectedTab) {
                                    1 -> "report"
                                    2 -> "complainlist"
                                    else -> "dashboard"
                                }
                                webViewRef?.loadUrl("https://care.plus.net.bd/customer/$p")
                            }) {
                                Icon(Icons.Default.Refresh, contentDescription = "Sync", tint = theme.textPrimary)
                            }
                            IconButton(onClick = {
                                CookieManager.getInstance().removeAllCookies(null)
                                CookieManager.getInstance().flush()
                                state = PlusnetState(isLoggedIn = false, isSyncing = true)
                                webViewRef?.loadUrl("https://care.plus.net.bd/customer/login")
                            }) {
                                Icon(Icons.Default.Logout, contentDescription = "Logout", tint = Color(0xFFEF4444))
                            }
                        }
                    }
                },
                bottomBar = {
                    NavigationBar(
                        containerColor = theme.card,
                        contentColor = theme.textPrimary,
                        modifier = Modifier.navigationBarsPadding()
                    ) {
                        NavigationBarItem(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            icon = { Icon(Icons.Default.Dashboard, contentDescription = null) },
                            label = { Text("Dashboard", fontSize = 11.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = theme.accent, selectedTextColor = theme.accent,
                                indicatorColor = theme.accent.copy(alpha = 0.16f),
                                unselectedIconColor = theme.textSecondary, unselectedTextColor = theme.textSecondary
                            )
                        )
                        NavigationBarItem(
                            selected = selectedTab == 1,
                            onClick = {
                                selectedTab = 1
                                state = state.copy(isSyncing = true)
                                webViewRef?.loadUrl("https://care.plus.net.bd/customer/report")
                            },
                            icon = { Icon(Icons.Default.Speed, contentDescription = null) },
                            label = { Text("Speed & Session", fontSize = 11.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = theme.accent, selectedTextColor = theme.accent,
                                indicatorColor = theme.accent.copy(alpha = 0.16f),
                                unselectedIconColor = theme.textSecondary, unselectedTextColor = theme.textSecondary
                            )
                        )
                        NavigationBarItem(
                            selected = selectedTab == 2,
                            onClick = {
                                selectedTab = 2
                                state = state.copy(isSyncing = true)
                                webViewRef?.loadUrl("https://care.plus.net.bd/customer/complainlist")
                            },
                            icon = { Icon(Icons.Default.ConfirmationNumber, contentDescription = null) },
                            label = { Text("Complain", fontSize = 11.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = theme.accent, selectedTextColor = theme.accent,
                                indicatorColor = theme.accent.copy(alpha = 0.16f),
                                unselectedIconColor = theme.textSecondary, unselectedTextColor = theme.textSecondary
                            )
                        )
                        NavigationBarItem(
                            selected = selectedTab == 3,
                            onClick = { selectedTab = 3 },
                            icon = { Icon(Icons.Default.AccountCircle, contentDescription = null) },
                            label = { Text("Profile", fontSize = 11.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = theme.accent, selectedTextColor = theme.accent,
                                indicatorColor = theme.accent.copy(alpha = 0.16f),
                                unselectedIconColor = theme.textSecondary, unselectedTextColor = theme.textSecondary
                            )
                        )
                    }
                }
            ) { padding ->
                Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                    if (state.isSyncing) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = theme.accent, trackColor = theme.card)
                    }
                    when (selectedTab) {
                        0 -> DashboardTab(state, theme)
                        1 -> ReportTab(state, theme)
                        2 -> ComplainTab(state, theme, onOpenNewTicket = { showNewTicketDialog = true })
                        3 -> ProfileTab(state, theme)
                    }
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(theme.card)
                        .statusBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PlusnetGreenLogo(size = 38.dp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("PLUSNET SELF-CARE", color = theme.textPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("Sign in once • Native App will open automatically", color = theme.accent, fontSize = 11.sp)
                    }
                }
                if (state.isSyncing) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = theme.accent, trackColor = theme.card)
                }
            }
        }

        // Background / Login WebView
        AndroidView(
            modifier = if (!state.isLoggedIn) {
                Modifier.fillMaxSize().statusBarsPadding().padding(top = 68.dp)
            } else {
                Modifier.size(1.dp).alpha(0.01f)
            },
            factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                    setBackgroundColor(android.graphics.Color.parseColor("#0A0E17"))
                    val cm = CookieManager.getInstance()
                    cm.setAcceptCookie(true)
                    cm.setAcceptThirdPartyCookies(this, true)

                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        databaseEnabled = true
                        loadWithOverviewMode = true
                        useWideViewPort = true
                        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        userAgentString = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
                    }

                    webViewClient = object : WebViewClient() {
                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                            state = state.copy(isSyncing = true)
                            if (url != null && url.contains("/dashboard")) {
                                state = state.copy(isLoggedIn = true)
                            }
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            CookieManager.getInstance().flush()

                            if (url != null && url.contains("/login")) {
                                state = state.copy(isLoggedIn = false, isSyncing = false)
                            } else if (url != null && url.contains("/dashboard")) {
                                state = state.copy(isLoggedIn = true)
                                view?.postDelayed({
                                    val dashJs = """
                                        (function() {
                                            var text = document.body.innerText || "";
                                            function findAfter(label) {
                                                var idx = text.indexOf(label);
                                                if (idx === -1) return "";
                                                var sub = text.substring(idx + label.length).trim();
                                                return sub.split('\n')[0].trim();
                                            }
                                            var pkg = findAfter("PACKAGE") || "Basic+PIP";
                                            var acc = findAfter("Account Status") || (text.includes("Active") ? "Active" : "Inactive");
                                            var conn = findAfter("Connection Status") || (text.includes("ONLINE") ? "ONLINE" : "OFFLINE");
                                            var exp = findAfter("Expiry Date") || "--";
                                            var rate = findAfter("Plan rate") || "--";
                                            var uName = findAfter("User Name:");
                                            var mob = findAfter("Mobile:");
                                            var cid = findAfter("ID:");

                                            var lines = text.split('\n').map(s => s.trim()).filter(s => s.length > 0);
                                            var fName = "", mail = "";
                                            for (var i = 0; i < lines.length; i++) {
                                                if (lines[i].includes("@") && lines[i].includes(".")) {
                                                    mail = lines[i];
                                                    if (i > 0) fName = lines[i-1];
                                                    break;
                                                }
                                            }

                                            var rows = [];
                                            document.querySelectorAll("table tr").forEach(function(tr, index) {
                                                if (index === 0) return;
                                                var tds = tr.querySelectorAll("td");
                                                if (tds.length >= 4) {
                                                    rows.push({
                                                        date: tds[0].innerText.trim(),
                                                        bill: tds[1].innerText.trim(),
                                                        receive: tds[2].innerText.trim(),
                                                        remarks: tds[3].innerText.trim()
                                                    });
                                                }
                                            });

                                            var chartData = [];
                                            var totalD = 0, totalU = 0;
                                            if (window.Highcharts && Highcharts.charts) {
                                                Highcharts.charts.forEach(function(c) {
                                                    if (c && c.series && c.series.length >= 1) {
                                                        var cats = (c.xAxis && c.xAxis[0] && c.xAxis[0].categories) ? c.xAxis[0].categories : [];
                                                        var s0 = c.series[0].data || [];
                                                        var s1 = (c.series.length > 1) ? c.series[1].data : [];
                                                        for (var k = 0; k < s0.length; k++) {
                                                            var dVal = s0[k].y || 0;
                                                            var uVal = (s1[k] ? (s1[k].y || 0) : 0);
                                                            totalD += dVal;
                                                            totalU += uVal;
                                                            chartData.push({
                                                                label: cats[k] || (s0[k].name || ("" + (k+1))),
                                                                down: dVal,
                                                                up: uVal
                                                            });
                                                        }
                                                    }
                                                });
                                            }
                                            return JSON.stringify({
                                                pkg: pkg, acc: acc, conn: conn, exp: exp, rate: rate,
                                                fullName: fName, email: mail, userName: uName, mobile: mob, cid: cid,
                                                payments: rows, chart: chartData, totalD: totalD, totalU: totalU
                                            });
                                        })();
                                    """.trimIndent()

                                    view.evaluateJavascript(dashJs) { raw ->
                                        try {
                                            val obj = JSONObject(JSONTokener(raw).nextValue().toString())
                                            val payList = mutableListOf<PaymentItem>()
                                            obj.optJSONArray("payments")?.let { arr ->
                                                for (i in 0 until arr.length()) {
                                                    val it = arr.getJSONObject(i)
                                                    payList.add(PaymentItem(it.optString("date"), it.optString("bill"), it.optString("receive"), it.optString("remarks")))
                                                }
                                            }
                                            val usageList = mutableListOf<DailyUsage>()
                                            obj.optJSONArray("chart")?.let { arr ->
                                                for (i in 0 until arr.length()) {
                                                    val it = arr.getJSONObject(i)
                                                    usageList.add(DailyUsage(it.optString("label"), it.optDouble("down", 0.0).toFloat(), it.optDouble("up", 0.0).toFloat()))
                                                }
                                            }
                                            state = state.copy(
                                                isLoggedIn = true,
                                                isSyncing = false,
                                                packageName = obj.optString("pkg").ifEmpty { state.packageName },
                                                accountStatus = obj.optString("acc").ifEmpty { "Active" },
                                                connectionStatus = obj.optString("conn").ifEmpty { "ONLINE" },
                                                expiryDate = obj.optString("exp").ifEmpty { state.expiryDate },
                                                planRate = obj.optString("rate").ifEmpty { state.planRate },
                                                fullName = obj.optString("fullName").ifEmpty { state.fullName },
                                                email = obj.optString("email").ifEmpty { state.email },
                                                userName = obj.optString("userName").ifEmpty { state.userName },
                                                mobile = obj.optString("mobile").ifEmpty { state.mobile },
                                                customerId = obj.optString("cid").ifEmpty { state.customerId },
                                                payments = if (payList.isNotEmpty()) payList else state.payments,
                                                dailyUsage = if (usageList.isNotEmpty()) usageList else state.dailyUsage,
                                                monthlyDownGB = obj.optDouble("totalD", state.monthlyDownGB.toDouble()).toFloat(),
                                                monthlyUpGB = obj.optDouble("totalU", state.monthlyUpGB.toDouble()).toFloat()
                                            )
                                            if (selectedTab == 0 && state.sessionLogs.isEmpty()) {
                                                view.loadUrl("https://care.plus.net.bd/customer/report")
                                            }
                                        } catch (_: Exception) {
                                            state = state.copy(isSyncing = false)
                                        }
                                    }
                                }, 2000)
                            } else if (url != null && url.contains("/report")) {
                                view?.postDelayed({
                                    val reportJs = """
                                        (function() {
                                            var text = document.body.innerText || "";
                                            var downM = text.match(/Download\s+(\d+\.?\d*\s*(?:Mbps|Kbps|mbps|kbps|bps))/i);
                                            var upM = text.match(/Upload\s+(\d+\.?\d*\s*(?:Mbps|Kbps|mbps|kbps|bps))/i);
                                            var speeds = text.match(/(\d+\.?\d*\s*(?:Mbps|Kbps|mbps|kbps|bps))/gi) || [];

                                            var sessions = [];
                                            document.querySelectorAll("table tr").forEach(function(tr, idx) {
                                                if (idx === 0) return;
                                                var tds = tr.querySelectorAll("td");
                                                if (tds.length >= 5) {
                                                    sessions.push({
                                                        conn: tds[0].innerText.trim(),
                                                        disc: tds[1].innerText.trim(),
                                                        up: tds[2].innerText.trim(),
                                                        down: tds[3].innerText.trim(),
                                                        dur: tds[4].innerText.trim()
                                                    });
                                                }
                                            });
                                            return JSON.stringify({
                                                downSpd: downM ? downM[1] : (speeds[0] || "0.0 kbps"),
                                                upSpd: upM ? upM[1] : (speeds[1] || "0.0 mbps"),
                                                sessions: sessions
                                            });
                                        })();
                                    """.trimIndent()

                                    view.evaluateJavascript(reportJs) { raw ->
                                        try {
                                            val obj = JSONObject(JSONTokener(raw).nextValue().toString())
                                            val dSpd = obj.optString("downSpd", "0.0 kbps")
                                            val uSpd = obj.optString("upSpd", "0.0 mbps")
                                            val rawNum = Regex("(\\d+\\.?\\d*)").find(uSpd)?.groupValues?.get(1)?.toFloatOrNull()
                                                ?: Regex("(\\d+\\.?\\d*)").find(dSpd)?.groupValues?.get(1)?.toFloatOrNull() ?: 2f
                                            val updatedHist = (state.liveSpeedHistory + rawNum).takeLast(10)

                                            val sessList = mutableListOf<SessionLogItem>()
                                            obj.optJSONArray("sessions")?.let { arr ->
                                                for (i in 0 until arr.length()) {
                                                    val it = arr.getJSONObject(i)
                                                    sessList.add(
                                                        SessionLogItem(
                                                            connDate = it.optString("conn"),
                                                            discDate = it.optString("disc").ifEmpty { "Active Now" },
                                                            upload = it.optString("up"),
                                                            download = it.optString("down"),
                                                            duration = it.optString("dur")
                                                        )
                                                    )
                                                }
                                            }

                                            state = state.copy(
                                                isSyncing = false,
                                                downSpeed = dSpd,
                                                upSpeed = uSpd,
                                                liveSpeedHistory = updatedHist,
                                                sessionLogs = if (sessList.isNotEmpty()) sessList else state.sessionLogs
                                            )
                                            if (state.tickets.isEmpty()) {
                                                view.loadUrl("https://care.plus.net.bd/customer/complainlist")
                                            }
                                        } catch (_: Exception) {
                                            state = state.copy(isSyncing = false)
                                        }
                                    }
                                }, 2000)
                            } else if (url != null && url.contains("/complainlist")) {
                                view?.postDelayed({
                                    val complainJs = """
                                        (function() {
                                            var tickets = [];
                                            document.querySelectorAll("table tr").forEach(function(tr, idx) {
                                                if (idx === 0) return;
                                                var tds = tr.querySelectorAll("td");
                                                if (tds.length >= 5) {
                                                    tickets.push({
                                                        id: tds[0].innerText.trim(),
                                                        status: tds[1].innerText.trim(),
                                                        title: tds[2].innerText.trim(),
                                                        desc: tds[3].innerText.trim(),
                                                        date: tds[4].innerText.trim(),
                                                        assigned: tds.length > 5 ? tds[5].innerText.trim() : ""
                                                    });
                                                }
                                            });

                                            var cats = [];
                                            document.querySelectorAll("select option").forEach(function(opt) {
                                                var v = opt.value;
                                                var t = opt.innerText.trim();
                                                if (v && t && !t.toLowerCase().includes("select") && isNaN(t)) {
                                                    cats.push({ val: v, label: t });
                                                }
                                            });
                                            return JSON.stringify({ tickets: tickets, cats: cats });
                                        })();
                                    """.trimIndent()

                                    view.evaluateJavascript(complainJs) { raw ->
                                        try {
                                            val obj = JSONObject(JSONTokener(raw).nextValue().toString())
                                            val tList = mutableListOf<TicketItem>()
                                            obj.optJSONArray("tickets")?.let { arr ->
                                                for (i in 0 until arr.length()) {
                                                    val it = arr.getJSONObject(i)
                                                    tList.add(
                                                        TicketItem(
                                                            id = it.optString("id"),
                                                            status = it.optString("status"),
                                                            title = it.optString("title"),
                                                            description = it.optString("desc"),
                                                            date = it.optString("date"),
                                                            assigned = it.optString("assigned")
                                                        )
                                                    )
                                                }
                                            }
                                            val cList = mutableListOf<TicketCategory>()
                                            obj.optJSONArray("cats")?.let { arr ->
                                                for (i in 0 until arr.length()) {
                                                    val it = arr.getJSONObject(i)
                                                    cList.add(TicketCategory(it.optString("val"), it.optString("label")))
                                                }
                                            }
                                            state = state.copy(
                                                isSyncing = false,
                                                tickets = if (tList.isNotEmpty()) tList else state.tickets,
                                                ticketCategories = if (cList.isNotEmpty()) cList else state.ticketCategories
                                            )
                                        } catch (_: Exception) {
                                            state = state.copy(isSyncing = false)
                                        }
                                    }
                                }, 2000)
                            }
                        }
                    }

                    webViewRef = this
                    loadUrl("https://care.plus.net.bd/customer/dashboard")
                }
            }
        )

        // Theme Picker Dialog (Default + 10 Windows Themes)
        if (showThemeDialog) {
            AlertDialog(
                onDismissRequest = { showThemeDialog = false },
                containerColor = theme.card,
                title = { Text("Select App Theme (11 Themes)", color = theme.textPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                text = {
                    LazyColumn(modifier = Modifier.heightIn(max = 360.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(AppThemes.indices.toList()) { idx ->
                            val item = AppThemes[idx]
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (idx == themeIndex) theme.accent.copy(alpha = 0.2f) else theme.bg)
                                    .border(1.dp, if (idx == themeIndex) theme.accent else theme.border, RoundedCornerShape(10.dp))
                                    .clickable {
                                        themeIndex = idx
                                        prefs.edit().putInt("theme_idx", idx).apply()
                                        showThemeDialog = false
                                    }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(16.dp).clip(CircleShape).background(item.accent))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(item.name, color = theme.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                }
                                if (idx == themeIndex) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = theme.accent, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showThemeDialog = false }) {
                        Text("Close", color = theme.accent)
                    }
                }
            )
        }

        // Create New Complain Ticket Dialog
        if (showNewTicketDialog) {
            CreateTicketDialog(
                categories = state.ticketCategories,
                theme = theme,
                onDismiss = { showNewTicketDialog = false },
                onSubmit = { catVal, descText ->
                    showNewTicketDialog = false
                    state = state.copy(isSyncing = true)
                    val submitJs = """
                        (function() {
                            var createBtn = Array.from(document.querySelectorAll('button, a')).find(el => el.innerText.includes('Create a New Ticket') || el.innerText.includes('New Ticket'));
                            if (createBtn) createBtn.click();
                            setTimeout(function() {
                                var sel = document.querySelector('select');
                                if (sel) {
                                    sel.value = ${JSONObject.quote(catVal)};
                                    sel.dispatchEvent(new Event('change', { bubbles: true }));
                                }
                                var ta = document.querySelector('textarea, input[name*="desc"], input[name*="note"]');
                                if (ta) {
                                    ta.value = ${JSONObject.quote(descText)};
                                    ta.dispatchEvent(new Event('input', { bubbles: true }));
                                }
                                setTimeout(function() {
                                    var subBtn = Array.from(document.querySelectorAll('button[type="submit"], button')).find(b => b.innerText.toLowerCase().includes('submit') || b.innerText.toLowerCase().includes('save') || b.innerText.toLowerCase().includes('create'));
                                    if (subBtn) subBtn.click();
                                }, 500);
                            }, 800);
                        })();
                    """.trimIndent()
                    webViewRef?.evaluateJavascript(submitJs) {
                        Toast.makeText(context, "Submitting ticket...", Toast.LENGTH_SHORT).show()
                        webViewRef?.postDelayed({
                            webViewRef?.loadUrl("https://care.plus.net.bd/customer/complainlist")
                        }, 2500)
                    }
                }
            )
        }
    }
}

@Composable
fun PlusnetGreenLogo(size: androidx.compose.ui.unit.Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF047835)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size * 0.68f)) {
            val w = this.size.width
            val h = this.size.height
            val stroke = w * 0.22f
            drawCircle(
                color = Color.White,
                radius = (w - stroke) / 2f,
                style = Stroke(width = stroke)
            )
            drawRect(
                color = Color(0xFF047835),
                topLeft = Offset(0f, h * 0.52f),
                size = Size(w * 0.45f, h * 0.48f)
            )
            drawRect(
                color = Color.White,
                topLeft = Offset(0f, h * 0.50f),
                size = Size(stroke, h * 0.50f)
            )
            drawCircle(
                color = Color.White,
                radius = w * 0.14f
            )
        }
    }
}

@Composable
fun DashboardTab(data: PlusnetState, theme: AppThemeConfig) {
    val isOnline = data.connectionStatus.contains("ONLINE", ignoreCase = true)
    val statusColor = if (isOnline) Color(0xFF10B981) else Color(0xFFEF4444)
    val grandTotalGB = data.monthlyDownGB + data.monthlyUpGB

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Hero Banner
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Brush.horizontalGradient(listOf(theme.heroStart, theme.heroEnd)))
                    .border(1.dp, theme.accent.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                    .padding(20.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("CURRENT PACKAGE", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(data.packageName, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(9.dp).clip(CircleShape).background(statusColor))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("${data.connectionStatus} • ${data.accountStatus}", color = statusColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Surface(color = Color.White.copy(alpha = 0.16f), shape = RoundedCornerShape(14.dp)) {
                        Text(data.planRate, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp))
                    }
                }
            }
        }

        // Expiry & Live Speed
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("Expiry Date", data.expiryDate, Icons.Default.EventAvailable, theme.secondary, theme, Modifier.weight(1f))
                StatCard("Live Speed", "${data.downSpeed} ↓", Icons.Default.Speed, theme.accent, theme, Modifier.weight(1f))
            }
        }

        // This Month's Total Usage Card
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(theme.card)
                    .border(1.dp, theme.border, RoundedCornerShape(20.dp))
                    .padding(16.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("This Month Total Usage", color = theme.textPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(String.format(Locale.US, "%.2f GB", grandTotalGB), color = theme.accent, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Total Download", color = theme.textSecondary, fontSize = 11.sp)
                        Text(String.format(Locale.US, "%.2f GB", data.monthlyDownGB), color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Total Upload", color = theme.textSecondary, fontSize = 11.sp)
                        Text(String.format(Locale.US, "%.2f GB", data.monthlyUpGB), color = Color(0xFFF97316), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }
        }

        // Daily Bar Chart
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(theme.card)
                    .border(1.dp, theme.border, RoundedCornerShape(20.dp))
                    .padding(16.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Daily Usage Chart (GB)", color = theme.textPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF38BDF8)))
                        Text(" Down  ", color = theme.textSecondary, fontSize = 11.sp)
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFF97316)))
                        Text(" Up", color = theme.textSecondary, fontSize = 11.sp)
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
                if (data.dailyUsage.isEmpty()) {
                    Text("Syncing monthly usage bars...", color = theme.textSecondary, fontSize = 12.sp)
                } else {
                    val bars = data.dailyUsage.takeLast(14)
                    val maxVal = bars.maxOfOrNull { maxOf(it.download, it.upload) }?.coerceAtLeast(1f) ?: 100f
                    Canvas(modifier = Modifier.fillMaxWidth().height(150.dp)) {
                        val groupWidth = size.width / bars.size
                        val barW = (groupWidth * 0.34f).coerceAtLeast(6f)
                        bars.forEachIndexed { i, item ->
                            val xStart = i * groupWidth + (groupWidth * 0.12f)
                            val dHeight = (item.download / maxVal) * size.height * 0.9f
                            val uHeight = (item.upload / maxVal) * size.height * 0.9f
                            drawRect(Color(0xFF38BDF8), Offset(xStart, size.height - dHeight), Size(barW, dHeight))
                            drawRect(Color(0xFFF97316), Offset(xStart + barW + 3f, size.height - uHeight), Size(barW, uHeight))
                        }
                    }
                }
            }
        }

        // Payment History
        item {
            Text("Payment History", color = theme.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
        }
        items(data.payments) { pay ->
            val amountDisplay = if (pay.receive != "0" && pay.receive.isNotEmpty()) "+৳ ${pay.receive}" else "৳ ${pay.bill}"
            val amountColor = if (pay.receive != "0" && pay.receive.isNotEmpty()) Color(0xFF10B981) else Color(0xFFF59E0B)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(theme.card)
                    .border(1.dp, theme.border, RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(pay.date, color = theme.textSecondary, fontSize = 12.sp)
                    Text(amountDisplay, color = amountColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(pay.remarks, color = theme.textPrimary, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun ReportTab(data: PlusnetState, theme: AppThemeConfig) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("Download Speed", data.downSpeed, Icons.Default.CloudDownload, Color(0xFF38BDF8), theme, Modifier.weight(1f))
                StatCard("Upload Speed", data.upSpeed, Icons.Default.CloudUpload, Color(0xFFF97316), theme, Modifier.weight(1f))
            }
        }

        // Live Speed Graph WITH Numeric Labels
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(theme.card)
                    .border(1.dp, theme.border, RoundedCornerShape(20.dp))
                    .padding(18.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Live Speed Graph (with Numbers)", color = theme.textPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text("● LIVE", color = Color(0xFF10B981), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Canvas(modifier = Modifier.fillMaxWidth().height(180.dp)) {
                    val pts = data.liveSpeedHistory
                    if (pts.size > 1) {
                        val maxV = (pts.maxOrNull() ?: 32f).coerceAtLeast(10f)
                        val leftPad = 65f
                        val chartW = size.width - leftPad - 20f
                        val chartH = size.height - 30f
                        val stepX = chartW / (pts.size - 1)

                        val labelPaint = Paint().apply {
                            color = android.graphics.Color.GRAY
                            textSize = 26f
                            isAntiAlias = true
                        }
                        val pointPaint = Paint().apply {
                            color = android.graphics.Color.parseColor("#38BDF8")
                            textSize = 24f
                            isAntiAlias = true
                            textAlign = Paint.Align.CENTER
                        }

                        // Y-Axis Numeric Markers (Max, Mid, 0)
                        drawContext.canvas.nativeCanvas.drawText(String.format(Locale.US, "%.0fM", maxV), 0f, 26f, labelPaint)
                        drawContext.canvas.nativeCanvas.drawText(String.format(Locale.US, "%.0fM", maxV / 2f), 0f, chartH / 2f + 10f, labelPaint)
                        drawContext.canvas.nativeCanvas.drawText("0", 15f, chartH, labelPaint)

                        val path = Path()
                        pts.forEachIndexed { i, v ->
                            val x = leftPad + (i * stepX)
                            val y = chartH - ((v / maxV) * (chartH - 25f))
                            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                            drawCircle(color = theme.accent, radius = 6f, center = Offset(x, y))
                            drawContext.canvas.nativeCanvas.drawText(
                                String.format(Locale.US, "%.1f", v),
                                x,
                                y - 14f,
                                pointPaint
                            )
                        }
                        drawPath(path = path, color = theme.accent, style = Stroke(width = 5f, cap = StrokeCap.Round))
                    }
                }
            }
        }

        // Previous Sessions History Header
        item {
            Text("Session Log (Previous & Current Sessions)", color = theme.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
        }

        if (data.sessionLogs.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(theme.card).padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Fetching previous session logs...", color = theme.textSecondary, fontSize = 13.sp)
                }
            }
        } else {
            items(data.sessionLogs) { sess ->
                val isCurrent = sess.discDate.contains("Active", ignoreCase = true) || sess.discDate.isBlank()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(theme.card)
                        .border(1.dp, if (isCurrent) Color(0xFF10B981) else theme.border, RoundedCornerShape(16.dp))
                        .padding(14.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (isCurrent) "● ACTIVE SESSION" else "Session Time: ${sess.duration}",
                            color = if (isCurrent) Color(0xFF10B981) else theme.accent,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        Text("↓ ${sess.download}  |  ↑ ${sess.upload}", color = theme.textPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Start: ${sess.connDate}", color = theme.textSecondary, fontSize = 12.sp)
                    Text("End:   ${sess.discDate}", color = theme.textSecondary, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun ComplainTab(data: PlusnetState, theme: AppThemeConfig, onOpenNewTicket: () -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Button(
                onClick = onOpenNewTicket,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = theme.accent)
            ) {
                Icon(Icons.Default.AddCircle, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Create a New Complain Ticket", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }

        item {
            Text("Complain & Ticket History", color = theme.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        if (data.tickets.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(theme.card).padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Loading complain tickets...", color = theme.textSecondary, fontSize = 13.sp)
                }
            }
        } else {
            items(data.tickets) { ticket ->
                val isCompleted = ticket.status.contains("COMPLET", ignoreCase = true)
                val badgeColor = if (isCompleted) Color(0xFF10B981) else Color(0xFFF59E0B)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(theme.card)
                        .border(1.dp, theme.border, RoundedCornerShape(16.dp))
                        .padding(14.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("#${ticket.id} • ${ticket.title}", color = theme.textPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Surface(color = badgeColor.copy(alpha = 0.2f), shape = RoundedCornerShape(8.dp)) {
                            Text(ticket.status, color = badgeColor, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(ticket.description, color = theme.textSecondary, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(ticket.date, color = theme.textSecondary, fontSize = 11.sp)
                        if (ticket.assigned.isNotEmpty()) {
                            Text("Assigned: ${ticket.assigned}", color = theme.accent, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTicketDialog(
    categories: List<TicketCategory>,
    theme: AppThemeConfig,
    onDismiss: () -> Unit,
    onSubmit: (String, String) -> Unit
) {
    var selectedCat by remember { mutableStateOf(categories.firstOrNull() ?: TicketCategory("Link Down", "Link Down")) }
    var expanded by remember { mutableStateOf(false) }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = theme.card,
        title = { Text("Create New Complain", color = theme.textPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Select Category (Fetched from Plusnet)", color = theme.textSecondary, fontSize = 12.sp)
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                    OutlinedTextField(
                        value = selectedCat.label,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = theme.textPrimary,
                            unfocusedTextColor = theme.textPrimary,
                            focusedBorderColor = theme.accent
                        )
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.label) },
                                onClick = {
                                    selectedCat = cat
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Problem Description") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = theme.textPrimary,
                        unfocusedTextColor = theme.textPrimary,
                        focusedBorderColor = theme.accent
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (description.isNotBlank()) onSubmit(selectedCat.value, description.trim()) },
                colors = ButtonDefaults.buttonColors(containerColor = theme.accent)
            ) {
                Text("Submit Ticket", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = theme.textSecondary)
            }
        }
    )
}

@Composable
fun ProfileTab(data: PlusnetState, theme: AppThemeConfig) {
    val context = LocalContext.current
    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(theme.card)
                .border(1.dp, theme.border, RoundedCornerShape(22.dp))
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PlusnetGreenLogo(size = 64.dp)
            Spacer(modifier = Modifier.height(12.dp))
            Text(data.fullName, color = theme.textPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            if (data.email.isNotEmpty()) {
                Text(data.email, color = theme.accent, fontSize = 13.sp)
            }
        }

        ProfileInfoRow("User Name", data.userName.ifEmpty { "--" }, Icons.Default.Badge, theme.accent, theme)
        ProfileInfoRow("Mobile Number", data.mobile.ifEmpty { "--" }, Icons.Default.PhoneAndroid, Color(0xFF10B981), theme)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(theme.card)
                .border(1.dp, theme.border, RoundedCornerShape(16.dp))
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Fingerprint, contentDescription = null, tint = theme.secondary, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("Customer ID", color = theme.textSecondary, fontSize = 12.sp)
                    Text(data.customerId.ifEmpty { "--" }, color = theme.textPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
            IconButton(onClick = {
                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("Customer ID", data.customerId))
                Toast.makeText(context, "Customer ID Copied", Toast.LENGTH_SHORT).show()
            }) {
                Icon(Icons.Default.ContentCopy, contentDescription = "Copy ID", tint = theme.accent)
            }
        }
    }
}

@Composable
fun ProfileInfoRow(label: String, value: String, icon: ImageVector, tint: Color, theme: AppThemeConfig) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(theme.card)
            .border(1.dp, theme.border, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(label, color = theme.textSecondary, fontSize = 12.sp)
            Text(value, color = theme.textPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    tint: Color,
    theme: AppThemeConfig,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(theme.card)
            .border(1.dp, theme.border, RoundedCornerShape(18.dp))
            .padding(16.dp)
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.height(10.dp))
        Text(title, color = theme.textSecondary, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(value, color = theme.textPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}
