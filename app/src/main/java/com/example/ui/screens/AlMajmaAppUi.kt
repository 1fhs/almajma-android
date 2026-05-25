package com.example.ui.screens

import android.widget.Toast
import kotlinx.coroutines.launch
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.database.*
import com.example.data.network.NetworkMode
import com.example.ui.PlatformViewModel

@Composable
fun AlMajmaAppUi(viewModel: PlatformViewModel) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val currentRole by viewModel.currentRole.collectAsStateWithLifecycle()
    val networkMode by viewModel.networkMode.collectAsStateWithLifecycle()
    val latencyMs by viewModel.latencyMs.collectAsStateWithLifecycle()
    val meshPeersNearby by viewModel.meshPeersNearby.collectAsStateWithLifecycle()

    val clientScreen by viewModel.clientScreen.collectAsStateWithLifecycle()
    val merchantScreen by viewModel.merchantScreen.collectAsStateWithLifecycle()
    val driverScreen by viewModel.driverScreen.collectAsStateWithLifecycle()

    val context = LocalContext.current

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("al_majma_root_scaffold")
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (currentUser == null) {
                // Auth system screen
                AuthScreenAr(viewModel = viewModel)
            } else if (currentRole != "admin" && currentUser?.isProfileComplete != true) {
                // لا تفتح السوق/الصيدلية/التوصيل قبل اكتمال بيانات الحساب.
                ProfileScreen(viewModel = viewModel)
            } else {
                // Render the selected Profile (4-apps-in-1)
                Crossfade(targetState = currentRole, label = "RoleAppTransition") { role ->
                    when (role) {
                        "client" -> ClientAppRoot(viewModel = viewModel, currentScreen = clientScreen)
                        "merchant" -> MerchantAppRoot(viewModel = viewModel, currentScreen = merchantScreen)
                        "driver" -> DriverAppRoot(viewModel = viewModel, currentScreen = driverScreen)
                        "admin" -> SuperAdminDashboardScreen(viewModel = viewModel)
                    }
                }
            }
        }
    }
}

// ARABIC TRANSLATION HELPERS
fun getRoleArName(role: String): String = when (role) {
    "client" -> "الزبون"
    "merchant" -> "المستثمر والتاجر"
    "driver" -> "كابتن التوصيل"
    "admin" -> "مدير النظام"
    else -> role
}

fun getBusinessTypeArName(type: String): String = when (type) {
    "pharmacy" -> "صيدلية"
    "marketplace" -> "تاجر سوق / ملابس / جملة"
    "delivery" -> "توصيل"
    "admin" -> "إدارة"
    else -> "حساب فردي"
}

fun isMarketplaceCategory(category: String): Boolean = category in listOf(
    "clothing", "wholesale", "general_market", "market", "souk"
)

fun isMedicineCategory(category: String): Boolean = category == "medicine"

fun isDeliveryCategory(category: String): Boolean = category in listOf("ride", "delivery")

fun isInfluencerCategory(category: String): Boolean = category == "influencer"

fun categoryArName(category: String): String = when (category) {
    "medicine" -> "💊 دواء"
    "ride" -> "🏍️ مشوار"
    "delivery" -> "🚚 خدمة توصيل"
    "clothing" -> "👕 ملابس"
    "wholesale" -> "📦 جملة"
    "general_market", "market", "souk" -> "🛍️ سوق"
    "influencer" -> "⭐ مشاهير"
    else -> "🧾 $category"
}

fun merchantSectionTitle(user: UserEntity?): String = when (user?.businessType) {
    "pharmacy" -> "واجهة الصيدلية وإدارة الدواء 💊"
    "marketplace" -> "واجهة تاجر السوق والملابس 🛍️"
    else -> "واجهة التاجر — أكمل بيانات النشاط"
}

fun getNetworkArName(mode: NetworkMode): String = when (mode) {
    NetworkMode.ONLINE -> "إنترنت عادي السيرفر الموحد"
    NetworkMode.ZERO_DATA -> "توفير البيانات (اتصال APN مغلق)"
    NetworkMode.OFFLINE_MESH -> "الشبكة المحلية (Wi-Fi Mesh / BLE)"
}


fun orderStatusAr(status: String): String = when (status) {
    "waiting_offers" -> "بانتظار عروض الصيدليات"
    "offer_received" -> "وصل عرض صيدلية"
    "pending" -> "قيد التفاوض"
    "funds_frozen" -> "أموال مجمدة بالضمان"
    "preparing" -> "قيد التجهيز"
    "ready" -> "جاهز للتسليم"
    "delivering" -> "قيد التوصيل"
    "completed" -> "مكتمل ومغلق"
    "cancelled" -> "ملغي"
    "disputed" -> "نزاع مفتوح"
    "refunded" -> "مسترجع للعميل"
    "CONFLICT" -> "تعارض سعر"
    else -> status
}

@Composable
fun statusBadgeContainerColor(status: String): Color = when (status) {
    "funds_frozen", "ready", "preparing", "delivering" -> MaterialTheme.colorScheme.secondary.copy(0.20f)
    "completed" -> MaterialTheme.colorScheme.primary.copy(0.20f)
    "offer_received", "waiting_offers", "pending" -> MaterialTheme.colorScheme.surfaceVariant
    "CONFLICT", "disputed", "refunded", "cancelled" -> MaterialTheme.colorScheme.error.copy(0.16f)
    else -> MaterialTheme.colorScheme.surfaceVariant
}

@Composable
fun statusBadgeTextColor(status: String): Color = when (status) {
    "funds_frozen", "ready", "preparing", "delivering" -> MaterialTheme.colorScheme.secondary
    "completed" -> MaterialTheme.colorScheme.primary
    "CONFLICT", "disputed", "refunded", "cancelled" -> MaterialTheme.colorScheme.error
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}

@Composable
fun OrderLifecycleStrip(status: String) {
    val steps = listOf(
        "waiting_offers" to "طلب",
        "offer_received" to "عرض",
        "funds_frozen" to "ضمان",
        "preparing" to "تجهيز",
        "ready" to "جاهز",
        "delivering" to "توصيل",
        "completed" to "إغلاق"
    )
    val activeIndex = steps.indexOfFirst { it.first == status }.let { if (it < 0 && status == "pending") 0 else it }
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(steps) { (key, label) ->
            val idx = steps.indexOfFirst { it.first == key }
            val done = activeIndex >= 0 && idx <= activeIndex
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (done) MaterialTheme.colorScheme.primary.copy(0.16f) else MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, if (key == status) MaterialTheme.colorScheme.primary else Color.Transparent)
            ) {
                Text(
                    text = label,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    fontSize = 10.sp,
                    fontWeight = if (done) FontWeight.Bold else FontWeight.Normal,
                    color = if (done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ==========================================
// CENTRAL COCKPIT PANEL FOR SIMULATING NETWORKS & ROLES
// ==========================================
@Composable
fun SimulationCockpitPanel(
    currentRole: String,
    networkMode: NetworkMode,
    latencyMs: Int,
    peersNearby: Int,
    onRoleSwitch: (String) -> Unit,
    onNetworkSwitch: (NetworkMode) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Network simulator row
            Text(
                text = "📱 لوحة تفاعلية لبيئة الشبكة الهجينة والملفات والمستشعرات",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Connection mode buttons
                NetworkModeChip(
                    title = "إنترنت عام",
                    active = networkMode == NetworkMode.ONLINE,
                    icon = Icons.Default.Share,
                    color = MaterialTheme.colorScheme.primary,
                    onClick = { onNetworkSwitch(NetworkMode.ONLINE) }
                )
                NetworkModeChip(
                    title = "APN مخفَّف",
                    active = networkMode == NetworkMode.ZERO_DATA,
                    icon = Icons.Default.Refresh,
                    color = MaterialTheme.colorScheme.secondary,
                    onClick = { onNetworkSwitch(NetworkMode.ZERO_DATA) }
                )
                NetworkModeChip(
                    title = "شبكة Mesh",
                    active = networkMode == NetworkMode.OFFLINE_MESH,
                    icon = Icons.Default.Settings,
                    color = MaterialTheme.colorScheme.tertiary,
                    onClick = { onNetworkSwitch(NetworkMode.OFFLINE_MESH) }
                )
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.surfaceVariant)

            // Dynamic live network latency/mesh tag
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(
                                when (networkMode) {
                                    NetworkMode.ONLINE -> MaterialTheme.colorScheme.primary
                                    NetworkMode.ZERO_DATA -> MaterialTheme.colorScheme.secondary
                                    NetworkMode.OFFLINE_MESH -> MaterialTheme.colorScheme.tertiary
                                }
                            )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = when (networkMode) {
                            NetworkMode.ONLINE -> "متصل مركزي - سرعة الاستجابة: $latencyMs ملي ثانية"
                            NetworkMode.ZERO_DATA -> "وضع توفير الرصيد (نصوص مضغوطة وبدون صور)"
                            NetworkMode.OFFLINE_MESH -> "وضع الأوفلاين - أجهزة مجاورة بالـ Bluetooth: $peersNearby"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Account role shortcut
                Row {
                    RoleSwitchButton(label = "زبون", active = currentRole == "client", onClick = { onRoleSwitch("client") })
                    Spacer(modifier = Modifier.width(4.dp))
                    RoleSwitchButton(label = "تاجر", active = currentRole == "merchant", onClick = { onRoleSwitch("merchant") })
                    Spacer(modifier = Modifier.width(4.dp))
                    RoleSwitchButton(label = "سائق", active = currentRole == "driver", onClick = { onRoleSwitch("driver") })
                    Spacer(modifier = Modifier.width(4.dp))
                    RoleSwitchButton(label = "مدير", active = currentRole == "admin", onClick = { onRoleSwitch("admin") })
                }
            }
        }
    }
}

@Composable
fun NetworkModeChip(
    title: String,
    active: Boolean,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (active) color.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, if (active) color else Color.Transparent),
        modifier = Modifier
            .height(34.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(imageVector = icon, contentDescription = title, tint = if (active) color else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = title, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = if (active) color else MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun RoleSwitchButton(label: String, active: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(6.dp),
        color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.height(26.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 8.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = if (active) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold
            )
        }
    }
}


// ==========================================
// SCREEN 1: THE LOCALIZED SYSTEM AUTH (تسجيل الدخول والتحقق الآمن)
// ==========================================
@Composable
fun AuthScreenAr(viewModel: PlatformViewModel) {
    val phoneInput by viewModel.phoneInput.collectAsStateWithLifecycle()
    val otpInput by viewModel.otpInput.collectAsStateWithLifecycle()
    val isWaitingForOtp by viewModel.isWaitingForOtp.collectAsStateWithLifecycle()
    val selectedAuthRole by viewModel.selectedAuthRole.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
            .testTag("auth_screen_container"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(90.dp)
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFF10B981).copy(0.3f), Color.Transparent)
                        ),
                        radius = size.width
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.foundation.Image(
                painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.almajma_logo_1779666748298),
                contentDescription = "شعار التطبيق",
                modifier = Modifier.size(64.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "مَجْمَع الضمان والاقتصاد الهجين",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )
        Text(
            text = "نظام التبادل والخدمات اللامركزي مع/بدون إنترنت",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        if (!isWaitingForOtp) {
            // Phone entry
            OutlinedTextField(
                value = phoneInput,
                onValueChange = { viewModel.phoneInput.value = it },
                label = { Text("أدخل رقم هاتفك الجوال (77xxxxxxx)", textAlign = TextAlign.End) },
                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = "هاتف", tint = MaterialTheme.colorScheme.primary) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("phone_input_field")
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Account type selector during signup
            Text(
                text = "اختر طبيعة حساب الدخول للتجربة:",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                textAlign = TextAlign.Start
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RoleSelectionCard(
                        title = "عميل",
                        desc = "صيدليات + سوق + مشاهير + توصيل",
                        active = selectedAuthRole == "client",
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.selectedAuthRole.value = "client" }
                    )
                    RoleSelectionCard(
                        title = "صيدلية",
                        desc = "أدوية، وصفات، ترخيص، عروض",
                        active = selectedAuthRole == "pharmacy",
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.selectedAuthRole.value = "pharmacy" }
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RoleSelectionCard(
                        title = "تاجر سوق",
                        desc = "ملابس، جملة، منتجات عامة",
                        active = selectedAuthRole == "market_merchant",
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.selectedAuthRole.value = "market_merchant" }
                    )
                    RoleSelectionCard(
                        title = "سائق",
                        desc = "طلبات توصيل مستقلة ودواء جاهز",
                        active = selectedAuthRole == "driver",
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.selectedAuthRole.value = "driver" }
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RoleSelectionCard(
                        title = "أدمن",
                        desc = "لوحة الإدارة، الاعتماد، التقارير",
                        active = selectedAuthRole == "admin",
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.selectedAuthRole.value = "admin" }
                    )
                    Spacer(modifier = Modifier.weight(1f))
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { viewModel.requestOtp() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("request_otp_button"),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("المتابعة والتسجيل 🚀", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
            }

        } else {
            // Waiting for OTP Verification simulation
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "محاكاة رمز التأكيد الموجه للجوال $phoneInput",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "الرمز الافتراضي للتجربة هو: 1234",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = otpInput,
                onValueChange = { viewModel.otpInput.value = it },
                label = { Text("رمز OTP المكون من 4 أرقام") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "رمز التأكيد", tint = MaterialTheme.colorScheme.primary) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("otp_input_field")
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { viewModel.verifyLoginOtp() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("confirm_login_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("الدخول للمنصة والمزامنة", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(
                onClick = { viewModel.isWaitingForOtp.value = false },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("رجوع لتعديل الرقم", color = MaterialTheme.colorScheme.secondary)
            }
        }
    }
}

@Composable
fun RoleSelectionCard(
    title: String,
    desc: String,
    active: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(110.dp)
            .clickable { onClick() }
            .border(
                width = 1.5.dp,
                color = if (active) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (active) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                desc,
                fontSize = 9.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 11.sp
            )
        }
    }
}


@Composable
fun AlMajmaTopBar(viewModel: PlatformViewModel, titleRole: String) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val currentRole by viewModel.currentRole.collectAsStateWithLifecycle()
    val unreadCount by viewModel.unreadNotificationCount.collectAsStateWithLifecycle()
    
    val topBarColor = when(currentRole) {
        "client" -> MaterialTheme.colorScheme.primary
        "merchant" -> MaterialTheme.colorScheme.secondary
        "driver" -> MaterialTheme.colorScheme.tertiary
        "admin" -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.primaryContainer
    }
    val contentColor = Color.White
    
    // Header for status bar and title
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(topBarColor)
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .statusBarsPadding(), // Support edge-to-edge
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(titleRole, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = contentColor)
            Text("مرحباً بك: ${currentUser?.phone ?: ""} | رصيد المحفظة: ${currentUser?.let { Money.formatMinor(it.walletBalanceMinor) } ?: "0.00"} ريال", fontSize = 11.sp, color = contentColor.copy(0.85f))
        }
        
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            if (unreadCount > 0) {
                Surface(
                    color = contentColor.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.clickable { viewModel.markMyNotificationsRead() }
                ) {
                    Text(
                        text = "تنبيهات $unreadCount",
                        color = contentColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                    )
                }
            }
            IconButton(
                onClick = { 
                    when(currentRole) {
                        "client" -> viewModel.navigateClientTo("profile")
                        "merchant" -> viewModel.navigateMerchantTo("profile")
                        "driver" -> viewModel.navigateDriverTo("profile")
                    }
                },
                modifier = Modifier
                    .background(contentColor.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                    .size(36.dp)
            ) {
                Icon(Icons.Default.Person, contentDescription = "الملف الشخصي", tint = contentColor, modifier = Modifier.size(20.dp))
            }
            
            IconButton(
                onClick = { viewModel.logout() },
                modifier = Modifier
                    .background(contentColor.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                    .size(36.dp)
            ) {
                Icon(Icons.Default.ExitToApp, contentDescription = "تسجيل الخروج", tint = contentColor, modifier = Modifier.size(20.dp))
            }
        }
    }
}

// ==========================================
// FIRST APPLICATION HIERARCHY: CLIENT PERSPECTIVE (زبون المجمع)
// ==========================================
@Composable
fun ClientAppRoot(viewModel: PlatformViewModel, currentScreen: String) {
    Column(modifier = Modifier.fillMaxSize()) {
        AlMajmaTopBar(viewModel = viewModel, titleRole = "واجهة عميل المجمع 🌟")
        // Client specific screens
        AnimatedContent(
            targetState = currentScreen,
            label = "ClientScreenTransition",
            modifier = Modifier.weight(1f)
        ) { screen ->
            when (screen) {
                "home" -> ClientHomeScreen(viewModel = viewModel)
                "ride", "delivery" -> ClientDeliveryServiceScreen(viewModel = viewModel)
                "pharmacy" -> ClientPharmacyScreen(viewModel = viewModel)
                "Souk", "market" -> ClientSoukScreen(viewModel = viewModel)
                "influencers" -> ClientInfluencersScreen(viewModel = viewModel)
                "chat" -> ChatRoomScreen(viewModel = viewModel)
                "wallet" -> WalletScreenAr(viewModel = viewModel)
                "profile" -> ProfileScreen(viewModel = viewModel)
                else -> ClientHomeScreen(viewModel = viewModel)
            }
        }

        // Inner Sub Bottom-Bar for Client navigation
        ClientBottomNav(
            currentScreen = currentScreen,
            onNav = { viewModel.navigateClientTo(it) }
        )
    }
}

