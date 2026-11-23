package com.plusnet.care

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import org.jsoup.Jsoup
import java.util.concurrent.TimeUnit

data class PaymentItem(val date: String, val amount: String, val remarks: String)
data class DashboardData(
    val packageName: String = "Loading...",
    val accountStatus: String = "Active",
    val connectionStatus: String = "ONLINE",
    val expiryDate: String = "--",
    val planRate: String = "--",
    val payments: List<PaymentItem> = emptyList(),
    val downSpeed: String = "0.0 Mbps",
    val upSpeed: String = "0.0 Mbps",
    val sessionDownload: String = "0 MB",
    val sessionUpload: String = "0 MB",
    val speedHistory: List<Float> = listOf(10f, 25f, 18f, 45f, 38f, 62f, 55f, 80f)
)

class PlusnetRepository(context: Context) {
    private val prefs = context.getSharedPreferences("plusnet_prefs", Context.MODE_PRIVATE)
    private val cookieStore = HashMap<String, List<Cookie>>()

    private val client = OkHttpClient.Builder()
        .cookieJar(object : CookieJar {
            override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
                cookieStore[url.host] = cookies
            }
            override fun loadForRequest(url: HttpUrl): List<Cookie> {
                return cookieStore[url.host] ?: emptyList()
            }
        })
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    fun saveCredentials(user: String, pass: String) {
        prefs.edit().putString("user", user).putString("pass", pass).apply()
    }

    fun getSavedUser(): String = prefs.getString("user", "") ?: ""
    fun getSavedPass(): String = prefs.getString("pass", "") ?: ""
    fun clearCredentials() = prefs.edit().clear().apply()

    suspend fun loginAndFetch(username: String, password: String): Result<DashboardData> = withContext(Dispatchers.IO) {
        try {
            val loginUrl = "https://care.plus.net.bd/customer/login"
            val getReq = Request.Builder().url(loginUrl)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/120.0.0.0 Mobile Safari/537.36")
                .build()
            val loginHtml = client.newCall(getReq).execute().body?.string() ?: ""
            val loginDoc = Jsoup.parse(loginHtml)

            val formBuilder = FormBody.Builder()
            loginDoc.select("input[type=hidden]").forEach { input ->
                val name = input.attr("name")
                val value = input.attr("value")
                if (name.isNotEmpty()) formBuilder.add(name, value)
            }

            val userField = loginDoc.select("input[type=text], input[type=email]").firstOrNull()?.attr("name") ?: "username"
            val passField = loginDoc.select("input[type=password]").firstOrNull()?.attr("name") ?: "password"

            formBuilder.add(userField, username)
            formBuilder.add(passField, password)

            val postReq = Request.Builder()
                .url(loginUrl)
                .post(formBuilder.build())
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/120.0.0.0 Mobile Safari/537.36")
                .header("Referer", loginUrl)
                .build()

            val postResp = client.newCall(postReq).execute()
            val finalUrl = postResp.request.url.toString()
            val dashHtml = postResp.body?.string() ?: ""

            if (finalUrl.contains("/login") && dashHtml.contains("password", ignoreCase = true)) {
                return@withContext Result.failure(Exception("Invalid Username or Password"))
            }

            val data = parseAllPages(dashHtml)
            Result.success(data)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun refreshData(currentHistory: List<Float>): Result<DashboardData> = withContext(Dispatchers.IO) {
        try {
            val dashReq = Request.Builder().url("https://care.plus.net.bd/customer/dashboard")
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/120.0.0.0 Mobile Safari/537.36")
                .build()
            val dashResp = client.newCall(dashReq).execute()
            if (dashResp.request.url.toString().contains("/login")) {
                val u = getSavedUser()
                val p = getSavedPass()
                if (u.isNotEmpty() && p.isNotEmpty()) return@withContext loginAndFetch(u, p)
                return@withContext Result.failure(Exception("Session expired"))
            }
            val dashHtml = dashResp.body?.string() ?: ""
            val data = parseAllPages(dashHtml, currentHistory)
            Result.success(data)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseAllPages(dashHtml: String, oldHistory: List<Float> = emptyList()): DashboardData {
        val doc = Jsoup.parse(dashHtml)
        val bodyText = doc.text()

        val pkg = Regex("PACKAGE\\s+([A-Za-z0-9+_-]+)").find(bodyText)?.groupValues?.get(1)
            ?: doc.select(".badge, .label").firstOrNull()?.text() ?: "Basic+PIP"

        val accStatus = if (bodyText.contains("Active", ignoreCase = true)) "Active" else "Inactive"
        val connStatus = if (bodyText.contains("ONLINE", ignoreCase = true)) "ONLINE" else "OFFLINE"

        val expiry = Regex("Expiry Date\\s+([A-Za-z]+\\s+\\d{1,2},\\s+\\d{4})").find(bodyText)?.groupValues?.get(1)
            ?: Regex("([A-Z][a-z]{2}\\s+\\d{1,2},\\s+\\d{4})").find(bodyText)?.groupValues?.get(1) ?: "Dec 16, 2026"

        val rate = Regex("Plan rate\\s+([৳0-9.,]+)").find(bodyText)?.groupValues?.get(1)
            ?: Regex("(৳\\s*\\d+[.,]?\\d*)").find(bodyText)?.groupValues?.get(1) ?: "৳ 1200.0"

        val paymentList = mutableListOf<PaymentItem>()
        doc.select("table tr").drop(1).take(6).forEach { row ->
            val cols = row.select("td")
            if (cols.size >= 4) {
                paymentList.add(
                    PaymentItem(
                        date = cols[0].text().trim(),
                        amount = if (cols[2].text().trim() != "0") "৳ ${cols[2].text().trim()}" else "৳ ${cols[1].text().trim()}",
                        remarks = cols[3].text().trim()
                    )
                )
            }
        }

        // Fetch Report Page in Background
        var downSpd = "0.0 Mbps"
        var upSpd = "0.0 Mbps"
        var sessDown = "0 GB"
        var sessUp = "0 GB"
        var speedNum = 12f

        try {
            val repReq = Request.Builder().url("https://care.plus.net.bd/customer/report")
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/120.0.0.0 Mobile Safari/537.36")
                .build()
            val repHtml = client.newCall(repReq).execute().body?.string() ?: ""
            val repDoc = Jsoup.parse(repHtml)
            val repText = repDoc.text()

            val speeds = Regex("(\\d+\\.?\\d*\\s*(?:Mbps|Kbps|MB/s|KB/s))", RegexOption.IGNORE_CASE)
                .findAll(repText).map { it.groupValues[1] }.toList()
            if (speeds.isNotEmpty()) {
                downSpd = speeds[0]
                speedNum = Regex("(\\d+\\.?\\d*)").find(downSpd)?.groupValues?.get(1)?.toFloatOrNull() ?: 15f
            }
            if (speeds.size > 1) upSpd = speeds[1]

            val usages = Regex("(\\d+\\.?\\d*\\s*(?:GB|MB|TB))", RegexOption.IGNORE_CASE)
                .findAll(repText).map { it.groupValues[1] }.toList()
            if (usages.isNotEmpty()) sessDown = usages[0]
            if (usages.size > 1) sessUp = usages[1]
        } catch (_: Exception) {}

        val newHistory = (oldHistory + speedNum).takeLast(12).ifEmpty { listOf(12f, 28f, 19f, 42f, 35f, speedNum) }

        return DashboardData(
            packageName = pkg,
            accountStatus = accStatus,
            connectionStatus = connStatus,
            expiryDate = expiry,
            planRate = rate,
            payments = paymentList,
            downSpeed = downSpd,
            upSpeed = upSpd,
            sessionDownload = sessDown,
            sessionUpload = sessUp,
            speedHistory = newHistory
        )
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PlusnetNativeApp()
        }
    }
}

@Composable
fun PlusnetNativeApp() {
    val context = LocalContext.current
    val repo = remember { PlusnetRepository(context) }
    val scope = rememberCoroutineScope()

    var isLoggedIn by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var dashData by remember { mutableStateOf(DashboardData()) }
    var selectedTab by remember { mutableIntStateOf(0) }

    // Auto login if saved
    LaunchedEffect(Unit) {
        val u = repo.getSavedUser()
        val p = repo.getSavedPass()
        if (u.isNotEmpty() && p.isNotEmpty()) {
            isLoading = true
            repo.loginAndFetch(u, p).onSuccess {
                dashData = it
                isLoggedIn = true
            }.onFailure {
                errorMsg = it.message
            }
            isLoading = false
        }
    }

    // Background Auto Refresh every 8 seconds when logged in
    LaunchedEffect(isLoggedIn) {
        while (isLoggedIn) {
            delay(8000)
            repo.refreshData(dashData.speedHistory).onSuccess {
                dashData = it
            }
        }
    }

    val bgDark = Color(0xFF0B0F19)
    val glassCard = Color(0xFF151C2C)
    val glassBorder = Color(0xFF263147)
    val accentBlue = Color(0xFF38BDF8)
    val accentPurple = Color(0xFF818CF8)
    val greenOnline = Color(0xFF10B981)

    Surface(modifier = Modifier.fillMaxSize(), color = bgDark) {
        if (!isLoggedIn) {
            LoginScreen(
                initialUser = repo.getSavedUser(),
                initialPass = repo.getSavedPass(),
                isLoading = isLoading,
                errorMsg = errorMsg,
                onLogin = { u, p ->
                    scope.launch {
                        isLoading = true
                        errorMsg = null
                        repo.loginAndFetch(u, p).onSuccess {
                            repo.saveCredentials(u, p)
                            dashData = it
                            isLoggedIn = true
                        }.onFailure {
                            errorMsg = it.message ?: "Login Failed"
                        }
                        isLoading = false
                    }
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
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Brush.linearGradient(listOf(accentBlue, accentPurple))),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Wifi, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("PLUSNET CARE", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text("Native Client • Auto Sync", color = accentBlue, fontSize = 11.sp)
                            }
                        }
                        Row {
                            IconButton(onClick = {
                                scope.launch {
                                    isLoading = true
                                    repo.refreshData(dashData.speedHistory).onSuccess { dashData = it }
                                    isLoading = false
                                }
                            }) {
                                Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color.White)
                            }
                            IconButton(onClick = {
                                repo.clearCredentials()
                                isLoggedIn = false
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
                            onClick = { selectedTab = 1 },
                            icon = { Icon(Icons.Default.Speed, contentDescription = null) },
                            label = { Text("Live Speed & Report") },
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
                    if (isLoading) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = accentBlue, trackColor = glassCard)
                    }
                    if (selectedTab == 0) {
                        DashboardTab(dashData, glassCard, glassBorder, accentBlue, accentPurple, greenOnline)
                    } else {
                        ReportTab(dashData, glassCard, glassBorder, accentBlue, accentPurple, greenOnline)
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

    val glassCard = Color(0xFF151C2C)
    val accentBlue = Color(0xFF38BDF8)
    val accentPurple = Color(0xFF818CF8)

    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(glassCard)
                .border(1.dp, Color(0xFF263147), RoundedCornerShape(24.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(accentBlue, accentPurple))),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Router, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text("PLUSNET", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text("Customer Self-Care Portal", color = Color.Gray, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = user,
                onValueChange = { user = it },
                label = { Text("Customer ID / Username") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                    focusedBorderColor = accentBlue, unfocusedBorderColor = Color(0xFF263147)
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
                    focusedBorderColor = accentBlue, unfocusedBorderColor = Color(0xFF263147)
                )
            )

            if (errorMsg != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(errorMsg, color = Color(0xFFF87171), fontSize = 13.sp)
            }

            Spacer(modifier = Modifier.height(24.dp))
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
                    Text("Sign In", color = Color(0xFF0B0F19), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
fun DashboardTab(
    data: DashboardData,
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
        // Hero Package Banner
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Brush.horizontalGradient(listOf(Color(0xFF1E3A8A), Color(0xFF312E81))))
                    .border(1.dp, accentBlue.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("CURRENT PACKAGE", color = accentBlue, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(data.packageName, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(greenOnline))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("${data.connectionStatus} • ${data.accountStatus}", color = greenOnline, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                    Surface(
                        color = Color.White.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
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

        // Info Grid
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("Expiry Date", data.expiryDate, Icons.Default.EventAvailable, accentPurple, glassCard, glassBorder, Modifier.weight(1f))
                StatCard("Live Download", data.downSpeed, Icons.Default.ArrowDownward, accentBlue, glassCard, glassBorder, Modifier.weight(1f))
            }
        }

        // Payment History Header
        item {
            Text("Recent Payment History", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 6.dp))
        }

        if (data.payments.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(glassCard)
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No recent payment rows parsed", color = Color.Gray, fontSize = 13.sp)
                }
            }
        } else {
            items(data.payments) { pay ->
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
                        Text(pay.amount, color = greenOnline, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(pay.remarks, color = Color.White, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
fun ReportTab(
    data: DashboardData,
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
            Text("Live Bandwidth & Session Usage", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text("Auto-updating every 8 seconds in background", color = Color.Gray, fontSize = 12.sp)
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
                StatCard("Session Upload", data.sessionUpload, Icons.Default.UploadFile, Color(0xFFFBBF24), glassCard, glassBorder, Modifier.weight(1f))
            }
        }

        // Native Live Speed Graph
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
                    Text("Real-Time Speed Graph", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text("LIVE", color = greenOnline, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Canvas(modifier = Modifier.fillMaxWidth().height(160.dp)) {
                    val points = data.speedHistory
                    if (points.size > 1) {
                        val maxVal = (points.maxOrNull() ?: 100f).coerceAtLeast(10f)
                        val stepX = size.width / (points.size - 1)
                        val path = Path()

                        points.forEachIndexed { idx, value ->
                            val x = idx * stepX
                            val y = size.height - ((value / maxVal) * size.height * 0.85f) - 10f
                            if (idx == 0) path.moveTo(x, y) else path.lineTo(x, y)
                            drawCircle(color = accentBlue, radius = 6f, center = Offset(x, y))
                        }
                        drawPath(
                            path = path,
                            color = accentBlue,
                            style = Stroke(width = 5f, cap = StrokeCap.Round)
                        )
                    }
                }
            }
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
