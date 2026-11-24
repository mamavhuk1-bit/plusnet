package com.plusnet.care

import android.accounts.AccountManager
import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Paint
import android.os.Bundle
import android.text.Html
import android.view.ViewGroup
import android.webkit.*
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeOut
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
import androidx.compose.ui.draw.scale
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

const val SUPABASE_URL = "https://qwshtbuvvsxehrkomukd.supabase.co"
const val SUPABASE_ANON_KEY = "sb_publishable_iFwTS5TmnBxIMqJzIcRXcA_MhTMeKJs"
const val SUPABASE_BUCKET = "plusnet_backups"

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
data class TicketItem(val id: String, val status: String, val title: String, val description: String, val date: String, val assigned: String, val detailUrl: String = "")
data class TicketCategory(val value: String, val label: String)
data class CloudFileItem(val name: String, val updatedAt: String)

fun cleanHtml(raw: String): String {
    return Html.fromHtml(raw, Html.FROM_HTML_MODE_LEGACY).toString().trim()
}

fun calculateLiveDuration(startStr: String, nowMillis: Long): String {
    val formats = listOf("yyyy-MM-dd HH:mm:ss", "dd-MM-yyyy HH:mm:ss", "yyyy/MM/dd HH:mm:ss")
    for (fmt in formats) {
        try {
            val sdf = SimpleDateFormat(fmt, Locale.US)
            val parsed = sdf.parse(startStr.trim()) ?: continue
            val diffSec = ((nowMillis - parsed.time) / 1000L).coerceAtLeast(0L)
            val hours = diffSec / 3600
            val mins = (diffSec % 3600) / 60
            val secs = diffSec % 60
            return String.format(Locale.US, "%02d:%02d:%02d", hours, mins, secs)
        } catch (_: Exception) {}
    }
    return "00:00:00"
}

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
    val dashPayments: List<PaymentItem> = emptyList(),
    val fullPaymentHistory: List<PaymentItem> = emptyList(),
    val chartMonthTitle: String = "Usage of Current Month",
    val dailyUsage: List<DailyUsage> = emptyList(),
    val monthlyDownGB: Float = 0f,
    val monthlyUpGB: Float = 0f,
    val downSpeed: String = "0.0 kbps",
    val upSpeed: String = "0.0 mbps",
    val liveSpeedHistory: List<Float> = listOf(2f, 5f, 4f, 8f, 6f, 10f),
    val sessionLogs: List<SessionLogItem> = emptyList(),
    val tickets: List<TicketItem> = emptyList(),
    val ticketCategories: List<TicketCategory> = emptyList()
)

object SupabaseCloudManager {
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    fun serializeState(state: PlusnetState, gmail: String): String {
        val root = JSONObject()
        root.put("gmail", gmail)
        root.put("updatedAt", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date()))
        root.put("fullName", state.fullName)
        root.put("email", state.email)
        root.put("userName", state.userName)
        root.put("mobile", state.mobile)
        root.put("customerId", state.customerId)
        root.put("packageName", state.packageName)
        root.put("accountStatus", state.accountStatus)
        root.put("connectionStatus", state.connectionStatus)
        root.put("expiryDate", state.expiryDate)
        root.put("planRate", state.planRate)
        root.put("chartMonthTitle", state.chartMonthTitle)
        root.put("monthlyDownGB", state.monthlyDownGB.toDouble())
        root.put("monthlyUpGB", state.monthlyUpGB.toDouble())

        val dArr = JSONArray()
        state.dailyUsage.forEach { d ->
            dArr.put(JSONObject().apply {
                put("label", d.label)
                put("download", d.download.toDouble())
                put("upload", d.upload.toDouble())
            })
        }
        root.put("dailyUsage", dArr)

        val pArr = JSONArray()
        (state.fullPaymentHistory.ifEmpty { state.dashPayments }).forEach { p ->
            pArr.put(JSONObject().apply {
                put("date", p.date)
                put("bill", p.bill)
                put("receive", p.receive)
                put("remarks", p.remarks)
            })
        }
        root.put("payments", pArr)

        val sArr = JSONArray()
        state.sessionLogs.forEach { s ->
            sArr.put(JSONObject().apply {
                put("connDate", s.connDate)
                put("discDate", s.discDate)
                put("upload", s.upload)
                put("download", s.download)
                put("duration", s.duration)
            })
        }
        root.put("sessionLogs", sArr)

