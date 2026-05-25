package com.example.data.repository

import com.example.data.database.*
import com.example.data.network.ConnectionManager
import com.example.data.network.NetworkMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlin.random.Random

class PlatformRepository(
    private val userDao: UserDao,
    private val productDao: ProductDao,
    private val orderDao: OrderDao,
    private val transactionDao: TransactionDao,
    private val chatDao: ChatDao,
    private val reviewDao: ReviewDao,
    private val pharmacyOfferDao: PharmacyOfferDao,
    private val orderTimelineDao: OrderTimelineDao,
    private val notificationDao: NotificationDao,
    private val disputeDao: DisputeDao,
    private val prescriptionDao: PrescriptionDao,
    private val pharmacyVerificationDao: PharmacyVerificationDao,
    private val systemConfigDao: SystemConfigDao,
    private val ledgerEntryDao: LedgerEntryDao,
    private val outboxEventDao: OutboxEventDao,
    val connectionManager: ConnectionManager
) {
    val allUsers: Flow<List<UserEntity>> = userDao.getAllUsers()
    val allProducts: Flow<List<ProductEntity>> = productDao.getAllProducts()
    val allOrders: Flow<List<OrderEntity>> = orderDao.getAllOrders()
    val allReviews: Flow<List<ReviewEntity>> = reviewDao.getAllReviews()
    val allPharmacyOffers: Flow<List<PharmacyOfferEntity>> = pharmacyOfferDao.getAllOffers()
    val allOrderTimeline: Flow<List<OrderTimelineEntity>> = orderTimelineDao.getAllTimeline()
    val allDisputes: Flow<List<DisputeEntity>> = disputeDao.getAllDisputes()
    val openDisputes: Flow<List<DisputeEntity>> = disputeDao.getOpenDisputes()
    val allPrescriptions: Flow<List<PrescriptionEntity>> = prescriptionDao.getAllPrescriptions()
    val allPharmacyVerifications: Flow<List<PharmacyVerificationEntity>> = pharmacyVerificationDao.getAllVerifications()
    val pendingPharmacyVerifications: Flow<List<PharmacyVerificationEntity>> = pharmacyVerificationDao.getPendingVerifications()
    val systemConfig: Flow<SystemConfigEntity?> = systemConfigDao.getConfigFlow()
    val allLedgerEntries: Flow<List<LedgerEntryEntity>> = ledgerEntryDao.getAllLedgerEntries()

    // Outbox streams for first-day monitoring cockpit (Outbox Monitor)
    val allOutboxEvents: Flow<List<OutboxEventEntity>> = outboxEventDao.getAllEvents()
    val pendingOutboxCount: Flow<Int> = outboxEventDao.getPendingEventsCount()
    val failedOutboxCount: Flow<Int> = outboxEventDao.getFailedEventsCount()
    val totalOutboxCount: Flow<Int> = outboxEventDao.getTotalEventsCount()

    fun getProductsByCategory(category: String): Flow<List<ProductEntity>> =
        productDao.getProductsByCategory(category)

    fun getOrdersForClient(clientId: Int): Flow<List<OrderEntity>> =
        orderDao.getOrdersForClient(clientId)

    fun getOrdersForMerchant(merchantId: Int): Flow<List<OrderEntity>> =
        orderDao.getOrdersForMerchant(merchantId)

    fun getOrdersForDriver(driverId: Int): Flow<List<OrderEntity>> =
        orderDao.getOrdersForDriver(driverId)

    fun getMessagesForOrder(orderId: Int): Flow<List<ChatMessageEntity>> =
        chatDao.getMessagesForOrder(orderId)

    fun getTimelineForOrder(orderId: Int): Flow<List<OrderTimelineEntity>> =
        orderTimelineDao.getTimelineForOrder(orderId)

    fun getOffersForOrder(orderId: Int): Flow<List<PharmacyOfferEntity>> =
        pharmacyOfferDao.getOffersForOrder(orderId)

    fun getNotificationsForUser(userId: Int): Flow<List<NotificationEntity>> =
        notificationDao.getNotificationsForUser(userId)

    fun getUnreadNotificationCount(userId: Int): Flow<Int> =
        notificationDao.getUnreadCountForUser(userId)

    suspend fun getUserById(userId: Int): UserEntity? = userDao.getUserById(userId)
    suspend fun getUserByPhone(phone: String): UserEntity? = userDao.getUserByPhone(phone)

    suspend fun registerOrLoginUser(phone: String, role: String): UserEntity {
        val existing = userDao.getUserByPhone(phone)
        val defaultTenant = if (phone.endsWith("2") || phone.endsWith("5")) "tenant_عدن_صيدلة" else "tenant_صنعاء_وسط"
        
        if (existing != null) {
            val updated = existing.copy(role = role)
            userDao.updateUser(updated)
            return updated
        }
        val defaultBalance = when (role) {
            "driver" -> 6000.0 // Pre-charged motor drivers balance for commissions
            "merchant" -> 25000.0
            else -> 12000.0 // Clients
        }
        val inferredBusinessType = when (role) {
            "merchant" -> if (phone.endsWith("2")) "pharmacy" else "marketplace"
            "driver" -> "delivery"
            "admin" -> "admin"
            else -> "none"
        }
        val inferredDisplayName = when (inferredBusinessType) {
            "pharmacy" -> "صيدلية غير مكتملة البيانات"
            "marketplace" -> "تاجر سوق غير مكتمل البيانات"
            "delivery" -> "كابتن توصيل"
            "admin" -> "مدير النظام"
            else -> "عميل المجمع"
        }
        val newUser = UserEntity(
            tenantId = defaultTenant,
            phone = phone,
            role = role,
            walletBalance = defaultBalance,
            status = "active",
            fullName = inferredDisplayName,
            displayName = inferredDisplayName,
            businessType = inferredBusinessType,
            businessName = if (role == "merchant") inferredDisplayName else "",
            contactPhone = phone,
            city = if (defaultTenant.contains("عدن")) "عدن" else "صنعاء",
            district = "",
            address = "",
            licenseNumber = "",
            merchantCategory = if (inferredBusinessType == "pharmacy") "pharmacy" else if (inferredBusinessType == "marketplace") "clothes" else "",
            approvalStatus = "incomplete",
            isProfileComplete = false
        )
        val id = userDao.insertUser(newUser).toInt()

        // Write introductory ledger record (Infrastructural Double Entry)
        ledgerEntryDao.insertLedgerEntry(
            LedgerEntryEntity(
                tenantId = defaultTenant,
                debitWalletId = id,
                creditWalletId = -1, // Sourced externally
                amount = defaultBalance,
                narrative = "رصيد افتتاح المحفظة التأسيسي"
            )
        )

        // Enqueue Outbox event to notify centralized platform of login
        enqueueOutboxEvent(defaultTenant, "USER_REGISTERED_AUTH", "{ userId: $id, phone: '$phone', role: '$role' }")

        return newUser.copy(id = id)
    }

    /**
     * Helper to write Centralized Outbox System events securely
     */
    private suspend fun enqueueOutboxEvent(tenantId: String, eventType: String, payload: String) {
        outboxEventDao.insertEvent(
            OutboxEventEntity(
                tenantId = tenantId,
                eventType = eventType,
                payload = payload,
                status = "pending",
                attempts = 0,
                lastError = null
            )
        )
    }


    private suspend fun appendTimeline(
        order: OrderEntity,
        actorRole: String,
        title: String,
        note: String,
        statusSnapshot: String = order.status
    ) {
        orderTimelineDao.insertTimeline(
            OrderTimelineEntity(
                tenantId = order.tenantId,
                orderId = order.id,
                actorRole = actorRole,
                title = title,
                note = note,
                statusSnapshot = statusSnapshot
            )
        )
    }

    private suspend fun notifyUser(
        tenantId: String,
        userId: Int?,
        orderId: Int?,
        title: String,
        body: String,
        severity: String = "info"
    ) {
        if (userId == null || userId <= 0) return
        notificationDao.insertNotification(
            NotificationEntity(
                tenantId = tenantId,
                userId = userId,
                orderId = orderId,
                title = title,
                body = body,
                severity = severity
            )
        )
    }

    suspend fun markNotificationsRead(userId: Int) {
        notificationDao.markAllRead(userId)
    }

    suspend fun updateUserProfile(
        userId: Int,
        fullName: String,
        displayName: String,
        businessType: String,
        businessName: String,
        responsibleName: String,
        contactPhone: String,
        city: String,
        district: String,
        address: String,
        gpsLatitude: Double,
        gpsLongitude: Double,
        licenseNumber: String,
        licenseImageUri: String,
        workingHours: String,
        deliversOrders: Boolean,
        serviceRadiusKm: Int,
        merchantCategory: String,
        deliveryPolicy: String,
        vehicleType: String,
        vehiclePlate: String
    ) {
        val user = userDao.getUserById(userId) ?: return
        val cleanFullName = fullName.trim()
        val cleanDisplayName = displayName.trim().ifBlank { cleanFullName.ifBlank { user.phone } }
        val cleanBusinessName = businessName.trim()
        val cleanResponsibleName = responsibleName.trim()
        val cleanContactPhone = contactPhone.trim().ifBlank { user.phone }
        val cleanCity = city.trim()
        val cleanDistrict = district.trim()
        val cleanAddress = address.trim()
        val cleanLicenseNumber = licenseNumber.trim()
        val cleanLicenseImageUri = licenseImageUri.trim()
        val cleanWorkingHours = workingHours.trim()
        val cleanMerchantCategory = merchantCategory.trim()
        val cleanDeliveryPolicy = deliveryPolicy.trim()
        val cleanVehicleType = vehicleType.trim()
        val cleanVehiclePlate = vehiclePlate.trim()
        val safeBusinessType = when (user.role) {
            "merchant" -> if (businessType == "pharmacy") "pharmacy" else "marketplace"
            "driver" -> "delivery"
            "admin" -> "admin"
            else -> "none"
        }
        val safeServiceRadius = serviceRadiusKm.coerceIn(0, 300)
        val profileComplete = when (user.role) {
            "client" -> cleanFullName.isNotBlank() && cleanCity.isNotBlank() && cleanDistrict.isNotBlank() && cleanAddress.isNotBlank()
            "merchant" -> {
                val baseMerchantOk = cleanFullName.isNotBlank() && cleanResponsibleName.isNotBlank() &&
                    cleanBusinessName.isNotBlank() && cleanCity.isNotBlank() && cleanDistrict.isNotBlank() && cleanAddress.isNotBlank()
                if (safeBusinessType == "pharmacy") {
                    baseMerchantOk && cleanLicenseNumber.isNotBlank() && cleanWorkingHours.isNotBlank()
                } else {
                    baseMerchantOk && cleanMerchantCategory.isNotBlank() && cleanDeliveryPolicy.isNotBlank()
                }
            }
            "driver" -> cleanFullName.isNotBlank() && cleanContactPhone.isNotBlank() && cleanCity.isNotBlank() &&
                cleanDistrict.isNotBlank() && cleanVehicleType.isNotBlank() && cleanVehiclePlate.isNotBlank()
            "admin" -> cleanFullName.isNotBlank()
            else -> false
        }
        val approvalStatus = when {
            user.role == "admin" || user.role == "client" -> "approved"
            profileComplete && user.approvalStatus == "approved" -> "approved"
            profileComplete && user.approvalStatus == "suspended" -> "suspended"
            profileComplete -> "pending"
            else -> "incomplete"
        }

        userDao.updateUserProfile(
            id = userId,
            fullName = cleanFullName,
            displayName = cleanDisplayName,
            businessType = safeBusinessType,
            businessName = if (user.role == "merchant") cleanBusinessName else "",
            responsibleName = cleanResponsibleName,
            contactPhone = cleanContactPhone,
            city = cleanCity,
            district = cleanDistrict,
            address = cleanAddress,
            gpsLatitude = gpsLatitude,
            gpsLongitude = gpsLongitude,
            licenseNumber = cleanLicenseNumber,
            licenseImageUri = cleanLicenseImageUri,
            workingHours = cleanWorkingHours,
            deliversOrders = deliversOrders,
            serviceRadiusKm = safeServiceRadius,
            merchantCategory = cleanMerchantCategory,
            deliveryPolicy = cleanDeliveryPolicy,
            vehicleType = cleanVehicleType,
            vehiclePlate = cleanVehiclePlate,
            approvalStatus = approvalStatus,
            isProfileComplete = profileComplete
        )

        if (user.role == "merchant" && safeBusinessType == "pharmacy") {
            val existingVerification = pharmacyVerificationDao.getVerificationForMerchant(userId)
            val verification = PharmacyVerificationEntity(
                id = existingVerification?.id ?: 0,
                tenantId = user.tenantId,
                merchantId = userId,
                pharmacyName = cleanBusinessName.ifBlank { cleanDisplayName.ifBlank { "صيدلية بدون اسم" } },
                licenseNumber = cleanLicenseNumber.ifBlank { "PENDING-DOC" },
                city = cleanCity.ifBlank { "غير محدد" },
                status = existingVerification?.status ?: if (profileComplete) "pending" else "incomplete",
                rejectionReason = existingVerification?.rejectionReason ?: ""
            )
            pharmacyVerificationDao.insertVerification(verification)
        }

        enqueueOutboxEvent(
            user.tenantId,
            "USER_PROFILE_UPDATED",
            "{ userId: $userId, role: '${user.role}', businessType: '$safeBusinessType', complete: $profileComplete, approval: '$approvalStatus' }"
        )
    }

    /**
     * Step 1 of Escrow: Client submits order, funds are Frozen.
     * Rewritten to enforce ledger double entries instead of directly mutating fields!
     */
    suspend fun createOrder(
        clientId: Int,
        merchantId: Int?,
        driverId: Int?,
        productName: String,
        category: String,
        totalPrice: Double,
        deliveryFee: Double,
        paymentMethod: String, // "wallet", "cash"
        prescriptionAttached: Boolean = false
    ): Int {
        val mode = connectionManager.networkMode.value
        val client = userDao.getUserById(clientId) ?: return -1
        val tenantId = client.tenantId
        val releaseCode = (1000..9999).random().toString()
        val isMeshSigned = mode == NetworkMode.OFFLINE_MESH
        val isReversePharmacyRequest = category == "medicine" && merchantId == null && totalPrice <= 0.0
        val shouldSimulateConflict = !isReversePharmacyRequest && (mode == NetworkMode.OFFLINE_MESH || mode == NetworkMode.ZERO_DATA) && Random.nextBoolean()

        val initialStatus = when {
            shouldSimulateConflict -> "CONFLICT"
            isReversePharmacyRequest -> "waiting_offers"
            paymentMethod == "wallet" && !isMeshSigned && totalPrice + deliveryFee > 0.0 -> "funds_frozen"
            else -> "pending"
        }

        val conflictPrice = if (shouldSimulateConflict) totalPrice + 700.0 else totalPrice

        val order = OrderEntity(
            tenantId = tenantId,
            clientId = clientId,
            merchantId = merchantId,
            driverId = driverId,
            productName = productName,
            category = category,
            totalPrice = totalPrice,
            deliveryFee = deliveryFee,
            commissionAmount = 0.0,
            status = initialStatus,
            otpReleaseCode = releaseCode,
            isLocalMeshSigned = isMeshSigned,
            originalPriceAtRequest = totalPrice,
            serverUpdatedPriceConflict = conflictPrice
        )

        val orderId = orderDao.insertOrder(order).toInt()
        val persistedOrder = order.copy(id = orderId)

        if (category == "medicine") {
            prescriptionDao.insertPrescription(
                PrescriptionEntity(
                    tenantId = tenantId,
                    orderId = orderId,
                    clientId = clientId,
                    medicineName = productName,
                    isRequired = true,
                    hasAttachment = prescriptionAttached,
                    status = if (prescriptionAttached) "uploaded" else "required"
                )
            )
        }

        when {
            initialStatus == "funds_frozen" && paymentMethod == "wallet" -> {
                freezeEscrowFundsInLedger(client, orderId, totalPrice + deliveryFee)
                appendTimeline(persistedOrder, "system", "تجميد الضمان", "تم تجميد مبلغ ${totalPrice + deliveryFee} ريال حتى تأكيد التسليم.", initialStatus)
            }
            initialStatus == "CONFLICT" -> {
                chatDao.insertMessage(
                    ChatMessageEntity(
                        orderId = orderId,
                        senderRole = "system",
                        senderName = "الضامِن المالي",
                        messageText = "⚠️ تعارض السعر: السعر القديم للطلب هو $totalPrice ريال. السعر المحدث على الخادم حالياً هو $conflictPrice ريال. الرجاء الموافقة على السعر الجديد للتثبيت والخصم."
                    )
                )
                appendTimeline(persistedOrder, "system", "تعارض سعر", "تم تعليق الالتزام المالي حتى يوافق العميل على السعر الجديد.", initialStatus)
            }
            initialStatus == "waiting_offers" -> {
                appendTimeline(persistedOrder, "client", "طلب دواء جديد", "تم بث طلب الدواء للصيدليات القريبة، ولا يتم تجميد أي مبلغ قبل وصول عرض صيدلية.", initialStatus)
            }
            else -> {
                appendTimeline(persistedOrder, "client", "إنشاء طلب", "تم إنشاء الطلب وحجز غرفة تفاوض آمنة.", initialStatus)
            }
        }

        chatDao.insertMessage(
            ChatMessageEntity(
                orderId = orderId,
                senderRole = "system",
                senderName = "النظام المالي المحكم",
                messageText = when (initialStatus) {
                    "waiting_offers" -> "تم استقبال طلب الدواء. سيتم عرض عروض الصيدليات هنا، ولا يتم تجميد الضمان إلا بعد قبول عرض محدد."
                    "funds_frozen" -> "تم إنشاء الطلب وتجميد الضمان المالي. كود التسليم السري هو: $releaseCode"
                    "CONFLICT" -> "الطلب يحتاج موافقة سعر جديدة قبل التجميد."
                    else -> "تم إنشاء الطلب. تابع التفاوض من هذه الغرفة. كود التسليم: $releaseCode"
                }
            )
        )

        notifyUser(tenantId, clientId, orderId, "تم إنشاء الطلب", "الحالة الحالية: $initialStatus", if (initialStatus == "CONFLICT") "warning" else "success")
        enqueueOutboxEvent(tenantId, "ORDER_CREATED", "{ orderId: $orderId, status: '$initialStatus', totalPrice: $totalPrice }")
        return orderId
    }

    private suspend fun freezeEscrowFundsInLedger(client: UserEntity, orderId: Int, requiredAmount: Double) {
        val escrowWalletId = -99 // System Escrow Pool ID
        
        // Write double-entry ledger debiting client bank, crediting system escrow
        ledgerEntryDao.insertLedgerEntry(
            LedgerEntryEntity(
                tenantId = client.tenantId,
                debitWalletId = escrowWalletId, // Inflows go to system escrow pool
                creditWalletId = client.id,     // Outflow from client wallet
                amount = requiredAmount,
                orderId = orderId,
                narrative = "تجميد ضمان مالي برسم المعاملة #$orderId"
            )
        )

        // Recalculate and synchronize User balance cache safely
        // Comment: "Future scalability note: Beyond 500k daily records, current_balance is updated atomically with audit checks."
        val correctedBalance = ledgerEntryDao.calculateWalletBalance(client.id)
        userDao.updateWalletBalance(client.id, correctedBalance)

        transactionDao.insertTransaction(
            TransactionEntity(
                userId = client.id,
                orderId = orderId,
                type = "hold",
                amount = requiredAmount,
                providerName = "رصيد معلق برسم التوصيل"
            )
        )
    }

    /**
     * Resolves the "CONFLICT" status gracefully when client clicks "Approve New Price" Dialog.
     * Re-adjusts pricing, processes ledger hold, and generates outbox sync log.
     */
    suspend fun resolvePriceConflictAndApprove(orderId: Int): Boolean {
        val order = orderDao.getOrderById(orderId) ?: return false
        if (order.status != "CONFLICT") return false

        val client = userDao.getUserById(order.clientId) ?: return false
        val newPrice = order.serverUpdatedPriceConflict
        val totalDebitAmount = newPrice + order.deliveryFee

        // Deduct/hold ledger funds
        freezeEscrowFundsInLedger(client, orderId, totalDebitAmount)

        // Persist local order update
        val updatedOrder = order.copy(
            status = "funds_frozen",
            totalPrice = newPrice
        )
        orderDao.updateOrder(updatedOrder)

        // Create Chat notice
        chatDao.insertMessage(
            ChatMessageEntity(
                orderId = orderId,
                senderRole = "system",
                senderName = "الضامِن المحاسبي",
                messageText = "تمت الموافقة من قبلكم على السعر المعدل من السيرفر بقيمة $newPrice ريال بنجاح. وتم تجميد رصيد الضمان بالخصم Ledger-Based."
            )
        )

        appendTimeline(updatedOrder, "client", "اعتماد السعر الجديد", "وافق العميل على السعر المحدث وتم تجميد الضمان.", "funds_frozen")
        notifyUser(order.tenantId, order.clientId, orderId, "تم تثبيت السعر", "تم تجميد ${totalDebitAmount} ريال بعد قبول السعر المعدل.", "success")

        // Sync Outbox
        enqueueOutboxEvent(order.tenantId, "ORDER_PRICE_CONFLICT_RESOLVED", "{ orderId: $orderId, clearedPrice: $newPrice }")
        return true
    }

    /**
     * Step 2 of Escrow: Release code confirmed, disperse the funds minus the commissions using Ledger entries.
     */
    suspend fun releaseOrderEscrow(orderId: Int, enteredCode: String): Boolean {
        val order = orderDao.getOrderById(orderId) ?: return false
        if (order.status == "completed") return true // Already completed

        if (order.otpReleaseCode != enteredCode) {
            return false // Validation failed
        }

        val updatedOrder = order.copy(status = "completed")
        orderDao.updateOrder(updatedOrder)
        appendTimeline(updatedOrder, "system", "إغلاق الطلب", "تم إدخال كود التسليم وتحرير الضمان حسب دفتر القيود.", "completed")

        val escrowPoolWalletId = -99

        // Process Financial Releases directly in Double-Entry Ledger and synchronize cached balances
        if (order.merchantId != null) {
            val commission = order.totalPrice * 0.02
            val payoutAmount = order.totalPrice - commission

            val merchant = userDao.getUserById(order.merchantId)
            if (merchant != null) {
                // Charge escrow pool, pay merchant wallet
                ledgerEntryDao.insertLedgerEntry(
                    LedgerEntryEntity(
                        tenantId = order.tenantId,
                        debitWalletId = merchant.id,
                        creditWalletId = escrowPoolWalletId,
                        amount = payoutAmount,
                        orderId = orderId,
                        narrative = "صرف مستحقات سلع طلب الصيدلية #$orderId بعد خصم عمولة مجمع 2%"
                    )
                )

                // Update cached balance
                val correctedMerchBalance = ledgerEntryDao.calculateWalletBalance(merchant.id)
                userDao.updateWalletBalance(merchant.id, correctedMerchBalance)

                transactionDao.insertTransaction(
                    TransactionEntity(
                        userId = order.merchantId,
                        orderId = orderId,
                        type = "release",
                        amount = payoutAmount,
                        providerName = "محفظة مجمع الأرباح"
                    )
                )
            }
        }

        if (order.driverId != null) {
            val driverCommission = order.deliveryFee * 0.10
            val driver = userDao.getUserById(order.driverId)
            if (driver != null) {
                val netDriverPayout = order.deliveryFee - driverCommission

                // Ledger transfer from Escrow pool to Driver
                ledgerEntryDao.insertLedgerEntry(
                    LedgerEntryEntity(
                        tenantId = order.tenantId,
                        debitWalletId = driver.id,
                        creditWalletId = escrowPoolWalletId,
                        amount = netDriverPayout,
                        orderId = orderId,
                        narrative = "تحرير مستحقات مشوار التوصيل للطلب #$orderId"
                    )
                )

                // Update driver cache balance
                val correctedDrvBal = ledgerEntryDao.calculateWalletBalance(driver.id)
                userDao.updateWalletBalance(driver.id, correctedDrvBal)

                transactionDao.insertTransaction(
                    TransactionEntity(
                        userId = order.driverId,
                        orderId = orderId,
                        type = "release",
                        amount = order.deliveryFee,
                        providerName = "تفريغ مستحقات مشوار طرود"
                    )
                )
            }
        }

        chatDao.insertMessage(
            ChatMessageEntity(
                orderId = orderId,
                senderRole = "system",
                senderName = "الضامِن المالي",
                messageText = "تم إدخال كود التحرير بنجاح. تم فك تجميد الأموال للتاجر/السائق وتمرير الأرباح وتطبيق العمولات التنظيمية بنجاح."
            )
        )

        notifyUser(order.tenantId, order.clientId, orderId, "تم إغلاق الطلب", "تم تأكيد الاستلام وتحرير الضمان بنجاح.", "success")
        notifyUser(order.tenantId, order.merchantId, orderId, "تم تحرير مستحقاتك", "تم تحويل صافي المستحقات من الضمان إلى محفظتك.", "success")
        notifyUser(order.tenantId, order.driverId, orderId, "تم تحرير مستحقات التوصيل", "تم إثبات التسليم وتحرير صافي التوصيل.", "success")

        // Queue Outbox Sync Log
        enqueueOutboxEvent(order.tenantId, "ORDER_ESCROW_RELEASED", "{ orderId: $orderId, enteredCode: '$enteredCode' }")

        return true
    }

    /**
     * Local driver accepts order & reserves commission from balance.
     */
    suspend fun acceptOrderAsDriver(orderId: Int, driverId: Int) {
        val order = orderDao.getOrderById(orderId) ?: return
        val driver = userDao.getUserById(driverId) ?: return

        val commission = order.deliveryFee * 0.10
        if (driver.walletBalance >= commission) {
            val updatedOrder = order.copy(
                driverId = driverId,
                status = if (order.status == "ready" || order.status == "funds_frozen") "delivering" else order.status,
                commissionAmount = commission
            )
            orderDao.updateOrder(updatedOrder)
            appendTimeline(updatedOrder, "driver", "قبول التوصيل", "قبل السائق الطلب وتم حجز تأمين العمولة.", updatedOrder.status)

            // Double Entry: Transfer driver's deposit to Escrow hold system
            ledgerEntryDao.insertLedgerEntry(
                LedgerEntryEntity(
                    tenantId = driver.tenantId,
                    debitWalletId = -99, // Escrow pool
                    creditWalletId = driverId, // Driver out-booking
                    amount = commission,
                    orderId = orderId,
                    narrative = "خصم تأمين عمولة المشوار المعلق #$orderId"
                )
            )

            // Synchronize cache
            val correctedDrvBal = ledgerEntryDao.calculateWalletBalance(driverId)
            userDao.updateWalletBalance(driverId, correctedDrvBal)

            transactionDao.insertTransaction(
                TransactionEntity(
                    userId = driverId,
                    orderId = orderId,
                    type = "hold",
                    amount = commission,
                    providerName = "تأمين عمولة مشوار معلق"
                )
            )

            chatDao.insertMessage(
                ChatMessageEntity(
                    orderId = orderId,
                    senderRole = "driver",
                    senderName = "السائق: ${driver.phone.takeLast(4)}",
                    messageText = "تم قبول التوصيل، أنا منطلق إليك الآن! الرجاء تجهيز كود الإفراج عن المبلغ لتأكيده عند الوصول."
                )
            )

            notifyUser(order.tenantId, order.clientId, orderId, "السائق قبل الطلب", "السائق في الطريق، لا تعطِ كود التسليم إلا عند الاستلام.", "info")
            notifyUser(order.tenantId, order.merchantId, orderId, "تم تعيين سائق", "السائق قبل التوصيل للطلب #$orderId.", "info")

            // Sync outbox
            enqueueOutboxEvent(driver.tenantId, "ORDER_ACCEPTED_BY_DRIVER", "{ orderId: $orderId, driverId: $driverId }")
        }
    }

    suspend fun merchantOfferPriceForMedication(
        orderId: Int,
        merchantId: Int,
        proposedPrice: Double,
        availableQuantity: Int = 1,
        preparationMinutes: Int = 30,
        note: String = "",
        alternativeMedicineName: String = "",
        expiryDateText: String = "غير محدد"
    ) {
        val order = orderDao.getOrderById(orderId) ?: return
        val merchant = userDao.getUserById(merchantId) ?: return
        if (order.category != "medicine" || order.status in listOf("completed", "cancelled", "refunded")) return
        if (merchant.businessType != "pharmacy") {
            notifyUser(order.tenantId, merchantId, orderId, "طلب دواء محجوب", "هذا الحساب ليس صيدلية، ولا يسمح له بالتسعير على طلبات الدواء.", "danger")
            return
        }

        val verification = pharmacyVerificationDao.getVerificationForMerchant(merchantId)
        if (verification != null && verification.status != "approved") {
            notifyUser(order.tenantId, merchantId, orderId, "لا يمكن إرسال العرض", "حساب الصيدلية غير معتمد بعد. أكمل اعتماد الترخيص أولاً.", "danger")
            return
        }

        val offerId = pharmacyOfferDao.insertOffer(
            PharmacyOfferEntity(
                tenantId = order.tenantId,
                orderId = orderId,
                merchantId = merchantId,
                price = proposedPrice,
                availableQuantity = availableQuantity,
                preparationMinutes = preparationMinutes,
                note = note,
                alternativeMedicineName = alternativeMedicineName,
                expiryDateText = expiryDateText,
                status = "offered"
            )
        ).toInt()

        val updatedOrder = order.copy(
            merchantId = merchantId,
            totalPrice = proposedPrice,
            status = "offer_received"
        )
        orderDao.updateOrder(updatedOrder)
        appendTimeline(updatedOrder, "merchant", "عرض صيدلية", "تم تقديم عرض رقم #$offerId بقيمة $proposedPrice ريال. الكمية: $availableQuantity، التجهيز: $preparationMinutes دقيقة.", "offer_received")

        chatDao.insertMessage(
            ChatMessageEntity(
                orderId = orderId,
                senderRole = "merchant",
                senderName = "الصيدلي: ${merchant.phone.takeLast(4)}",
                messageText = buildString {
                    append("الدواء متوفر. السعر المقترح: $proposedPrice ريال. الكمية المتاحة: $availableQuantity. وقت التجهيز: $preparationMinutes دقيقة.")
                    if (alternativeMedicineName.isNotBlank()) append(" البديل المقترح: $alternativeMedicineName.")
                    if (expiryDateText.isNotBlank()) append(" الصلاحية: $expiryDateText.")
                    if (note.isNotBlank()) append(" ملاحظة: $note")
                }
            )
        )

        notifyUser(order.tenantId, order.clientId, orderId, "وصل عرض صيدلية", "عرض بقيمة $proposedPrice ريال. راجعه واقبل التجميد إن كان مناسباً.", "success")
        enqueueOutboxEvent(order.tenantId, "ORDER_MEDICATION_BID", "{ orderId: $orderId, merchantId: $merchantId, proposal: $proposedPrice, offerId: $offerId }")
    }

    /**
     * Client-side approval for a merchant medication quote.
     * The previous flow stopped at "pending" after the pharmacy submitted a price,
     * so the customer had no UI-backed action to freeze funds and commit the order.
     */
    suspend fun acceptMerchantQuoteAndFreezeEscrow(orderId: Int): Boolean {
        val order = orderDao.getOrderById(orderId) ?: return false
        if (order.category != "medicine" || order.merchantId == null || order.totalPrice <= 0.0) return false
        if (order.status in listOf("funds_frozen", "preparing", "ready", "delivering", "completed")) return true

        val client = userDao.getUserById(order.clientId) ?: return false
        val requiredAmount = order.totalPrice + order.deliveryFee
        val offer = pharmacyOfferDao.getLatestActiveOfferForOrder(orderId)

        freezeEscrowFundsInLedger(client, orderId, requiredAmount)
        if (offer != null) {
            pharmacyOfferDao.updateOffer(offer.copy(status = "accepted"))
        }
        val updatedOrder = order.copy(status = "funds_frozen")
        orderDao.updateOrder(updatedOrder)
        appendTimeline(updatedOrder, "client", "قبول العرض وتجميد الضمان", "وافق العميل على عرض الصيدلية بقيمة ${order.totalPrice} ريال وتم تجميد إجمالي $requiredAmount ريال.", "funds_frozen")

        chatDao.insertMessage(
            ChatMessageEntity(
                orderId = orderId,
                senderRole = "system",
                senderName = "الضامِن المالي",
                messageText = "وافق العميل على عرض الصيدلية بقيمة ${order.totalPrice} ريال. تم تجميد إجمالي ${requiredAmount} ريال في الضمان المالي حتى تأكيد التسليم بالكود."
            )
        )

        notifyUser(order.tenantId, order.clientId, orderId, "تم تجميد الضمان", "تم تجميد $requiredAmount ريال للطلب #$orderId.", "success")
        notifyUser(order.tenantId, order.merchantId, orderId, "العميل قبل عرضك", "ابدأ تجهيز الطلب ثم حدد جاهز للتسليم.", "success")
        enqueueOutboxEvent(order.tenantId, "ORDER_MEDICATION_QUOTE_ACCEPTED", "{ orderId: $orderId, merchantId: ${order.merchantId}, lockedAmount: $requiredAmount }")
        return true
    }

    /**
     * Pharmacy cannot fulfil a broadcast request.
     * We intentionally do not cancel the whole broadcast because another pharmacy may still bid.
     */
    suspend fun markMedicationRequestUnavailable(orderId: Int, merchantId: Int) {
        val order = orderDao.getOrderById(orderId) ?: return
        val merchant = userDao.getUserById(merchantId) ?: return
        if (order.category != "medicine" || order.status == "completed") return

        pharmacyOfferDao.insertOffer(
            PharmacyOfferEntity(
                tenantId = order.tenantId,
                orderId = orderId,
                merchantId = merchantId,
                price = 0.0,
                status = "unavailable",
                note = "غير متوفر لدى هذه الصيدلية"
            )
        )
        appendTimeline(order, "merchant", "اعتذار صيدلية", "الصيدلية ${merchant.phone.takeLast(4)} لا تملك الدواء حالياً. يبقى الطلب مفتوحاً لباقي الصيدليات.", order.status)

        chatDao.insertMessage(
            ChatMessageEntity(
                orderId = orderId,
                senderRole = "merchant",
                senderName = "الصيدلي: ${merchant.phone.takeLast(4)}",
                messageText = "نعتذر، هذا الدواء غير متوفر لدينا حالياً. أبقينا الطلب مفتوحاً لباقي الصيدليات القريبة حتى لا يتعطل العميل."
            )
        )

        notifyUser(order.tenantId, order.clientId, orderId, "رد صيدلية", "إحدى الصيدليات اعتذرت عن توفير الدواء؛ الطلب لا يزال مفتوحاً.", "warning")
        enqueueOutboxEvent(order.tenantId, "ORDER_MEDICATION_UNAVAILABLE", "{ orderId: $orderId, merchantId: $merchantId }")
    }

    suspend fun markOrderPreparing(orderId: Int, merchantId: Int): Boolean {
        val order = orderDao.getOrderById(orderId) ?: return false
        if (order.merchantId != merchantId || order.status !in listOf("funds_frozen", "ready")) return false
        val updated = order.copy(status = "preparing")
        orderDao.updateOrder(updated)
        appendTimeline(updated, "merchant", "بدء التجهيز", "الصيدلية بدأت تجهيز الطلب والتحقق من الوصفة والدفعة والصلاحية.", "preparing")
        notifyUser(order.tenantId, order.clientId, orderId, "الصيدلية بدأت التجهيز", "طلبك دخل مرحلة التحضير.", "info")
        enqueueOutboxEvent(order.tenantId, "ORDER_PREPARING", "{ orderId: $orderId, merchantId: $merchantId }")
        return true
    }

    suspend fun markOrderReady(orderId: Int, merchantId: Int): Boolean {
        val order = orderDao.getOrderById(orderId) ?: return false
        if (order.merchantId != merchantId || order.status !in listOf("funds_frozen", "preparing")) return false
        val updated = order.copy(status = "ready")
        orderDao.updateOrder(updated)
        appendTimeline(updated, "merchant", "جاهز للتسليم", "الطلب جاهز لدى الصيدلية. يمكن للسائق أو العميل استلامه بكود التسليم.", "ready")
        notifyUser(order.tenantId, order.clientId, orderId, "طلبك جاهز", "الطلب جاهز للتسليم. لا تشارك الكود قبل الاستلام.", "success")
        notifyUser(order.tenantId, order.driverId, orderId, "طلب جاهز للاستلام", "الطلب #$orderId جاهز لدى الصيدلية.", "info")
        enqueueOutboxEvent(order.tenantId, "ORDER_READY", "{ orderId: $orderId, merchantId: $merchantId }")
        return true
    }

    suspend fun cancelOrderAndRefund(orderId: Int, actorUserId: Int, reason: String): Boolean {
        val order = orderDao.getOrderById(orderId) ?: return false
        if (order.status in listOf("completed", "cancelled", "refunded")) return false
        val client = userDao.getUserById(order.clientId) ?: return false
        val shouldRefundEscrow = order.status in listOf("funds_frozen", "preparing", "ready", "delivering", "disputed")
        val refundAmount = order.totalPrice + order.deliveryFee
        if (shouldRefundEscrow && refundAmount > 0.0) {
            ledgerEntryDao.insertLedgerEntry(
                LedgerEntryEntity(
                    tenantId = order.tenantId,
                    debitWalletId = client.id,
                    creditWalletId = -99,
                    amount = refundAmount,
                    orderId = orderId,
                    narrative = "استرجاع ضمان للعميل بسبب إلغاء الطلب #$orderId: $reason"
                )
            )
            userDao.updateWalletBalance(client.id, ledgerEntryDao.calculateWalletBalance(client.id))
            transactionDao.insertTransaction(
                TransactionEntity(
                    userId = client.id,
                    orderId = orderId,
                    type = "refund",
                    amount = refundAmount,
                    providerName = "استرجاع من ضمان مجمع"
                )
            )
        }
        val updated = order.copy(status = if (shouldRefundEscrow) "refunded" else "cancelled")
        orderDao.updateOrder(updated)
        appendTimeline(updated, "system", "إلغاء/استرجاع", reason, updated.status)
        notifyUser(order.tenantId, order.clientId, orderId, "تم إغلاق الطلب", if (shouldRefundEscrow) "تم استرجاع $refundAmount ريال إلى محفظتك." else "تم إلغاء الطلب قبل التجميد.", "warning")
        notifyUser(order.tenantId, order.merchantId, orderId, "تم إلغاء الطلب", reason, "warning")
        enqueueOutboxEvent(order.tenantId, "ORDER_CANCELLED_OR_REFUNDED", "{ orderId: $orderId, actorUserId: $actorUserId, status: '${updated.status}' }")
        return true
    }

    suspend fun openDispute(orderId: Int, openedByUserId: Int, reason: String, details: String): Boolean {
        val order = orderDao.getOrderById(orderId) ?: return false
        if (order.status in listOf("completed", "cancelled", "refunded")) return false
        val disputeId = disputeDao.insertDispute(
            DisputeEntity(
                tenantId = order.tenantId,
                orderId = orderId,
                openedByUserId = openedByUserId,
                reason = reason,
                details = details,
                status = "open"
            )
        ).toInt()
        val updated = order.copy(status = "disputed")
        orderDao.updateOrder(updated)
        appendTimeline(updated, "system", "فتح نزاع", "سبب النزاع: $reason. التفاصيل: $details", "disputed")
        notifyUser(order.tenantId, order.clientId, orderId, "تم فتح نزاع", "تم تعليق الطلب لحين قرار الإدارة.", "danger")
        notifyUser(order.tenantId, order.merchantId, orderId, "نزاع على طلب", "تم فتح نزاع على الطلب #$orderId.", "danger")
        notifyUser(order.tenantId, order.driverId, orderId, "نزاع على طلب", "تم تعليق حالة الطلب #$orderId لحين قرار الإدارة.", "danger")
        enqueueOutboxEvent(order.tenantId, "ORDER_DISPUTE_OPENED", "{ disputeId: $disputeId, orderId: $orderId, reason: '$reason' }")
        return true
    }

    suspend fun resolveDispute(disputeId: Int, releaseToMerchant: Boolean, adminDecision: String): Boolean {
        val dispute = disputeDao.getDisputeById(disputeId) ?: return false
        if (dispute.status != "open") return false
        val order = orderDao.getOrderById(dispute.orderId) ?: return false
        val success = if (releaseToMerchant) {
            releaseOrderEscrow(order.id, order.otpReleaseCode)
        } else {
            cancelOrderAndRefund(order.id, dispute.openedByUserId, "قرار الإدارة في النزاع #$disputeId: $adminDecision")
        }
        if (!success) return false
        disputeDao.updateDispute(
            dispute.copy(
                status = if (releaseToMerchant) "release_merchant" else "refund_client",
                adminDecision = adminDecision,
                resolvedAt = System.currentTimeMillis()
            )
        )
        appendTimeline(order.copy(status = if (releaseToMerchant) "completed" else "refunded"), "admin", "قرار النزاع", adminDecision, if (releaseToMerchant) "completed" else "refunded")
        enqueueOutboxEvent(order.tenantId, "ORDER_DISPUTE_RESOLVED", "{ disputeId: $disputeId, releaseToMerchant: $releaseToMerchant }")
        return true
    }

    suspend fun approvePharmacyVerification(merchantId: Int): Boolean {
        val current = pharmacyVerificationDao.getVerificationForMerchant(merchantId) ?: return false
        pharmacyVerificationDao.updateVerification(current.copy(status = "approved", rejectionReason = ""))
        notifyUser(current.tenantId, merchantId, null, "تم اعتماد الصيدلية", "تم تفعيل حسابك لقبول طلبات الدواء وإرسال عروض أسعار.", "success")
        enqueueOutboxEvent(current.tenantId, "PHARMACY_VERIFICATION_APPROVED", "{ merchantId: $merchantId }")
        return true
    }

    suspend fun rejectPharmacyVerification(merchantId: Int, reason: String): Boolean {
        val current = pharmacyVerificationDao.getVerificationForMerchant(merchantId) ?: return false
        pharmacyVerificationDao.updateVerification(current.copy(status = "rejected", rejectionReason = reason))
        notifyUser(current.tenantId, merchantId, null, "رفض اعتماد الصيدلية", reason, "danger")
        enqueueOutboxEvent(current.tenantId, "PHARMACY_VERIFICATION_REJECTED", "{ merchantId: $merchantId, reason: '$reason' }")
        return true
    }

    suspend fun sendChatMessage(
        orderId: Int,
        senderRole: String,
        senderName: String,
        messageText: String,
        isVoiceNote: Boolean = false,
        durationSec: Int = 0
    ) {
        val mode = connectionManager.networkMode.value
        val fileSizeBytes = if (isVoiceNote) {
            durationSec * 600 // Extremely light Opus representation
        } else {
            0
        }

        val textPayload = if (isVoiceNote) "[رسالة صوتية مدمجة بحجم ${fileSizeBytes / 1000.0} ك.ب]" else messageText

        val msg = ChatMessageEntity(
            orderId = orderId,
            senderRole = senderRole,
            senderName = senderName,
            messageText = textPayload,
            isVoiceNote = isVoiceNote,
            voiceDurationSec = durationSec,
            voiceFileSizeBytes = fileSizeBytes
        )

        chatDao.insertMessage(msg)

        if (senderRole == "client") {
            simulateAutomatedReply(orderId, senderName, messageText, isVoiceNote)
        }
    }

    private suspend fun simulateAutomatedReply(
        orderId: Int,
        clientName: String,
        clientMsg: String,
        isVoice: Boolean
    ) {
        val order = orderDao.getOrderById(orderId) ?: return
        
        if (order.driverId != null) {
            val driverId = order.driverId
            val driver = userDao.getUserById(driverId)
            val driverPhone = driver?.phone ?: "5001"
            val replyText = when {
                isVoice -> "فهمتك يا غالي، جاري تعديل المسار والالتصاف مع الزحام. دقيقتين وبكون عندك إن شاء الله."
                clientMsg.contains("أين") || clientMsg.contains("وين") -> "أنا الآن في جولة ريحة، الخط فيه دبابات وزحام بسيط، دقيقتين إن شاء الله."
                clientMsg.contains("تخفيض") || clientMsg.contains("رخيص") -> "البترول غالي يا رفيق، لكن ما نختلف نوصل وبنفك الضمان المالي سوياً."
                else -> "تم المتابعة يا فندم، الصندوق مُحكم الإغلاق."
            }
            chatDao.insertMessage(
                ChatMessageEntity(
                    orderId = orderId,
                    senderRole = "driver",
                    senderName = "السائق تلقائي (${driverPhone.takeLast(4)})",
                    messageText = replyText
                )
            )
        } else if (order.merchantId != null) {
            val merchantId = order.merchantId
            val merchant = userDao.getUserById(merchantId)
            val replyText = when {
                isVoice -> "الروشتة واضحة ومخزنة، قمنا بمطابقتها مع الباقي وجاري التوضيب."
                clientMsg.contains("جاهز") -> "الطلب مبرشم ومطابق ومفرز مائة بالمائة."
                else -> "مرحباً بكم، تم الحفظ بالكامل في صيدليتنا."
            }
            chatDao.insertMessage(
                ChatMessageEntity(
                    orderId = orderId,
                    senderRole = "merchant",
                    senderName = "الصيدلي تلقائي (${merchant?.phone?.takeLast(4) ?: "6002"})",
                    messageText = replyText
                )
            )
        }
    }

    suspend fun rechargeWallet(userId: Int, amount: Double, provider: String) {
        val user = userDao.getUserById(userId) ?: return
        
        // Ledger debit user account, credit external wire partner
        ledgerEntryDao.insertLedgerEntry(
            LedgerEntryEntity(
                tenantId = user.tenantId,
                debitWalletId = user.id,
                creditWalletId = -1, // Criminal Wire الكريمي
                amount = amount,
                narrative = "إيداع وتغذية محفظة عبر $provider"
            )
        )

        // Cache synch
        val correctedBal = ledgerEntryDao.calculateWalletBalance(user.id)
        userDao.updateWalletBalance(user.id, correctedBal)

        transactionDao.insertTransaction(
            TransactionEntity(
                userId = userId,
                orderId = null,
                type = "credit",
                amount = amount,
                providerName = "إيداع عبر نظام $provider"
            )
        )

        enqueueOutboxEvent(user.tenantId, "WALLET_RECHARGED", "{ userId: $userId, amount: $amount, gateway: '$provider' }")
    }

    suspend fun createProduct(merchantId: Int, name: String, price: Double, category: String, location: String) {
        val merchant = userDao.getUserById(merchantId)
        val tenant = merchant?.tenantId ?: "tenant_صنعاء_وسط"
        val safeCategory = when {
            merchant?.businessType == "pharmacy" -> "medicine"
            category == "medicine" -> "clothing"
            category in listOf("clothing", "wholesale", "general_market", "influencer") -> category
            else -> "general_market"
        }

        val product = ProductEntity(
            tenantId = tenant,
            merchantId = merchantId,
            name = name,
            price = price,
            category = safeCategory,
            locationName = location,
            batchNumber = if (safeCategory == "medicine") "B-SANA-${(10..99).random()}" else "NON-MED",
            expiryTimestamp = if (safeCategory == "medicine") System.currentTimeMillis() + (180 * 24 * 60 * 60 * 1000L) else 0L,
            purchaseCost = if (safeCategory == "medicine") price * 0.75 else 0.0,
            isRecalled = false,
            totalStock = (20..200).random()
        )
        productDao.insertProduct(product)

        enqueueOutboxEvent(tenant, "PRODUCT_CREATED", "{ name: '$name', price: $price, category: '$safeCategory' }")
    }

    suspend fun createProductFull(
        merchantId: Int,
        name: String,
        price: Double,
        category: String,
        location: String,
        batchNumber: String,
        expiryTimestamp: Long,
        purchaseCost: Double,
        totalStock: Int
    ) {
        val merchant = userDao.getUserById(merchantId)
        val tenant = merchant?.tenantId ?: "tenant_صنعاء_وسط"

        val safeCategory = if (merchant?.businessType == "pharmacy") "medicine" else category

        val product = ProductEntity(
            tenantId = tenant,
            merchantId = merchantId,
            name = name,
            price = price,
            category = safeCategory,
            locationName = location,
            batchNumber = batchNumber,
            expiryTimestamp = expiryTimestamp,
            purchaseCost = purchaseCost,
            isRecalled = false,
            totalStock = totalStock
        )
        productDao.insertProduct(product)
        enqueueOutboxEvent(tenant, "PRODUCT_CREATED", "{ name: '$name', price: $price, batch: '$batchNumber' }")
    }

    suspend fun toggleProductRecall(productId: Int) {
        val all = productDao.getAllProducts().first()
        val p = all.find { it.id == productId } ?: return
        val updated = p.copy(isRecalled = !p.isRecalled)
        productDao.insertProduct(updated)
        enqueueOutboxEvent(p.tenantId, "PRODUCT_RECALL_TOGGLE", "{ productId: $productId, isRecalled: ${updated.isRecalled} }")
    }

    suspend fun seedDatabaseIfEmpty() {
        val users = userDao.getAllUsers().first()
        if (users.isEmpty()) {
            // Seed Clients
            val clientId = userDao.insertUser(
                UserEntity(tenantId = "tenant_صنعاء_وسط", phone = "770000001", role = "client", walletBalance = 0.0, status = "active", fullName = "أحمد بن علي", displayName = "أحمد بن علي", businessType = "none", contactPhone = "770000001", city = "صنعاء", district = "التحرير", address = "خلف مدرسة بلقيس", approvalStatus = "approved", isProfileComplete = true)
            ).toInt()

            // Seed Merchants
            val pharmacyId = userDao.insertUser(
                UserEntity(tenantId = "tenant_عدن_صيدلة", phone = "770000002", role = "merchant", walletBalance = 0.0, status = "active", fullName = "صيدلي المناوبة", displayName = "صيدلية اليمن الكبرى", businessType = "pharmacy", businessName = "صيدلية اليمن الكبرى", responsibleName = "صيدلي المناوبة", contactPhone = "770000002", city = "عدن - كريتر", district = "كريتر", address = "كريتر - الشارع الرئيسي", licenseNumber = "PH-YE-2026-3302", workingHours = "09:00 - 22:00", deliversOrders = true, serviceRadiusKm = 8, merchantCategory = "pharmacy", approvalStatus = "approved", isProfileComplete = true)
            ).toInt()

            val boutiqueId = userDao.insertUser(
                UserEntity(tenantId = "tenant_صنعاء_وسط", phone = "770000003", role = "merchant", walletBalance = 0.0, status = "active", fullName = "تاجر سوق الجملة", displayName = "بوتيك المجمع اليمني", businessType = "marketplace", businessName = "بوتيك المجمع اليمني", responsibleName = "تاجر سوق الجملة", contactPhone = "770000003", city = "صنعاء", district = "باب اليمن", address = "سوق الجملة - باب اليمن", licenseNumber = "MARKET-LOCAL-3303", deliversOrders = true, serviceRadiusKm = 6, merchantCategory = "clothes", deliveryPolicy = "توصيل داخل صنعاء أو استلام من المتجر", approvalStatus = "approved", isProfileComplete = true)
            ).toInt()

            // Seed Driver
            val driverId = userDao.insertUser(
                UserEntity(tenantId = "tenant_صنعاء_وسط", phone = "770000004", role = "driver", walletBalance = 0.0, status = "active", fullName = "أبو رعد", displayName = "السائق أبو رعد", businessType = "delivery", contactPhone = "770000004", city = "صنعاء", district = "صنعاء القديمة", address = "نطاق صنعاء القديمة", vehicleType = "دراجة", vehiclePlate = "YEM-3304", approvalStatus = "approved", isProfileComplete = true)
            ).toInt()

            val adminId = userDao.insertUser(
                UserEntity(tenantId = "tenant_صنعاء_وسط", phone = "770000009", role = "admin", walletBalance = 0.0, status = "active", fullName = "إدارة المجمع", displayName = "مدير النظام", businessType = "admin", contactPhone = "770000009", city = "صنعاء", district = "الإدارة", approvalStatus = "approved", isProfileComplete = true)
            ).toInt()

            pharmacyVerificationDao.insertVerification(
                PharmacyVerificationEntity(
                    tenantId = "tenant_عدن_صيدلة",
                    merchantId = pharmacyId,
                    pharmacyName = "صيدلية اليمن الكبرى",
                    licenseNumber = "PH-YE-2026-3302",
                    city = "عدن - كريتر",
                    status = "approved"
                )
            )
            pharmacyVerificationDao.insertVerification(
                PharmacyVerificationEntity(
                    tenantId = "tenant_صنعاء_وسط",
                    merchantId = boutiqueId,
                    pharmacyName = "ملف تاجر غير صيدلي - يحتاج مراجعة",
                    licenseNumber = "PENDING-DOC",
                    city = "صنعاء",
                    status = "pending"
                )
            )

            // Seed Initial Ledgings for correctness
            ledgerEntryDao.insertLedgerEntry(LedgerEntryEntity(tenantId = "tenant_صنعاء_وسط", debitWalletId = clientId, creditWalletId = -1, amount = 12500.0, narrative = "رصيد تأسيسي للزبون"))
            ledgerEntryDao.insertLedgerEntry(LedgerEntryEntity(tenantId = "tenant_عدن_صيدلة", debitWalletId = pharmacyId, creditWalletId = -1, amount = 150000.0, narrative = "رصيد تأسيسي للصيدلية الكبرى"))
            ledgerEntryDao.insertLedgerEntry(LedgerEntryEntity(tenantId = "tenant_صنعاء_وسط", debitWalletId = boutiqueId, creditWalletId = -1, amount = 40000.0, narrative = "رصيد تأسيسي لمحرِّر الملابس"))
            ledgerEntryDao.insertLedgerEntry(LedgerEntryEntity(tenantId = "tenant_صنعاء_وسط", debitWalletId = driverId, creditWalletId = -1, amount = 7500.0, narrative = "رصيد تأسيسي للكابتن"))
            ledgerEntryDao.insertLedgerEntry(LedgerEntryEntity(tenantId = "tenant_صنعاء_وسط", debitWalletId = adminId, creditWalletId = -1, amount = 0.0, narrative = "حساب إدارة بدون محفظة تشغيلية"))

            // Synchronize displays with ledger sums
            userDao.updateWalletBalance(clientId, 12500.0)
            userDao.updateWalletBalance(pharmacyId, 150000.0)
            userDao.updateWalletBalance(boutiqueId, 40000.0)
            userDao.updateWalletBalance(driverId, 7500.0)
            userDao.updateWalletBalance(adminId, 0.0)

            // Seed Products for Pharmacy with pharmacological details (Batch, Expiry, FIFO, stock)
            productDao.insertProduct(ProductEntity(tenantId = "tenant_عدن_صيدلة", merchantId = pharmacyId, name = "بندول اكسترا فضي 24 حبة", price = 1500.0, category = "medicine", locationName = "صنعاء - شارع حدة", batchNumber = "B-8822-EXP", expiryTimestamp = System.currentTimeMillis() + (220 * 24 * 3600 * 1000L), purchaseCost = 1100.0, isRecalled = false, totalStock = 120))
            productDao.insertProduct(ProductEntity(tenantId = "tenant_عدن_صيدلة", merchantId = pharmacyId, name = "مضاد حيوي أوجمنتين 1 جم", price = 4800.0, category = "medicine", locationName = "صنعاء - شارع حدة", batchNumber = "B-12C4-OUT", expiryTimestamp = System.currentTimeMillis() + (310 * 24 * 3600 * 1000L), purchaseCost = 3500.0, isRecalled = false, totalStock = 45))
            productDao.insertProduct(ProductEntity(tenantId = "tenant_عدن_صيدلة", merchantId = pharmacyId, name = "مشروب جفاف محلول للأطفال", price = 600.0, category = "medicine", locationName = "عدن - كريتر", batchNumber = "B-9102-SOU", expiryTimestamp = System.currentTimeMillis() - 2 * 24 * 3600 * 1000L, purchaseCost = 420.0, isRecalled = false, totalStock = 90)) // Expired to trigger indicators!
            productDao.insertProduct(ProductEntity(tenantId = "tenant_عدن_صيدلة", merchantId = pharmacyId, name = "شراب فيتامين د3 قطرات للأطفال", price = 1200.0, category = "medicine", locationName = "ش. صنعاء", batchNumber = "B-5489-REC", expiryTimestamp = System.currentTimeMillis() + (400 * 24 * 3600 * 1000L), purchaseCost = 800.0, isRecalled = true, totalStock = 300)) // Recalled to trigger alert filters!

            // Seed Clothes
            productDao.insertProduct(ProductEntity(tenantId = "tenant_صنعاء_وسط", merchantId = boutiqueId, name = "شال حرير دبل يمني مخيط", price = 14000.0, category = "clothing", locationName = "أبين - سوق الجملة"))
            productDao.insertProduct(ProductEntity(tenantId = "tenant_صنعاء_وسط", merchantId = boutiqueId, name = "معوز حضرمي ممتاز حياكة يدوية", price = 25000.0, category = "clothing", locationName = "حضرموت - تريم"))

            // Seed Wholesale and general market
            productDao.insertProduct(ProductEntity(tenantId = "tenant_صنعاء_وسط", merchantId = boutiqueId, name = "قطر أرز بسمتي هندي فاخر 10 كجم", price = 11000.0, category = "wholesale", locationName = "الحديدة - الرصيف م7"))
            productDao.insertProduct(ProductEntity(tenantId = "tenant_صنعاء_وسط", merchantId = boutiqueId, name = "سلة تموين منزلية شهرية", price = 18500.0, category = "general_market", locationName = "صنعاء - سوق المحلي"))
            productDao.insertProduct(ProductEntity(tenantId = "tenant_صنعاء_وسط", merchantId = boutiqueId, name = "باقة إعلان مشاهير محليين - ستوري + منشور", price = 30000.0, category = "influencer", locationName = "صنعاء / عدن", description = "باقة ترويجية تجريبية للمحال والخدمات المحلية"))

            // Seed Order
            val activeOrderId = orderDao.insertOrder(
                OrderEntity(
                    tenantId = "tenant_عدن_صيدلة",
                    clientId = clientId,
                    merchantId = pharmacyId,
                    driverId = driverId,
                    productName = "مجموعة دواء ضغط وعلاج سكري منظم",
                    category = "medicine",
                    totalPrice = 8500.0,
                    deliveryFee = 1500.0,
                    commissionAmount = 150.0,
                    status = "funds_frozen",
                    otpReleaseCode = "7711",
                    originalPriceAtRequest = 8500.0
                )
            ).toInt()

            prescriptionDao.insertPrescription(
                PrescriptionEntity(
                    tenantId = "tenant_عدن_صيدلة",
                    orderId = activeOrderId,
                    clientId = clientId,
                    medicineName = "مجموعة دواء ضغط وعلاج سكري منظم",
                    isRequired = true,
                    hasAttachment = true,
                    status = "approved"
                )
            )
            orderTimelineDao.insertTimeline(OrderTimelineEntity(tenantId = "tenant_عدن_صيدلة", orderId = activeOrderId, actorRole = "client", title = "إنشاء طلب دواء", note = "تم إرفاق الروشتة وبث الطلب للصيدلية.", statusSnapshot = "waiting_offers"))
            orderTimelineDao.insertTimeline(OrderTimelineEntity(tenantId = "tenant_عدن_صيدلة", orderId = activeOrderId, actorRole = "merchant", title = "عرض صيدلية", note = "تم توفير الدواء بسعر 8500 ريال والتحقق من الصلاحية.", statusSnapshot = "offer_received"))
            orderTimelineDao.insertTimeline(OrderTimelineEntity(tenantId = "tenant_عدن_صيدلة", orderId = activeOrderId, actorRole = "client", title = "تجميد الضمان", note = "تم تجميد 10000 ريال حتى التسليم.", statusSnapshot = "funds_frozen"))
            notificationDao.insertNotification(NotificationEntity(tenantId = "tenant_عدن_صيدلة", userId = clientId, orderId = activeOrderId, title = "طلب دواء نشط", body = "لديك طلب دواء في الضمان، لا تعطِ الكود إلا عند الاستلام.", severity = "warning"))

            // Chat messages for active demonstration
            chatDao.insertMessage(ChatMessageEntity(orderId = activeOrderId, senderRole = "system", senderName = "النظام المالي المحكم", messageText = "تم إنشاء تجميد الأموال للعملية بنجاح بقيمة 10,000 ريال (شامل التوصيل). الكود السري للتحرير هو 7711"))
            chatDao.insertMessage(ChatMessageEntity(orderId = activeOrderId, senderRole = "client", senderName = "أحمد بن علي", messageText = "أرجو توصيله سريعاً للمنزل، خلف مدرسة بلقيس، الروشتة مصورة ومرفوعة."))
            chatDao.insertMessage(ChatMessageEntity(orderId = activeOrderId, senderRole = "merchant", senderName = "صيدلية اليمن الكبرى", messageText = "الطلب جاهز ومحجوز، ومطابق مع تواريخ الصلاحية والباتش الدوائي."))
            chatDao.insertMessage(ChatMessageEntity(orderId = activeOrderId, senderRole = "driver", senderName = "أبو رعد الموتور", messageText = "استلمت الصندوق المعمّم من الصيدلية وأنا قادم بالطريق."))
        }
    }

    /**
     * Recommendation 5 (Nightly Financial Reconciliation):
     * Programmatic daily double-entry security reconciliation check.
     * Reports matching balance sums against virtual balances, throwing severe alarm logs on deviation.
     */
    suspend fun runNightlyFinancialReconciliation(): ReconciliationReport {
        val usersList = userDao.getAllUsers().first()
        val audits = mutableListOf<ReconciliationAudit>()
        var alarmsTripped = false

        usersList.forEach { user ->
            val calculatedMinor = ledgerEntryDao.calculateWalletBalanceMinor(user.id)
            val cachedMinor = user.walletBalanceMinor
            val devianceMinor = kotlin.math.abs(cachedMinor - calculatedMinor)
            val calculatedSum = Money.toMajor(calculatedMinor)
            val deviance = Money.toMajor(devianceMinor)
            val isBreached = devianceMinor != 0L

            if (isBreached) {
                alarmsTripped = true
            }

            audits.add(
                ReconciliationAudit(
                    userId = user.id,
                    phone = user.phone,
                    cachedBalance = Money.toMajor(cachedMinor),
                    calculatedLedgerSum = calculatedSum,
                    deviance = deviance,
                    status = if (isBreached) "🚨 BREACH_ALERT" else "✅ RECONCILED_OK"
                )
            )
        }

        return ReconciliationReport(
            timestamp = System.currentTimeMillis(),
            audits = audits,
            isSecurityAlarmTripped = alarmsTripped,
            summaryMessage = if (alarmsTripped) {
                "⚠️ حادثة أمنية عاجلة: كشفت مراجعة الحسابات الليلية عن وجود تباين مالي بين فهارس المحافظ وحسابات الـ Ledger! تم عزل الاتصالات وتحويل التقارير للأمن السيبراني المركزي."
            } else {
                "✅ نجاح المصالحة المحاسبية: كافة الأرصدة الافتراضية مطابقة بنسبة مائة بالمائة مع الـ Double-Entry Ledger المالي الموزع."
            }
        )
    }

    /**
     * Recommendation 2 (Tenant Context Isolation in Background Task):
     * Simulates Background Service processing of pending Outbox items.
     * Extracts 'tenantId' dynamically from each Outbox event, creating isolated processing
     * boundaries (emulating IServiceScope separation) to avoid cross-tenant data leaks.
     */
    suspend fun processPendingOutboxSync(onStep: (String) -> Unit): SyncOutboxResult {
        val pending = outboxEventDao.getPendingEventsDirect()
        if (pending.isEmpty()) {
            return SyncOutboxResult(0, 0, "لا توجد أحداث جديدة في قائمة الانتظار للـ Outbox اليوم.", "الـ Outbox فارغ. لا توجد أحداث مزامنة معلقة.")
        }

        var successCount = 0
        var failCount = 0
        val detailsLog = StringBuilder()

        pending.forEach { event ->
            val currentAttempt = event.attempts + 1
            
            // Randomly simulate a 4% network packets dropout to showcase diagnostic Outbox monitors alerts (>5% failure alert)
            val simulatedFail = Random.nextInt(100) < 4

            detailsLog.append("\n[Outbox Sync Channel]\n")
            detailsLog.append("• Event ID: ${event.id} | Action: ${event.eventType}\n")
            
            // DEMONSTRATE SCOPING & DATA ISOLATION (Recommendation 2)
            detailsLog.append("  ↳ Extracted Tenant ID context: '${event.tenantId}' from outbox tuple.\n")
            detailsLog.append("  ↳ Creating scoped sub-context: IServiceScope [Tenant: ${event.tenantId}]\n")
            detailsLog.append("  ↳ Injecting ITenantService scoped instance for safe processing...\n")

            if (simulatedFail) {
                failCount++
                val errMsg = "فشل الإرسال: خطأ 503 خطوط الاتصال مشغولة أو تالفة في اليمن"
                val updated = event.copy(
                    attempts = currentAttempt,
                    status = "failed",
                    lastError = errMsg
                )
                outboxEventDao.updateEvent(updated)
                detailsLog.append("  ↳ 🚨 Sync Attempt #$currentAttempt failed: $errMsg\n")
            } else {
                successCount++
                val updated = event.copy(
                    attempts = currentAttempt,
                    status = "synced",
                    lastError = null
                )
                outboxEventDao.updateEvent(updated)
                detailsLog.append("  ↳ ✅ Scope cleared and synced to SaaS central PostgreSQL central pool successfully.\n")
            }
            onStep(detailsLog.toString())
        }

        // Trigger dynamic monitoring evaluation (Recommendation 1: app.UseOutboxMonitor() diagnostic check)
        val totalProcessed = pending.size
        val failRate = (failCount.toDouble() / totalProcessed) * 100
        val alertTriggered = failRate >= 5.0

        val summary = """
            تمت معالجة $totalProcessed حدث معلّق.
            المرسل بنجاح: $successCount | الفاشل مؤقتاً: $failCount
            نسبة الخطأ في المزامنة: %.1f%% ${if (alertTriggered) "🚨 [تحذير: تجاوز حد الـ 5% الفاشل!]" else "✅ [الحدود طبيعية]"}
        """.trimIndent().format(failRate)

        return SyncOutboxResult(
            syncedCount = successCount,
            failedCount = failCount,
            logReport = detailsLog.toString(),
            summaryText = summary,
            isFailedAlertActive = alertTriggered
        )
    }

    // ==========================================
    // RATING & MUTUAL REVIEW ENGINE
    // ==========================================
    suspend fun submitReview(
        orderId: Int,
        reviewerId: Int,
        revieweeId: Int,
        rating: Int,
        tags: String,
        comment: String
    ): Boolean {
        val review = ReviewEntity(
            orderId = orderId,
            reviewerId = reviewerId,
            revieweeId = revieweeId,
            rating = rating,
            tags = tags,
            comment = comment
        )
        reviewDao.insertReview(review)
        evaluateRevieweeAccountStatus(revieweeId)
        return true
    }

    private suspend fun evaluateRevieweeAccountStatus(userId: Int) {
        reviewDao.getReviewsForUser(userId).first().let { reviews ->
            if (reviews.size >= 3) {
                val average = reviews.map { it.rating }.average()
                if (average < 3.0) {
                    val user = userDao.getUserById(userId)
                    if (user != null) {
                        userDao.updateUser(user.copy(status = "suspended"))
                    }
                }
            }
        }
    }

    // ==========================================
    // SERVER-DRIVEN UI & WHITELABEL COLOR DESIGN SYSTEM
    // ==========================================
    suspend fun saveSystemConfig(config: SystemConfigEntity) {
        systemConfigDao.insertConfig(config)
    }

    suspend fun getSystemConfig(): SystemConfigEntity {
        return systemConfigDao.getConfig() ?: SystemConfigEntity()
    }

    // ==========================================
    // CRYPTOGRAPHIC OFFLINE BACKUP & RESTORE
    // ==========================================
    suspend fun backupDataToFile(file: java.io.File, isCloudSync: Boolean): String {
        val users = userDao.getAllUsers().first()
        val products = productDao.getAllProducts().first()
        val orders = orderDao.getAllOrders().first()
        val reviews = reviewDao.getAllReviews().first()
        val config = getSystemConfig()

        val sb = StringBuilder()
        sb.append("BACKUP_VERSION:3\n")
        sb.append("UTC:${System.currentTimeMillis()}\n")
        
        sb.append("[USERS]\n")
        users.forEach { sb.append("${it.id}|${it.phone}|${it.role}|${it.walletBalance}|${it.status}|${it.tenantId}\n") }
        
        sb.append("[PRODUCTS]\n")
        products.forEach { sb.append("${it.id}|${it.merchantId}|${it.name}|${it.price}|${it.category}|${it.locationName}|${it.isAvailable}|${it.batchNumber}|${it.expiryTimestamp}|${it.purchaseCost}|${it.isRecalled}|${it.totalStock}\n") }
        
        sb.append("[ORDERS]\n")
        orders.forEach { sb.append("${it.id}|${it.clientId}|${it.merchantId}|${it.driverId}|${it.productName}|${it.category}|${it.totalPrice}|${it.deliveryFee}|${it.commissionAmount}|${it.status}|${it.otpReleaseCode}|${it.isLocalMeshSigned}|${it.timestamp}|${it.tenantId}|${it.originalPriceAtRequest}|${it.serverUpdatedPriceConflict}\n") }
        
        sb.append("[REVIEWS]\n")
        reviews.forEach { sb.append("${it.id}|${it.orderId}|${it.reviewerId}|${it.revieweeId}|${it.rating}|${it.tags}|${it.comment}|${it.timestamp}\n") }

        sb.append("[CONFIG]\n")
        sb.append("${config.primaryColor}|${config.secondaryColor}|${config.appTitle}|${config.rideLabel}|${config.pharmacyLabel}|${config.clothingLabel}|${config.marketplaceOrder}|${config.isPromoBannerVisible}|${config.promoBannerText}\n")

        val rawText = sb.toString()
        val encryptedText = encryptDecryptText(rawText, 'M')

        if (!isCloudSync) {
            file.parentFile?.mkdirs()
            file.writeText(encryptedText)
        }
        return encryptedText
    }

    suspend fun restoreDataFromFile(file: java.io.File, fromCloudPayload: String?): Boolean {
        val encryptedPayload = fromCloudPayload ?: if (file.exists()) file.readText() else return false
        if (encryptedPayload.isBlank()) return false

        try {
            val decrypted = encryptDecryptText(encryptedPayload, 'M')
            if (!decrypted.startsWith("BACKUP_VERSION:")) return false

            val lines = decrypted.lines()
            var currentHeader = ""
            
            lines.forEach { line ->
                if (line.isBlank()) return@forEach
                if (line.startsWith("[")) {
                    currentHeader = line
                    return@forEach
                }
                if (line.startsWith("BACKUP_VERSION:") || line.startsWith("UTC:")) return@forEach

                val parts = line.split("|")
                try {
                    when (currentHeader) {
                        "[USERS]" -> {
                            if (parts.size >= 5) {
                                val user = UserEntity(
                                    id = parts[0].toInt(),
                                    phone = parts[1],
                                    role = parts[2],
                                    walletBalance = parts[3].toDouble(),
                                    status = parts[4],
                                    tenantId = if (parts.size >= 6) parts[5] else "tenant_صنعاء_وسط"
                                )
                                userDao.insertUser(user)
                             }
                        }
                        "[PRODUCTS]" -> {
                            if (parts.size >= 7) {
                                val prd = ProductEntity(
                                    id = parts[0].toInt(),
                                    merchantId = parts[1].toInt(),
                                    name = parts[2],
                                    price = parts[3].toDouble(),
                                    category = parts[4],
                                    locationName = parts[5],
                                    isAvailable = parts[6].toBoolean(),
                                    batchNumber = if (parts.size >= 8) parts[7] else "B-REG-D",
                                    expiryTimestamp = if (parts.size >= 9) parts[8].toLong() else 0L,
                                    purchaseCost = if (parts.size >= 10) parts[9].toDouble() else 0.0,
                                    isRecalled = if (parts.size >= 11) parts[10].toBoolean() else false,
                                    totalStock = if (parts.size >= 12) parts[11].toInt() else 100
                                )
                                productDao.insertProduct(prd)
                            }
                        }
                        "[ORDERS]" -> {
                            if (parts.size >= 13) {
                                val ord = OrderEntity(
                                    id = parts[0].toInt(),
                                    clientId = parts[1].toInt(),
                                    merchantId = if (parts[2] == "null") null else parts[2].toIntOrNull(),
                                    driverId = if (parts[3] == "null") null else parts[3].toIntOrNull(),
                                    productName = parts[4],
                                    category = parts[5],
                                    totalPrice = parts[6].toDouble(),
                                    deliveryFee = parts[7].toDouble(),
                                    commissionAmount = parts[8].toDouble(),
                                    status = parts[9],
                                    otpReleaseCode = parts[10],
                                    isLocalMeshSigned = parts[11].toBoolean(),
                                    timestamp = parts[12].toLong(),
                                    tenantId = if (parts.size >= 14) parts[13] else "tenant_صنعاء_وسط",
                                    originalPriceAtRequest = if (parts.size >= 15) parts[14].toDouble() else parts[6].toDouble(),
                                    serverUpdatedPriceConflict = if (parts.size >= 16) parts[15].toDouble() else parts[6].toDouble()
                                )
                                orderDao.insertOrder(ord)
                            }
                        }
                    }
                } catch (e: Exception) {
                    println("RESTORE INNER REGRESSION ERR: ${e.message}")
                }
            }
            return true
        } catch (e: Exception) {
            return false
        }
    }

    private fun encryptDecryptText(input: String, key: Char): String {
        val output = StringBuilder()
        for (i in input.indices) {
            val charCode = input[i].code
            val cipher = charCode xor key.code xor (i % 7)
            output.append(cipher.toChar())
        }
        return output.toString()
    }
}

// Custom data wrappers for reporting and sync outputs
data class ReconciliationAudit(
    val userId: Int,
    val phone: String,
    val cachedBalance: Double,
    val calculatedLedgerSum: Double,
    val deviance: Double,
    val status: String
)

data class ReconciliationReport(
    val timestamp: Long,
    val audits: List<ReconciliationAudit>,
    val isSecurityAlarmTripped: Boolean,
    val summaryMessage: String
)

data class SyncOutboxResult(
    val syncedCount: Int,
    val failedCount: Int,
    val logReport: String,
    val summaryText: String,
    val isFailedAlertActive: Boolean = false
)
