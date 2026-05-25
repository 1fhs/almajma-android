package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.data.database.*
import com.example.data.network.ConnectionManager
import com.example.data.network.NetworkMode
import com.example.data.repository.PlatformRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class PlatformViewModel(application: Application) : AndroidViewModel(application) {

    private val db = Room.databaseBuilder(
        application,
        AppDatabase::class.java, "al_majma_database"
    )
    .addMigrations(
        AppDatabase.MIGRATION_1_2,
        AppDatabase.MIGRATION_2_3,
        AppDatabase.MIGRATION_3_4,
        AppDatabase.MIGRATION_4_5,
        AppDatabase.MIGRATION_5_6
    )
    .build()

    private val connectionManager = ConnectionManager()
    
    val repository = PlatformRepository(
        userDao = db.userDao(),
        productDao = db.productDao(),
        orderDao = db.orderDao(),
        transactionDao = db.transactionDao(),
        chatDao = db.chatDao(),
        reviewDao = db.reviewDao(),
        pharmacyOfferDao = db.pharmacyOfferDao(),
        orderTimelineDao = db.orderTimelineDao(),
        notificationDao = db.notificationDao(),
        disputeDao = db.disputeDao(),
        prescriptionDao = db.prescriptionDao(),
        pharmacyVerificationDao = db.pharmacyVerificationDao(),
        systemConfigDao = db.systemConfigDao(),
        ledgerEntryDao = db.ledgerEntryDao(),
        outboxEventDao = db.outboxEventDao(),
        connectionManager = connectionManager
    )

    // Current logged-in user state
    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

    // Current app UI perspective/role (Client, Merchant, Driver)
    private val _currentRole = MutableStateFlow("client")
    val currentRole: StateFlow<String> = _currentRole.asStateFlow()

    // Screen navigation tracking for each role
    private val _clientScreen = MutableStateFlow("home") // home, auth, ride, pharmacy, Souk, chat, wallet
    val clientScreen: StateFlow<String> = _clientScreen.asStateFlow()

    private val _merchantScreen = MutableStateFlow("dashboard") // dashboard, incoming, products, orders
    val merchantScreen: StateFlow<String> = _merchantScreen.asStateFlow()

    private val _driverScreen = MutableStateFlow("radar") // radar, hud, wallet
    val driverScreen: StateFlow<String> = _driverScreen.asStateFlow()

    // Connectivity state flows
    val networkMode: StateFlow<NetworkMode> = connectionManager.networkMode
    val latencyMs: StateFlow<Int> = connectionManager.latencyMs
    val meshPeersNearby: StateFlow<Int> = connectionManager.meshPeersNearby

    // Users and product streams
    val users: StateFlow<List<UserEntity>> = repository.allUsers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Interactive product streams
    val products: StateFlow<List<ProductEntity>> = repository.allProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val orders: StateFlow<List<OrderEntity>> = repository.allOrders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Tracks open chat order ID
    private val _selectedOrderId = MutableStateFlow<Int?>(null)
    val selectedOrderId: StateFlow<Int?> = _selectedOrderId.asStateFlow()

    // Live chat updater
    val chatMessages: StateFlow<List<ChatMessageEntity>> = _selectedOrderId
        .flatMapLatest { id ->
            if (id != null) repository.getMessagesForOrder(id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedOrderTimeline: StateFlow<List<OrderTimelineEntity>> = _selectedOrderId
        .flatMapLatest { id ->
            if (id != null) repository.getTimelineForOrder(id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedOrderOffers: StateFlow<List<PharmacyOfferEntity>> = _selectedOrderId
        .flatMapLatest { id ->
            if (id != null) repository.getOffersForOrder(id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userNotifications: StateFlow<List<NotificationEntity>> = _currentUser
        .flatMapLatest { user ->
            if (user != null) repository.getNotificationsForUser(user.id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unreadNotificationCount: StateFlow<Int> = _currentUser
        .flatMapLatest { user ->
            if (user != null) repository.getUnreadNotificationCount(user.id) else flowOf(0)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Live transaction history for current logged-in user
    val userTransactions: StateFlow<List<TransactionEntity>> = _currentUser
        .flatMapLatest { user ->
            if (user != null) db.transactionDao().getTransactionsForUser(user.id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // System configuration for dynamic thematic parameters & server-driven UI
    val systemConfig: StateFlow<SystemConfigEntity> = repository.systemConfig
        .map { it ?: SystemConfigEntity() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SystemConfigEntity())

    val primaryColor: StateFlow<String> = systemConfig
        .map { it.primaryColor }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "#10B981")

    val secondaryColor: StateFlow<String> = systemConfig
        .map { it.secondaryColor }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "#FBBF24")

    // P2P Mutual Reviews Stream
    val allReviews: StateFlow<List<ReviewEntity>> = repository.allReviews
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPharmacyOffers: StateFlow<List<PharmacyOfferEntity>> = repository.allPharmacyOffers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allDisputes: StateFlow<List<DisputeEntity>> = repository.allDisputes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPrescriptions: StateFlow<List<PrescriptionEntity>> = repository.allPrescriptions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPharmacyVerifications: StateFlow<List<PharmacyVerificationEntity>> = repository.allPharmacyVerifications
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Outbox metrics flows (Recommendation 1)
    val pendingOutboxCount: StateFlow<Int> = repository.pendingOutboxCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val failedOutboxCount: StateFlow<Int> = repository.failedOutboxCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalOutboxCount: StateFlow<Int> = repository.totalOutboxCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val allOutboxEvents: StateFlow<List<OutboxEventEntity>> = repository.allOutboxEvents
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allLedgerEntries: StateFlow<List<LedgerEntryEntity>> = repository.allLedgerEntries
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Audit and Sync state containers
    private val _reconciliationReport = MutableStateFlow<com.example.data.repository.ReconciliationReport?>(null)
    val reconciliationReport: StateFlow<com.example.data.repository.ReconciliationReport?> = _reconciliationReport.asStateFlow()

    private val _outboxSyncLog = MutableStateFlow("مراقبة الـ Outbox نشطة. اضغط على زر 'تشغيل التدقيق وبدء المزامنة' لبدء التبادل.")
    val outboxSyncLog: StateFlow<String> = _outboxSyncLog.asStateFlow()

    private val _outboxSyncFailureAlert = MutableStateFlow(false)
    val outboxSyncFailureAlert: StateFlow<Boolean> = _outboxSyncFailureAlert.asStateFlow()

    // Simulated Cloud Backup logs logbook
    private val _backupLogs = MutableStateFlow("لا توجد عمليات مزامنة نشطة أوفلاين حالياً.")
    val backupLogs: StateFlow<String> = _backupLogs.asStateFlow()

    // Auth screen inputs
    var phoneInput = MutableStateFlow("")
    var otpInput = MutableStateFlow("")
    var selectedAuthRole = MutableStateFlow("client")
    var isWaitingForOtp = MutableStateFlow(false)
    var isRecordingSimulated = MutableStateFlow(false)
    var recordingDurationSec = MutableStateFlow(0)
    
    private var recordingJob: Job? = null

    init {
        viewModelScope.launch {
            repository.seedDatabaseIfEmpty()
        }
    }

    // Role controller
    fun switchRole(role: String) {
        viewModelScope.launch {
            _currentRole.value = role
            // Auto switch simulated backend login matching the seeded users to provide quick high fidelity interactions
            val matchedUser = when (role) {
                "client" -> repository.getUserByPhone("770000001")
                "merchant" -> repository.getUserByPhone("770000002") // Default to grand pharmacy
                "driver" -> repository.getUserByPhone("770000004")
                "admin" -> repository.getUserByPhone("770000009")
                else -> null
            }
            if (matchedUser != null) {
                _currentUser.value = matchedUser
            }
        }
    }

    // Set network status
    fun setNetworkMode(mode: NetworkMode) {
        connectionManager.setMode(mode)
    }

    // Auth Login triggers
    fun requestOtp() {
        if (phoneInput.value.length >= 7) {
            isWaitingForOtp.value = true
        }
    }

    fun verifyLoginOtp() {
        viewModelScope.launch {
            val user = repository.registerOrLoginUser(phoneInput.value, selectedAuthRole.value)
            _currentUser.value = user
            _currentRole.value = user.role
            isWaitingForOtp.value = false
            
            // Navigate to appropriate homepage
            when (user.role) {
                "client" -> _clientScreen.value = "home"
                "merchant" -> _merchantScreen.value = "dashboard"
                "driver" -> _driverScreen.value = "radar"
            }
        }
    }

    fun triggerQuickLogin(phone: String, role: String) {
        viewModelScope.launch {
            phoneInput.value = phone
            selectedAuthRole.value = role
            val user = repository.registerOrLoginUser(phone, role)
            _currentUser.value = user
            _currentRole.value = user.role
            isWaitingForOtp.value = false
            
            when (user.role) {
                "client" -> _clientScreen.value = "home"
                "merchant" -> _merchantScreen.value = "dashboard"
                "driver" -> _driverScreen.value = "radar"
            }
        }
    }

    fun logout() {
        _currentUser.value = null
        isWaitingForOtp.value = false
        phoneInput.value = ""
        otpInput.value = ""
    }

    // Navigation triggers
    fun navigateClientTo(screen: String) {
        _clientScreen.value = screen
    }

    fun navigateMerchantTo(screen: String) {
        _merchantScreen.value = screen
    }

    fun navigateDriverTo(screen: String) {
        _driverScreen.value = screen
    }

    fun selectOrderForChat(orderId: Int) {
        _selectedOrderId.value = orderId
        // Depending on role, redirect to safe in-app chat screen
        if (_currentRole.value == "client") {
            _clientScreen.value = "chat"
        } else if (_currentRole.value == "merchant") {
            _merchantScreen.value = "chat"
        } else if (_currentRole.value == "driver") {
            _driverScreen.value = "chat"
        }
    }

    // Interactive operations: Client creating ride request
    fun requestRide(deliveryPriceBargain: Double, textDestination: String, paymentMethod: String) {
        viewModelScope.launch {
            val client = _currentUser.value ?: return@launch
            val orderId = repository.createOrder(
                clientId = client.id,
                merchantId = null,
                driverId = null,
                productName = "طلب مشوار موتور سريع إلى: $textDestination",
                category = "ride",
                totalPrice = 0.0, // Passenger price bidding
                deliveryFee = deliveryPriceBargain,
                paymentMethod = paymentMethod
            )
            // Immediately open chat room for ride bargaining negotiations
            _selectedOrderId.value = orderId
            _clientScreen.value = "chat"
        }
    }

    // Interactive operations: Client Broadcast Prescription medicine request to nearby pharmacies (Reverse Bidding)
    fun requestPrescriptionDawaa(medicineName: String, prescriptionAttached: Boolean) {
        viewModelScope.launch {
            val client = _currentUser.value ?: return@launch
            val orderId = repository.createOrder(
                clientId = client.id,
                merchantId = null,
                driverId = null,
                productName = "بحث عكسي عن دواء: $medicineName" + (if (prescriptionAttached) " (مرفق صورة الروشتة 📸)" else ""),
                category = "medicine",
                totalPrice = 0.0, // awaiting pharmacy bid price
                deliveryFee = 1500.0, // normal flat delivery fee
                paymentMethod = "wallet",
                prescriptionAttached = prescriptionAttached
            )
            // Save state and open chat room/offer responders list
            _selectedOrderId.value = orderId
            _clientScreen.value = "chat"
        }
    }

    // Interactive operations: Client buying product or bargaining from souk
    fun purchaseSoukItem(product: ProductEntity, offerBargainPrice: Double?) {
        viewModelScope.launch {
            val client = _currentUser.value ?: return@launch
            val finalPrice = offerBargainPrice ?: product.price
            val orderId = repository.createOrder(
                clientId = client.id,
                merchantId = product.merchantId,
                driverId = null,
                productName = product.name,
                category = product.category,
                totalPrice = finalPrice,
                deliveryFee = 1000.0, // local delivery fee
                paymentMethod = "wallet"
            )
            _selectedOrderId.value = orderId
            _clientScreen.value = "chat"
        }
    }

    // Merchant proposing reverse biddings pricing for medications
    fun submitPrescriptionPriceQuote(orderId: Int, priceProposal: Double) {
        submitPrescriptionOfferFull(orderId, priceProposal, 1, 30, "", "", "غير محدد")
    }

    fun submitPrescriptionOfferFull(
        orderId: Int,
        priceProposal: Double,
        availableQuantity: Int,
        preparationMinutes: Int,
        note: String,
        alternativeMedicineName: String,
        expiryDateText: String
    ) {
        viewModelScope.launch {
            val merchant = _currentUser.value ?: return@launch
            repository.merchantOfferPriceForMedication(
                orderId = orderId,
                merchantId = merchant.id,
                proposedPrice = priceProposal,
                availableQuantity = availableQuantity,
                preparationMinutes = preparationMinutes,
                note = note,
                alternativeMedicineName = alternativeMedicineName,
                expiryDateText = expiryDateText
            )
        }
    }

    fun acceptMerchantQuote(orderId: Int) {
        viewModelScope.launch {
            val accepted = repository.acceptMerchantQuoteAndFreezeEscrow(orderId)
            if (accepted) {
                val curr = _currentUser.value
                if (curr != null) {
                    _currentUser.value = repository.getUserById(curr.id)
                }
            }
        }
    }

    fun markMedicationUnavailable(orderId: Int) {
        viewModelScope.launch {
            val merchant = _currentUser.value ?: return@launch
            repository.markMedicationRequestUnavailable(orderId, merchant.id)
        }
    }

    // Driver claims trip on Radar
    fun acceptDeliveryOrder(orderId: Int) {
        viewModelScope.launch {
            val driver = _currentUser.value ?: return@launch
            repository.acceptOrderAsDriver(orderId, driver.id)
            _selectedOrderId.value = orderId
            _driverScreen.value = "chat"
        }
    }

    // Wallet recharges
    fun processRecharge(amount: Double, sourceProvider: String) {
        viewModelScope.launch {
            val user = _currentUser.value ?: return@launch
            repository.rechargeWallet(user.id, amount, sourceProvider)
            
            // Re-fetch user to refresh view UI balance
            val refreshed = repository.getUserById(user.id)
            if (refreshed != null) {
                _currentUser.value = refreshed
            }
        }
    }

    // Merchant adds product
    fun createMerchantProduct(name: String, price: Double, category: String, location: String) {
        viewModelScope.launch {
            val merchant = _currentUser.value ?: return@launch
            repository.createProduct(merchant.id, name, price, category, location)
            _merchantScreen.value = "dashboard" // back
        }
    }

    // Escrow verification Release OTP scanner/payout resolver
    fun confirmDeliveryOtp(orderId: Int, pinFilled: String, onFinished: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = repository.releaseOrderEscrow(orderId, pinFilled)
            
            // Refresh users
            val curr = _currentUser.value
            if (curr != null) {
                _currentUser.value = repository.getUserById(curr.id)
            }
            onFinished(success)
        }
    }

    // Simulated Recording Engine for Voice Notes
    fun startRecordingVoice() {
        isRecordingSimulated.value = true
        recordingDurationSec.value = 0
        recordingJob = viewModelScope.launch {
            while (isRecordingSimulated.value) {
                delay(1000)
                recordingDurationSec.value += 1
            }
        }
    }

    fun stopAndSendVoiceNote() {
        isRecordingSimulated.value = false
        recordingJob?.cancel()
        val duration = recordingDurationSec.value
        val id = _selectedOrderId.value
        val user = _currentUser.value
        if (id != null && user != null && duration > 0) {
            viewModelScope.launch {
                repository.sendChatMessage(
                    orderId = id,
                    senderRole = user.role,
                    senderName = if (user.role == "client") "أحمد بن علي" else if (user.role == "driver") "السائق أبو رعد" else "الصيدلي المناوب",
                    messageText = "رسالة صوتية مدمجة",
                    isVoiceNote = true,
                    durationSec = duration
                )
            }
        }
        recordingDurationSec.value = 0
    }

    fun submitTextMessage(txt: String) {
        val id = _selectedOrderId.value
        val user = _currentUser.value
        if (id != null && user != null && txt.isNotBlank()) {
            viewModelScope.launch {
                val name = if (user.role == "client") "أحمد بن علي" else if (user.role == "driver") "السائق أبو رعد" else "التشغيل"
                repository.sendChatMessage(id, user.role, name, txt, false, 0)
            }
        }
    }

    // ==========================================
    // MULTIVERSE FEATURE EXTENSIONS TRIGGER METHODS
    // ==========================================
    
    // 1. Peer-to-Peer Review Actions
    fun submitUserReview(orderId: Int, reviewerId: Int, revieweeId: Int, rating: Int, tags: String, comment: String) {
        viewModelScope.launch {
            repository.submitReview(orderId, reviewerId, revieweeId, rating, tags, comment)
            
            // Re-fetch users to immediately apply suspension status if needed
            val curr = _currentUser.value
            if (curr != null) {
                _currentUser.value = repository.getUserById(curr.id)
            }
        }
    }

    // 2. Dynamic Theme & Server-Driven UI Configuration Editor
    fun saveSystemConfigChanges(config: SystemConfigEntity) {
        viewModelScope.launch {
            repository.saveSystemConfig(config)
        }
    }

    // 3. Cryptographic and offline-resilient backup
    fun executeLocalBackup(fileDir: java.io.File, onFinished: (String) -> Unit) {
        viewModelScope.launch {
            val file = java.io.File(fileDir, "al_majma_secure_backup.dat")
            val raw = repository.backupDataToFile(file, false)
            val logMsg = "تم توليد حفظ محلي مشفر ومخفي بنجاح. ملف الحفظ: ${file.name}. الحجم: ${raw.length} حرف مشفر."
            _backupLogs.value = logMsg
            onFinished(logMsg)
        }
    }

    fun executeCloudSimulationBackup(onFinished: (String) -> Unit) {
        viewModelScope.launch {
            val raw = repository.backupDataToFile(java.io.File(""), true)
            val logMsg = "تم تشفير وإرسال حزم التزامن السحابية التدريجية بنجاح إلى خادم مجمع اللامركزي. حجم المغلف: ${raw.length} بايت."
            _backupLogs.value = logMsg
            onFinished(logMsg)
        }
    }

    fun executeSystemRestore(fileDir: java.io.File, customPayload: String?, onFinished: (Boolean) -> Unit) {
        viewModelScope.launch {
            val file = java.io.File(fileDir, "al_majma_secure_backup.dat")
            val success = repository.restoreDataFromFile(file, customPayload)
            if (success) {
                _backupLogs.value = "تمت استعادة قواعد البيانات وخصائص الواجهات والأموال بنجاح وبدون أي فاقد!"
                
                // Refresh ViewModel login user
                val curr = _currentUser.value
                if (curr != null) {
                    _currentUser.value = repository.getUserById(curr.id)
                }
            } else {
                _backupLogs.value = "خطأ: لم يتم العثور على حزمة تبيان صحيحة للاستعادة."
            }
            onFinished(success)
        }
    }

    // 4. Resolve Order Price Mismatch Conflict (Recommendation 4)
    fun resolveOrderPriceConflict(orderId: Int) {
        viewModelScope.launch {
            val approved = repository.resolvePriceConflictAndApprove(orderId)
            if (approved) {
                // Refresh current user cache balance
                val curr = _currentUser.value
                if (curr != null) {
                    _currentUser.value = repository.getUserById(curr.id)
                }
            }
        }
    }

    // 5. Trigger Nightly Financial Reconciliation Audit (Recommendation 5)
    fun runNightlyFinancialAudit() {
        viewModelScope.launch {
            val report = repository.runNightlyFinancialReconciliation()
            _reconciliationReport.value = report
        }
    }

    // 6. Trigger Simulated Outbox Synchronization Process (Recommendation 2 & 1)
    fun runOutboxSynchronization() {
        viewModelScope.launch {
            _outboxSyncLog.value = "بدء مزامنة الـ Outbox سحابياً وبناء الحزم المعزولة..."
            delay(800)
            val res = repository.processPendingOutboxSync { progressLog ->
                _outboxSyncLog.value = progressLog
            }
            _outboxSyncLog.value = res.logReport + "\n\n=== الخاتمة التشخيصية ===\n" + res.summaryText
            _outboxSyncFailureAlert.value = res.isFailedAlertActive
        }
    }

    // 7. Pharmaceutical Product Management & Recalls
    fun createProductFull(
        name: String,
        price: Double,
        category: String,
        location: String,
        batchNumber: String,
        expiryTimestamp: Long,
        purchaseCost: Double,
        totalStock: Int
    ) {
        viewModelScope.launch {
            val merchant = _currentUser.value ?: return@launch
            repository.createProductFull(
                merchantId = merchant.id,
                name = name,
                price = price,
                category = category,
                location = location,
                batchNumber = batchNumber,
                expiryTimestamp = expiryTimestamp,
                purchaseCost = purchaseCost,
                totalStock = totalStock
            )
            _merchantScreen.value = "dashboard"
        }
    }

    fun toggleProductRecall(productId: Int) {
        viewModelScope.launch {
            repository.toggleProductRecall(productId)
        }
    }


    fun markOrderPreparing(orderId: Int) {
        viewModelScope.launch {
            val merchant = _currentUser.value ?: return@launch
            repository.markOrderPreparing(orderId, merchant.id)
        }
    }

    fun markOrderReady(orderId: Int) {
        viewModelScope.launch {
            val merchant = _currentUser.value ?: return@launch
            repository.markOrderReady(orderId, merchant.id)
        }
    }

    fun cancelOrderAndRefund(orderId: Int, reason: String = "إلغاء من الواجهة قبل اكتمال التسليم") {
        viewModelScope.launch {
            val user = _currentUser.value ?: return@launch
            repository.cancelOrderAndRefund(orderId, user.id, reason)
            _currentUser.value = repository.getUserById(user.id)
        }
    }

    fun openOrderDispute(orderId: Int, reason: String, details: String) {
        viewModelScope.launch {
            val user = _currentUser.value ?: return@launch
            repository.openDispute(orderId, user.id, reason, details)
        }
    }

    fun resolveDisputeAsAdmin(disputeId: Int, releaseToMerchant: Boolean, adminDecision: String) {
        viewModelScope.launch {
            repository.resolveDispute(disputeId, releaseToMerchant, adminDecision)
        }
    }

    fun approvePharmacy(merchantId: Int) {
        viewModelScope.launch { repository.approvePharmacyVerification(merchantId) }
    }

    fun rejectPharmacy(merchantId: Int, reason: String = "الترخيص غير واضح أو غير مكتمل") {
        viewModelScope.launch { repository.rejectPharmacyVerification(merchantId, reason) }
    }

    fun markMyNotificationsRead() {
        viewModelScope.launch {
            val user = _currentUser.value ?: return@launch
            repository.markNotificationsRead(user.id)
        }
    }

    fun saveMyProfileDetails(
        fullName: String,
        displayName: String,
        businessType: String,
        businessName: String,
        city: String,
        address: String,
        licenseNumber: String
    ) {
        viewModelScope.launch {
            val user = _currentUser.value ?: return@launch
            repository.updateUserProfile(
                userId = user.id,
                fullName = fullName,
                displayName = displayName,
                businessType = businessType,
                businessName = businessName,
                city = city,
                address = address,
                licenseNumber = licenseNumber
            )
            _currentUser.value = repository.getUserById(user.id)
        }
    }

}