package com.plusnet.care

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.webkit.*
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay
import org.json.JSONObject
import org.json.JSONTokener

data class PaymentItem(val date: String, val bill: String, val receive: String, val remarks: String)
data class DailyUsage(val label: String, val download: Float, val upload: Float)

data class PlusnetState(
    val isLoggedIn: Boolean = false,
    val isSyncing: Boolean = false,
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
    val downSpeed: String = "0.0 Mbps",
    val upSpeed: String = "0.0 Mbps",
    val sessionDownload: String = "0 MB",
    val sessionUpload: String = "0 MB",
    val liveSpeedHistory: List<Float> = listOf(5f, 12f, 8f, 20f, 15f, 25f)
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
    val prefs = remember { context.getSharedPreferences("plusnet_native_v4", Context.MODE_PRIVATE) }

    var savedUser by remember { mutableStateOf(prefs.getString("user", "") ?: "") }
    var savedPass by remember { mutableStateOf(prefs.getString("pass", "") ?: "") }
    var state by remember {
        mutableStateOf(
            PlusnetState(
                isLoggedIn = false,
                isSyncing = savedUser.isNotEmpty() && savedPass.isNotEmpty()
            )
        )
    }
    var selectedTab by remember { mutableIntStateOf(0) }
    var loginError by remember { mutableStateOf<String?>(null) }
    var hiddenWebView by remember { mutableStateOf<WebView?>(null) }
    var loginAttemptCount by remember { mutableIntStateOf(0) }

    // Safety timeout so loading spinner never gets stuck
    LaunchedEffect(state.isSyncing) {
        if (state.isSyncing) {
            delay(16000)
            if (state.isSyncing && !state.isLoggedIn) {
                state = state.copy(isSyncing = false)
                loginError = "Login timeout. Check credentials or internet and try again."
            }
        }
    }

    // Auto refresh live data every 12 seconds when logged in
    LaunchedEffect(state.isLoggedIn) {
        while (state.isLoggedIn) {
            delay(12000)
            val target = if (selectedTab == 0) "dashboard" else "report"
            hiddenWebView?.loadUrl("https://care.plus.net.bd/customer/$target")
        }
    }

    val bgDark = Color(0xFF0A0E17)
    val glassCard = Color(0xFF141B2D)
    val glassBorder = Color(0xFF24304A)
    val accentBlue = Color(0xFF38BDF8)
    val accentPurple = Color(0xFF818CF8)
    val greenOnline = Color(0xFF10B981)
    val orangeUpload = Color(0xFFF97316)

    Box(modifier = Modifier.fillMaxSize().background(bgDark)) {
        // Active 1dp Background WebView (never suspended by Android OS)
        AndroidView(
            modifier = Modifier.size(1.dp).alpha(0.01f),
            factory = { ctx ->
                WebView(ctx).apply {
                    val cm = CookieManager.getInstance()
                    cm.setAcceptCookie(true)
                    cm.setAcceptThirdPartyCookies(this, true)
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        databaseEnabled = true
                        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        userAgentString = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
                    }

                    webViewClient = object : WebViewClient() {
                        override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                            if (request?.isForMainFrame == true) {
                                state = state.copy(isSyncing = false)
                                loginError = "Network error: ${error?.description ?: "Unable to connect"}"
                            }
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            CookieManager.getInstance().flush()
                            val u = prefs.getString("user", "") ?: ""
                            val p = prefs.getString("pass", "") ?: ""

                            if (url != null && url.contains("/login")) {
                                if (u.isNotEmpty() && p.isNotEmpty()) {
                                    if (loginAttemptCount >= 2) {
                                        state = state.copy(isSyncing = false)
                                        loginError = "Invalid User Name or Password"
                                        return
                                    }
                                    loginAttemptCount++
                                    view?.postDelayed({
                                        val jsLogin = """
                                            (function() {
                                                function setNativeValue(el, value) {
                                                    var lastValue = el.value;
                                                    el.value = value;
                                                    var event = new Event('input', { bubbles: true });
                                                    var tracker = el._valueTracker;
                                                    if (tracker) { tracker.setValue(lastValue); }
                                                    el.dispatchEvent(event);
                                                    el.dispatchEvent(new Event('change', { bubbles: true }));
                                                }
                                                var inputs = Array.from(document.querySelectorAll('input'));
                                                var pInput = inputs.find(i => i.type === 'password');
                                                var uInput = inputs.find(i => i.type === 'text' || i.type === 'email' || (!i.type && i !== pInput));
                                                if (uInput && pInput) {
                                                    uInput.focus();
                                                    setNativeValue(uInput, ${JSONObject.quote(u)});
                                                    pInput.focus();
                                                    setNativeValue(pInput, ${JSONObject.quote(p)});
                                                    setTimeout(function() {
                                                        var btn = document.querySelector('button[type="submit"], input[type="submit"], button');
                                                        if (btn) {
                                                            btn.click();
                                                        } else if (pInput.form) {
                                                            pInput.form.submit();
                                                        }
                                                    }, 500);
                                                }
                                            })();
                                        """.trimIndent()
                                        view.evaluateJavascript(jsLogin, null)
                                    }, 1200)
                                }
                            } else if (url != null && url.contains("/dashboard")) {
                                loginAttemptCount = 0
                                view?.postDelayed({
                                    val extractDashJs = """
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
                                            var fName = "";
                                            var mail = "";
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
                                            if (window.Highcharts && Highcharts.charts) {
                                                Highcharts.charts.forEach(function(c) {
                                                    if (c && c.series && c.series.length >= 1) {
                                                        var cats = (c.xAxis && c.xAxis[0] && c.xAxis[0].categories) ? c.xAxis[0].categories : [];
                                                        var s0 = c.series[0].data || [];
                                                        var s1 = (c.series.length > 1) ? c.series[1].data : [];
                                                        for (var k = 0; k < s0.length; k++) {
                                                            chartData.push({
                                                                label: cats[k] || (s0[k].name || ("" + (k+1))),
                                                                down: s0[k].y || 0,
                                                                up: (s1[k] ? (s1[k].y || 0) : 0)
                                                            });
                                                        }
                                                    }
                                                });
                                            }

                                            return JSON.stringify({
                                                pkg: pkg, acc: acc, conn: conn, exp: exp, rate: rate,
                                                fullName: fName, email: mail, userName: uName, mobile: mob, cid: cid,
                                                payments: rows, chart: chartData
                                            });
                                        })();
                                    """.trimIndent()

                                    view.evaluateJavascript(extractDashJs) { raw ->
                                        try {
                                            val unquoted = JSONTokener(raw).nextValue().toString()
                                            val obj = JSONObject(unquoted)

                                            val payList = mutableListOf<PaymentItem>()
                                            val pArr = obj.optJSONArray("payments")
                                            if (pArr != null) {
                                                for (i in 0 until pArr.length()) {
                                                    val item = pArr.getJSONObject(i)
                                                    payList.add(
                                                        PaymentItem(
                                                            date = item.optString("date"),
                                                            bill = item.optString("bill"),
                                                            receive = item.optString("receive"),
                                                            remarks = item.optString("remarks")
                                                        )
                                                    )
                                                }
                                            }

                                            val usageList = mutableListOf<DailyUsage>()
                                            val cArr = obj.optJSONArray("chart")
                                            if (cArr != null) {
                                                for (i in 0 until cArr.length()) {
                                                    val cItem = cArr.getJSONObject(i)
                                                    usageList.add(
                                                        DailyUsage(
                                                            label = cItem.optString("label"),
                                                            download = cItem.optDouble("down", 0.0).toFloat(),
                                                            upload = cItem.optDouble("up", 0.0).toFloat()
                                                        )
                                                    )
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
                                                userName = obj.optString("userName").ifEmpty { u },
                                                mobile = obj.optString("mobile").ifEmpty { state.mobile },
                                                customerId = obj.optString("cid").ifEmpty { state.customerId },
                                                payments = if (payList.isNotEmpty()) payList else state.payments,
                                                dailyUsage = if (usageList.isNotEmpty()) usageList else state.dailyUsage
                                            )
                                            loginError = null
                                            view.loadUrl("https://care.plus.net.bd/customer/report")
                                        } catch (_: Exception) {
                                            state = state.copy(isLoggedIn = true, isSyncing = false)
                                        }
                                    }
                                }, 2200)
                            } else if (url != null && url.contains("/report")) {
                                view?.postDelayed({
                                    val extractReportJs = """
                                        (function() {
                                            var text = document.body.innerText || "";
                                            var speeds = text.match(/(\d+\.?\d*\s*(?:Mbps|Kbps|MB\/s|KB\/s|bps))/gi) || [];
                                            var usages = text.match(/(\d+\.?\d*\s*(?:GB|MB|TB|KB))/gi) || [];
                                            return JSON.stringify({
                                                downSpd: speeds[0] || "0.0 Mbps",
                                                upSpd: speeds[1] || "0.0 Mbps",
                                                sessDown: usages[0] || "0 MB",
                                                sessUp: usages[1] || "0 MB"
                                            });
                                        })();
                                    """.trimIndent()

                                    view.evaluateJavascript(extractReportJs) { raw ->
                                        try {
                                            val unquoted = JSONTokener(raw).nextValue().toString()
                                            val obj = JSONObject(unquoted)
                                            val dSpd = obj.optString("downSpd", "0.0 Mbps")
                                            val uSpd = obj.optString("upSpd", "0.0 Mbps")
                                            val num = Regex("(\\d+\\.?\\d*)").find(dSpd)?.groupValues?.get(1)?.toFloatOrNull() ?: 5f
                                            val updatedHist = (state.liveSpeedHistory + num).takeLast(12)

                                            state = state.copy(
                                                isSyncing = false,
                                                downSpeed = dSpd,
                                                upSpeed = uSpd,
                                                sessionDownload = obj.optString("sessDown", "0 MB"),
                                                sessionUpload = obj.optString("sessUp", "0 MB"),
                                                liveSpeedHistory = updatedHist
                                            )
                                        } catch (_: Exception) {
                                            state = state.copy(isSyncing = false)
                                        }
                                    }
                                }, 2200)
                            }
                        }
                    }

                    hiddenWebView = this
                    if (savedUser.isNotEmpty() && savedPass.isNotEmpty()) {
                        loadUrl("https://care.plus.net.bd/customer/dashboard")
                    }
                }
            }
        )

        if (!state.isLoggedIn) {
            LoginScreen(
                initialUser = savedUser,
                initialPass = savedPass,
                isLoading = state.isSyncing,
                errorMsg = loginError,
                onLogin = { u, p ->
                    savedUser = u
                    savedPass = p
                    loginAttemptCount = 0
                    loginError = null
                    prefs.edit().putString("user", u).putString("pass", p).apply()
                    state = state.copy(isSyncing = true)
                    hiddenWebView?.loadUrl("https://care.plus.net.bd/customer/login")
                }
            )
        } else {
            Scaffold(
                containerColor = bgDark,
                topBar = {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(glassCard)
                            .padding(horizontal = 18.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(11.dp))
                                    .background(Brush.linearGradient(listOf(accentBlue, accentPurple))),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Wifi, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("PLUSNET CARE", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text(
                                    text = if (state.isSyncing) "Syncing live data..." else "Connected • ${state.userName}",
                                    color = if (state.isSyncing) Color(0xFFFBBF24) else greenOnline,
                                    fontSize = 11.sp
                                )
                            }
                        }
                        Row {
                            IconButton(onClick = {
                                state = state.copy(isSyncing = true)
                                hiddenWebView?.loadUrl("https://care.plus.net.bd/customer/dashboard")
                            }) {
                                Icon(Icons.Default.Refresh, contentDescription = "Sync", tint = Color.White)
                            }
                            IconButton(onClick = {
                                prefs.edit().clear().apply()
                                CookieManager.getInstance().removeAllCookies(null)
                                state = PlusnetState(isLoggedIn = false)
                            }) {
                                Icon(Icons.Default.Logout, contentDescription = "Logout", tint = Color(0xFFF87171))
                            }
                        }
                    }
                },
                bottomBar = {
                    NavigationBar(containerColor = glassCard, contentColor = Color.White) {
                        NavigationBarItem(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            icon = { Icon(Icons.Default.Dashboard, contentDescription = null) },
                            label = { Text("Dashboard") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = accentBlue, selectedTextColor = accentBlue,
                                indicatorColor = accentBlue.copy(alpha = 0.15f),
                                unselectedIconColor = Color.Gray, unselectedTextColor = Color.Gray
                            )
                        )
                        NavigationBarItem(
                            selected = selectedTab == 1,
                            onClick = {
                                selectedTab = 1
                                state = state.copy(isSyncing = true)
                                hiddenWebView?.loadUrl("https://care.plus.net.bd/customer/report")
                            },
                            icon = { Icon(Icons.Default.Speed, contentDescription = null) },
                            label = { Text("Live Speed & Session") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = accentBlue, selectedTextColor = accentBlue,
                                indicatorColor = accentBlue.copy(alpha = 0.15f),
                                unselectedIconColor = Color.Gray, unselectedTextColor = Color.Gray
                            )
                        )
                        NavigationBarItem(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            icon = { Icon(Icons.Default.AccountCircle, contentDescription = null) },
                            label = { Text("Profile") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = accentBlue, selectedTextColor = accentBlue,
                                indicatorColor = accentBlue.copy(alpha = 0.15f),
                                unselectedIconColor = Color.Gray, unselectedTextColor = Color.Gray
                            )
                        )
                    }
                }
            ) { padding ->
                Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                    if (state.isSyncing) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = accentBlue, trackColor = glassCard)
                    }
                    when (selectedTab) {
                        0 -> DashboardTab(state, glassCard, glassBorder, accentBlue, accentPurple, greenOnline, orangeUpload)
                        1 -> ReportTab(state, glassCard, glassBorder, accentBlue, accentPurple, greenOnline)
                        2 -> ProfileTab(state, glassCard, glassBorder, accentBlue, accentPurple, greenOnline)
                    }
                }
            }
        }
    }
}