        val tArr = JSONArray()
        state.tickets.forEach { t ->
            tArr.put(JSONObject().apply {
                put("id", t.id)
                put("status", t.status)
                put("title", t.title)
                put("description", t.description)
                put("date", t.date)
                put("assigned", t.assigned)
            })
        }
        root.put("tickets", tArr)
        return root.toString(2)
    }

    fun deserializeToState(jsonStr: String, current: PlusnetState): PlusnetState {
        val obj = JSONObject(jsonStr)
        val dList = mutableListOf<DailyUsage>()
        obj.optJSONArray("dailyUsage")?.let { arr ->
            for (i in 0 until arr.length()) {
                val it = arr.getJSONObject(i)
                dList.add(DailyUsage(it.optString("label"), it.optDouble("download", 0.0).toFloat(), it.optDouble("upload", 0.0).toFloat()))
            }
        }
        val pList = mutableListOf<PaymentItem>()
        obj.optJSONArray("payments")?.let { arr ->
            for (i in 0 until arr.length()) {
                val it = arr.getJSONObject(i)
                pList.add(PaymentItem(it.optString("date"), it.optString("bill"), it.optString("receive"), it.optString("remarks")))
            }
        }
        val sList = mutableListOf<SessionLogItem>()
        obj.optJSONArray("sessionLogs")?.let { arr ->
            for (i in 0 until arr.length()) {
                val it = arr.getJSONObject(i)
                sList.add(SessionLogItem(it.optString("connDate"), it.optString("discDate"), it.optString("upload"), it.optString("download"), it.optString("duration")))
            }
        }
        val tList = mutableListOf<TicketItem>()
        obj.optJSONArray("tickets")?.let { arr ->
            for (i in 0 until arr.length()) {
                val it = arr.getJSONObject(i)
                tList.add(TicketItem(it.optString("id"), it.optString("status"), it.optString("title"), it.optString("description"), it.optString("date"), it.optString("assigned")))
            }
        }
        return current.copy(
            fullName = obj.optString("fullName").ifEmpty { current.fullName },
            email = obj.optString("email").ifEmpty { current.email },
            userName = obj.optString("userName").ifEmpty { current.userName },
            mobile = obj.optString("mobile").ifEmpty { current.mobile },
            customerId = obj.optString("customerId").ifEmpty { current.customerId },
            packageName = obj.optString("packageName").ifEmpty { current.packageName },
            accountStatus = obj.optString("accountStatus").ifEmpty { current.accountStatus },
            connectionStatus = obj.optString("connectionStatus").ifEmpty { current.connectionStatus },
            expiryDate = obj.optString("expiryDate").ifEmpty { current.expiryDate },
            planRate = obj.optString("planRate").ifEmpty { current.planRate },
            chartMonthTitle = obj.optString("chartMonthTitle").ifEmpty { current.chartMonthTitle },
            monthlyDownGB = obj.optDouble("monthlyDownGB", current.monthlyDownGB.toDouble()).toFloat(),
            monthlyUpGB = obj.optDouble("monthlyUpGB", current.monthlyUpGB.toDouble()).toFloat(),
            dailyUsage = if (dList.isNotEmpty()) dList else current.dailyUsage,
            fullPaymentHistory = if (pList.isNotEmpty()) pList else current.fullPaymentHistory,
            dashPayments = if (pList.isNotEmpty()) pList else current.dashPayments,
            sessionLogs = if (sList.isNotEmpty()) sList else current.sessionLogs,
            tickets = if (tList.isNotEmpty()) tList else current.tickets
        )
    }

    suspend fun uploadBackup(folderUser: String, gmail: String, state: PlusnetState): Result<String> = withContext(Dispatchers.IO) {
        try {
            val safeFolder = folderUser.replace(Regex("[^A-Za-z0-9_-]"), "_").ifEmpty { "default_user" }
            val jsonPayload = serializeState(state, gmail)
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val monthTag = SimpleDateFormat("yyyy_MM", Locale.US).format(Date())

            val paths = listOf(
                "$safeFolder/latest_backup.json",
                "$safeFolder/monthly_${monthTag}.json"
            )

            for (path in paths) {
                val url = "$SUPABASE_URL/storage/v1/object/$SUPABASE_BUCKET/$path"
                val req = Request.Builder()
                    .url(url)
                    .post(jsonPayload.toRequestBody(mediaType))
                    .header("apikey", SUPABASE_ANON_KEY)
                    .header("Authorization", "Bearer $SUPABASE_ANON_KEY")
                    .header("x-upsert", "true")
                    .header("Content-Type", "application/json")
                    .build()
                client.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) {
                        val err = resp.body?.string() ?: resp.message
                        return@withContext Result.failure(Exception("Upload failed (${resp.code}): $err"))
                    }
                }
            }
            val timeNow = SimpleDateFormat("hh:mm:ss a, dd MMM", Locale.US).format(Date())
            Result.success(timeNow)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun listFolderFiles(folderUser: String): Result<List<CloudFileItem>> = withContext(Dispatchers.IO) {
        try {
            val safeFolder = folderUser.replace(Regex("[^A-Za-z0-9_-]"), "_").ifEmpty { "default_user" }
            val bodyJson = JSONObject().apply {
                put("prefix", "$safeFolder/")
                put("limit", 50)
                put("offset", 0)
            }.toString()

            val req = Request.Builder()
                .url("$SUPABASE_URL/storage/v1/object/list/$SUPABASE_BUCKET")
                .post(bodyJson.toRequestBody("application/json".toMediaType()))
                .header("apikey", SUPABASE_ANON_KEY)
                .header("Authorization", "Bearer $SUPABASE_ANON_KEY")
                .build()

            client.newCall(req).execute().use { resp ->
                val raw = resp.body?.string() ?: "[]"
                if (!resp.isSuccessful) return@withContext Result.failure(Exception("List failed: $raw"))
                val arr = JSONArray(raw)
                val files = mutableListOf<CloudFileItem>()
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val name = o.optString("name")
                    if (name.endsWith(".json")) {
                        files.add(CloudFileItem(name, o.optString("updated_at", "").take(19).replace("T", " ")))
                    }
                }
                Result.success(files)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun downloadBackup(folderUser: String, fileName: String = "latest_backup.json"): Result<String> = withContext(Dispatchers.IO) {
        try {
            val safeFolder = folderUser.replace(Regex("[^A-Za-z0-9_-]"), "_").ifEmpty { "default_user" }
            val url = "$SUPABASE_URL/storage/v1/object/$SUPABASE_BUCKET/$safeFolder/$fileName"
            val req = Request.Builder()
                .url(url)
                .get()
                .header("apikey", SUPABASE_ANON_KEY)
                .header("Authorization", "Bearer $SUPABASE_ANON_KEY")
                .build()

            client.newCall(req).execute().use { resp ->
                val body = resp.body?.string() ?: ""
                if (!resp.isSuccessful) return@withContext Result.failure(Exception("Restore failed (${resp.code})"))
                Result.success(body)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
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

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun PlusnetNativeApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences("plusnet_prefs_v8", Context.MODE_PRIVATE) }
    var themeIndex by remember { mutableIntStateOf(prefs.getInt("theme_idx", 0).coerceIn(0, AppThemes.lastIndex)) }
    val theme = AppThemes[themeIndex]

    var savedUser by remember { mutableStateOf(prefs.getString("auto_user", "") ?: "") }
    var savedPass by remember { mutableStateOf(prefs.getString("auto_pass", "") ?: "") }
    var connectedGmail by remember { mutableStateOf(prefs.getString("cloud_gmail", "") ?: "") }
    var lastCloudBackupTime by remember { mutableStateOf(prefs.getString("last_backup_time", "Not backed up yet") ?: "Not backed up yet") }
    var isCloudBusy by remember { mutableStateOf(false) }
    var cloudFiles by remember { mutableStateOf<List<CloudFileItem>>(emptyList()) }

    val hasSavedCreds = savedUser.isNotEmpty() && savedPass.isNotEmpty()

    var state by remember { mutableStateOf(PlusnetState(isLoggedIn = hasSavedCreds, userName = savedUser)) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showNewTicketDialog by remember { mutableStateOf(false) }
    var selectedTicketDetail by remember { mutableStateOf<TicketItem?>(null) }
    var ticketConversation by remember { mutableStateOf<String?>(null) }
    var showSplash by remember { mutableStateOf(true) }
    var autoLoginTries by remember { mutableIntStateOf(0) }
    var lastAutoBackupHash by remember { mutableIntStateOf(0) }

    // Android System Gmail Account Picker Launcher
    val gmailPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val pickedEmail = result.data?.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)
        if (!pickedEmail.isNullOrBlank()) {
            connectedGmail = pickedEmail
            prefs.edit().putString("cloud_gmail", pickedEmail).apply()
            Toast.makeText(context, "Connected: $pickedEmail", Toast.LENGTH_SHORT).show()

            val folder = state.userName.ifEmpty { savedUser }
            if (folder.isNotEmpty()) {
                scope.launch {
                    isCloudBusy = true
                    SupabaseCloudManager.uploadBackup(folder, pickedEmail, state).onSuccess { ts ->
                        lastCloudBackupTime = ts
                        prefs.edit().putString("last_backup_time", ts).apply()
                    }
                    SupabaseCloudManager.listFolderFiles(folder).onSuccess { cloudFiles = it }
                    isCloudBusy = false
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        delay(1500)
        showSplash = false
    }

    // Automatic Cloud Backup whenever fresh portal data is synced & Gmail is connected
    LaunchedEffect(state.dailyUsage, state.fullPaymentHistory, state.tickets, connectedGmail) {
        val folder = state.userName.ifEmpty { savedUser }
        if (state.isLoggedIn && connectedGmail.isNotEmpty() && folder.isNotEmpty() && state.dailyUsage.isNotEmpty()) {
            val currentHash = (state.monthlyDownGB.toString() + state.fullPaymentHistory.size + state.tickets.size).hashCode()
            if (currentHash != lastAutoBackupHash) {
                lastAutoBackupHash = currentHash
                SupabaseCloudManager.uploadBackup(folder, connectedGmail, state).onSuccess { ts ->
                    lastCloudBackupTime = ts
                    prefs.edit().putString("last_backup_time", ts).apply()
                }
                SupabaseCloudManager.listFolderFiles(folder).onSuccess { cloudFiles = it }
            }
        }
    }

    LaunchedEffect(state.isLoggedIn) {
        while (state.isLoggedIn) {
            delay(14000)
            if (!showNewTicketDialog && selectedTicketDetail == null) {
                val target = when (selectedTab) {
                    0 -> "dashboard"
                    1 -> "report"
                    2 -> "customerhistory"
                    3 -> "complainlist"
                    else -> "dashboard"
                }
                webViewRef?.loadUrl("https://care.plus.net.bd/customer/$target")
            }
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
                                    2 -> "customerhistory"
                                    3 -> "complainlist"
                                    else -> "dashboard"
                                }
                                webViewRef?.loadUrl("https://care.plus.net.bd/customer/$p")
                            }) {
                                Icon(Icons.Default.Refresh, contentDescription = "Sync", tint = theme.textPrimary)
                            }
                            IconButton(onClick = {
                                prefs.edit().remove("auto_user").remove("auto_pass").apply()
                                savedUser = ""
                                savedPass = ""
                                autoLoginTries = 0
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
                        val navItems = listOf(
                            Triple("Home", Icons.Default.Dashboard, 0),
                            Triple("Speed", Icons.Default.Speed, 1),
                            Triple("Payments", Icons.Default.ReceiptLong, 2),
                            Triple("Complain", Icons.Default.ConfirmationNumber, 3),
                            Triple("Cloud", Icons.Default.CloudSync, 4),
                            Triple("Profile", Icons.Default.AccountCircle, 5)
                        )
                        navItems.forEach { (label, icon, idx) ->
                            NavigationBarItem(
                                selected = selectedTab == idx,
                                onClick = {
                                    selectedTab = idx
                                    when (idx) {
                                        1 -> {
                                            state = state.copy(isSyncing = true)
                                            webViewRef?.loadUrl("https://care.plus.net.bd/customer/report")
                                        }
                                        2 -> {
                                            state = state.copy(isSyncing = true)
                                            webViewRef?.loadUrl("https://care.plus.net.bd/customer/customerhistory")
                                        }
                                        3 -> {
                                            state = state.copy(isSyncing = true)
                                            webViewRef?.loadUrl("https://care.plus.net.bd/customer/complainlist")
                                        }
                                        4 -> {
                                            val folder = state.userName.ifEmpty { savedUser }
                                            if (folder.isNotEmpty()) {
                                                scope.launch {
                                                    SupabaseCloudManager.listFolderFiles(folder).onSuccess { cloudFiles = it }
                                                }
                                            }
                                        }
                                    }
                                },
                                icon = { Icon(icon, contentDescription = label, modifier = Modifier.size(20.dp)) },
                                label = { Text(label, fontSize = 9.sp, maxLines = 1) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = theme.accent, selectedTextColor = theme.accent,
                                    indicatorColor = theme.accent.copy(alpha = 0.16f),
                                    unselectedIconColor = theme.textSecondary, unselectedTextColor = theme.textSecondary
                                )
                            )
                        }
                    }
                }
            ) { padding ->
                Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                    if (state.isSyncing || isCloudBusy) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = theme.accent, trackColor = theme.card)
                    }
                    when (selectedTab) {
                        0 -> DashboardTab(state, theme)
                        1 -> ReportTab(state, theme)
                        2 -> PaymentHistoryTab(state, theme)
                        3 -> ComplainTab(
                            data = state,
                            theme = theme,
                            onOpenNewTicket = {
                                showNewTicketDialog = true
                                val openModalJs = """
                                    (function() {
                                        var btn = Array.from(document.querySelectorAll('button, a')).find(el => el.innerText.includes('Create a New Ticket') || el.innerText.includes('New Ticket'));
                                        if (btn) btn.click();
                                    })();
                                """.trimIndent()
                                webViewRef?.evaluateJavascript(openModalJs, null)
                                webViewRef?.postDelayed({
                                    val readCatsJs = """
                                        (function() {
                                            var cats = [];
                                            document.querySelectorAll('select option').forEach(function(opt) {
                                                var v = opt.value;
                                                var t = opt.innerText.trim();
                                                if (v && t && !t.toLowerCase().includes('select') && isNaN(t)) {
                                                    cats.push({ val: v, label: t });
                                                }
                                            });
                                            return JSON.stringify(cats);
                                        })();
                                    """.trimIndent()
                                    webViewRef?.evaluateJavascript(readCatsJs) { raw ->
                                        try {
                                            val arr = JSONArray(JSONTokener(raw).nextValue().toString())
                                            val list = mutableListOf<TicketCategory>()
                                            for (i in 0 until arr.length()) {
                                                val o = arr.getJSONObject(i)
                                                list.add(TicketCategory(o.optString("val"), cleanHtml(o.optString("label"))))
                                            }
                                            if (list.isNotEmpty()) {
                                                state = state.copy(ticketCategories = list)
                                            }
                                        } catch (_: Exception) {}
                                    }
                                }, 1200)
                            },
                            onTicketClick = { ticket ->
                                selectedTicketDetail = ticket
                                ticketConversation = null
                                val clickRowJs = """
                                    (function() {
                                        var rows = Array.from(document.querySelectorAll('table tr'));
                                        var target = rows.find(r => r.innerText.includes(${JSONObject.quote(ticket.id)}));
                                        if (target) {
                                            var link = target.querySelector('a, button') || target;
                                            link.click();
                                        }
                                    })();
                                """.trimIndent()
                                webViewRef?.evaluateJavascript(clickRowJs, null)
                                webViewRef?.postDelayed({
                                    val readDetailJs = """
                                        (function() {
                                            var modal = document.querySelector('.modal.show, [role="dialog"], .card-body');
                                            return modal ? modal.innerText : "";
                                        })();
                                    """.trimIndent()
                                    webViewRef?.evaluateJavascript(readDetailJs) { raw ->
                                        try {
                                            val txt = JSONTokener(raw).nextValue().toString().trim()
                                            if (txt.isNotEmpty()) ticketConversation = txt
                                        } catch (_: Exception) {}
                                    }
                                }, 1000)
                            }
                        )
                        4 -> CloudTab(
                            data = state,
                            theme = theme,
                            connectedGmail = connectedGmail,
                            lastBackupTime = lastCloudBackupTime,
                            isBusy = isCloudBusy,
                            cloudFiles = cloudFiles,
                            onPickGmail = {
                                val intent = AccountManager.newChooseAccountIntent(
                                    null, null, arrayOf("com.google"), null, null, null, null
                                )
                                gmailPickerLauncher.launch(intent)
                            },
                            onDisconnectGmail = {
                                connectedGmail = ""
                                prefs.edit().remove("cloud_gmail").apply()
                            },
                            onBackupNow = {
                                val folder = state.userName.ifEmpty { savedUser }
                                if (connectedGmail.isEmpty()) {
                                    Toast.makeText(context, "Please pick a Gmail account first", Toast.LENGTH_SHORT).show()
                                } else {
                                    scope.launch {
                                        isCloudBusy = true
                                        SupabaseCloudManager.uploadBackup(folder, connectedGmail, state)
                                            .onSuccess { ts ->
                                                lastCloudBackupTime = ts
                                                prefs.edit().putString("last_backup_time", ts).apply()
                                                Toast.makeText(context, "Backed up to $folder/", Toast.LENGTH_SHORT).show()
                                            }
                                            .onFailure { err ->
                                                Toast.makeText(context, err.message ?: "Backup error", Toast.LENGTH_LONG).show()
                                            }
                                        SupabaseCloudManager.listFolderFiles(folder).onSuccess { cloudFiles = it }
                                        isCloudBusy = false
                                    }
                                }
                            },
                            onRestoreFile = { fileName ->
                                val folder = state.userName.ifEmpty { savedUser }
                                scope.launch {
                                    isCloudBusy = true
                                    SupabaseCloudManager.downloadBackup(folder, fileName)
                                        .onSuccess { jsonStr ->
                                            state = SupabaseCloudManager.deserializeToState(jsonStr, state)
                                            Toast.makeText(context, "Restored from $folder/$fileName", Toast.LENGTH_SHORT).show()
                                        }
                                        .onFailure { err ->
                                            Toast.makeText(context, err.message ?: "Restore error", Toast.LENGTH_LONG).show()
                                        }
                                    isCloudBusy = false
                                }
                            }
                        )
                        5 -> ProfileTab(state, theme)
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
                        Text("Sign in once • Auto-Login & Cloud Sync active", color = theme.accent, fontSize = 11.sp)
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

                    addJavascriptInterface(object {
                        @JavascriptInterface
                        fun saveCreds(u: String, p: String) {
                            if (u.isNotBlank() && p.isNotBlank()) {
                                prefs.edit().putString("auto_user", u.trim()).putString("auto_pass", p.trim()).apply()
                                savedUser = u.trim()
                                savedPass = p.trim()
                            }
                        }
                    }, "AndroidAutoAuth")

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
                                val u = prefs.getString("auto_user", "") ?: ""
                                val p = prefs.getString("auto_pass", "") ?: ""

                                val hookJs = """
                                    (function() {
                                        var inputs = Array.from(document.querySelectorAll('input'));
                                        var pIn = inputs.find(i => i.type === 'password');
                                        var uIn = inputs.find(i => i.type === 'text' || i.type === 'email' || (!i.type && i !== pIn));
                                        function sync() {
                                            if (uIn && pIn && uIn.value && pIn.value && window.AndroidAutoAuth) {
                                                AndroidAutoAuth.saveCreds(uIn.value, pIn.value);
                                            }
                                        }
                                        if (uIn) uIn.addEventListener('input', sync);
                                        if (pIn) pIn.addEventListener('input', sync);
                                        var form = document.querySelector('form');
                                        if (form) form.addEventListener('submit', sync);
                                        var btn = document.querySelector('button[type="submit"], button');
                                        if (btn) btn.addEventListener('click', sync);
                                    })();
                                """.trimIndent()
                                view?.evaluateJavascript(hookJs, null)

                                if (u.isNotEmpty() && p.isNotEmpty() && autoLoginTries < 2) {
                                    autoLoginTries++
                                    view?.postDelayed({
                                        val autoFillJs = """
                                            (function() {
                                                function setNative(el, val) {
                                                    var last = el.value;
                                                    el.value = val;
                                                    var ev = new Event('input', { bubbles: true });
                                                    if (el._valueTracker) el._valueTracker.setValue(last);
                                                    el.dispatchEvent(ev);
                                                    el.dispatchEvent(new Event('change', { bubbles: true }));
                                                }
                                                var inputs = Array.from(document.querySelectorAll('input'));
                                                var pIn = inputs.find(i => i.type === 'password');
                                                var uIn = inputs.find(i => i.type === 'text' || i.type === 'email' || (!i.type && i !== pIn));
                                                if (uIn && pIn) {
                                                    setNative(uIn, ${JSONObject.quote(u)});
                                                    setNative(pIn, ${JSONObject.quote(p)});
                                                    setTimeout(function() {
                                                        var btn = document.querySelector('button[type="submit"], input[type="submit"], button');
                                                        if (btn) btn.click();
                                                        else if (pIn.form) pIn.form.submit();
                                                    }, 400);
                                                }
                                            })();
                                        """.trimIndent()
                                        view.evaluateJavascript(autoFillJs, null)
                                    }, 800)
                                } else {
                                    state = state.copy(isLoggedIn = false, isSyncing = false)
                                }
                            } else if (url != null && url.contains("/dashboard")) {
                                autoLoginTries = 0
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

                                            var monthMatch = text.match(/Usage of\s+[A-Za-z]+/i);
                                            var yearMatch = text.match(/\b(202\d)\b/);
                                            var chartTitle = (monthMatch ? monthMatch[0] : "Usage of Month") + (yearMatch ? (" " + yearMatch[1]) : "");

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
                                                            var rawCat = cats[k] || s0[k].category || s0[k].name || ("" + (k+1));
                                                            chartData.push({
                                                                label: String(rawCat),
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
                                                chartTitle: chartTitle, payments: rows, chart: chartData, totalD: totalD, totalU: totalU
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
                                                    payList.add(PaymentItem(it.optString("date"), it.optString("bill"), it.optString("receive"), cleanHtml(it.optString("remarks"))))
                                                }
                                            }
                                            val usageList = mutableListOf<DailyUsage>()
                                            obj.optJSONArray("chart")?.let { arr ->
                                                for (i in 0 until arr.length()) {
                                                    val it = arr.getJSONObject(i)
                                                    usageList.add(DailyUsage(it.optString("label"), it.optDouble("down", 0.0).toFloat(), it.optDouble("up", 0.0).toFloat()))
                                                }
                                            }
                                            val fetchedUser = obj.optString("userName").ifEmpty { state.userName }
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
                                                userName = fetchedUser,
                                                mobile = obj.optString("mobile").ifEmpty { state.mobile },
                                                customerId = obj.optString("cid").ifEmpty { state.customerId },
                                                chartMonthTitle = obj.optString("chartTitle").ifEmpty { state.chartMonthTitle },
                                                dashPayments = if (payList.isNotEmpty()) payList else state.dashPayments,
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
                                }, 1800)
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
                                            if (state.fullPaymentHistory.isEmpty()) {
                                                view.loadUrl("https://care.plus.net.bd/customer/customerhistory")
                                            }
                                        } catch (_: Exception) {
                                            state = state.copy(isSyncing = false)
                                        }
                                    }
                                }, 1800)
                            } else if (url != null && url.contains("/customerhistory")) {
                                view?.postDelayed({
                                    val histJs = """
                                        (function() {
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
                                            return JSON.stringify(rows);
                                        })();
                                    """.trimIndent()
                                    view.evaluateJavascript(histJs) { raw ->
                                        try {
                                            val arr = JSONArray(JSONTokener(raw).nextValue().toString())
                                            val list = mutableListOf<PaymentItem>()
                                            for (i in 0 until arr.length()) {
                                                val it = arr.getJSONObject(i)
                                                list.add(PaymentItem(it.optString("date"), it.optString("bill"), it.optString("receive"), cleanHtml(it.optString("remarks"))))
                                            }
                                            state = state.copy(
                                                isSyncing = false,
                                                fullPaymentHistory = if (list.isNotEmpty()) list else state.fullPaymentHistory
                                            )
                                            if (state.tickets.isEmpty()) {
                                                view.loadUrl("https://care.plus.net.bd/customer/complainlist")
                                            }
                                        } catch (_: Exception) {
                                            state = state.copy(isSyncing = false)
                                        }
                                    }
                                }, 1800)
                            } else if (url != null && url.contains("/complainlist")) {
                                view?.postDelayed({
                                    val complainJs = """
                                        (function() {
                                            var tickets = [];
                                            document.querySelectorAll("table tr").forEach(function(tr, idx) {
                                                if (idx === 0) return;
                                                var tds = tr.querySelectorAll("td");
                                                if (tds.length >= 5) {
                                                    var aTag = tr.querySelector("a");
                                                    tickets.push({
                                                        id: tds[0].innerText.trim(),
                                                        status: tds[1].innerText.trim(),
                                                        title: tds[2].innerText.trim(),
                                                        desc: tds[3].innerText.trim(),
                                                        date: tds[4].innerText.trim(),
                                                        assigned: tds.length > 5 ? tds[5].innerText.trim() : "",
                                                        href: aTag ? aTag.href : ""
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
                                                            title = cleanHtml(it.optString("title")),
                                                            description = cleanHtml(it.optString("desc")),
                                                            date = it.optString("date"),
                                                            assigned = it.optString("assigned"),
                                                            detailUrl = it.optString("href")
                                                        )
                                                    )
                                                }
                                            }
                                            val cList = mutableListOf<TicketCategory>()
                                            obj.optJSONArray("cats")?.let { arr ->
                                                for (i in 0 until arr.length()) {
                                                    val it = arr.getJSONObject(i)
                                                    cList.add(TicketCategory(it.optString("val"), cleanHtml(it.optString("label"))))
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
                                }, 1800)
                            }
                        }
                    }

                    webViewRef = this
                    loadUrl("https://care.plus.net.bd/customer/dashboard")
                }
            }
        )

        // Theme Picker Dialog
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

        // Ticket Detail Dialog
        selectedTicketDetail?.let { ticket ->
            val isCompleted = ticket.status.contains("COMPLET", ignoreCase = true)
            val badgeColor = if (isCompleted) Color(0xFF10B981) else Color(0xFFF59E0B)
            AlertDialog(
                onDismissRequest = { selectedTicketDetail = null },
                containerColor = theme.card,
                title = {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Ticket #${ticket.id}", color = theme.textPrimary, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        Surface(color = badgeColor.copy(alpha = 0.2f), shape = RoundedCornerShape(8.dp)) {
                            Text(ticket.status, color = badgeColor, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                        }
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Category / Title:", color = theme.textSecondary, fontSize = 12.sp)
                        Text(ticket.title, color = theme.textPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)

                        Text("Description & Remarks:", color = theme.textSecondary, fontSize = 12.sp)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(theme.bg)
                                .padding(12.dp)
                        ) {
                            Text(
                                text = ticketConversation ?: ticket.description,
                                color = theme.textPrimary,
                                fontSize = 13.sp
                            )
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Date: ${ticket.date}", color = theme.textSecondary, fontSize = 12.sp)
                            if (ticket.assigned.isNotEmpty()) {
                                Text("Assigned: ${ticket.assigned}", color = theme.accent, fontSize = 12.sp)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { selectedTicketDetail = null }) {
                        Text("Close", color = theme.accent, fontWeight = FontWeight.Bold)
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
                        })();
                    """.trimIndent()
                    webViewRef?.evaluateJavascript(submitJs) {
                        Toast.makeText(context, "Submitting complain ticket...", Toast.LENGTH_SHORT).show()
                        webViewRef?.postDelayed({
                            webViewRef?.loadUrl("https://care.plus.net.bd/customer/complainlist")
                        }, 2500)
                    }
                }
            )
        }

        // Animated Plusnet Green Logo Splash Overlay
        AnimatedVisibility(
            visible = showSplash,
            exit = fadeOut(animationSpec = tween(400))
        ) {
            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
            val pulseScale by infiniteTransition.animateFloat(
                initialValue = 0.94f,
                targetValue = 1.06f,
                animationSpec = infiniteRepeatable(
                    animation = tween(700, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "logoScale"
            )
            val glowAlpha by infiniteTransition.animateFloat(
                initialValue = 0.25f,
                targetValue = 0.65f,
                animationSpec = infiniteRepeatable(
                    animation = tween(700, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "glowAlpha"
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0A0E17)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(contentAlignment = Alignment.Center) {
                        Box(
                            modifier = Modifier
                                .size(132.dp)
                                .scale(pulseScale)
                                .clip(RoundedCornerShape(32.dp))
                                .background(Color(0xFF047835).copy(alpha = glowAlpha))
                        )
                        Box(modifier = Modifier.scale(pulseScale)) {
                            PlusnetGreenLogo(size = 96.dp)
                        }
                    }
                    Spacer(modifier = Modifier.height(22.dp))
                    Text(
                        text = "PLUSNET CARE",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Connecting Self-Care Portal...",
                        color = Color(0xFF10B981),
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
fun CloudTab(
    data: PlusnetState,
    theme: AppThemeConfig,
    connectedGmail: String,
    lastBackupTime: String,
    isBusy: Boolean,
    cloudFiles: List<CloudFileItem>,
    onPickGmail: () -> Unit,
    onDisconnectGmail: () -> Unit,
    onBackupNow: () -> Unit,
    onRestoreFile: (String) -> Unit
) {
    val folderName = data.userName.ifEmpty { "PPG092" }
    val payments = data.fullPaymentHistory.ifEmpty { data.dashPayments }
    val receivedPayments = payments.filter { it.receive != "0" && it.receive.isNotEmpty() }
    val totalPaidTk = receivedPayments.sumOf { it.receive.replace(",", "").toDoubleOrNull() ?: 0.0 }
    val avgDailyGB = if (data.dailyUsage.isNotEmpty()) {
        (data.monthlyDownGB + data.monthlyUpGB) / data.dailyUsage.size
    } else 0f

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Supabase Gmail Connection Card
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Brush.horizontalGradient(listOf(theme.heroStart, theme.heroEnd)))
                    .border(1.dp, theme.accent.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                    .padding(18.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CloudDone, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Supabase Cloud Sync", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("Folder: plusnet_backups/$folderName/", color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp)
                        }
                    }
                    Surface(color = Color.White.copy(alpha = 0.2f), shape = RoundedCornerShape(8.dp)) {
                        Text(
                            text = if (connectedGmail.isNotEmpty()) "CONNECTED" else "NOT LINKED",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (connectedGmail.isEmpty()) {
                    Button(
                        onClick = onPickGmail,
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                    ) {
                        Icon(Icons.Default.Email, contentDescription = null, tint = Color(0xFF047835))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Pick Gmail from Phone to Connect", color = Color(0xFF047835), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black.copy(alpha = 0.25f))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(connectedGmail, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("Last Backup: $lastBackupTime", color = Color(0xFF6EE7B7), fontSize = 11.sp)
                        }
                        IconButton(onClick = onDisconnectGmail) {
                            Icon(Icons.Default.LinkOff, contentDescription = "Unlink", tint = Color(0xFFFCA5A5))
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = onBackupNow,
                            enabled = !isBusy,
                            modifier = Modifier.weight(1f).height(44.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                        ) {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, tint = Color(0xFF047835), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Backup Now", color = Color(0xFF047835), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Button(
                            onClick = { onRestoreFile("latest_backup.json") },
                            enabled = !isBusy,
                            modifier = Modifier.weight(1f).height(44.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.22f))
                        ) {
                            Icon(Icons.Default.CloudDownload, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Restore Latest", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Cloud Analytics Summary Cards
        item {
            Text("Cloud Usage & Billing Analytics", color = theme.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(
                    title = "Daily Avg Usage",
                    value = String.format(Locale.US, "%.2f GB/d", avgDailyGB),
                    icon = Icons.Default.Analytics,
                    tint = Color(0xFF38BDF8),
                    theme = theme,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Total Paid (Cloud)",
                    value = String.format(Locale.US, "৳ %.0f", totalPaidTk),
                    icon = Icons.Default.Payments,
                    tint = Color(0xFF10B981),
                    theme = theme,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Monthly Data Usage Analytics Chart
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(theme.card)
                    .border(1.dp, theme.border, RoundedCornerShape(20.dp))
                    .padding(16.dp)
            ) {
                Text("Monthly Data Consumption (${data.chartMonthTitle})", color = theme.textPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("Download: ${String.format(Locale.US, "%.1f", data.monthlyDownGB)} GB  •  Upload: ${String.format(Locale.US, "%.1f", data.monthlyUpGB)} GB", color = theme.textSecondary, fontSize = 11.sp)
                Spacer(modifier = Modifier.height(14.dp))

                if (data.dailyUsage.isNotEmpty()) {
                    val bars = data.dailyUsage.takeLast(10)
                    val maxVal = bars.maxOfOrNull { it.download + it.upload }?.coerceAtLeast(1f) ?: 100f
                    Canvas(modifier = Modifier.fillMaxWidth().height(150.dp)) {
                        val bottomPad = 32f
                        val chartH = size.height - bottomPad
                        val stepW = size.width / bars.size
                        val barW = (stepW * 0.55f).coerceAtLeast(10f)
                        val datePaint = Paint().apply {
                            color = android.graphics.Color.LTGRAY
                            textSize = 20f
                            isAntiAlias = true
                            textAlign = Paint.Align.CENTER
                        }
                        bars.forEachIndexed { i, item ->
                            val total = item.download + item.upload
                            val h = (total / maxVal) * (chartH - 14f)
                            val x = i * stepW + (stepW - barW) / 2f
                            drawRect(theme.accent, Offset(x, chartH - h), Size(barW, h))
                            drawContext.canvas.nativeCanvas.drawText(item.label.take(5), x + barW / 2f, size.height - 4f, datePaint)
                        }
                    }
                } else {
                    Text("No usage data synced yet.", color = theme.textSecondary, fontSize = 12.sp)
                }
            }
        }

        // Bill Payment Date & Amount Chart
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(theme.card)
                    .border(1.dp, theme.border, RoundedCornerShape(20.dp))
                    .padding(16.dp)
            ) {
                Text("Bill Payment Dates & Amounts", color = theme.textPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(12.dp))
                if (receivedPayments.isEmpty()) {
                    Text("No payment records found.", color = theme.textSecondary, fontSize = 12.sp)
                } else {
                    val maxPay = receivedPayments.maxOfOrNull { it.receive.replace(",", "").toFloatOrNull() ?: 0f }?.coerceAtLeast(100f) ?: 1200f
                    receivedPayments.take(6).forEach { pay ->
                        val amt = pay.receive.replace(",", "").toFloatOrNull() ?: 0f
                        val ratio = (amt / maxPay).coerceIn(0.1f, 1f)
                        Column(modifier = Modifier.padding(vertical = 5.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(pay.date.take(10), color = theme.textSecondary, fontSize = 12.sp)
                                Text("৳ ${pay.receive}", color = Color(0xFF10B981), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(theme.bg)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(ratio)
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFF10B981))
                                )
                            }
                        }
                    }
                }
            }
        }

        // Cloud Folder Backups List (Restore any file)
        if (cloudFiles.isNotEmpty()) {
            item {
                Text("Saved Files in $folderName/ Folder", color = theme.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
            items(cloudFiles) { file ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(theme.card)
                        .border(1.dp, theme.border, RoundedCornerShape(14.dp))
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(file.name, color = theme.textPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        if (file.updatedAt.isNotEmpty()) {
                            Text("Updated: ${file.updatedAt}", color = theme.textSecondary, fontSize = 11.sp)
                        }
                    }
                    TextButton(onClick = { onRestoreFile(file.name) }) {
                        Text("Restore", color = theme.accent, fontWeight = FontWeight.Bold)
                    }
                }
            }
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
            drawCircle(color = Color.White, radius = (w - stroke) / 2f, style = Stroke(width = stroke))
            drawRect(color = Color(0xFF047835), topLeft = Offset(0f, h * 0.52f), size = Size(w * 0.45f, h * 0.48f))
            drawRect(color = Color.White, topLeft = Offset(0f, h * 0.50f), size = Size(stroke, h * 0.50f))
            drawCircle(color = Color.White, radius = w * 0.14f)
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

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("Expiry Date", data.expiryDate, Icons.Default.EventAvailable, theme.secondary, theme, Modifier.weight(1f))
                StatCard("Live Speed", "${data.downSpeed} ↓", Icons.Default.Speed, theme.accent, theme, Modifier.weight(1f))
            }
        }

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
                    Column {
                        Text(data.chartMonthTitle, color = theme.textPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text("Daily Data in GB", color = theme.textSecondary, fontSize = 11.sp)
                    }
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
                    val bars = data.dailyUsage.takeLast(12)
                    val maxVal = bars.maxOfOrNull { maxOf(it.download, it.upload) }?.coerceAtLeast(1f) ?: 100f
                    Canvas(modifier = Modifier.fillMaxWidth().height(185.dp)) {
                        val leftPad = 60f
                        val bottomPad = 36f
                        val chartW = size.width - leftPad
                        val chartH = size.height - bottomPad
                        val groupWidth = chartW / bars.size
                        val barW = (groupWidth * 0.34f).coerceAtLeast(6f)

                        val axisPaint = Paint().apply {
                            color = android.graphics.Color.GRAY
                            textSize = 24f
                            isAntiAlias = true
                        }
                        val datePaint = Paint().apply {
                            color = android.graphics.Color.LTGRAY
                            textSize = 20f
                            isAntiAlias = true
                            textAlign = Paint.Align.CENTER
                        }

                        drawContext.canvas.nativeCanvas.drawText(String.format(Locale.US, "%.0fG", maxVal), 0f, 24f, axisPaint)
                        drawContext.canvas.nativeCanvas.drawText(String.format(Locale.US, "%.0fG", maxVal / 2f), 0f, chartH / 2f + 8f, axisPaint)
                        drawContext.canvas.nativeCanvas.drawText("0", 18f, chartH, axisPaint)

                        bars.forEachIndexed { i, item ->
                            val xStart = leftPad + (i * groupWidth) + (groupWidth * 0.1f)
                            val dHeight = (item.download / maxVal) * (chartH - 16f)
                            val uHeight = (item.upload / maxVal) * (chartH - 16f)
                            drawRect(Color(0xFF38BDF8), Offset(xStart, chartH - dHeight), Size(barW, dHeight))
                            drawRect(Color(0xFFF97316), Offset(xStart + barW + 2f, chartH - uHeight), Size(barW, uHeight))

                            val shortDate = if (item.label.length >= 5) item.label.take(5) else item.label
                            drawContext.canvas.nativeCanvas.drawText(
                                shortDate,
                                xStart + barW,
                                size.height - 6f,
                                datePaint
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PaymentHistoryTab(data: PlusnetState, theme: AppThemeConfig) {
    val list = data.fullPaymentHistory.ifEmpty { data.dashPayments }
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Payment & Billing History", color = theme.textPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("Fetched from /customer/customerhistory", color = theme.textSecondary, fontSize = 12.sp)
                }
                Surface(color = theme.accent.copy(alpha = 0.2f), shape = RoundedCornerShape(10.dp)) {
                    Text("${list.size} Records", color = theme.accent, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp))
                }
            }
        }

        if (list.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(theme.card).padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Loading payment history records...", color = theme.textSecondary, fontSize = 13.sp)
                }
            }
        } else {
            items(list) { pay ->
                val isCredit = pay.receive != "0" && pay.receive.isNotEmpty()
                val amountDisplay = if (isCredit) "+৳ ${pay.receive}" else "৳ ${pay.bill}"
                val amountColor = if (isCredit) Color(0xFF10B981) else Color(0xFFF59E0B)
                val badgeText = if (isCredit) "RECEIVED" else "BILLED"

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(theme.card)
                        .border(1.dp, theme.border, RoundedCornerShape(16.dp))
                        .padding(14.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(color = amountColor.copy(alpha = 0.16f), shape = RoundedCornerShape(6.dp)) {
                                Text(badgeText, color = amountColor, fontWeight = FontWeight.Bold, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(pay.date, color = theme.textSecondary, fontSize = 12.sp)
                        }
                        Text(amountDisplay, color = amountColor, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(pay.remarks, color = theme.textPrimary, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
fun ReportTab(data: PlusnetState, theme: AppThemeConfig) {
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            nowMillis = System.currentTimeMillis()
        }
    }

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
                    Text("Live Speed Graph", color = theme.textPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
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

                        drawContext.canvas.nativeCanvas.drawText(String.format(Locale.US, "%.0fM", maxV), 0f, 26f, labelPaint)
                        drawContext.canvas.nativeCanvas.drawText(String.format(Locale.US, "%.0fM", maxV / 2f), 0f, chartH / 2f + 10f, labelPaint)
                        drawContext.canvas.nativeCanvas.drawText("0", 15f, chartH, labelPaint)

                        val path = Path()
                        pts.forEachIndexed { i, v ->
                            val x = leftPad + (i * stepX)
                            val y = chartH - ((v / maxV) * (chartH - 25f))
                            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                            drawCircle(color = theme.accent, radius = 6f, center = Offset(x, y))
                            drawContext.canvas.nativeCanvas.drawText(String.format(Locale.US, "%.1f", v), x, y - 14f, pointPaint)
                        }
                        drawPath(path = path, color = theme.accent, style = Stroke(width = 5f, cap = StrokeCap.Round))
                    }
                }
            }
        }

        item {
            Text("Session Log (Live & Previous Sessions)", color = theme.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
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
                val displaySessionTime = if (isCurrent) {
                    calculateLiveDuration(sess.connDate, nowMillis)
                } else {
                    sess.duration.ifEmpty { "00:00:00" }
                }

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
                            text = if (isCurrent) "● ACTIVE SESSION" else "COMPLETED SESSION",
                            color = if (isCurrent) Color(0xFF10B981) else theme.accent,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        Text("↓ ${sess.download}  |  ↑ ${sess.upload}", color = theme.textPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column {
                            Text("Start: ${sess.connDate}", color = theme.textSecondary, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("End:   ${sess.discDate}", color = theme.textSecondary, fontSize = 12.sp)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = if (isCurrent) "LIVE DURATION" else "SESSION TIME",
                                color = theme.textSecondary,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = displaySessionTime,
                                color = if (isCurrent) Color(0xFF10B981) else theme.textPrimary,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ComplainTab(
    data: PlusnetState,
    theme: AppThemeConfig,
    onOpenNewTicket: () -> Unit,
    onTicketClick: (TicketItem) -> Unit
) {
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
            Text("Complain & Ticket History (Tap any ticket to view)", color = theme.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
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
                        .clickable { onTicketClick(ticket) }
                        .padding(14.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "#${ticket.id} • ${ticket.title}",
                            color = theme.textPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f).padding(end = 8.dp)
                        )
                        Surface(color = badgeColor.copy(alpha = 0.2f), shape = RoundedCornerShape(8.dp)) {
                            Text(ticket.status, color = badgeColor, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(ticket.description, color = theme.textSecondary, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(ticket.date, color = theme.textSecondary, fontSize = 11.sp)
                        Text("Tap to view details →", color = theme.accent, fontSize = 11.sp, fontWeight = FontWeight.Medium)
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
    val effectiveCategories = categories.ifEmpty {
        listOf(
            TicketCategory("NO INTERNET", "NO INTERNET"),
            TicketCategory("Slow Speed", "Slow Speed"),
            TicketCategory("বিলিং", "বিলিং (Billing)"),
            TicketCategory("TJ box position change/Room change", "TJ box position change/Room change"),
            TicketCategory("Router Configuration", "Router Configuration")
        )
    }
    var selectedCat by remember(effectiveCategories) { mutableStateOf(effectiveCategories.first()) }
    var expanded by remember { mutableStateOf(false) }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = theme.card,
        title = { Text("Create New Complain", color = theme.textPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Select Category (${effectiveCategories.size} options loaded)", color = theme.textSecondary, fontSize = 12.sp)
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
                        effectiveCategories.forEach { cat ->
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