@Composable
fun ClientBottomNav(currentScreen: String, onNav: (String) -> Unit) {
    NavigationBar(
        modifier = Modifier.height(65.dp),
        tonalElevation = 8.dp,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        NavigationBarItem(
            selected = currentScreen == "home" || currentScreen == "chat",
            onClick = { onNav("home") },
            icon = { Icon(Icons.Default.Home, contentDescription = "الرئيسية") },
            label = { Text("الرئيسية", fontSize = 10.sp) },
            colors = NavigationBarItemDefaults.colors(selectedIconColor = MaterialTheme.colorScheme.primary)
        )
        NavigationBarItem(
            selected = currentScreen == "pharmacy",
            onClick = { onNav("pharmacy") },
            icon = { Icon(Icons.Default.Star, contentDescription = "الصيدليات") },
            label = { Text("صيدليات", fontSize = 10.sp) },
            colors = NavigationBarItemDefaults.colors(selectedIconColor = MaterialTheme.colorScheme.primary)
        )
        NavigationBarItem(
            selected = currentScreen == "market" || currentScreen == "Souk",
            onClick = { onNav("market") },
            icon = { Icon(Icons.Default.ShoppingCart, contentDescription = "السوق") },
            label = { Text("السوق", fontSize = 10.sp) },
            colors = NavigationBarItemDefaults.colors(selectedIconColor = MaterialTheme.colorScheme.primary)
        )
        NavigationBarItem(
            selected = currentScreen == "influencers",
            onClick = { onNav("influencers") },
            icon = { Icon(Icons.Default.Favorite, contentDescription = "مشاهير") },
            label = { Text("مشاهير", fontSize = 10.sp) },
            colors = NavigationBarItemDefaults.colors(selectedIconColor = MaterialTheme.colorScheme.primary)
        )
        NavigationBarItem(
            selected = currentScreen == "delivery" || currentScreen == "ride",
            onClick = { onNav("delivery") },
            icon = { Icon(Icons.Default.LocationOn, contentDescription = "توصيل") },
            label = { Text("توصيل", fontSize = 10.sp) },
            colors = NavigationBarItemDefaults.colors(selectedIconColor = MaterialTheme.colorScheme.primary)
        )
    }
}