@Composable
fun LoginScreen(
    initialUser: String,
    initialPass: String,
    isLoading: Boolean,
    errorMsg: String?,
    onLogin: (String, String) -> Unit
) {
    var user by remember { mutableStateOf(initialUser) }
    var pass by remember { mutableStateOf(initialPass) }
    var showPass by remember { mutableStateOf(false) }

    val glassCard = Color(0xFF141B2D)
    val accentBlue = Color(0xFF38BDF8)
    val accentPurple = Color(0xFF818CF8)

    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(glassCard)
                .border(1.dp, Color(0xFF24304A), RoundedCornerShape(24.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(accentBlue, accentPurple))),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Router, contentDescription = null, tint = Color.White, modifier = Modifier.size(34.dp))
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text("PLUSNET", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text("Native Self-Care Client", color = Color.Gray, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(22.dp))

            OutlinedTextField(
                value = user,
                onValueChange = { user = it },
                label = { Text("User Name (e.g. PPG092)") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                    focusedBorderColor = accentBlue, unfocusedBorderColor = Color(0xFF24304A)
                )
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = pass,
                onValueChange = { pass = it },
                label = { Text("Password") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                trailingIcon = {
                    IconButton(onClick = { showPass = !showPass }) {
                        Icon(if (showPass) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = null)
                    }
                },
                visualTransformation = if (showPass) VisualTransformation.None else PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                    focusedBorderColor = accentBlue, unfocusedBorderColor = Color(0xFF24304A)
                )
            )

            if (errorMsg != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(errorMsg, color = Color(0xFFF87171), fontSize = 13.sp)
            }

            Spacer(modifier = Modifier.height(22.dp))
            Button(
                onClick = { if (user.isNotBlank() && pass.isNotBlank()) onLogin(user.trim(), pass.trim()) },
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accentBlue)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                } else {
                    Text("Sign In", color = Color(0xFF0A0E17), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
fun DashboardTab(
    data: PlusnetState,
    glassCard: Color,
    glassBorder: Color,
    accentBlue: Color,
    accentPurple: Color,
    greenOnline: Color,
    orangeUpload: Color
) {
    val isOnline = data.connectionStatus.contains("ONLINE", ignoreCase = true)
    val statusColor = if (isOnline) greenOnline else Color(0xFFF87171)

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Brush.horizontalGradient(listOf(Color(0xFF1E3A8A), Color(0xFF312E81))))
                    .border(1.dp, accentBlue.copy(alpha = 0.45f), RoundedCornerShape(20.dp))
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("PACKAGE", color = accentBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(data.packageName, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(9.dp).clip(CircleShape).background(statusColor))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("${data.connectionStatus} • ${data.accountStatus}", color = statusColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Surface(color = Color.White.copy(alpha = 0.14f), shape = RoundedCornerShape(14.dp)) {
                        Text(
                            text = data.planRate,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                        )
                    }
                }
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("Expiry Date", data.expiryDate, Icons.Default.EventAvailable, accentPurple, glassCard, glassBorder, Modifier.weight(1f))
                StatCard("Live Speed", "${data.downSpeed} ↓", Icons.Default.Speed, accentBlue, glassCard, glassBorder, Modifier.weight(1f))
            }
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(glassCard)
                    .border(1.dp, glassBorder, RoundedCornerShape(20.dp))
                    .padding(16.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Daily Data Usage (GB)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(accentBlue))
                        Text(" Down  ", color = Color.Gray, fontSize = 11.sp)
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(orangeUpload))
                        Text(" Up", color = Color.Gray, fontSize = 11.sp)
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
                if (data.dailyUsage.isEmpty()) {
                    Text("Loading monthly chart bars...", color = Color.Gray, fontSize = 12.sp)
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
                            drawRect(
                                color = accentBlue,
                                topLeft = Offset(xStart, size.height - dHeight),
                                size = Size(barW, dHeight)
                            )
                            drawRect(
                                color = orangeUpload,
                                topLeft = Offset(xStart + barW + 3f, size.height - uHeight),
                                size = Size(barW, uHeight)
                            )
                        }
                    }
                }
            }
        }

        item {
            Text("Payment History", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
        }

        if (data.payments.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(glassCard).padding(18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Syncing payment history...", color = Color.Gray, fontSize = 13.sp)
                }
            }
        } else {
            items(data.payments) { pay ->
                val amountDisplay = if (pay.receive != "0" && pay.receive.isNotEmpty()) "+৳ ${pay.receive}" else "৳ ${pay.bill}"
                val amountColor = if (pay.receive != "0" && pay.receive.isNotEmpty()) greenOnline else Color(0xFFFBBF24)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(glassCard)
                        .border(1.dp, glassBorder, RoundedCornerShape(16.dp))
                        .padding(14.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(pay.date, color = Color.Gray, fontSize = 12.sp)
                        Text(amountDisplay, color = amountColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(pay.remarks, color = Color.White, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun ReportTab(
    data: PlusnetState,
    glassCard: Color,
    glassBorder: Color,
    accentBlue: Color,
    accentPurple: Color,
    greenOnline: Color
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("Session Usage & Live Speed", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text("Real-time data fetched from /customer/report", color = Color.Gray, fontSize = 12.sp)
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("Download Speed", data.downSpeed, Icons.Default.CloudDownload, accentBlue, glassCard, glassBorder, Modifier.weight(1f))
                StatCard("Upload Speed", data.upSpeed, Icons.Default.CloudUpload, accentPurple, glassCard, glassBorder, Modifier.weight(1f))
            }
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("Session Download", data.sessionDownload, Icons.Default.DataUsage, greenOnline, glassCard, glassBorder, Modifier.weight(1f))
                StatCard("Session Upload", data.sessionUpload, Icons.Default.UploadFile, Color(0xFFF97316), glassCard, glassBorder, Modifier.weight(1f))
            }
        }
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(glassCard)
                    .border(1.dp, glassBorder, RoundedCornerShape(20.dp))
                    .padding(18.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Live Speed Graph", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text("● LIVE", color = greenOnline, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Canvas(modifier = Modifier.fillMaxWidth().height(160.dp)) {
                    val pts = data.liveSpeedHistory
                    if (pts.size > 1) {
                        val maxV = (pts.maxOrNull() ?: 50f).coerceAtLeast(10f)
                        val stepX = size.width / (pts.size - 1)
                        val path = Path()
                        pts.forEachIndexed { i, v ->
                            val x = i * stepX
                            val y = size.height - ((v / maxV) * size.height * 0.85f) - 10f
                            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                            drawCircle(color = accentBlue, radius = 6f, center = Offset(x, y))
                        }
                        drawPath(path = path, color = accentBlue, style = Stroke(width = 5f, cap = StrokeCap.Round))
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileTab(
    data: PlusnetState,
    glassCard: Color,
    glassBorder: Color,
    accentBlue: Color,
    accentPurple: Color,
    greenOnline: Color
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(glassCard)
                .border(1.dp, glassBorder, RoundedCornerShape(22.dp))
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(accentBlue, accentPurple))),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(38.dp))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(data.fullName, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            if (data.email.isNotEmpty()) {
                Text(data.email, color = accentBlue, fontSize = 13.sp)
            }
        }

        ProfileInfoRow("User Name", data.userName, Icons.Default.Badge, accentBlue, glassCard, glassBorder)
        ProfileInfoRow("Mobile Number", data.mobile.ifEmpty { "--" }, Icons.Default.PhoneAndroid, greenOnline, glassCard, glassBorder)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(glassCard)
                .border(1.dp, glassBorder, RoundedCornerShape(16.dp))
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Fingerprint, contentDescription = null, tint = accentPurple, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("Customer ID", color = Color.Gray, fontSize = 12.sp)
                    Text(data.customerId.ifEmpty { "--" }, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
            IconButton(onClick = {
                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("Customer ID", data.customerId))
                Toast.makeText(context, "Customer ID Copied", Toast.LENGTH_SHORT).show()
            }) {
                Icon(Icons.Default.ContentCopy, contentDescription = "Copy ID", tint = accentBlue)
            }
        }
    }
}

@Composable
fun ProfileInfoRow(label: String, value: String, icon: ImageVector, tint: Color, bg: Color, border: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(label, color = Color.Gray, fontSize = 12.sp)
            Text(value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    tint: Color,
    bg: Color,
    border: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(18.dp))
            .padding(16.dp)
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.height(10.dp))
        Text(title, color = Color.Gray, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}