// ------------------------------------------
// CLIENT SCREEN 1: CLIENT HOME DASHBOARD (اللوحة الرئيسية لزبون المجمع)
// ------------------------------------------
@Composable
fun ClientHomeScreen(viewModel: PlatformViewModel) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val orders by viewModel.orders.collectAsStateWithLifecycle()
    val systemConfig by viewModel.systemConfig.collectAsStateWithLifecycle()
    val notifications by viewModel.userNotifications.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("client_home_scroller"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Clear Client Application Profile Data
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(0.5f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("إحصائيات حسابك:", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("رقم الهاتف المتصل", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(currentUser?.phone ?: "غير معروف", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("حالة النشاط", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(if (currentUser?.status == "active") "متصل ومفعل" else "قيد المراجعة", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }

        // Welcome and e-Wallet balance header
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(0.3f), RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "مرحباً بك، ${currentUser?.phone?.takeLast(4) ?: "أحمد"}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "رصيد المحفظة الآمنة",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = "${currentUser?.let { Money.formatMinor(it.walletBalanceMinor) } ?: "12,000.00"} ريال يمني",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Surface(
                        onClick = { viewModel.navigateClientTo("wallet") },
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.height(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Add, contentDescription = "شحن", tint = Color.Black)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("إيداع سريع", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color.Black)
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(12.dp)) }

        // Dynamic Server-driven Promo Banner notice
        if (systemConfig.isPromoBannerVisible && systemConfig.promoBannerText.isNotBlank()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(0.12f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, contentDescription = "إعلان", tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = systemConfig.promoBannerText,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }


        if (notifications.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondary.copy(0.10f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(0.35f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("آخر التنبيهات التشغيلية", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary, fontSize = 13.sp)
                            TextButton(onClick = { viewModel.markMyNotificationsRead() }) { Text("تعليم كمقروء", fontSize = 11.sp) }
                        }
                        notifications.take(3).forEach { n ->
                            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                Text(n.title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (n.isRead) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface)
                                Text(n.body, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
        item { Spacer(modifier = Modifier.height(12.dp)) }

        // Four clear customer tracks. Do not mix pharmacy, market, influencers and delivery.
        item {
            Text(
                text = "اختر مسار الخدمة",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start
            )
            Text(
                text = "تم فصل المسارات حتى لا تختلط طلبات الدواء مع السوق أو المشاهير أو التوصيل.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ServiceBox(
                    title = "صيدليات وأدوية",
                    desc = "بحث دواء وبث طلب للصيدليات",
                    icon = Icons.Default.Star,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.navigateClientTo("pharmacy") }
                )
                ServiceBox(
                    title = "سوق وملابس",
                    desc = "ملابس وجملة ومنتجات عامة",
                    icon = Icons.Default.ShoppingCart,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.navigateClientTo("market") }
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ServiceBox(
                    title = "مشاهير",
                    desc = "إعلانات وترويج محلي بضمان",
                    icon = Icons.Default.Favorite,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.navigateClientTo("influencers") }
                )
                ServiceBox(
                    title = "خدمة توصيل",
                    desc = "مندوب/موتور لنقل طلب أو طرد",
                    icon = Icons.Default.LocationOn,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.navigateClientTo("delivery") }
                )
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }

        item { Spacer(modifier = Modifier.height(24.dp)) }

        // Live Escrow tracking panel
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🔔 تتبع الضمان المالي والطلبات النشطة (Escrow)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "الكود السري مطلوب عند اللقاء",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        val clientOrders = orders.filter { it.clientId == (currentUser?.id ?: -1) }
        if (clientOrders.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Box(modifier = Modifier.padding(16.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = "لا توجد أي طلبات نشطة حالياً. اختر خدمة بالأعلى لبدء الطلب والمساومة الفورية.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(clientOrders) { order ->
                OrderTrackingCard(
                    order = order,
                    viewModel = viewModel,
                    onClick = { viewModel.selectOrderForChat(order.id) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun ServiceBox(
    title: String,
    desc: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(115.dp)
            .clickable { onClick() }
            .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(color.copy(0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = title, tint = color, modifier = Modifier.size(20.dp))
            }

            Column {
                Text(text = title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = desc, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
fun OrderTrackingCard(order: OrderEntity, viewModel: PlatformViewModel, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .border(
                width = 1.dp,
                color = when (order.status) {
                    "funds_frozen", "preparing", "ready", "delivering" -> MaterialTheme.colorScheme.secondary
                    "completed" -> MaterialTheme.colorScheme.primary
                    "CONFLICT", "disputed", "refunded", "cancelled" -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.surfaceVariant
                },
                shape = RoundedCornerShape(12.dp)
            )
            .testTag("track_card_${order.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category Tag
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = categoryArName(order.category),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Escrow Status Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(statusBadgeContainerColor(order.status))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = orderStatusAr(order.status),
                        color = statusBadgeTextColor(order.status),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = order.productName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = if (order.status == "CONFLICT") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(6.dp))

            OrderLifecycleStrip(order.status)

            Spacer(modifier = Modifier.height(6.dp))

            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "سعر الطلب الأصلي: ${order.originalPriceAtRequest} ريال",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "أجر الناقل: ${order.deliveryFee} ريال",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (order.status == "CONFLICT") {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(0.8f))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "⚠️ تباين الأسعار: تم رصد تحديث للسعر من الصيدلية في السيرفر بسبب تغييرات التسعير الميدانية.",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("السعر القديم: ${order.originalPriceAtRequest} ريال", fontSize = 11.sp, textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough)
                                Text("السعر الجديد: ${order.serverUpdatedPriceConflict} ريال", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                            }
                            Button(
                                onClick = { viewModel.resolveOrderPriceConflict(order.id) },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                modifier = Modifier.height(34.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                            ) {
                                Text("موافق وتثبيت الالتزام ✓", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                }
            }

            if (order.category == "medicine" && order.merchantId != null && order.status in listOf("pending", "offer_received") && order.totalPrice > 0.0) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(0.10f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(0.35f))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "💊 عرض صيدلية جاهز للتأكيد",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("سعر الدواء: ${Money.formatMinor(order.totalPriceMinor)} ريال", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text("الإجمالي مع التوصيل: ${Money.formatMinor(order.totalPriceMinor + order.deliveryFeeMinor)} ريال", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Button(
                                onClick = { viewModel.acceptMerchantQuote(order.id) },
                                modifier = Modifier.height(34.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                            ) {
                                Text("قبول وتجميد الضمان ✓", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                }
            }


            if (order.status in listOf("waiting_offers", "offer_received", "funds_frozen", "preparing", "ready", "delivering")) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (order.status in listOf("waiting_offers", "offer_received", "funds_frozen")) {
                        OutlinedButton(
                            onClick = { viewModel.cancelOrderAndRefund(order.id, "إلغاء من العميل قبل اكتمال التسليم") },
                            modifier = Modifier.weight(1f).height(34.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) { Text("إلغاء/استرجاع", fontSize = 10.sp) }
                    }
                    if (order.status in listOf("funds_frozen", "preparing", "ready", "delivering")) {
                        Button(
                            onClick = { viewModel.openOrderDispute(order.id, "مشكلة في الطلب", "تم فتح نزاع من بطاقة الطلب لمراجعة الإدارة.") },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error.copy(0.85f)),
                            modifier = Modifier.weight(1f).height(34.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) { Text("فتح نزاع", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                    }
                }
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.surfaceVariant)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Secret Release Code
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lock, contentDescription = "قفل", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "كود استلام القيمة للناقل: ",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = order.otpReleaseCode,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                // Chat link
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (order.status != "completed") "فتح المساومة وغرفة الأمان" else "سجل التفاوض",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "دخول",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(14.dp)
                    )
                }
            }

            if (order.status == "completed") {
                Spacer(modifier = Modifier.height(8.dp))
                Divider(color = MaterialTheme.colorScheme.surfaceVariant.copy(0.5f))
                Spacer(modifier = Modifier.height(6.dp))

                var isFeedbackExpanded by remember { mutableStateOf(false) }
                var ratingVal by remember { mutableStateOf(5) }
                var selectedTag by remember { mutableStateOf("") }
                var customComment by remember { mutableStateOf("") }
                var isSubmitted by remember { mutableStateOf(false) }

                if (!isSubmitted) {
                    if (!isFeedbackExpanded) {
                        Button(
                            onClick = { isFeedbackExpanded = true },
                            modifier = Modifier.fillMaxWidth().height(32.dp),
                            contentPadding = PaddingValues(0.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(0.12f))
                        ) {
                            Text("⭐ قيم كابتن التوصيل والتاجر (التقييم مشروط بالطلب)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    } else {
                        Column(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant.copy(0.3f)).padding(8.dp)) {
                            Text("تقييم متبادل لحفظ الثقة والضمان اللامركزي:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                            
                            // Stars selector
                            Row(modifier = Modifier.padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                (1..5).forEach { star ->
                                    Icon(
                                        imageVector = if (star <= ratingVal) Icons.Default.Star else Icons.Default.FavoriteBorder,
                                        contentDescription = "Star",
                                        tint = if (star <= ratingVal) MaterialTheme.colorScheme.secondary else Color.Gray,
                                        modifier = Modifier.size(22.dp).clickable { ratingVal = star }
                                    )
                                }
                            }

                            // Quick Tags Chips
                            Text("خيارات سريعة:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            val quickTags = when (order.category) {
                                "medicine" -> listOf("دواء أصلي بالكامل", "سعر مبرر وممتاز", "أمان في التوصيل", "استجابة سريعة")
                                "ride" -> listOf("سرعة الوصول", "سياقة حذرة آمنة", "مطابق للأجر التفاوضي", "أمين ومحترم")
                                else -> listOf("جودة مطابقة", "سعر منخفض ومناسب", "تغليف مبرشم ونظيف", "أموال معادة بالدقة")
                            }

                            LazyRow(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(quickTags) { tag ->
                                    Surface(
                                        onClick = { selectedTag = tag },
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (selectedTag == tag) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant,
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(0.3f))
                                    ) {
                                        Text(tag, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            // Custom Comment Text Field
                            OutlinedTextField(
                                value = customComment,
                                onValueChange = { customComment = it },
                                label = { Text("تعليق تفصيلي (اختياري)...", fontSize = 10.sp) },
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                textStyle = MaterialTheme.typography.bodySmall
                            )

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        val reviewerId = order.clientId
                                        val revieweeId = order.driverId ?: order.merchantId ?: 1
                                        viewModel.submitUserReview(
                                            orderId = order.id,
                                            reviewerId = reviewerId,
                                            revieweeId = revieweeId,
                                            rating = ratingVal,
                                            tags = selectedTag,
                                            comment = customComment
                                        )
                                        isSubmitted = true
                                    },
                                    modifier = Modifier.weight(1f).height(32.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("تأكيد التقييم ✓", color = Color.Black, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                                }

                                OutlinedButton(
                                    onClick = { isFeedbackExpanded = false },
                                    modifier = Modifier.weight(0.5f).height(32.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("إلغاء", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Submitted", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("تم تسجيل تقييمكم كطرف أول في العقد المالي بنجاح! شكراً لك.", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ------------------------------------------
// CLIENT SCREEN 2: RIDE HAILING & BARGAINING (طلب موتور - الحارات ومساومة الأجر)
// ------------------------------------------
@Composable
fun ClientRideHailingScreen(viewModel: PlatformViewModel) {
    val networkMode by viewModel.networkMode.collectAsStateWithLifecycle()
    var startingPoint by remember { mutableStateOf("حارة اللقية") }
    var destinationPoint by remember { mutableStateOf("جولة السفارة") }
    var userOfferPriceInput by remember { mutableStateOf("1500") }
    var paymentMethod by remember { mutableStateOf("wallet") } // wallet, cash

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
            .testTag("ride_screen_container")
    ) {
        Text(
            text = "🏍️ طلب ناقل أو موتور بالمساومة والشبكة الهجينة",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        // MOCK MAP WITH OFFLINE FALLBACK AS REQUESTED IN PRD
        Text(
            text = "محاكاة خريطة الإحداثيات الجغرافية:",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.secondary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(1.dp, MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (networkMode == NetworkMode.OFFLINE_MESH || networkMode == NetworkMode.ZERO_DATA) {
                // TEXT ONLY / NAMES FOR OFFLINE
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(12.dp)) {
                    Icon(Icons.Default.Warning, contentDescription = "أوفلاين", tint = MaterialTheme.colorScheme.secondary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "تم تفعيل دليل أسماء الحارات لتوفير استهلاك الخارطة النشطة",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "الدليل الجغرافي النشط: [حشيش - حدة - اللقية - مسيك - حارة أ إلى ب]",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                // SIMULATOR MAP VECTOR GAUGE
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .drawBehind {
                            // Draw mock grid and destination line
                            drawLine(
                                color = Color(0xFF10B981),
                                start = Offset(50f, 150f),
                                end = Offset(size.width - 50f, size.height - 100f),
                                strokeWidth = 5f
                            )
                            drawCircle(
                                color = Color(0xFFFBBF24),
                                radius = 20f,
                                center = Offset(50f, 150f)
                            )
                            drawCircle(
                                color = Color(0xFF22D3EE),
                                radius = 25f,
                                center = Offset(size.width - 50f, size.height - 100f)
                            )
                        }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(10.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("📍 نقطة الانطلاق: حارة اللقية", fontSize = 10.sp, color = Color.White, modifier = Modifier.background(Color.Black.copy(0.6f)).padding(2.dp))
                        Text("🏁 الوجهة: جولة السفارة", fontSize = 10.sp, color = Color.White, modifier = Modifier.align(Alignment.End).background(Color.Black.copy(0.6f)).padding(2.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Input Fields
        OutlinedTextField(
            value = startingPoint,
            onValueChange = { startingPoint = it },
            label = { Text("موقع الانطلاق (الحارة / المعلم)") },
            leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = "الانطلاق", tint = MaterialTheme.colorScheme.primary) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = destinationPoint,
            onValueChange = { destinationPoint = it },
            label = { Text("جهة الوصول المطلوبة") },
            leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = "الوصول", tint = MaterialTheme.colorScheme.tertiary) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        // BARGAIN PRICE FIELD
        OutlinedTextField(
            value = userOfferPriceInput,
            onValueChange = { userOfferPriceInput = it },
            label = { Text("كم تود دفع كمقترح معروض؟ (المساومة)") },
            leadingIcon = { Icon(Icons.Default.ShoppingCart, contentDescription = "مساومة", tint = MaterialTheme.colorScheme.secondary) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            suffix = { Text("ريال يمني") },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("ride_price_bargain")
        )

        Spacer(modifier = Modifier.height(12.dp))

        // PAYMENT TYPE SELECTOR
        Text("طريقة تجميد الضمان المالي:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f).clickable { paymentMethod = "wallet" }) {
                RadioButton(selected = paymentMethod == "wallet", onClick = { paymentMethod = "wallet" })
                Text("المحفظة الإلكترونية", fontSize = 12.sp)
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f).clickable { paymentMethod = "cash" }) {
                RadioButton(selected = paymentMethod == "cash", onClick = { paymentMethod = "cash" })
                Text("نقد كاش (توقيع اللقاء)", fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                val price = userOfferPriceInput.toDoubleOrNull() ?: 1500.0
                viewModel.requestRide(price, "$startingPoint إلى $destinationPoint", paymentMethod)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("submit_ride_request_button"),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = if (networkMode == NetworkMode.OFFLINE_MESH) "بث الطلب الفوري عبر شبكة Mesh الهجينة 📡" else "إرسال طلب تفاوض الموتور",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}


// ------------------------------------------
// CLIENT SCREEN 3: PHARMACY MEDICINE REVERSE SEARCH (البحث العكسي عن الدواء والروشتة)
// ------------------------------------------
@Composable
fun ClientPharmacyScreen(viewModel: PlatformViewModel) {
    val products by viewModel.products.collectAsStateWithLifecycle()

    var searchMedicineText by remember { mutableStateOf("") }
    var activeIngredientQuery by remember { mutableStateOf("") }
    var selectedMedicineCity by remember { mutableStateOf("all") }
    var selectedTherapeuticCategory by remember { mutableStateOf("all") }
    var selectedForm by remember { mutableStateOf("all") }
    var prescriptionFilter by remember { mutableStateOf("all") } // all, required, not_required
    var onlyAvailable by remember { mutableStateOf(true) }
    var sortMode by remember { mutableStateOf("relevance") } // relevance, price_asc, stock_desc, expiry_asc
    var mockImageAttached by remember { mutableStateOf(false) }
    var isSimulatingCamera by remember { mutableStateOf(false) }
    var runningOcrAudit by remember { mutableStateOf(false) }
    var ocrAuditFinished by remember { mutableStateOf(false) }
    var medicineQuantity by remember { mutableStateOf("1") }
    var medicineDeliveryMethod by remember { mutableStateOf("delivery") }
    var medicineDeliveryAddress by remember { mutableStateOf("") }
    var medicineCustomerNote by remember { mutableStateOf("") }

    val allMedicines = products.filter { it.category == "medicine" }
    val now = System.currentTimeMillis()
    val cityOptions = listOf("all" to "كل المدن", "صنعاء" to "صنعاء", "عدن" to "عدن")
    val categoryOptions = listOf(
        "all" to "كل التصنيفات",
        "painkiller" to "مسكنات",
        "antibiotic" to "مضادات",
        "diabetes" to "سكري",
        "pressure" to "ضغط",
        "vitamins" to "فيتامينات",
        "dehydration" to "جفاف"
    )
    val formOptions = listOf(
        "all" to "كل الأشكال",
        "tablet" to "حبوب",
        "syrup" to "شراب",
        "injection" to "حقن",
        "drops" to "قطرات",
        "solution" to "محلول",
        "cream" to "كريم"
    )

    fun normalize(value: String): String = value.trim().lowercase()
    fun matchesQuery(medicine: ProductEntity): Boolean {
        val q = normalize(searchMedicineText)
        val ingredientQ = normalize(activeIngredientQuery)
        val textHit = q.isBlank() || listOf(
            medicine.name,
            medicine.brand,
            medicine.description,
            medicine.batchNumber,
            medicine.barcode,
            medicine.strengthText,
            medicine.manufacturerCountry,
            medicine.therapeuticCategory
        ).any { normalize(it).contains(q) }
        val ingredientHit = ingredientQ.isBlank() || normalize(medicine.activeIngredient).contains(ingredientQ)
        return textHit && ingredientHit
    }

    val filteredMedicines = allMedicines
        .filter { medicine ->
            val available = medicine.isAvailable && !medicine.isRecalled && medicine.expiryTimestamp > now && medicine.totalStock > 0
            val cityOk = selectedMedicineCity == "all" || medicine.locationName.contains(selectedMedicineCity, ignoreCase = true)
            val categoryOk = selectedTherapeuticCategory == "all" || medicine.therapeuticCategory == selectedTherapeuticCategory
            val formOk = selectedForm == "all" || medicine.medicineForm == selectedForm
            val prescriptionOk = when (prescriptionFilter) {
                "required" -> medicine.requiresPrescription
                "not_required" -> !medicine.requiresPrescription
                else -> true
            }
            matchesQuery(medicine) && (!onlyAvailable || available) && cityOk && categoryOk && formOk && prescriptionOk
        }
        .let { list ->
            when (sortMode) {
                "price_asc" -> list.sortedBy { it.priceMinor }
                "stock_desc" -> list.sortedByDescending { it.totalStock }
                "expiry_asc" -> list.sortedBy { it.expiryTimestamp }
                else -> list.sortedWith(compareByDescending<ProductEntity> {
                    val q = normalize(searchMedicineText)
                    q.isNotBlank() && normalize(it.name).contains(q)
                }.thenBy { it.priceMinor })
            }
        }

    if (isSimulatingCamera) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(18.dp)) {
                Text("📸 كاميرا المجمع: التقاط صورة الوصفة الطبية", color = Color.White, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .size(220.dp)
                        .border(2.dp, Color.White, RoundedCornerShape(10.dp))
                        .background(Color.DarkGray),
                    contentAlignment = Alignment.Center
                ) {
                    Text("وجّه العدسة على الوصفة بوضوح", color = Color.LightGray, fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.height(18.dp))
                Button(
                    onClick = {
                        mockImageAttached = true
                        ocrAuditFinished = false
                        isSimulatingCamera = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) { Text("التقاط الصورة", color = Color.Black, fontWeight = FontWeight.Bold) }
            }
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("pharmacy_screen_container")
    ) {
        Text(
            text = "💊 بحث الدواء وطلبه من الصيدليات",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "البحث هنا يفصل بين كتالوج الدواء وطلب الدواء غير الموجود. لا يتم قبول دواء منتهي أو مسحوب.",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = searchMedicineText,
            onValueChange = { searchMedicineText = it },
            label = { Text("اسم الدواء / الشركة / الباركود / الباتش") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "بحث") },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = activeIngredientQuery,
            onValueChange = { activeIngredientQuery = it },
            label = { Text("المادة الفعالة: Paracetamol / Amoxicillin...") },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            item { FilterTabChip(title = if (onlyAvailable) "المتاح فقط ✓" else "كل النتائج", active = onlyAvailable, onClick = { onlyAvailable = !onlyAvailable }) }
            cityOptions.forEach { (value, label) ->
                item { FilterTabChip(title = label, active = selectedMedicineCity == value, onClick = { selectedMedicineCity = value }) }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            categoryOptions.forEach { (value, label) ->
                item { FilterTabChip(title = label, active = selectedTherapeuticCategory == value, onClick = { selectedTherapeuticCategory = value }) }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            formOptions.forEach { (value, label) ->
                item { FilterTabChip(title = label, active = selectedForm == value, onClick = { selectedForm = value }) }
            }
            item { FilterTabChip(title = "يتطلب وصفة", active = prescriptionFilter == "required", onClick = { prescriptionFilter = if (prescriptionFilter == "required") "all" else "required" }) }
            item { FilterTabChip(title = "بدون وصفة", active = prescriptionFilter == "not_required", onClick = { prescriptionFilter = if (prescriptionFilter == "not_required") "all" else "not_required" }) }
        }
        Spacer(modifier = Modifier.height(6.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            item { FilterTabChip(title = "الأقرب تطابقًا", active = sortMode == "relevance", onClick = { sortMode = "relevance" }) }
            item { FilterTabChip(title = "الأرخص", active = sortMode == "price_asc", onClick = { sortMode = "price_asc" }) }
            item { FilterTabChip(title = "الأكثر مخزونًا", active = sortMode == "stock_desc", onClick = { sortMode = "stock_desc" }) }
            item { FilterTabChip(title = "الأقرب انتهاءً", active = sortMode == "expiry_asc", onClick = { sortMode = "expiry_asc" }) }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(0.55f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = "نتائج البحث: ${filteredMedicines.size} من أصل ${allMedicines.size} دواء.",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "إن لم يظهر الدواء، أرسل طلبًا للصيدليات وسيتم استقبال عروض بسعر وكمية ووقت تجهيز.",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("تفاصيل طلب الدواء قبل البث", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = medicineQuantity,
                        onValueChange = { medicineQuantity = it.filter { ch -> ch.isDigit() }.take(3).ifBlank { "1" } },
                        label = { Text("الكمية") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(0.55f)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f).clickable { medicineDeliveryMethod = if (medicineDeliveryMethod == "delivery") "pickup" else "delivery" }) {
                        Checkbox(checked = medicineDeliveryMethod == "pickup", onCheckedChange = { checked -> medicineDeliveryMethod = if (checked) "pickup" else "delivery" })
                        Text(if (medicineDeliveryMethod == "pickup") "استلام من الصيدلية" else "توصيل للعنوان", fontSize = 11.sp)
                    }
                }
                OutlinedTextField(
                    value = medicineDeliveryAddress,
                    onValueChange = { medicineDeliveryAddress = it },
                    label = { Text("عنوان التوصيل / منطقة الاستلام") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = medicineCustomerNote,
                    onValueChange = { medicineCustomerNote = it },
                    label = { Text("ملاحظة للصدلية: التركيز/الشركة/أي تنبيه") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = { isSimulatingCamera = true },
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "وصفة", modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (mockImageAttached) "تغيير الوصفة" else "إرفاق وصفة", fontSize = 11.sp)
            }
            Button(
                onClick = {
                    val medicineName = searchMedicineText.ifBlank { activeIngredientQuery.ifBlank { "طلب دواء غير محدد" } }
                    viewModel.requestPrescriptionDawaa(
                        medicineName = medicineName,
                        prescriptionAttached = mockImageAttached,
                        quantity = medicineQuantity.toIntOrNull() ?: 1,
                        deliveryMethod = medicineDeliveryMethod,
                        deliveryAddress = medicineDeliveryAddress,
                        customerNote = medicineCustomerNote
                    )
                    mockImageAttached = false
                    ocrAuditFinished = false
                },
                enabled = !ocrAuditFinished,
                modifier = Modifier.weight(1.2f).height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("بث طلب للصيدليات", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }

        if (mockImageAttached) {
            Spacer(modifier = Modifier.height(6.dp))
            Text("✓ تم إرفاق وصفة مؤقتة. الرفع الحقيقي للصور سيكون مع Backend.", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
        }

        if (mockImageAttached && !ocrAuditFinished) {
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(0.65f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(0.4f))
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text("تدقيق وصفة مبدئي", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("هذا تدقيق محلي تجريبي. الإنتاج يحتاج OCR/Backend وسجل أدوية مسحوبة رسمي.", fontSize = 10.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    if (runningOcrAudit) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("جاري التدقيق...", fontSize = 10.sp)
                        }
                    } else {
                        Button(
                            onClick = {
                                runningOcrAudit = true
                                ocrAuditFinished = false
                            },
                            modifier = Modifier.fillMaxWidth().height(34.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) { Text("تشغيل التدقيق التجريبي", fontSize = 10.sp) }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text("قائمة الأدوية المطابقة", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(6.dp))

        LazyColumn(modifier = Modifier.weight(1f)) {
            if (filteredMedicines.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(0.20f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(0.35f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("لا توجد نتيجة مطابقة", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                            Text("اكتب اسم الدواء أو المادة الفعالة ثم اضغط بث طلب للصيدليات. لا تجعل العميل يضيع بسبب كتالوج ناقص.", fontSize = 11.sp)
                        }
                    }
                }
            }
            items(filteredMedicines) { medicine ->
                val isExpired = medicine.expiryTimestamp < now
                val isRecalled = medicine.isRecalled
                val canPurchase = medicine.isAvailable && !isExpired && !isRecalled && medicine.totalStock > 0
                val remainingDays = ((medicine.expiryTimestamp - now) / (24 * 60 * 60 * 1000L)).coerceAtLeast(0)

                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (!canPurchase) MaterialTheme.colorScheme.errorContainer.copy(0.15f) else MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(1.dp, if (canPurchase) MaterialTheme.colorScheme.outlineVariant else MaterialTheme.colorScheme.error.copy(0.35f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                            Column(modifier = Modifier.weight(1.4f)) {
                                Text(medicine.name, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                                Text("المادة الفعالة: ${medicine.activeIngredient.ifBlank { "غير محددة" }}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("الشركة/العلامة: ${medicine.brand.ifBlank { "غير محددة" }} | القوة: ${medicine.strengthText.ifBlank { "-" }}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("الشكل: ${medicine.medicineForm.ifBlank { "غير محدد" }} | التصنيف: ${medicine.therapeuticCategory.ifBlank { "غير محدد" }}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("الموقع: ${medicine.locationName}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("باتش: ${medicine.batchNumber} | باركود: ${medicine.barcode.ifBlank { "غير مدخل" }}", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f))
                            }
                            Column(horizontalAlignment = Alignment.End, modifier = Modifier.weight(0.8f)) {
                                Text("${medicine.price} ريال", fontWeight = FontWeight.ExtraBold, color = if (canPurchase) MaterialTheme.colorScheme.primary else Color.Gray)
                                Text("مخزون: ${medicine.totalStock}", fontSize = 10.sp)
                                Text("الصلاحية: $remainingDays يوم", fontSize = 9.sp, color = if (remainingDays < 30) Color(0xFFEA580C) else MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (medicine.requiresPrescription) item { FilterTabChip("يتطلب وصفة", true, onClick = {}) }
                            if (isRecalled) item { FilterTabChip("مسحوب", true, onClick = {}) }
                            if (isExpired) item { FilterTabChip("منتهي", true, onClick = {}) }
                            if (canPurchase) item { FilterTabChip("متاح", true, onClick = {}) }
                        }

                        if (medicine.dosageHint.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("ملاحظة جرعة: ${medicine.dosageHint}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    viewModel.purchaseMedicineProduct(
                                        product = medicine,
                                        prescriptionAttached = mockImageAttached,
                                        quantity = medicineQuantity.toIntOrNull() ?: 1,
                                        deliveryMethod = medicineDeliveryMethod,
                                        deliveryAddress = medicineDeliveryAddress,
                                        customerNote = medicineCustomerNote
                                    )
                                },
                                enabled = canPurchase && (!medicine.requiresPrescription || mockImageAttached),
                                modifier = Modifier.weight(1f).height(38.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = if (canPurchase) MaterialTheme.colorScheme.primary else Color.DarkGray)
                            ) {
                                Text(
                                    text = when {
                                        isRecalled -> "محجوب"
                                        isExpired -> "منتهي"
                                        medicine.requiresPrescription && !mockImageAttached -> "أرفق وصفة"
                                        else -> "طلب هذا الدواء"
                                    },
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            OutlinedButton(
                                onClick = {
                                    searchMedicineText = medicine.activeIngredient.ifBlank { medicine.name }
                                    viewModel.requestPrescriptionDawaa(
                                        medicineName = searchMedicineText,
                                        prescriptionAttached = mockImageAttached,
                                        quantity = medicineQuantity.toIntOrNull() ?: 1,
                                        deliveryMethod = medicineDeliveryMethod,
                                        deliveryAddress = medicineDeliveryAddress,
                                        customerNote = "طلب بدائل لنفس المادة الفعالة. $medicineCustomerNote"
                                    )
                                },
                                modifier = Modifier.weight(1f).height(38.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) { Text("طلب بدائل", fontSize = 11.sp) }
                        }
                    }
                }
            }
        }
    }
}


// ------------------------------------------
// CLIENT SCREEN 4: MARKETPLACE SHOP & BARGIN (سوق المجمع والتبادل التجاري)
// ------------------------------------------
@Composable
fun ClientSoukScreen(viewModel: PlatformViewModel) {
    val products by viewModel.products.collectAsStateWithLifecycle()
    var selectedCategoryFilter by remember { mutableStateOf("all") } // all, clothing, wholesale
    var showBargainingDialog by remember { mutableStateOf<ProductEntity?>(null) }
    var bargainPriceDraft by remember { mutableStateOf("") }
    var marketQuantityDraft by remember { mutableStateOf("1") }
    var marketDeliveryMethod by remember { mutableStateOf("delivery") }
    var marketDeliveryAddress by remember { mutableStateOf("") }
    var marketOrderNote by remember { mutableStateOf("") }

    val filteredProducts = products.filter {
        it.category != "medicine" && (selectedCategoryFilter == "all" || it.category == selectedCategoryFilter)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("souk_screen_container")
    ) {
        Text(
            text = "🛍️ سوق وملابس — مسار منفصل عن الصيدليات",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Filters categories raw horizontal
        LazyRow(modifier = Modifier.fillMaxWidth()) {
            item {
                FilterTabChip(title = "الكل", active = selectedCategoryFilter == "all", onClick = { selectedCategoryFilter = "all" })
                Spacer(modifier = Modifier.width(6.dp))
                FilterTabChip(title = "شالات وملابس", active = selectedCategoryFilter == "clothing", onClick = { selectedCategoryFilter = "clothing" })
                Spacer(modifier = Modifier.width(6.dp))
                FilterTabChip(title = "مخرجات وسوق الجملة", active = selectedCategoryFilter == "wholesale", onClick = { selectedCategoryFilter = "wholesale" })
                Spacer(modifier = Modifier.width(6.dp))
                FilterTabChip(title = "منتجات عامة", active = selectedCategoryFilter == "general_market", onClick = { selectedCategoryFilter = "general_market" })
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(filteredProducts) { item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        // Image Mock Circle
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(85.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = when (item.category) {
                                    "clothing" -> "🧣"
                                    else -> "🌾"
                                },
                                fontSize = 32.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        val catName = when(item.category) {
                            "clothing" -> "ملابس وأزياء"
                            "wholesale" -> "تجزئة وجملة"
                            "general_market" -> "منتجات عامة"
                            else -> item.category
                        }
                        Text(
                            text = catName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = item.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            text = item.locationName,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "${item.price} ريال يمني",
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 14.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            // Buy Button
                            Button(
                                onClick = {
                                    showBargainingDialog = item
                                    bargainPriceDraft = item.price.toInt().toString()
                                    marketQuantityDraft = "1"
                                    marketDeliveryMethod = "delivery"
                                    marketDeliveryAddress = ""
                                    marketOrderNote = ""
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(32.dp),
                                contentPadding = PaddingValues(1.dp)
                            ) {
                                Text("شراء", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                            }

                            // Bargain Button
                            Button(
                                onClick = {
                                    showBargainingDialog = item
                                    bargainPriceDraft = (item.price * 0.9).toInt().toString()
                                    marketQuantityDraft = "1"
                                    marketDeliveryMethod = "delivery"
                                    marketDeliveryAddress = ""
                                    marketOrderNote = ""
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(32.dp),
                                contentPadding = PaddingValues(1.dp)
                            ) {
                                Text("مساومة", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                            }
                        }
                    }
                }
            }
        }
    }

    // SIMULATED BARGAINING MODAL
    showBargainingDialog?.let { item ->
        AlertDialog(
            onDismissRequest = { showBargainingDialog = null },
            title = { Text("تقديم عرض مساومة جديدة", textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth()) },
            text = {
                Column(horizontalAlignment = Alignment.End, modifier = Modifier.fillMaxWidth()) {
                    Text("المنتج: ${item.name}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("السعر الأصلي: ${item.price} ريال يمني", style = MaterialTheme.typography.bodySmall)

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = bargainPriceDraft,
                        onValueChange = { bargainPriceDraft = it },
                        label = { Text("سعر الوحدة المقترح") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = marketQuantityDraft,
                            onValueChange = { marketQuantityDraft = it.filter { ch -> ch.isDigit() }.take(3).ifBlank { "1" } },
                            label = { Text("الكمية") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(0.65f)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f).clickable { marketDeliveryMethod = if (marketDeliveryMethod == "delivery") "pickup" else "delivery" }) {
                            Checkbox(checked = marketDeliveryMethod == "pickup", onCheckedChange = { checked -> marketDeliveryMethod = if (checked) "pickup" else "delivery" })
                            Text(if (marketDeliveryMethod == "pickup") "استلام" else "توصيل", fontSize = 11.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = marketDeliveryAddress,
                        onValueChange = { marketDeliveryAddress = it },
                        label = { Text("عنوان التوصيل / منطقة الاستلام") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = marketOrderNote,
                        onValueChange = { marketOrderNote = it },
                        label = { Text("ملاحظة: مقاس/لون/تفاوض") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val prop = bargainPriceDraft.toDoubleOrNull() ?: item.price
                        viewModel.purchaseMarketItemFull(
                            product = item,
                            offerBargainPrice = prop,
                            quantity = marketQuantityDraft.toIntOrNull() ?: 1,
                            deliveryMethod = marketDeliveryMethod,
                            deliveryAddress = marketDeliveryAddress,
                            customerNote = marketOrderNote
                        )
                        showBargainingDialog = null
                    }
                ) {
                    Text("إرسال عرض لغرفة المفاوضات", color = Color.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBargainingDialog = null }) {
                    Text("إلغاء", color = MaterialTheme.colorScheme.error)
                }
            }
        )
    }
}


// ------------------------------------------
// CLIENT SCREEN: INFLUENCERS TRACK (مشاهير)
// ------------------------------------------
@Composable
fun ClientInfluencersScreen(viewModel: PlatformViewModel) {
    val products by viewModel.products.collectAsStateWithLifecycle()
    val influencerServices = products.filter { isInfluencerCategory(it.category) }
    var campaignTitle by remember { mutableStateOf("إعلان افتتاح متجر محلي") }
    var campaignBudget by remember { mutableStateOf("25000") }
    var campaignNotes by remember { mutableStateOf("ستوري + منشور قصير للجمهور المحلي") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("influencers_screen_container"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "⭐ مشاهير — إعلانات وترويج بضمان",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "هذا المسار منفصل عن الصيدليات والسوق. مناسب لإعلانات المتاجر والخدمات المحلية.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(0.45f)), shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("طلب حملة جديدة", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    OutlinedTextField(
                        value = campaignTitle,
                        onValueChange = { campaignTitle = it },
                        label = { Text("عنوان الحملة") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = campaignBudget,
                        onValueChange = { campaignBudget = it },
                        label = { Text("الميزانية المقترحة") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        suffix = { Text("ريال") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = campaignNotes,
                        onValueChange = { campaignNotes = it },
                        label = { Text("تفاصيل الإعلان") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = { viewModel.requestInfluencerCampaign(campaignTitle, campaignBudget.toDoubleOrNull() ?: 0.0, campaignNotes) },
                        modifier = Modifier.fillMaxWidth().height(44.dp)
                    ) {
                        Text("إرسال طلب حملة مشاهير", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Text("باقات جاهزة للتجربة", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        }

        if (influencerServices.isEmpty()) {
            item {
                Text(
                    text = "لا توجد باقات مشاهير مضافة بعد. استخدم نموذج الحملة بالأعلى.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(18.dp)
                )
            }
        } else {
            items(influencerServices) { item ->
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(12.dp)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(item.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(item.description.ifBlank { "باقة ترويج محلي مع متابعة تنفيذ الحملة." }, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("${Money.formatMinor(item.priceMinor)} ريال", fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                            Button(onClick = { viewModel.purchaseSoukItem(item, null) }, modifier = Modifier.height(34.dp), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)) {
                                Text("طلب الباقة", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ------------------------------------------
// CLIENT SCREEN: DELIVERY SERVICE TRACK (خدمة توصيل)
// ------------------------------------------
@Composable
fun ClientDeliveryServiceScreen(viewModel: PlatformViewModel) {
    var pickupAddress by remember { mutableStateOf("من باب اليمن") }
    var dropoffAddress by remember { mutableStateOf("إلى شارع حدة") }
    var packageDetails by remember { mutableStateOf("طرد صغير / مستندات") }
    var offeredFee by remember { mutableStateOf("1500") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
            .testTag("delivery_service_screen"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "🚚 خدمة توصيل مستقلة",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "هذا المسار لخدمات النقل والتوصيل فقط. لا يعرض طلبات دواء ولا منتجات سوق.",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedTextField(
            value = pickupAddress,
            onValueChange = { pickupAddress = it },
            label = { Text("موقع الاستلام") },
            leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = "استلام") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        OutlinedTextField(
            value = dropoffAddress,
            onValueChange = { dropoffAddress = it },
            label = { Text("موقع التسليم") },
            leadingIcon = { Icon(Icons.Default.Send, contentDescription = "تسليم") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        OutlinedTextField(
            value = packageDetails,
            onValueChange = { packageDetails = it },
            label = { Text("تفاصيل الطرد / الخدمة") },
            minLines = 2,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = offeredFee,
            onValueChange = { offeredFee = it },
            label = { Text("الأجر المقترح للكابتن") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            suffix = { Text("ريال") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Button(
            onClick = {
                viewModel.requestDeliveryService(
                    pickupAddress = pickupAddress,
                    dropoffAddress = dropoffAddress,
                    packageDetails = packageDetails,
                    deliveryFee = offeredFee.toDoubleOrNull() ?: 1500.0
                )
            },
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            Text("بث طلب التوصيل للكباتن", color = Color.Black, fontWeight = FontWeight.Bold)
        }

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(0.10f)), border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(0.25f))) {
            Text(
                text = "ملاحظة تشغيلية: في الإنتاج سيتم فلترة الكباتن حسب المدينة والنطاق وحالة التفعيل من السيرفر، وليس من واجهة الهاتف فقط.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}

@Composable
fun FilterTabChip(title: String, active: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, if (active) Color.Transparent else MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = if (active) Color.Black else MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
        }
    }
}


// ==========================================
// CENTRAL COMPONENT: THE HYBRID IN-APP CHAT & SECURE VOICE NOTES (غرف التواصل وغرف المساومة)
// ==========================================
@Composable
fun ChatRoomScreen(viewModel: PlatformViewModel) {
    val selectedOrderId by viewModel.selectedOrderId.collectAsStateWithLifecycle()
    val orders by viewModel.orders.collectAsStateWithLifecycle()
    val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val isRecordingSimulated by viewModel.isRecordingSimulated.collectAsStateWithLifecycle()
    val recordingDurationSec by viewModel.recordingDurationSec.collectAsStateWithLifecycle()
    val timeline by viewModel.selectedOrderTimeline.collectAsStateWithLifecycle()
    val offers by viewModel.selectedOrderOffers.collectAsStateWithLifecycle()

    val activeOrder = orders.find { it.id == selectedOrderId }
    var chatTextInput by remember { mutableStateOf("") }

    if (activeOrder == null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Default.Info, contentDescription = "لا توجد محادثة", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(42.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "لا توجد غرفة نشطة. اختر طلباً من شاشة الطلبات أولاً.",
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    when (currentUser?.role) {
                        "client" -> viewModel.navigateClientTo("home")
                        "merchant" -> viewModel.navigateMerchantTo("orders")
                        "driver" -> viewModel.navigateDriverTo("radar")
                    }
                }
            ) {
                Text("رجوع للشاشة المناسبة", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            // Chat Header
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(0.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Back button
                    IconButton(onClick = {
                        // Route back to parent home properly depending on role
                        when (currentUser?.role) {
                            "client" -> viewModel.navigateClientTo("home")
                            "merchant" -> viewModel.navigateMerchantTo("orders")
                            "driver" -> viewModel.navigateDriverTo("radar")
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "رمز المفاوضة: #${activeOrder.id}",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Lock, contentDescription = "حماية", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(11.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "الهوية آمنة ومخفية تماماً",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Escrow info status strip
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.secondary.copy(0.15f))
                    .padding(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "كود استلام القيمة للزبون: ${activeOrder.otpReleaseCode}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                    Text(
                        text = "القيمة معلقة بالضمان (Frozen)",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }


            // Operational lifecycle and offer/timeline strip
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("حالة الطلب: ${orderStatusAr(activeOrder.status)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = statusBadgeTextColor(activeOrder.status))
                        Text("#${activeOrder.id}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    OrderLifecycleStrip(activeOrder.status)

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "الكمية: ${activeOrder.quantity} | طريقة التسليم: ${if (activeOrder.deliveryMethod == "pickup") "استلام ذاتي" else "توصيل"} | القطاع: ${activeOrder.marketSector.ifBlank { activeOrder.category }}",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (activeOrder.deliveryAddress.isNotBlank()) {
                        Text("العنوان: ${activeOrder.deliveryAddress}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    if (activeOrder.customerNote.isNotBlank()) {
                        Text("ملاحظة العميل: ${activeOrder.customerNote}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                    if (activeOrder.category == "medicine") {
                        Text(
                            text = if (activeOrder.needsPrescription) if (activeOrder.prescriptionAttached) "الوصفة: مرفقة مؤقتًا" else "الوصفة: مطلوبة ولم ترفق" else "الوصفة: غير مطلوبة",
                            fontSize = 10.sp,
                            color = if (activeOrder.needsPrescription && !activeOrder.prescriptionAttached) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                    }

                    if (offers.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        val latestOffer = offers.first()
                        Text(
                            text = "آخر عرض: ${latestOffer.price} ريال | كمية ${latestOffer.availableQuantity} | تجهيز ${latestOffer.preparationMinutes} دقيقة" + if (latestOffer.alternativeMedicineName.isNotBlank()) " | بديل: ${latestOffer.alternativeMedicineName}" else "",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (timeline.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("آخر حدث: ${timeline.last().title} — ${timeline.last().note}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }

                    if (currentUser?.role == "merchant" && activeOrder.merchantId == currentUser?.id && activeOrder.status in listOf("funds_frozen", "preparing")) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (activeOrder.status == "funds_frozen") {
                                Button(onClick = { viewModel.markOrderPreparing(activeOrder.id) }, modifier = Modifier.weight(1f).height(34.dp), contentPadding = PaddingValues(0.dp)) {
                                    Text("بدء التجهيز", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Button(onClick = { viewModel.markOrderReady(activeOrder.id) }, modifier = Modifier.weight(1f).height(34.dp), contentPadding = PaddingValues(0.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)) {
                                Text("جاهز للتسليم", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Chat logs content
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(chatMessages) { msg ->
                    val isMe = msg.senderRole == currentUser?.role
                    ChatBubble(msg = msg, isMe = isMe)
                }
            }

            // Chat Input Bar (Supporting Text & Audio Opus Recorder Simulation)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    if (isRecordingSimulated) {
                        // Recording audio details
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.error)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("جاري تسجيل نوتة صوتية مضغوطة Opus...", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                            }

                            // Weight size estimation
                            val weightEstimation = recordingDurationSec * 0.6
                            Text(
                                text = "${recordingDurationSec}ثانية  (${String.format("%.1f", weightEstimation)} ك.ب)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = { viewModel.stopAndSendVoiceNote() },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("إيقاف وبث النوتة الطبية الرقمية 📨", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        // Standard Input Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Micro recording voice button
                            IconButton(onClick = { viewModel.startRecordingVoice() }) {
                                Icon(Icons.Default.Star, contentDescription = "تسجيل صوتي وبث", tint = MaterialTheme.colorScheme.secondary)
                            }

                            OutlinedTextField(
                                value = chatTextInput,
                                onValueChange = { chatTextInput = it },
                                placeholder = { Text("اكتب رسالة أو استعلم عن السعر...", fontSize = 12.sp) },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("chat_text_field"),
                                shape = RoundedCornerShape(16.dp)
                            )

                            Spacer(modifier = Modifier.width(4.dp))

                            IconButton(
                                onClick = {
                                    if (chatTextInput.isNotBlank()) {
                                        viewModel.submitTextMessage(chatTextInput)
                                        chatTextInput = ""
                                    }
                                },
                                modifier = Modifier.testTag("send_chat_button")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "إرسال", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChatBubble(msg: ChatMessageEntity, isMe: Boolean) {
    val bubbleColor = if (msg.senderRole == "system") {
        MaterialTheme.colorScheme.surfaceVariant
    } else if (isMe) {
        MaterialTheme.colorScheme.primary.copy(0.18f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    val bubbleBorder = if (msg.senderRole == "system") {
        BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(0.4f))
    } else if (isMe) {
        BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
    } else {
        BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
    }

    val alignment = if (msg.senderRole == "system") Alignment.CenterHorizontally else if (isMe) Alignment.End else Alignment.Start

    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = alignment) {
        Text(
            text = msg.senderName,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        )
        Card(
            shape = RoundedCornerShape(12.dp),
            border = bubbleBorder,
            colors = CardDefaults.cardColors(containerColor = bubbleColor),
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                if (msg.isVoiceNote) {
                    // Audio Playback simulation widget
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "تشغيل النوتة", tint = MaterialTheme.colorScheme.secondary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            // Mock play bar
                            Box(
                                modifier = Modifier
                                    .width(130.dp)
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "رسالة صوتية • ${msg.voiceDurationSec} ثانية (${msg.voiceFileSizeBytes / 1000.0} ك.ب)",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                } else {
                    Text(text = msg.messageText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                }

                // Time
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = java.text.SimpleDateFormat("HH:mm", java.util.Locale.US).format(java.util.Date(msg.timestamp)),
                    fontSize = 8.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}


// ------------------------------------------
// CLIENT SCREEN 5: WALLET & CREAMI/CASH BILLING (المحفظة الإلكترونية لزبون المجمع)
// ------------------------------------------
@Composable
fun WalletScreenAr(viewModel: PlatformViewModel) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val userTransactions by viewModel.userTransactions.collectAsStateWithLifecycle()
    var rechargeAmountInput by remember { mutableStateOf("5000") }
    var selectedBankProvider by remember { mutableStateOf("الكريمي للتمويل") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
            .testTag("wallet_screen_container")
    ) {
        Text(
            text = "💳 محفظة المَجْمَع والربط البنكي المحلي للمستخدم",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Balance Display
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("المعرف الرقمي الفريد لحسابك البنكي:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("ALMAJMA-ID-67504", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)

                Spacer(modifier = Modifier.height(10.dp))

                Text("الرصيد الكلي المتوفر للعمليات:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    text = "${currentUser?.let { Money.formatMinor(it.walletBalanceMinor) } ?: "12,000.00"} ريال يمني",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // YEMEN MULTI-CURRENCY CONVERTER WIDGET
        var currencyAmountInput by remember { mutableStateOf("100") }
        var selectedInputCurrency by remember { mutableStateOf("USD") } // USD, SAR

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(0.2f))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📈 مقاصة وحاسبة عملات الصرف المتوازي",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.secondary.copy(0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                            .clip(RoundedCornerShape(4.dp))
                    ) {
                        Text("الأسعار الميدانية الحرة", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = currencyAmountInput,
                        onValueChange = { currencyAmountInput = it },
                        label = { Text("المبلغ المحول", fontSize = 10.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1.5f).height(50.dp)
                    )

                    // Select currency
                    Row(
                        modifier = Modifier.weight(1.2f),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Button(
                            onClick = { selectedInputCurrency = "USD" },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedInputCurrency == "USD") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier.weight(1f).height(38.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("USD $", fontSize = 10.sp, color = if (selectedInputCurrency == "USD") Color.Black else MaterialTheme.colorScheme.onSurface)
                        }

                        Button(
                            onClick = { selectedInputCurrency = "SAR" },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedInputCurrency == "SAR") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier.weight(1f).height(38.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("ر.س (SAR)", fontSize = 10.sp, color = if (selectedInputCurrency == "SAR") Color.Black else MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                val amount = currencyAmountInput.toDoubleOrNull() ?: 1.0
                val rateSanaa = if (selectedInputCurrency == "USD") 530.0 else 140.0
                val rateAden = if (selectedInputCurrency == "USD") 1680.0 else 445.0

                val valSanaa = amount * rateSanaa
                val valAden = amount * rateAden

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Sanaa Local Rate Card
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(0.4f)),
                        border = BorderStroke(0.5.dp, Color.Gray.copy(0.3f))
                    ) {
                        Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("صنعاء وشمال اليمن 🔴", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("${String.format("%,.0f", valSanaa)} YR", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                            Text("بمعدل صرف $rateSanaa", fontSize = 8.sp, color = Color.Gray)
                        }
                    }

                    // Aden Local Rate Card
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(0.4f)),
                        border = BorderStroke(0.5.dp, Color.Gray.copy(0.3f))
                    ) {
                        Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("عدن وجنوب اليمن 🔵", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("${String.format("%,.0f", valAden)} YR", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.secondary)
                            Text("بمعدل صرف $rateAden", fontSize = 8.sp, color = Color.Gray)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "* الأسعار تقاربية تتناغم مع مجمع الصرف المحلي.",
                        fontSize = 8.sp,
                        color = Color.Gray
                    )

                    TextButton(
                        modifier = Modifier.height(26.dp),
                        contentPadding = PaddingValues(0.dp),
                        onClick = {
                            // Automatically set as deposit recharge simulation value
                            rechargeAmountInput = valSanaa.toInt().toString()
                        }
                    ) {
                        Text("استخدم كقيمة للإيداع ↙️", fontSize = 9.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // RECHARGE SIMULATION FORM
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("شحن المحفظة الفورية عبر قنوات التحويل اليمنية:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = rechargeAmountInput,
                    onValueChange = { rechargeAmountInput = it },
                    label = { Text("مبلغ الشحن") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    suffix = { Text("ريال يمني") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Grid selection provider
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    BankSelectionChip(
                        name = "نظام الكريمي",
                        active = selectedBankProvider == "الكريمي للتمويل",
                        modifier = Modifier.weight(1f),
                        onClick = { selectedBankProvider = "الكريمي للتمويل" }
                    )
                    BankSelectionChip(
                        name = "سويري كاش",
                        active = selectedBankProvider == "سويري كاش",
                        modifier = Modifier.weight(1f),
                        onClick = { selectedBankProvider = "سويري كاش" }
                    )
                    BankSelectionChip(
                        name = "تحويل كاش يدوي",
                        active = selectedBankProvider == "تحويل كاش يدوي",
                        modifier = Modifier.weight(1f),
                        onClick = { selectedBankProvider = "تحويل كاش يدوي" }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        val amt = rechargeAmountInput.toDoubleOrNull() ?: 5000.0
                        viewModel.processRecharge(amt, selectedBankProvider)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .testTag("recharge_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Text("إجراء الإيداع بالمحفظة فوراً ✓", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Historical Log
        Text(
            text = "📜 سجل العمليات وتوزيع الضمان المالي بالمحفظة (Escrow Logs):",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (userTransactions.isEmpty()) {
            Text("لم يجرى أي تبادل حتى الآن. قم بشحن محفظتك أو شراء سلعة للبدء.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            userTransactions.forEach { tx ->
                TransactionRow(tx = tx)
                Spacer(modifier = Modifier.height(6.dp))
            }
        }
    }
}

@Composable
fun BankSelectionChip(name: String, active: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (active) MaterialTheme.colorScheme.primary.copy(0.2f) else MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, if (active) MaterialTheme.colorScheme.primary else Color.Transparent),
        modifier = modifier.height(35.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(name, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun TransactionRow(tx: TransactionEntity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(tx.providerName, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text(
                    text = when (tx.type) {
                        "credit" -> "📥 إيداع شحن"
                        "debit" -> "📤 سحب مستقطع"
                        "hold" -> "🔒 تجميد الضمان (HOLD)"
                        "release" -> "🔓 فك التجميد وتمرير (RELEASE)"
                        else -> tx.type
                    },
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = "${if (tx.type == "credit" || tx.type == "release") "+" else "-"}${Money.formatMinor(tx.amountMinor)} ريال",
                fontWeight = FontWeight.ExtraBold,
                color = if (tx.type == "credit" || tx.type == "release") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                fontSize = 13.sp
            )
        }
    }
}


// ==========================================
// SECOND APPLICATION HIERARCHY: MERCHANT / PHARMACIST PERSPECTIVE (التاجر والصيدلي)
// ==========================================
@Composable
fun MerchantAppRoot(viewModel: PlatformViewModel, currentScreen: String) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    Column(modifier = Modifier.fillMaxSize()) {
        AlMajmaTopBar(viewModel = viewModel, titleRole = merchantSectionTitle(currentUser))
        AnimatedContent(
            targetState = currentScreen,
            label = "MerchantScreenTransition",
            modifier = Modifier.weight(1f)
        ) { screen ->
            when (screen) {
                "dashboard" -> MerchantDashboardScreen(viewModel = viewModel)
                "incoming" -> MerchantIncomingRequestsScreen(viewModel = viewModel)
                "products" -> AddProductCatalogScreen(viewModel = viewModel)
                "orders" -> MerchantEscrowTrackerScreen(viewModel = viewModel)
                "chat" -> ChatRoomScreen(viewModel = viewModel)
                "profile" -> ProfileScreen(viewModel = viewModel)
                else -> MerchantDashboardScreen(viewModel = viewModel)
            }
        }

        // Merchant specific tabs bottom
        MerchantBottomNav(
            currentScreen = currentScreen,
            businessType = currentUser?.businessType ?: "marketplace",
            onNav = { viewModel.navigateMerchantTo(it) }
        )
    }
}

@Composable
fun MerchantBottomNav(currentScreen: String, businessType: String, onNav: (String) -> Unit) {
    NavigationBar(
        modifier = Modifier.height(65.dp),
        tonalElevation = 8.dp,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        NavigationBarItem(
            selected = currentScreen == "dashboard",
            onClick = { onNav("dashboard") },
            icon = { Icon(Icons.Default.Home, contentDescription = "لوحة التحكم") },
            label = { Text("لوحة التحكم", fontSize = 10.sp) }
        )
        NavigationBarItem(
            selected = currentScreen == "incoming",
            onClick = { onNav("incoming") },
            icon = { Icon(Icons.Default.Star, contentDescription = "طلبات الدواء") },
            label = { Text(if (businessType == "pharmacy") "طلبات الدواء" else "طلبات السوق", fontSize = 10.sp) }
        )
        NavigationBarItem(
            selected = currentScreen == "products",
            onClick = { onNav("products") },
            icon = { Icon(Icons.Default.Add, contentDescription = "إضافة منتج") },
            label = { Text("إضافة", fontSize = 10.sp) }
        )
        NavigationBarItem(
            selected = currentScreen == "orders" || currentScreen == "chat",
            onClick = { onNav("orders") },
            icon = { Icon(Icons.Default.Check, contentDescription = "صفقات وأمان") },
            label = { Text("الضمان", fontSize = 10.sp) }
        )
    }
}

// ------------------------------------------
// MERCHANT SCREEN 1: MERCHANT DASHBOARD & STATUS (لوحة التاجر والصيدلي)
// ------------------------------------------
@Composable
fun MerchantDashboardScreen(viewModel: PlatformViewModel) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val products by viewModel.products.collectAsStateWithLifecycle()
    var storeIsOpen by remember { mutableStateOf(true) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("merchant_home_scroller")
    ) {
        // Welcoming & Stats Grid
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(currentUser?.businessName?.ifBlank { currentUser?.displayName ?: "نشاط غير مكتمل" } ?: "نشاط غير مكتمل", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("النوع: ${getBusinessTypeArName(currentUser?.businessType ?: "none")} | المدينة: ${currentUser?.city?.ifBlank { "غير محددة" } ?: "غير محددة"}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        // Status switch OPEN/CLOSED
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(if (storeIsOpen) "نشط ومتبخر" else "مغلق", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(4.dp))
                            Switch(checked = storeIsOpen, onCheckedChange = { storeIsOpen = it })
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("رصيد المبيعات الجاهز", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${currentUser?.let { Money.formatMinor(it.walletBalanceMinor) } ?: "150,000.00"} ريال", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                        }
                        Column {
                            Text("أرباح معلقة قيد الضمان", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("18,500 ريال", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(20.dp)) }

        // Action shortcuts buttons
        item {
            Button(
                onClick = { viewModel.navigateMerchantTo("products") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Add, contentDescription = "منتج")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("إدراج منتج أو دواء جديد", fontWeight = FontWeight.Bold)
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }

        // MOCK STATISTICAL CHART WITH POWER EFFICIENCY
        item {
            Text("📈 الرسم البياني لحجم مبيعات ومفاوضات اليوم بالمجمع:", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .drawBehind {
                                // Draw mock grid bars keeping energy saving theme
                                drawRoundRect(
                                    color = Color(0xFF10B981).copy(0.3f),
                                    topLeft = Offset(40f, 20f),
                                    size = androidx.compose.ui.geometry.Size(60f, size.height - 20f)
                                )
                                drawRoundRect(
                                    color = Color(0xFFFBBF24).copy(0.3f),
                                    topLeft = Offset(140f, 50f),
                                    size = androidx.compose.ui.geometry.Size(60f, size.height - 50f)
                                )
                                drawRoundRect(
                                    color = Color(0xFF10B981).copy(0.3f),
                                    topLeft = Offset(240f, 10f),
                                    size = androidx.compose.ui.geometry.Size(60f, size.height - 10f)
                                )
                            }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Text("مبيعات مباشرة", fontSize = 10.sp)
                        Text("عبر الموتور", fontSize = 10.sp)
                        Text("بث طبي عكسي", fontSize = 10.sp)
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }

        // Core Pharmacological Dashboard Section Header
        item {
            Text(
                text = if (currentUser?.businessType == "pharmacy") "📋 إدارة سلامة الأدوية وجرد الـ FIFO والـ COGS" else "📋 إدارة منتجات السوق والمخزون والطلبات",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = if (currentUser?.businessType == "pharmacy") "راقب فترات انتظام الصلاحية وتكلفة البضاعة بدقة وسجّل سحب الدفعات النشط فورياً." else "راقب منتجات الملابس والجملة وطلبات الشراء بعيداً عن تدفق الصيدليات والأدوية.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item { Spacer(modifier = Modifier.height(12.dp)) }

        // Dynamic FIFO & COGS Valuation Simulator Widget
        item {
            var inputFifoQty by remember { mutableStateOf("50") }
            var showFifoResult by remember { mutableStateOf(false) }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Refresh, contentDescription = "فولدر FIFO", tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("حاسبة التسعير والربح بنظرية الوارد أولاً يصرف أولاً (FIFO COGS)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    Text("سجل الدفعات الحالي للمورد الافتراضي بالمستودعات:\nالدفعة أ (الأقدم): 30 وحدة وتكلفة الشراء 700 ريال لكل علبة\nالدفعة ب (الأحدث): 100 وحدة وتكلفة الشراء 900 ريال لكل علبة\nسعر البيع الموحد بالمجمع: 1,500 ريال لكل علبة.", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = inputFifoQty,
                            onValueChange = { inputFifoQty = it },
                            label = { Text("كمية المبيعات المراد جردها", fontSize = 10.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f).height(50.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { showFifoResult = true },
                            modifier = Modifier.height(44.dp)
                        ) {
                            Text("محاكاة الربح الكلي", fontSize = 10.sp, color = Color.Black)
                        }
                    }

                    if (showFifoResult) {
                        val qty = inputFifoQty.toIntOrNull() ?: 50
                        Spacer(modifier = Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.primary.copy(0.1f))
                                .padding(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                        ) {
                            Column {
                                Text("📊 تفصيل حساب تكلفة السلع المبيعة (COGS):", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                val sellingPrice = 1500.0
                                val totalRevenue = qty * sellingPrice
                                var totalCogs = 0.0
                                var breakdownString = ""

                                if (qty <= 30) {
                                    totalCogs = qty * 700.0
                                    breakdownString = "تم سحب كامل الكمية ($qty وحدة) من الدفعة أ بسعر تكلفة 700 ريال."
                                } else {
                                    val partA = 30 * 700.0
                                    val partBQty = qty - 30
                                    val partB = partBQty * 900.0
                                    totalCogs = partA + partB
                                    breakdownString = "تم سحب 30 وحدة من الدفعة أ (تكلفة 700 ريال) وسحب الباقي ($partBQty وحدة) من الدفعة ب (تكلفة 900 ريال)."
                                }

                                val netProfit = totalRevenue - totalCogs
                                val profitPercent = (netProfit / totalRevenue) * 100.0

                                Text("• $breakdownString", fontSize = 9.sp)
                                Text("• إجمالي الإيراد: ${totalRevenue.toInt()} ريال يمني", fontSize = 9.sp)
                                Text("• إجمالي الـ COGS: ${totalCogs.toInt()} ريال يمني", fontSize = 9.sp)
                                Text("• صافي ربح الصفقة: ${netProfit.toInt()} ريال يمني (مجمل الربح: ${profitPercent.toInt()}%)", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }

        // List Medicines & Recalls Dashboard of Pharmacies
        val myProducts = products

        if (myProducts.isNotEmpty()) {
            item {
                Text(
                    text = "📋 جرد دفعات الرف الحالي وقائمة الأصناف:",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            items(myProducts) { prd ->
                val isExpired = prd.category == "medicine" && prd.expiryTimestamp < System.currentTimeMillis()
                val isRecalled = prd.isRecalled

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isExpired) MaterialTheme.colorScheme.errorContainer.copy(0.12f)
                        else if (isRecalled) Color(0xFF1E1E2C)
                        else MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (isExpired) MaterialTheme.colorScheme.error.copy(0.5f)
                        else if (isRecalled) Color(0xFFE11D48).copy(0.5f)
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(prd.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                if (prd.category == "medicine") {
                                    Text("الدفعة الباتش: ${prd.batchNumber}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                } else {
                                    val catName = when(prd.category) { "clothing" -> "أزياء وملابس" "wholesale" -> "جملة" else -> prd.category }
                                    Text("القسم: $catName", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("سعر البيع: ${prd.price} ريال", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                if (prd.category == "medicine") {
                                    Text("سعر الشراء: ${prd.purchaseCost} ريال", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Info, contentDescription = "تفاصيل المخزون", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.secondary)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("المخزون المتبقي: ${prd.totalStock} علبة / وحدة", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            if (prd.category == "medicine") {
                                if (isExpired) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color(0xFFEA580C).copy(0.2f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text("🚨 منتهي الصلاحية وصالح للإتلاف فوراً", fontSize = 9.sp, color = Color(0xFFF97316), fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(MaterialTheme.colorScheme.primary.copy(0.2f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text("صالح وآمن ✅", fontSize = 9.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Divider(color = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isRecalled) "🔴 هذا الصنف تم سحبه تماماً من أسواق المجمع" else "🟢 حالياً معروض ومتاح بحسابات الزبائن",
                                fontSize = 10.sp,
                                color = if (isRecalled) Color(0xFFFDA4AF) else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = if (isRecalled) FontWeight.Bold else FontWeight.Normal
                            )

                            Button(
                                onClick = { viewModel.toggleProductRecall(prd.id) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isRecalled) MaterialTheme.colorScheme.secondary else Color(0xFFE11D48)
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text(
                                    text = if (isRecalled) "أعد للتداول التجاري ✓" else "إيقاف وحظر الصنف 🚫",
                                    fontSize = 9.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}


// ------------------------------------------
// MERCHANT SCREEN 2: PHARMACIST REVERSE OFFERS (استقبال عروض البث الدوائي من المرضى)
// ------------------------------------------
@Composable
fun MerchantIncomingRequestsScreen(viewModel: PlatformViewModel) {
    val orders by viewModel.orders.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val isPharmacy = currentUser?.businessType == "pharmacy"
    val marketplaceIncoming = orders.filter { it.merchantId == currentUser?.id && it.category != "medicine" && it.status in listOf("pending", "funds_frozen", "preparing", "ready", "delivering") }
    val dismissedMedicationRequestIds = remember { mutableStateListOf<Int>() }
    val openMedicationRequests = orders.filter {
        it.category == "medicine" &&
            it.status in listOf("waiting_offers", "pending") &&
            it.merchantId == null &&
            it.id !in dismissedMedicationRequestIds
    }

    var selectedQuoteOrder by remember { mutableStateOf<OrderEntity?>(null) }
    var inputPriceProposal by remember { mutableStateOf("3200") }
    var inputAvailableQty by remember { mutableStateOf("1") }
    var inputPrepMinutes by remember { mutableStateOf("30") }
    var inputAlternative by remember { mutableStateOf("") }
    var inputExpiryDate by remember { mutableStateOf("2027-12") }
    var inputOfferNote by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("incoming_requests_merchant")
    ) {
        Text(
            text = if (isPharmacy) "📡 رادار طلبات وبث الأدوية والوصفات الطبية" else "📦 طلبات السوق والملابس الخاصة بمتجرك",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = if (isPharmacy) "اطلع على الروشتات الطبية وقدم عرض سعر منافس فوري للمريض" else "هنا تظهر طلبات الشراء والمساومة الخاصة بمنتجات الملابس والجملة فقط؛ لا تختلط مع الصيدليات.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (!isPharmacy) {
            if (marketplaceIncoming.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Text("لا توجد طلبات سوق نشطة على منتجاتك حالياً.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(marketplaceIncoming) { req ->
                        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("طلب سوق #${req.id}", fontWeight = FontWeight.Bold)
                                Text(req.productName, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("الحالة: ${orderStatusAr(req.status)} | المبلغ: ${Money.formatMinor(req.totalPriceMinor)} ريال", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                Text("الكمية: ${req.quantity} | التسليم: ${if (req.deliveryMethod == "pickup") "استلام" else "توصيل"}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                if (req.deliveryAddress.isNotBlank()) Text("العنوان: ${req.deliveryAddress}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                if (req.customerNote.isNotBlank()) Text("ملاحظة: ${req.customerNote}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    when (req.status) {
                                        "pending", "funds_frozen" -> Button(onClick = { viewModel.markOrderPreparing(req.id) }, modifier = Modifier.weight(1f).height(32.dp), contentPadding = PaddingValues(0.dp)) { Text("قبول وتجهيز", color = Color.Black, fontSize = 10.sp) }
                                        "preparing" -> Button(onClick = { viewModel.markOrderReady(req.id) }, modifier = Modifier.weight(1f).height(32.dp), contentPadding = PaddingValues(0.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)) { Text("جاهز", color = Color.Black, fontSize = 10.sp) }
                                        else -> OutlinedButton(onClick = { viewModel.selectOrderForChat(req.id) }, modifier = Modifier.weight(1f).height(32.dp), contentPadding = PaddingValues(0.dp)) { Text("متابعة", fontSize = 10.sp) }
                                    }
                                    OutlinedButton(onClick = { viewModel.selectOrderForChat(req.id) }, modifier = Modifier.weight(1f).height(32.dp), contentPadding = PaddingValues(0.dp)) { Text("محادثة", fontSize = 10.sp) }
                                }
                                if (req.status in listOf("pending", "funds_frozen")) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    TextButton(onClick = { viewModel.rejectMarketplaceOrder(req.id) }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                                        Text("رفض الطلب واسترجاع الضمان للعميل", fontSize = 10.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            return@Column
        }

        if (openMedicationRequests.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "لا توجد طلبات بث دواء نشطة حالياً بقربك. رادارات البحث تفحص المحيط باستمرار...",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(openMedicationRequests) { req ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("طلب استعلام مريض #${req.id}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(MaterialTheme.colorScheme.secondary.copy(0.2f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("بث رادار", fontSize = 10.sp, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(req.productName, style = MaterialTheme.typography.bodySmall)

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = { selectedQuoteOrder = req },
                                    modifier = Modifier.height(32.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp)
                                ) {
                                    Text("عرض مسعَّر مالي 💰", fontSize = 11.sp, color = Color.Black)
                                }

                                TextButton(
                                    onClick = {
                                        dismissedMedicationRequestIds.add(req.id)
                                        viewModel.markMedicationUnavailable(req.id)
                                    },
                                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Text("غير متوفر لدينا", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // MERCHANT RESPONSE QUOTATION MODAL
    selectedQuoteOrder?.let { order ->
        AlertDialog(
            onDismissRequest = { selectedQuoteOrder = null },
            title = { Text("تقديم تسعيرة الدواء العكسي", textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth()) },
            text = {
                Column(horizontalAlignment = Alignment.End, modifier = Modifier.fillMaxWidth()) {
                    Text("اسم طلب بث المريض: ", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(order.productName, fontWeight = FontWeight.Bold, fontSize = 13.sp)

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = inputPriceProposal,
                        onValueChange = { inputPriceProposal = it },
                        label = { Text("السعر المقترح والمدعوم للدواء") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        suffix = { Text("ريال يمني") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = inputAvailableQty,
                            onValueChange = { inputAvailableQty = it },
                            label = { Text("الكمية") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = inputPrepMinutes,
                            onValueChange = { inputPrepMinutes = it },
                            label = { Text("دقائق التجهيز") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = inputAlternative,
                        onValueChange = { inputAlternative = it },
                        label = { Text("بديل دوائي اختياري / نفس المادة الفعالة") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = inputExpiryDate,
                        onValueChange = { inputExpiryDate = it },
                        label = { Text("تاريخ الصلاحية / الباتش") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = inputOfferNote,
                        onValueChange = { inputOfferNote = it },
                        label = { Text("ملاحظة للصنف أو الوصفة") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val pr = inputPriceProposal.toDoubleOrNull() ?: 3000.0
                        viewModel.submitPrescriptionOfferFull(
                            orderId = order.id,
                            priceProposal = pr,
                            availableQuantity = inputAvailableQty.toIntOrNull() ?: 1,
                            preparationMinutes = inputPrepMinutes.toIntOrNull() ?: 30,
                            note = inputOfferNote,
                            alternativeMedicineName = inputAlternative,
                            expiryDateText = inputExpiryDate
                        )
                        dismissedMedicationRequestIds.add(order.id)
                        selectedQuoteOrder = null
                    }
                ) {
                    Text("إرسال التسعيرة للمريض ✓", color = Color.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedQuoteOrder = null }) {
                    Text("إلغاء", color = MaterialTheme.colorScheme.error)
                }
            }
        )
    }
}


// ------------------------------------------
// MERCHANT SCREEN 3: ADD PRODUCTS & CATALOGUE (إثراء فيترينة البيع بالمظهر الخفيف)
// ------------------------------------------
@Composable
fun AddProductCatalogScreen(viewModel: PlatformViewModel) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val isPharmacy = currentUser?.businessType == "pharmacy"
    var productName by remember { mutableStateOf("") }
    var productPrice by remember { mutableStateOf("") }
    var productCategory by remember(currentUser?.businessType) { mutableStateOf(if (isPharmacy) "medicine" else "clothing") } // clothing, wholesale, medicine
    var productLocation by remember(currentUser?.address, currentUser?.businessName) { mutableStateOf(currentUser?.address?.ifBlank { currentUser?.businessName ?: "موقع المتجر" } ?: "موقع المتجر") }

    // Pharmacological-specific fields
    var batchNumber by remember { mutableStateOf("B-SANA-${(10..99).random()}") }
    var initialStock by remember { mutableStateOf("100") }
    var purchaseCost by remember { mutableStateOf("") }
    var expiryDaysOffset by remember { mutableStateOf("180") }
    var activeIngredient by remember { mutableStateOf("") }
    var medicineForm by remember { mutableStateOf("tablet") }
    var strengthText by remember { mutableStateOf("") }
    var therapeuticCategory by remember { mutableStateOf("painkiller") }
    var requiresPrescription by remember { mutableStateOf(false) }
    var manufacturerCountry by remember { mutableStateOf("") }
    var medicineBarcode by remember { mutableStateOf("") }
    var dosageHint by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
            .testTag("add_product_screen")
    ) {
        Text(
            text = if (isPharmacy) "✏️ إدراج دواء جديد ببيانات دفعة وصلاحية" else "✏️ إدراج منتج سوق / ملابس / جملة — ممنوع إضافة أدوية هنا",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Name
        OutlinedTextField(
            value = productName,
            onValueChange = { productName = it },
            label = { Text("اسم المنتج أو المستحضر الطبي") },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Price
        OutlinedTextField(
            value = productPrice,
            onValueChange = { productPrice = it },
            label = { Text("سعر البيع") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            suffix = { Text("ريال يمني") },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Category Selection - separated by business type
        Text("القسم التجاري الخاص بالمنتج:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (isPharmacy) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(0.10f)), modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                Text("هذا الحساب مصنف كصيدلية؛ يسمح بإضافة الأدوية فقط مع بيانات الباتش والصلاحية.", modifier = Modifier.padding(10.dp), fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        } else {
            Column(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f).clickable { productCategory = "clothing" }) {
                        RadioButton(selected = productCategory == "clothing", onClick = { productCategory = "clothing" })
                        Text("ملابس", fontSize = 11.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f).clickable { productCategory = "wholesale" }) {
                        RadioButton(selected = productCategory == "wholesale", onClick = { productCategory = "wholesale" })
                        Text("جملة وغذاء", fontSize = 11.sp)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { productCategory = "general_market" }) {
                    RadioButton(selected = productCategory == "general_market", onClick = { productCategory = "general_market" })
                    Text("منتجات عامة / سوق", fontSize = 11.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // If category is medicine, reveal strict batch tracking dashboard
        if (productCategory == "medicine") {
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(0.4f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(0.3f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("⚙️ بيانات الدفعة والمواصفات الدوائية (FIFO Batch Spec)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = activeIngredient,
                        onValueChange = { activeIngredient = it },
                        label = { Text("المادة الفعالة", fontSize = 10.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = strengthText,
                            onValueChange = { strengthText = it },
                            label = { Text("القوة: 500mg / 1g", fontSize = 10.sp) },
                            singleLine = true,
                            modifier = Modifier.weight(1f).height(50.dp).padding(end = 4.dp)
                        )
                        OutlinedTextField(
                            value = manufacturerCountry,
                            onValueChange = { manufacturerCountry = it },
                            label = { Text("بلد/شركة التصنيع", fontSize = 10.sp) },
                            singleLine = true,
                            modifier = Modifier.weight(1f).height(50.dp).padding(start = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text("الشكل الدوائي", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                        listOf("tablet" to "حبوب", "syrup" to "شراب", "injection" to "حقن", "drops" to "قطرات", "solution" to "محلول", "cream" to "كريم").forEach { (value, label) ->
                            item { FilterTabChip(label, medicineForm == value, onClick = { medicineForm = value }) }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text("التصنيف العلاجي", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                        listOf("painkiller" to "مسكن", "antibiotic" to "مضاد", "diabetes" to "سكري", "pressure" to "ضغط", "vitamins" to "فيتامين", "dehydration" to "جفاف").forEach { (value, label) ->
                            item { FilterTabChip(label, therapeuticCategory == value, onClick = { therapeuticCategory = value }) }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { requiresPrescription = !requiresPrescription }) {
                        Checkbox(checked = requiresPrescription, onCheckedChange = { requiresPrescription = it })
                        Text("هذا الدواء لا يصرف إلا بوصفة", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = medicineBarcode,
                        onValueChange = { medicineBarcode = it },
                        label = { Text("الباركود الدوائي / GTIN إن وجد", fontSize = 10.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = dosageHint,
                        onValueChange = { dosageHint = it },
                        label = { Text("ملاحظة جرعة مختصرة / تنبيه", fontSize = 10.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = batchNumber,
                        onValueChange = { batchNumber = it },
                        label = { Text("رقم الدفعة / الباتش", fontSize = 10.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = initialStock,
                            onValueChange = { initialStock = it },
                            label = { Text("الكمية بالعلبة", fontSize = 10.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f).height(50.dp).padding(end = 4.dp)
                        )
                        OutlinedTextField(
                            value = purchaseCost.ifEmpty { (productPrice.toDoubleOrNull()?.let { (it * 0.75).toInt().toString() } ?: "").toString() },
                            onValueChange = { purchaseCost = it },
                            label = { Text("سعر التكلفة للعلبة", fontSize = 10.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f).height(50.dp).padding(start = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = expiryDaysOffset,
                        onValueChange = { expiryDaysOffset = it },
                        label = { Text("فترة الصلاحية بالأيام (من اليوم)", fontSize = 10.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Location
        OutlinedTextField(
            value = productLocation,
            onValueChange = { productLocation = it },
            label = { Text("العنوان الجغرافي للاستلام") },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Image optimization indicator
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "🔋 تقنية ضغط الصورة مدمجة تلقائياً للتاجر (حجم الفايل المتوقع: 4.5 كيلوبايت لضمان سرعة التحميل وضعف تغذية النت)",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                val pr = productPrice.toDoubleOrNull() ?: 1000.0
                if (productName.isNotBlank()) {
                    if (productCategory == "medicine") {
                        val stock = initialStock.toIntOrNull() ?: 100
                        val rawCost = purchaseCost.toDoubleOrNull() ?: (pr * 0.75)
                        val days = expiryDaysOffset.toLongOrNull() ?: 180
                        val expiryTime = System.currentTimeMillis() + (days * 24 * 60 * 60 * 1000L)
                        viewModel.createProductFull(
                            name = productName,
                            price = pr,
                            category = "medicine",
                            location = productLocation,
                            batchNumber = batchNumber,
                            expiryTimestamp = expiryTime,
                            purchaseCost = rawCost,
                            totalStock = stock,
                            activeIngredient = activeIngredient,
                            medicineForm = medicineForm,
                            strengthText = strengthText,
                            therapeuticCategory = therapeuticCategory,
                            requiresPrescription = requiresPrescription,
                            manufacturerCountry = manufacturerCountry,
                            barcode = medicineBarcode,
                            dosageHint = dosageHint
                        )
                    } else {
                        viewModel.createMerchantProduct(productName, pr, productCategory, productLocation)
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("submit_product_button"),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("حفظ ونشر المادة على رادار البائعين المجاورة ✓", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
        }
    }
}


// ------------------------------------------
// MERCHANT SCREEN 4: ESCROW TRANSACTION MANAGER (صفقات أمان الضمان واستلام المبالغ)
// ------------------------------------------
@Composable
fun MerchantEscrowTrackerScreen(viewModel: PlatformViewModel) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val orders by viewModel.orders.collectAsStateWithLifecycle()
    val merchantBargains = orders.filter { it.merchantId == currentUser?.id }

    var otpConfirmInput by remember { mutableStateOf("") }
    var selectedOrderForOtp by remember { mutableStateOf<OrderEntity?>(null) }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("merchant_escrow_tracker")
    ) {
        Text(
            text = "🔒 صفقات الضمان ومبيعات مَجْمَع المعلقة",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "لكي يتم تحرير المبلغ وحساب الإيرادات، يجب الحصول على كود الإفراج وعنوان الأرباح (2% عمولة للمجمع)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (merchantBargains.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text("لا توجد مبيعات جارية حالياً لمتجرك الكترونياً.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(merchantBargains) { deal ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("صفقة تداول رقم #${deal.id}", fontWeight = FontWeight.Bold)
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(
                                            if (deal.status == "completed") MaterialTheme.colorScheme.primary.copy(0.2f)
                                            else MaterialTheme.colorScheme.secondary.copy(0.2f)
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = orderStatusAr(deal.status),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (deal.status == "completed") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(deal.productName, fontSize = 12.sp)
                            Text("السعر: ${deal.totalPrice} ريال | عمولة المنصة المتوقعة: ${deal.totalPrice * 0.02} ريال (2%)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                            Divider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.surfaceVariant)

                            if (deal.status in listOf("funds_frozen", "preparing", "ready", "delivering")) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (deal.status == "funds_frozen") {
                                        OutlinedButton(
                                            onClick = { viewModel.markOrderPreparing(deal.id) },
                                            modifier = Modifier.weight(1f).height(34.dp),
                                            contentPadding = PaddingValues(0.dp)
                                        ) { Text("بدء التجهيز", fontSize = 10.sp) }
                                    }
                                    if (deal.status in listOf("funds_frozen", "preparing")) {
                                        Button(
                                            onClick = { viewModel.markOrderReady(deal.id) },
                                            modifier = Modifier.weight(1f).height(34.dp),
                                            contentPadding = PaddingValues(0.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                                        ) { Text("جاهز", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = { selectedOrderForOtp = deal },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("أدخل كود الإفراج الآمن لتحصيل الأموال 💰", color = Color.White)
                                }
                            } else if (deal.status == "completed") {
                                Text(
                                    text = "تم تحرير الأرباح مخصومة بنجاح ✓",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            } else {
                                Text(
                                    text = "الحالة الحالية: ${orderStatusAr(deal.status)}",
                                    fontSize = 11.sp,
                                    color = statusBadgeTextColor(deal.status),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // CONFIRM OTP RELEASE MODAL FOR ESCROW
    selectedOrderForOtp?.let { deal ->
        AlertDialog(
            onDismissRequest = { selectedOrderForOtp = null },
            title = { Text("تأكيد استلام كود الحزمة والفك مالي", textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth()) },
            text = {
                Column(horizontalAlignment = Alignment.End, modifier = Modifier.fillMaxWidth()) {
                    Text("أدخل كود التوصيل الفعلي الممنوح للمريض:", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = otpConfirmInput,
                        onValueChange = { otpConfirmInput = it },
                        label = { Text("رمز التحرير (4 أرقام)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.confirmDeliveryOtp(deal.id, otpConfirmInput) { success ->
                            if (success) {
                                Toast.makeText(context, "تم التحرير وتحويل الأموال فوراً!", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "الرمز المدخل غير صحيح بالمرة!", Toast.LENGTH_LONG).show()
                            }
                            selectedOrderForOtp = null
                            otpConfirmInput = ""
                        }
                    }
                ) {
                    Text("إطلاق سراح وتحصيل الأرباح", color = Color.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedOrderForOtp = null }) {
                    Text("إلغاء", color = MaterialTheme.colorScheme.error)
                }
            }
        )
    }
}


// ==========================================
// THIRD APPLICATION HIERARCHY: MOTOR / RIDE DRIVER PERSPECTIVE (سائق الموتور وقبول الرادار)
// ==========================================
@Composable
fun DriverAppRoot(viewModel: PlatformViewModel, currentScreen: String) {
    Column(modifier = Modifier.fillMaxSize()) {
        AlMajmaTopBar(viewModel = viewModel, titleRole = "واجهة كاباتن التوصيل 🛵")
        AnimatedContent(
            targetState = currentScreen,
            label = "DriverScreenTransition",
            modifier = Modifier.weight(1f)
        ) { screen ->
            when (screen) {
                "radar" -> DriverRadarScreen(viewModel = viewModel)
                "wallet" -> DriverWalletScreen(viewModel = viewModel)
                "chat" -> ChatRoomScreen(viewModel = viewModel)
                "profile" -> ProfileScreen(viewModel = viewModel)
                else -> DriverRadarScreen(viewModel = viewModel)
            }
        }

        // Driver Bottom navigation
        DriverBottomNav(
            currentScreen = currentScreen,
            onNav = { viewModel.navigateDriverTo(it) }
        )
    }
}

@Composable
fun DriverBottomNav(currentScreen: String, onNav: (String) -> Unit) {
    NavigationBar(
        modifier = Modifier.height(65.dp),
        tonalElevation = 8.dp,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        NavigationBarItem(
            selected = currentScreen == "radar" || currentScreen == "chat",
            onClick = { onNav("radar") },
            icon = { Icon(Icons.Default.LocationOn, contentDescription = "رادار المشاوير") },
            label = { Text("رادار الطلبات", fontSize = 10.sp) }
        )
        NavigationBarItem(
            selected = currentScreen == "wallet",
            onClick = { onNav("wallet") },
            icon = { Icon(Icons.Default.Person, contentDescription = "أرباح السائق") },
            label = { Text("محفظتي والتأمين", fontSize = 10.sp) }
        )
    }
}

// ------------------------------------------
// DRIVER SCREEN 1: RADAR & CLAIM JOB (رادار مشاوير الموتور القريبة وضبط السعر)
// ------------------------------------------
@Composable
fun DriverRadarScreen(viewModel: PlatformViewModel) {
    val orders by viewModel.orders.collectAsStateWithLifecycle()
    val availableRides = orders.filter {
        it.driverId == null &&
            (
                (isDeliveryCategory(it.category) && it.status in listOf("pending", "funds_frozen", "ready")) ||
                (it.category == "medicine" && it.status in listOf("funds_frozen", "ready"))
            )
    }
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()

    var radarIsRunning by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("driver_radar_screen")
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("الكابتن: السريع أبو رعد", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text("حالة الرادار النشط متبخر", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Switch(checked = radarIsRunning, onCheckedChange = { radarIsRunning = it })
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Dynamic Active delivery warning (if has active job showing HUD shortcut)
        val activeDelivery = orders.find { it.driverId == currentUser?.id && it.status in listOf("funds_frozen", "ready", "delivering") }
        if (activeDelivery != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.secondary, RoundedCornerShape(10.dp)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondary.copy(0.1f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1.5f)) {
                        Text("⚠️ لديك مهمة جار توصيلها الآن!", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                        Text(activeDelivery.productName, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }

                    Button(
                        onClick = { viewModel.selectOrderForChat(activeDelivery.id) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("الخارطة والشات", fontSize = 10.sp, color = Color.White)
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        Text(
            text = "📡 رادار الطلبات القريبة الجاهزة التفاعلي:",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))

        if (!radarIsRunning) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Text("قم بتشغيل المفتاح بالأعلى للبحث عن مشاوير طرود قريبة.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else if (availableRides.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Text("رادار الاستكشاف يسحب البيانات... لا توجد طلبات معلقة بالمنطقة حالياً.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(availableRides) { ride ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = when {
                                        isDeliveryCategory(ride.category) -> " خدمة توصيل مستقلة"
                                        ride.category == "medicine" -> " توصيل دواء دقيق"
                                        else -> categoryArName(ride.category)
                                    },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "${ride.deliveryFee} ريال أجر",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(ride.productName, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                            Divider(modifier = Modifier.padding(vertical = 6.dp), color = MaterialTheme.colorScheme.surfaceVariant)

                            // Quick Accept
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("العمولة المقررة للمجمع: ${ride.deliveryFee * 0.10} ريال (10%)", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Button(
                                    onClick = { viewModel.acceptDeliveryOrder(ride.id) },
                                    modifier = Modifier.height(30.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 2.dp)
                                ) {
                                    Text("قبول وتحديث", fontSize = 11.sp, color = Color.Black)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


// ------------------------------------------
// DRIVER SCREEN 2: COMMISSION ESCROW & PROFIT WALLET (محفظة السائق والتأمين المقتطع مسبقاً)
// ------------------------------------------
@Composable
fun DriverWalletScreen(viewModel: PlatformViewModel) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val transactions by viewModel.userTransactions.collectAsStateWithLifecycle()
    var depositRefillInput by remember { mutableStateOf("3000") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
            .testTag("driver_wallet_screen")
    ) {
        Text(
            text = "🏍️ محفظة السائق وحقيبة تغذية التوصيل",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Balance Tracker
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("المبلغ التراكمي لضمان عمولة المجمع مسبقاً:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("يجب بقاء الرصيد فوق 1000 ريال لتلقي المشاوير", fontSize = 10.sp, color = MaterialTheme.colorScheme.secondary)

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "${currentUser?.let { Money.formatMinor(it.walletBalanceMinor) } ?: "6,000.00"} ريال يمني",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Refill precharged deposit form
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("شحن تأمين عمولات السائقين بالمقدم:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = depositRefillInput,
                    onValueChange = { depositRefillInput = it },
                    label = { Text("المبلغ التعبوي المطلوب الشحن به") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    suffix = { Text("ريال") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = {
                        val ref = depositRefillInput.toDoubleOrNull() ?: 2000.0
                        viewModel.processRecharge(ref, "الكريمي شحن كابتن")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Text("تقديم تغذية رصيد التأمين مسبقاً ✓", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // History Log
        Text("📜 حزم الاستقطاعات وعمولات مشاوير السائق السابقة (10%):", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        if (transactions.isEmpty()) {
            Text("لم يخصم أي عمولات من محفظتك حتى الآن. استلم المشاوير بالأول.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            transactions.forEach { tx ->
                TransactionRow(tx = tx)
                Spacer(modifier = Modifier.height(6.dp))
            }
        }
    }
}

// ------------------------------------------
// SUPER ADMIN DASHBOARD PANEL (لوحة تحكم مدير النظام الشاملة)
// ------------------------------------------
@Composable
fun SuperAdminDashboardScreen(viewModel: PlatformViewModel) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val systemConfig by viewModel.systemConfig.collectAsStateWithLifecycle()
    val orders by viewModel.orders.collectAsStateWithLifecycle()
    val products by viewModel.products.collectAsStateWithLifecycle()
    val users by viewModel.users.collectAsStateWithLifecycle()
    val backupLogs by viewModel.backupLogs.collectAsStateWithLifecycle()
    val allReviews by viewModel.allReviews.collectAsStateWithLifecycle()
    val allDisputes by viewModel.allDisputes.collectAsStateWithLifecycle()
    val allPrescriptions by viewModel.allPrescriptions.collectAsStateWithLifecycle()
    val allPharmacyVerifications by viewModel.allPharmacyVerifications.collectAsStateWithLifecycle()

    // Outbox & Double-Entry Reconciliation live channels (Recommendation 1 & 5)
    val pendingOutboxCount by viewModel.pendingOutboxCount.collectAsStateWithLifecycle()
    val failedOutboxCount by viewModel.failedOutboxCount.collectAsStateWithLifecycle()
    val totalOutboxCount by viewModel.totalOutboxCount.collectAsStateWithLifecycle()
    val reconciliationReport by viewModel.reconciliationReport.collectAsStateWithLifecycle()
    val outboxSyncLog by viewModel.outboxSyncLog.collectAsStateWithLifecycle()
    val outboxSyncFailureAlert by viewModel.outboxSyncFailureAlert.collectAsStateWithLifecycle()

    val allOutboxEvents by viewModel.allOutboxEvents.collectAsStateWithLifecycle()
    val allLedgerEntries by viewModel.allLedgerEntries.collectAsStateWithLifecycle()

    val coroutineScope = rememberCoroutineScope()

    // Configuration text inputs
    var primaryColorInput by remember(systemConfig) { mutableStateOf(systemConfig.primaryColor) }
    var secondaryColorInput by remember(systemConfig) { mutableStateOf(systemConfig.secondaryColor) }
    var appTitleInput by remember(systemConfig) { mutableStateOf(systemConfig.appTitle) }
    var rideLabelInput by remember(systemConfig) { mutableStateOf(systemConfig.rideLabel) }
    var pharmacyLabelInput by remember(systemConfig) { mutableStateOf(systemConfig.pharmacyLabel) }
    var clothingLabelInput by remember(systemConfig) { mutableStateOf(systemConfig.clothingLabel) }
    var marketplaceOrderInput by remember(systemConfig) { mutableStateOf(systemConfig.marketplaceOrder) }
    var isPromoVisibleInput by remember(systemConfig) { mutableStateOf(systemConfig.isPromoBannerVisible) }
    var promoTextInput by remember(systemConfig) { mutableStateOf(systemConfig.promoBannerText) }

    // Mock Pharmacies Approval list
    val mockPharmaciesWaitingApproval = remember {
        mutableStateListOf(
            "صيدلية تريم المركزية",
            "صيدلية الشفاء الحديثة (حافة اللقية)",
            "صيدلية الدكتور علوي البار"
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        AlMajmaTopBar(viewModel = viewModel, titleRole = "لوحة مدير النظام 🛡️")
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .testTag("super_admin_pane")
        ) {
        // Welcome Header
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Settings, contentDescription = "Admin", tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "لوحة تحكم مدير النظام الشاملة (Super-Admin Cockpit)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "محطة إدارة العمولات، الأمان والضمان اللامركزي، النسخ الاحتياطية وتخصيص الواجهات والملصقات فورياً بدون أي حاجة لتحديث المتجر.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(0.8f)
                    )
                }
            }
        }



        // Section 00: Executive reports
        item {
            val pharmacyUsers = users.count { it.businessType == "pharmacy" }
            val marketUsers = users.count { it.businessType == "marketplace" }
            val medicineOrders = orders.count { it.category == "medicine" }
            val marketOrders = orders.count { it.category != "medicine" && it.category != "ride" }
            val completedOrders = orders.count { it.status == "completed" }
            val escrowOrders = orders.count { it.status in listOf("funds_frozen", "preparing", "ready", "delivering") }
            val medicineProducts = products.count { it.category == "medicine" }
            val marketProducts = products.count { it.category != "medicine" }
            val openDisputesCount = allDisputes.count { it.status == "open" }

            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(0.25f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("📊 تقارير تشغيلية مختصرة", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text("فصل واضح بين الصيدليات وسوق الملابس/التجار حتى لا تختلط العمليات والتقارير.", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AdminMiniReportCard("صيدليات", pharmacyUsers.toString(), "حسابات دوائية", Modifier.weight(1f))
                        AdminMiniReportCard("تجار سوق", marketUsers.toString(), "ملابس/جملة", Modifier.weight(1f))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AdminMiniReportCard("طلبات دواء", medicineOrders.toString(), "بحث ووصفات", Modifier.weight(1f))
                        AdminMiniReportCard("طلبات سوق", marketOrders.toString(), "شراء ومساومة", Modifier.weight(1f))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AdminMiniReportCard("بالضمان", escrowOrders.toString(), "أموال مجمدة", Modifier.weight(1f))
                        AdminMiniReportCard("نزاعات مفتوحة", openDisputesCount.toString(), "تحتاج قرار", Modifier.weight(1f))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("المنتجات: $medicineProducts دواء، $marketProducts منتج سوق. الطلبات المكتملة: $completedOrders.", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        // Section 0: Real operations control - disputes, prescriptions, pharmacy verification
        item {
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(0.25f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("🧭 مركز التشغيل الحقيقي: النزاعات، الوصفات، اعتماد الصيدليات", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(8.dp))

                    Text("النزاعات المفتوحة", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    if (allDisputes.none { it.status == "open" }) {
                        Text("لا توجد نزاعات مفتوحة حالياً.", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        allDisputes.filter { it.status == "open" }.take(5).forEach { d ->
                            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.error.copy(0.08f))) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text("نزاع #${d.id} على الطلب #${d.orderId}: ${d.reason}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text(d.details, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedButton(onClick = { viewModel.resolveDisputeAsAdmin(d.id, false, "قرار الإدارة: استرجاع المبلغ للعميل لعدم اكتمال الإثبات.") }, modifier = Modifier.weight(1f).height(32.dp), contentPadding = PaddingValues(0.dp)) {
                                            Text("استرجاع للعميل", fontSize = 9.sp)
                                        }
                                        Button(onClick = { viewModel.resolveDisputeAsAdmin(d.id, true, "قرار الإدارة: تحرير المبلغ للتاجر بعد قبول الإثبات.") }, modifier = Modifier.weight(1f).height(32.dp), contentPadding = PaddingValues(0.dp)) {
                                            Text("تحرير للتاجر", color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text("اعتماد الصيدليات", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    allPharmacyVerifications.take(5).forEach { v ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${v.pharmacyName} — ${v.licenseNumber}", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Text("الحالة: ${v.status} | ${v.city}", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (v.status == "pending") {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    TextButton(onClick = { viewModel.rejectPharmacy(v.merchantId) }) { Text("رفض", fontSize = 9.sp, color = MaterialTheme.colorScheme.error) }
                                    Button(onClick = { viewModel.approvePharmacy(v.merchantId) }, modifier = Modifier.height(28.dp), contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) { Text("اعتماد", color = Color.Black, fontSize = 9.sp) }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text("الوصفات الطبية", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    if (allPrescriptions.isEmpty()) {
                        Text("لا توجد وصفات مسجلة.", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        allPrescriptions.take(5).forEach { pr ->
                            Text("طلب #${pr.orderId}: ${pr.status} | مرفق: ${if (pr.hasAttachment) "نعم" else "لا"}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

                // Section 1: Server-Driven UI Configuration
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(0.2f), RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("🎨 تخصيص الواجهات والملصقات (Server-Driven UI)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = appTitleInput,
                        onValueChange = { appTitleInput = it },
                        label = { Text("عنوان المنصة ومجمع الخدمات") },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = primaryColorInput,
                            onValueChange = { primaryColorInput = it },
                            label = { Text("اللون رئيسي (Hex)") },
                            modifier = Modifier.weight(1f).padding(vertical = 4.dp),
                            textStyle = androidx.compose.ui.text.TextStyle(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        )
                        OutlinedTextField(
                            value = secondaryColorInput,
                            onValueChange = { secondaryColorInput = it },
                            label = { Text("اللون ثانوي (Hex)") },
                            modifier = Modifier.weight(1f).padding(vertical = 4.dp),
                            textStyle = androidx.compose.ui.text.TextStyle(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        )
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = rideLabelInput,
                            onValueChange = { rideLabelInput = it },
                            label = { Text("تسمية التوصيل") },
                            modifier = Modifier.weight(1f).padding(vertical = 4.dp)
                        )
                        OutlinedTextField(
                            value = pharmacyLabelInput,
                            onValueChange = { pharmacyLabelInput = it },
                            label = { Text("تسمية الصيدلية") },
                            modifier = Modifier.weight(1f).padding(vertical = 4.dp)
                        )
                    }

                    OutlinedTextField(
                        value = clothingLabelInput,
                        onValueChange = { clothingLabelInput = it },
                        label = { Text("تسمية سوق الملبوسات والسلع") },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    )

                    OutlinedTextField(
                        value = marketplaceOrderInput,
                        onValueChange = { marketplaceOrderInput = it },
                        label = { Text("ترتيب وأولوية المعروض بالرئيسية (مفصول بفاصلة)") },
                        placeholder = { Text("ride,pharmacy,clothing,wholesale,wallet") },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isPromoVisibleInput, onCheckedChange = { isPromoVisibleInput = it })
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("إظهار شريط الإعلانات العاجل الموحد", style = MaterialTheme.typography.bodySmall)
                    }

                    if (isPromoVisibleInput) {
                        OutlinedTextField(
                            value = promoTextInput,
                            onValueChange = { promoTextInput = it },
                            label = { Text("رسالة الإعلان الموحد") },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            viewModel.saveSystemConfigChanges(
                                com.example.data.database.SystemConfigEntity(
                                    primaryColor = primaryColorInput,
                                    secondaryColor = secondaryColorInput,
                                    appTitle = appTitleInput,
                                    rideLabel = rideLabelInput,
                                    pharmacyLabel = pharmacyLabelInput,
                                    clothingLabel = clothingLabelInput,
                                    marketplaceOrder = marketplaceOrderInput,
                                    isPromoBannerVisible = isPromoVisibleInput,
                                    promoBannerText = promoTextInput
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.fillMaxWidth().height(42.dp)
                    ) {
                        Text("حفظ التغييرات ونشرها فورياً ✓", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Section 2: Cryptographic local & cloud backups (AES equivalent)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .border(1.dp, MaterialTheme.colorScheme.secondary.copy(0.2f), RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("🛡️ نظام النسخ الاحتياطية المشفرة والمجدولة (Dynamic Encrypted Backups)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("حفظ وتأمين المعاملات والتحويلات أوفلاين لمنع ضياع أي سنت عند ترقية قاعدة البيانات.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(
                            onClick = {
                                viewModel.executeLocalBackup(context.filesDir) { _ -> }
                            },
                            modifier = Modifier.weight(1.5f).height(36.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                        ) {
                            Text("نسخة مشفرة محلية", fontSize = 10.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                        
                        Button(
                            onClick = {
                                viewModel.executeCloudSimulationBackup { _ -> }
                            },
                            modifier = Modifier.weight(1.5f).height(36.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                        ) {
                            Text("تزامن سحابي فوري", fontSize = 10.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                viewModel.executeSystemRestore(context.filesDir, null) { _ -> }
                            },
                            modifier = Modifier.weight(1.5f).height(36.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                        ) {
                            Text("فك تشفير واستعادة", fontSize = 10.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(0.4f))
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "سجل الإجراءات الأمنية والمطابقة:\n$backupLogs",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Section 3: Escrow Pending / Lockbox Wallets
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("🔒 الصناديق المعلقة ومحافظ العقد (Pending Escrows)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                    Spacer(modifier = Modifier.height(6.dp))

                    val frozenOrders = orders.filter { it.status == "funds_frozen" }
                    if (frozenOrders.isEmpty()) {
                        Text("لا يوجد أي أموال مجمدة معلقة في المجمع حالياً. كافة الصناديق جرى تسييلها.", fontSize = 11.sp, color = Color.LightGray)
                    } else {
                        frozenOrders.forEach { ord ->
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(0.4f))
                            ) {
                                Row(modifier = Modifier.padding(8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Column {
                                        Text("${ord.productName} (${ord.category})", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                                        Text("قيمة التجميد: ${Money.formatMinor(ord.totalPriceMinor + ord.deliveryFeeMinor)} ريال | عمولة المجمع: ${Money.formatMinor(ord.commissionAmountMinor)} ريال", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("مفتاح الأمان السري للفك: ${ord.otpReleaseCode}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section 4: Pharmacies Approval Desk
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("🏥 طلبات اعتماد ومطابقة الصيدليات المتنقلة", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))

                    if (mockPharmaciesWaitingApproval.isEmpty()) {
                        Text("تم اعتماد ومطابقة كافة الصيدليات بنجاح ✅", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    } else {
                        mockPharmaciesWaitingApproval.forEach { pharm ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(pharm, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Button(
                                    onClick = { mockPharmaciesWaitingApproval.remove(pharm) },
                                    modifier = Modifier.height(28.dp),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                                ) {
                                    Text("اعتماد ومطابقة الجودة ✓", fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section 5: Diagnostic Monitoring Crash Console and Outbox Monitor (Recommendation 1 & 2)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Black),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(if (outboxSyncFailureAlert) Color.Red else Color.Green))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "🖥️ بوابة الـ Outbox والتحقق المتعدد (SaaS Sync Monitor)",
                                style = MaterialTheme.typography.titleSmall,
                                color = if (outboxSyncFailureAlert) Color.Red else Color.Green,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.DarkGray)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "السجل: v3",
                                fontSize = 10.sp,
                                color = Color.LightGray,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    if (outboxSyncFailureAlert) {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.Red.copy(0.25f))
                        ) {
                            Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Warning, contentDescription = "Alarm", tint = Color.Red, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "🚨 إنذار فني: نسبة فشل المزامنة تجاوزت الـ 5%! افحص خطوط الـ APN الميدانية.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.Red,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Metrics dashboard row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("أحداث معلّقة (Pending)", color = Color.Gray, fontSize = 10.sp)
                            Text("$pendingOutboxCount حدث", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text("أحداث فاشلة (Failed)", color = Color.Gray, fontSize = 10.sp)
                            Text("$failedOutboxCount حدث", color = if (failedOutboxCount > 0) Color.Red else Color.LightGray, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text("إجمالي الأحداث (Total)", color = Color.Gray, fontSize = 10.sp)
                            Text("$totalOutboxCount سجل", color = Color.Green, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text("سير عمل المزامنة في الخلفية (Scoped Sync Engine & Isolation Logs):", color = Color.Gray, fontSize = 10.sp)
                    Spacer(modifier = Modifier.height(4.dp))

                    // Isolated Scoped logging console (Recommendation 2)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .background(Color.DarkGray.copy(0.5f))
                            .border(1.dp, Color.DarkGray)
                            .padding(8.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = outboxSyncLog,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            color = Color.Green,
                            fontSize = 11.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text("سجل الأحداث المنشورة الفعلي بقاعدة البيانات (Outbox Serialized Events Queue):", color = Color.Gray, fontSize = 10.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .background(Color(0xFF0F172A))
                            .border(1.dp, Color.DarkGray)
                            .padding(6.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Column {
                            if (allOutboxEvents.isEmpty()) {
                                Text("لا توجد أحداث ومزامنات نشطة بالـ Outbox حتى الآن.", color = Color.Gray, fontSize = 10.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                            } else {
                                allOutboxEvents.reversed().take(10).forEach { evt ->
                                    val statusColor = if (evt.status == "PENDING") Color(0xFFEAB308) else if (evt.status == "FAILED") Color(0xFFEF4444) else Color(0xFF22C55E)
                                    val bgIndicator = if (evt.status == "PENDING") "⏱️" else if (evt.status == "FAILED") "❌" else "✅"
                                    Text(
                                        text = "$bgIndicator ID #${evt.id} | ${evt.eventType} | [${evt.status}]\n  Payload: ${evt.payload}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                        color = statusColor,
                                        fontSize = 9.sp
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { viewModel.runOutboxSynchronization() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Green),
                        modifier = Modifier.fillMaxWidth().height(36.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("تشغيل معالج التزامن السحابي المعزول ⚡ (SaaS Sync)", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }

        // Section 6: Nightly Financial Reconciliation Auditor (Recommendation 5)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(0.3f), RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = "Ledger", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "⚖️ مصفاة ومصالحة الحسابات الدفترية (SaaS Ledger Auditor)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "يقوم النظام بمطابقة الأرصدة الافتراضية مع المعاملات الدفترية الدقيقة (Double-Entry Ledger) لمنع التباينات وتصفير الثقة في الـ Sync.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    if (reconciliationReport == null) {
                        Text(
                            text = "اضغط على زر الفحص للبدء بمطابقة الفهارس المحوسبة لجميع الأرصدة النشطة.",
                            fontSize = 11.sp,
                            color = Color.Gray,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                    } else {
                        val report = reconciliationReport!!
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (report.isSecurityAlarmTripped) MaterialTheme.colorScheme.errorContainer 
                                                 else MaterialTheme.colorScheme.primaryContainer.copy(0.4f)
                            )
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = report.summaryMessage,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (report.isSecurityAlarmTripped) MaterialTheme.colorScheme.onErrorContainer 
                                            else MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("جدول التدقيق المالي المالي المزدوج:", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                report.audits.forEach { audit ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("${audit.phone.takeLast(4)} (المستوى)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        Text("الافتراضي: ${audit.cachedBalance}ر.ي", fontSize = 10.sp, color = Color.Gray)
                                        Text("الـ Ledger: ${audit.calculatedLedgerSum}ر.ي", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        Text(
                                            text = if (audit.status == "✅ RECONCILED_OK") "مطابق ✓" else "🚨 اختراق!",
                                            fontSize = 11.sp,
                                            color = if (audit.status == "✅ RECONCILED_OK") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Button(
                        onClick = { viewModel.runNightlyFinancialAudit() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.fillMaxWidth().height(36.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("تشغيل التدقيق الليلي الفوري (Reconciliation Check)", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("سجل القيود المحاسبية الدفترية المزدوجة (Double-Entry Ledger Book):", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(0.3f))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            .padding(6.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Column {
                            if (allLedgerEntries.isEmpty()) {
                                Text("لا توجد قيود محاسبية مسجلة بالدفتر حتى الآن.", color = Color.Gray, fontSize = 10.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                            } else {
                                allLedgerEntries.reversed().take(20).forEach { entry ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                    ) {
                                        Column(modifier = Modifier.padding(6.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text("قيد #${entry.entryId}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                                Text("المبلغ: ${Money.formatMinor(entry.amountMinor)} ر.ي", fontSize = 10.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.secondary)
                                            }
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text("محفظة المدين: #${entry.debitWalletId}", fontSize = 9.sp, color = Color.Gray)
                                                Text("محفظة الدائن: #${entry.creditWalletId}", fontSize = 9.sp, color = Color.Gray)
                                            }
                                            Text("الوصف: ${entry.narrative}", fontSize = 9.sp, style = MaterialTheme.typography.bodySmall)
                                            Text("التاريخ: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).format(java.util.Date(entry.timestamp))}", fontSize = 8.sp, color = Color.Gray)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section 7: Offline-First Sync Conflict Scenario Simulator Wizard (Recommendation 6)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .border(1.dp, MaterialTheme.colorScheme.tertiary.copy(0.3f), RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, contentDescription = "Wizard", tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "🧙‍♂️ معالج عروض أوفلاين والتعارض الفوري (Demo Wizard)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "سيناريو تجريد تباعد الأسعار: قم بتوليد طلب دواء في وضع أوفلاين وسعر قديم، لترى كيف تظهر الشاشة الحمراء لتسوية الخلاف المالي تزامناً!",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            coroutineScope.launch {
                                // 1. Set mode to mesh/offline
                                viewModel.setNetworkMode(com.example.data.network.NetworkMode.OFFLINE_MESH)
                                // 2. Direct-insert a sugarcane medication order in CONFLICT state
                                val client = viewModel.repository.getUserByPhone("770000001")
                                val pharmacy = viewModel.repository.getUserByPhone("770000002")
                                if (client != null && pharmacy != null) {
                                    viewModel.repository.createOrder(
                                        clientId = client.id,
                                        merchantId = pharmacy.id,
                                        driverId = null,
                                        productName = "علاج منظم سكري جلوكوفاج 500 ملجم (محاكاة أوفلاين)",
                                        category = "medicine",
                                        totalPrice = 1800.0,
                                        deliveryFee = 1500.0,
                                        paymentMethod = "wallet"
                                    )
                                    Toast.makeText(context, "🪄 تم التحويل لأوفلاين بنجاح وتوليد الطلب بحالة [CONFLICT] لتجربته بالواجهة الأخرى!", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                        modifier = Modifier.fillMaxWidth().height(38.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("توليد سيناريو تعارض السعر أوفلاين 🪄", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Section 8: Mutual Peer Reviews ledger list
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("📜 دفتر مراجعات وتقييمات الشركاء (Community P2P Reviews)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))

                    if (allReviews.isEmpty()) {
                        Text("لا توجد مراجعات مسجلة في قاعدة البيانات حالياً لمطابقتها.", fontSize = 11.sp, color = Color.Gray)
                    } else {
                        allReviews.forEach { rev ->
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(0.4f))
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                        Text("رقم العقد: ${rev.orderId}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                                        Text("التقييم: ${String(CharArray(rev.rating) { '⭐' })}", color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.labelSmall)
                                    }
                                    if (rev.tags.isNotBlank()) {
                                        Text("الوسم السريع: ${rev.tags}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    }
                                    if (rev.comment.isNotBlank()) {
                                        Text("التعليق المكتوب: \"${rev.comment}\"", fontSize = 11.sp, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
}

@Composable
fun AdminMiniReportCard(title: String, value: String, subtitle: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(0.55f)),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(title, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
            Text(subtitle, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun ProfileScreen(viewModel: PlatformViewModel) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val currentRole by viewModel.currentRole.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val user = currentUser ?: return

    var fullName by remember(user.id, user.fullName) { mutableStateOf(user.fullName) }
    var displayName by remember(user.id, user.displayName) { mutableStateOf(user.displayName) }
    var businessType by remember(user.id, user.businessType, currentRole) {
        mutableStateOf(
            if (currentRole == "merchant" && (user.businessType.isBlank() || user.businessType == "none")) "pharmacy" else user.businessType
        )
    }
    var businessName by remember(user.id, user.businessName) { mutableStateOf(user.businessName) }
    var responsibleName by remember(user.id, user.responsibleName) { mutableStateOf(user.responsibleName) }
    var contactPhone by remember(user.id, user.contactPhone) { mutableStateOf(user.contactPhone.ifBlank { user.phone }) }
    var city by remember(user.id, user.city) { mutableStateOf(user.city) }
    var district by remember(user.id, user.district) { mutableStateOf(user.district) }
    var address by remember(user.id, user.address) { mutableStateOf(user.address) }
    var gpsLatitudeText by remember(user.id, user.gpsLatitude) { mutableStateOf(if (user.gpsLatitude == 0.0) "" else user.gpsLatitude.toString()) }
    var gpsLongitudeText by remember(user.id, user.gpsLongitude) { mutableStateOf(if (user.gpsLongitude == 0.0) "" else user.gpsLongitude.toString()) }
    var licenseNumber by remember(user.id, user.licenseNumber) { mutableStateOf(user.licenseNumber) }
    var licenseImageUri by remember(user.id, user.licenseImageUri) { mutableStateOf(user.licenseImageUri) }
    var workingHours by remember(user.id, user.workingHours) { mutableStateOf(user.workingHours) }
    var deliversOrders by remember(user.id, user.deliversOrders) { mutableStateOf(user.deliversOrders) }
    var serviceRadiusText by remember(user.id, user.serviceRadiusKm) { mutableStateOf(if (user.serviceRadiusKm <= 0) "" else user.serviceRadiusKm.toString()) }
    var merchantCategory by remember(user.id, user.merchantCategory) { mutableStateOf(user.merchantCategory.ifBlank { if (businessType == "pharmacy") "pharmacy" else "clothes" }) }
    var deliveryPolicy by remember(user.id, user.deliveryPolicy) { mutableStateOf(user.deliveryPolicy) }
    var vehicleType by remember(user.id, user.vehicleType) { mutableStateOf(user.vehicleType) }
    var vehiclePlate by remember(user.id, user.vehiclePlate) { mutableStateOf(user.vehiclePlate) }

    val safeBusinessType = when (currentRole) {
        "merchant" -> if (businessType == "pharmacy") "pharmacy" else "marketplace"
        "driver" -> "delivery"
        "admin" -> "admin"
        else -> "none"
    }
    val isPharmacy = currentRole == "merchant" && safeBusinessType == "pharmacy"
    val isMarketplace = currentRole == "merchant" && safeBusinessType == "marketplace"
    val profileIssues = mutableListOf<String>().apply {
        if (fullName.trim().isBlank()) add("الاسم الكامل مطلوب")
        if (city.trim().isBlank()) add("المدينة مطلوبة")
        if (currentRole != "admin" && district.trim().isBlank()) add("المديرية / المنطقة مطلوبة")
        if (currentRole in listOf("client", "merchant") && address.trim().isBlank()) add("العنوان التفصيلي مطلوب")
        if (currentRole == "merchant") {
            if (businessName.trim().isBlank()) add(if (isPharmacy) "اسم الصيدلية الرسمي مطلوب" else "اسم المتجر مطلوب")
            if (responsibleName.trim().isBlank()) add("اسم المسؤول مطلوب")
            if (isPharmacy && licenseNumber.trim().isBlank()) add("رقم ترخيص الصيدلية مطلوب")
            if (isPharmacy && workingHours.trim().isBlank()) add("ساعات عمل الصيدلية مطلوبة")
            if (isMarketplace && merchantCategory.trim().isBlank()) add("تصنيف نشاط التاجر مطلوب")
            if (isMarketplace && deliveryPolicy.trim().isBlank()) add("سياسة التوصيل/الاستلام مطلوبة")
        }
        if (currentRole == "driver") {
            if (contactPhone.trim().isBlank()) add("رقم تواصل السائق مطلوب")
            if (vehicleType.trim().isBlank()) add("نوع المركبة مطلوب")
            if (vehiclePlate.trim().isBlank()) add("رقم المركبة مطلوب")
        }
    }
    val canMarkComplete = profileIssues.isEmpty()
    val approvalText = when (user.approvalStatus) {
        "approved" -> "معتمد"
        "pending" -> "بانتظار مراجعة الإدارة"
        "incomplete" -> "ناقص البيانات"
        "suspended" -> "معلّق"
        else -> user.approvalStatus.ifBlank { "غير محدد" }
    }
    val saveResultText = if (currentRole == "merchant" || currentRole == "driver") "بانتظار اعتماد الإدارة" else "جاهز"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier.size(88.dp),
            shape = RoundedCornerShape(44.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            elevation = CardDefaults.cardElevation(6.dp)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "الملف الشخصي",
                    modifier = Modifier.size(52.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = displayName.ifBlank { user.phone },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "${getRoleArName(currentRole)} | ${getBusinessTypeArName(safeBusinessType)} | $approvalText",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 4.dp),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (canMarkComplete) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
            ),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, if (canMarkComplete) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = if (canMarkComplete) "ملفك مكتمل تشغيليًا" else "ملفك ناقص ولا يجب فتح كامل الصلاحيات بعد",
                    fontWeight = FontWeight.ExtraBold,
                    color = if (canMarkComplete) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
                if (profileIssues.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    profileIssues.take(5).forEach { issue ->
                        Text("• $issue", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(2.dp),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("1) بيانات الهوية", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                Text("هذه المرحلة تحدد صلاحيات المستخدم وتمنع خلط الصيدليات مع تجار السوق.", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("الاسم الكامل") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("اسم الظهور داخل التطبيق") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (currentRole == "merchant") {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("نوع النشاط", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f).clickable {
                            businessType = "pharmacy"
                            merchantCategory = "pharmacy"
                        }) {
                            RadioButton(selected = businessType == "pharmacy", onClick = {
                                businessType = "pharmacy"
                                merchantCategory = "pharmacy"
                            })
                            Text("صيدلية", fontSize = 11.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f).clickable {
                            businessType = "marketplace"
                            if (merchantCategory == "pharmacy") merchantCategory = "clothes"
                        }) {
                            RadioButton(selected = businessType == "marketplace", onClick = {
                                businessType = "marketplace"
                                if (merchantCategory == "pharmacy") merchantCategory = "clothes"
                            })
                            Text("سوق/ملابس", fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (currentRole == "merchant") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(if (isPharmacy) "2) بيانات الصيدلية" else "2) بيانات المتجر", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = businessName,
                        onValueChange = { businessName = it },
                        label = { Text(if (isPharmacy) "اسم الصيدلية الرسمي" else "اسم المتجر / البوتيك") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = responsibleName,
                        onValueChange = { responsibleName = it },
                        label = { Text("اسم المسؤول") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = contactPhone,
                        onValueChange = { contactPhone = it },
                        label = { Text("رقم التواصل") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (isPharmacy) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = licenseNumber,
                            onValueChange = { licenseNumber = it },
                            label = { Text("رقم ترخيص الصيدلية") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = licenseImageUri,
                            onValueChange = { licenseImageUri = it },
                            label = { Text("رابط/مسار صورة الترخيص مؤقتًا") },
                            supportingText = { Text("لاحقًا سيتم استبداله برفع صورة فعلي من الكاميرا أو المعرض.", fontSize = 10.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = workingHours,
                            onValueChange = { workingHours = it },
                            label = { Text("ساعات العمل مثال: 09:00 - 22:00") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("تصنيف النشاط", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Column {
                            listOf("clothes" to "ملابس", "wholesale" to "جملة", "general_market" to "سوق عام").forEach { (value, label) ->
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { merchantCategory = value }) {
                                    RadioButton(selected = merchantCategory == value, onClick = { merchantCategory = value })
                                    Text(label, fontSize = 12.sp)
                                }
                            }
                        }
                        OutlinedTextField(
                            value = licenseNumber,
                            onValueChange = { licenseNumber = it },
                            label = { Text("رقم سجل/تعريف التاجر - اختياري") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = deliveryPolicy,
                            onValueChange = { deliveryPolicy = it },
                            label = { Text("سياسة التوصيل أو الاستلام") },
                            minLines = 2,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { deliversOrders = !deliversOrders }) {
                        Checkbox(checked = deliversOrders, onCheckedChange = { deliversOrders = it })
                        Text(if (isPharmacy) "الصيدلية توفر توصيل" else "المتجر يوفر توصيل", fontSize = 12.sp)
                    }
                    if (deliversOrders) {
                        OutlinedTextField(
                            value = serviceRadiusText,
                            onValueChange = { serviceRadiusText = it.filter { ch -> ch.isDigit() }.take(3) },
                            label = { Text("نطاق الخدمة بالكيلومتر") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }

        if (currentRole == "driver") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("2) بيانات السائق", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = contactPhone,
                        onValueChange = { contactPhone = it },
                        label = { Text("رقم تواصل السائق") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = vehicleType,
                        onValueChange = { vehicleType = it },
                        label = { Text("نوع المركبة: دراجة / سيارة / باص") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = vehiclePlate,
                        onValueChange = { vehiclePlate = it },
                        label = { Text("رقم المركبة") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(2.dp),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(if (currentRole == "admin") "2) بيانات الإدارة" else "3) العنوان والموقع", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = city,
                        onValueChange = { city = it },
                        label = { Text("المدينة") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = district,
                        onValueChange = { district = it },
                        label = { Text("المديرية / المنطقة") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text(if (currentRole == "merchant") "العنوان التفصيلي / نطاق الخدمة" else "العنوان التفصيلي") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = gpsLatitudeText,
                        onValueChange = { gpsLatitudeText = it.filter { ch -> ch.isDigit() || ch == '.' || ch == '-' }.take(12) },
                        label = { Text("Latitude") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = gpsLongitudeText,
                        onValueChange = { gpsLongitudeText = it.filter { ch -> ch.isDigit() || ch == '.' || ch == '-' }.take(12) },
                        label = { Text("Longitude") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Text("إحداثيات GPS اختيارية الآن؛ لاحقًا ستؤخذ من خدمة الموقع بدل الكتابة اليدوية.", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = {
                if (!canMarkComplete) {
                    Toast.makeText(context, "أكمل الناقص: ${profileIssues.first()}", Toast.LENGTH_LONG).show()
                    return@Button
                }
                viewModel.saveMyProfileDetails(
                    fullName = fullName,
                    displayName = displayName.ifBlank { fullName },
                    businessType = safeBusinessType,
                    businessName = if (currentRole == "merchant") businessName else "",
                    responsibleName = responsibleName,
                    contactPhone = contactPhone,
                    city = city,
                    district = district,
                    address = address,
                    gpsLatitude = gpsLatitudeText.toDoubleOrNull() ?: 0.0,
                    gpsLongitude = gpsLongitudeText.toDoubleOrNull() ?: 0.0,
                    licenseNumber = licenseNumber,
                    licenseImageUri = licenseImageUri,
                    workingHours = workingHours,
                    deliversOrders = deliversOrders,
                    serviceRadiusKm = serviceRadiusText.toIntOrNull() ?: 0,
                    merchantCategory = if (isPharmacy) "pharmacy" else merchantCategory,
                    deliveryPolicy = deliveryPolicy,
                    vehicleType = vehicleType,
                    vehiclePlate = vehiclePlate
                )
                Toast.makeText(context, "تم حفظ الملف؛ الحساب $saveResultText", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = if (canMarkComplete) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
        ) {
            Text(if (canMarkComplete) "حفظ واعتماد اكتمال الملف" else "أكمل البيانات قبل الحفظ", color = Color.Black, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(2.dp),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("تفاصيل المحفظة والحالة", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                Divider(modifier = Modifier.padding(vertical = 12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("الرصيد المتاح:", fontSize = 14.sp)
                    Text("${Money.formatMinor(user.walletBalanceMinor)} ريال", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("حالة الحساب:", fontSize = 14.sp)
                    Text(if (user.status == "active") "نشط" else user.status, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("اكتمال البيانات:", fontSize = 14.sp)
                    Text(if (user.isProfileComplete) "مكتمل" else "ناقص", fontWeight = FontWeight.Bold, color = if (user.isProfileComplete) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("اعتماد الإدارة:", fontSize = 14.sp)
                    Text(approvalText, fontWeight = FontWeight.Bold, color = if (user.approvalStatus == "approved") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = { viewModel.logout() },
            modifier = Modifier.fillMaxWidth().height(54.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.ExitToApp, contentDescription = "خروج", tint = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Text("تسجيل الخروج الآمن", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}
