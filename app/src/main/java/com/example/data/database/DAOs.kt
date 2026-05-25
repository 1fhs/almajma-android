package com.example.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users")
    fun getAllUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE role = :role")
    fun getUsersByRole(role: String): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE id = :id")
    suspend fun getUserById(id: Int): UserEntity?

    @Query("SELECT * FROM users WHERE phone = :phone LIMIT 1")
    suspend fun getUserByPhone(phone: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity): Long

    @Update
    suspend fun updateUser(user: UserEntity)

    @Query("""
        UPDATE users
        SET fullName = :fullName,
            displayName = :displayName,
            businessType = :businessType,
            businessName = :businessName,
            city = :city,
            address = :address,
            licenseNumber = :licenseNumber,
            isProfileComplete = 1
        WHERE id = :id
    """)
    suspend fun updateUserProfile(
        id: Int,
        fullName: String,
        displayName: String,
        businessType: String,
        businessName: String,
        city: String,
        address: String,
        licenseNumber: String
    )

    @Query("UPDATE users SET walletBalance = :newBalance, walletBalanceMinor = CAST(ROUND(:newBalance * 100.0) AS INTEGER) WHERE id = :id")
    suspend fun updateWalletBalance(id: Int, newBalance: Double)
}

@Dao
interface ProductDao {
    @Query("SELECT * FROM products ORDER BY id DESC")
    fun getAllProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE category = :category ORDER BY id DESC")
    fun getProductsByCategory(category: String): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE merchantId = :merchantId ORDER BY id DESC")
    fun getProductsByMerchant(merchantId: Int): Flow<List<ProductEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity)

    @Delete
    suspend fun deleteProduct(product: ProductEntity)
}

@Dao
interface OrderDao {
    @Query("SELECT * FROM orders ORDER BY timestamp DESC")
    fun getAllOrders(): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE clientId = :clientId ORDER BY timestamp DESC")
    fun getOrdersForClient(clientId: Int): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE merchantId = :merchantId ORDER BY timestamp DESC")
    fun getOrdersForMerchant(merchantId: Int): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE driverId = :driverId ORDER BY timestamp DESC")
    fun getOrdersForDriver(driverId: Int): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE id = :id LIMIT 1")
    suspend fun getOrderById(id: Int): OrderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: OrderEntity): Long

    @Update
    suspend fun updateOrder(order: OrderEntity)
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions WHERE userId = :userId ORDER BY timestamp DESC")
    fun getTransactionsForUser(userId: Int): Flow<List<TransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity)
}

@Dao
interface ChatDao {
    @Query("SELECT * FROM chat_messages WHERE orderId = :orderId ORDER BY timestamp ASC")
    fun getMessagesForOrder(orderId: Int): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity)
}

@Dao
interface ReviewDao {
    @Query("SELECT * FROM reviews ORDER BY timestamp DESC")
    fun getAllReviews(): Flow<List<ReviewEntity>>

    @Query("SELECT * FROM reviews WHERE revieweeId = :revieweeId ORDER BY timestamp DESC")
    fun getReviewsForUser(revieweeId: Int): Flow<List<ReviewEntity>>

    @Query("SELECT * FROM reviews WHERE orderId = :orderId")
    suspend fun getReviewsForOrder(orderId: Int): List<ReviewEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReview(review: ReviewEntity)
}



@Dao
interface PharmacyOfferDao {
    @Query("SELECT * FROM pharmacy_offers ORDER BY timestamp DESC")
    fun getAllOffers(): Flow<List<PharmacyOfferEntity>>

    @Query("SELECT * FROM pharmacy_offers WHERE orderId = :orderId ORDER BY timestamp DESC")
    fun getOffersForOrder(orderId: Int): Flow<List<PharmacyOfferEntity>>

    @Query("SELECT * FROM pharmacy_offers WHERE orderId = :orderId AND status = 'offered' ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestActiveOfferForOrder(orderId: Int): PharmacyOfferEntity?

    @Query("SELECT * FROM pharmacy_offers WHERE merchantId = :merchantId ORDER BY timestamp DESC")
    fun getOffersForMerchant(merchantId: Int): Flow<List<PharmacyOfferEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOffer(offer: PharmacyOfferEntity): Long

    @Update
    suspend fun updateOffer(offer: PharmacyOfferEntity)
}

@Dao
interface OrderTimelineDao {
    @Query("SELECT * FROM order_timeline WHERE orderId = :orderId ORDER BY timestamp ASC")
    fun getTimelineForOrder(orderId: Int): Flow<List<OrderTimelineEntity>>

    @Query("SELECT * FROM order_timeline ORDER BY timestamp DESC")
    fun getAllTimeline(): Flow<List<OrderTimelineEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTimeline(event: OrderTimelineEntity): Long
}

@Dao
interface NotificationDao {
    @Query("SELECT * FROM notifications WHERE userId = :userId ORDER BY timestamp DESC")
    fun getNotificationsForUser(userId: Int): Flow<List<NotificationEntity>>

    @Query("SELECT COUNT(*) FROM notifications WHERE userId = :userId AND isRead = 0")
    fun getUnreadCountForUser(userId: Int): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationEntity): Long

    @Query("UPDATE notifications SET isRead = 1 WHERE userId = :userId")
    suspend fun markAllRead(userId: Int)
}

@Dao
interface DisputeDao {
    @Query("SELECT * FROM disputes ORDER BY timestamp DESC")
    fun getAllDisputes(): Flow<List<DisputeEntity>>

    @Query("SELECT * FROM disputes WHERE orderId = :orderId ORDER BY timestamp DESC")
    fun getDisputesForOrder(orderId: Int): Flow<List<DisputeEntity>>

    @Query("SELECT * FROM disputes WHERE status = 'open' ORDER BY timestamp ASC")
    fun getOpenDisputes(): Flow<List<DisputeEntity>>

    @Query("SELECT * FROM disputes WHERE id = :id LIMIT 1")
    suspend fun getDisputeById(id: Int): DisputeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDispute(dispute: DisputeEntity): Long

    @Update
    suspend fun updateDispute(dispute: DisputeEntity)
}

@Dao
interface PrescriptionDao {
    @Query("SELECT * FROM prescriptions ORDER BY timestamp DESC")
    fun getAllPrescriptions(): Flow<List<PrescriptionEntity>>

    @Query("SELECT * FROM prescriptions WHERE orderId = :orderId LIMIT 1")
    suspend fun getPrescriptionForOrder(orderId: Int): PrescriptionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrescription(prescription: PrescriptionEntity): Long

    @Update
    suspend fun updatePrescription(prescription: PrescriptionEntity)
}

@Dao
interface PharmacyVerificationDao {
    @Query("SELECT * FROM pharmacy_verifications ORDER BY timestamp DESC")
    fun getAllVerifications(): Flow<List<PharmacyVerificationEntity>>

    @Query("SELECT * FROM pharmacy_verifications WHERE merchantId = :merchantId LIMIT 1")
    suspend fun getVerificationForMerchant(merchantId: Int): PharmacyVerificationEntity?

    @Query("SELECT * FROM pharmacy_verifications WHERE status = 'pending' ORDER BY timestamp ASC")
    fun getPendingVerifications(): Flow<List<PharmacyVerificationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVerification(verification: PharmacyVerificationEntity): Long

    @Update
    suspend fun updateVerification(verification: PharmacyVerificationEntity)
}

@Dao
interface SystemConfigDao {
    @Query("SELECT * FROM system_configs WHERE id = 'central_config' LIMIT 1")
    fun getConfigFlow(): Flow<SystemConfigEntity?>

    @Query("SELECT * FROM system_configs WHERE id = 'central_config' LIMIT 1")
    suspend fun getConfig(): SystemConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConfig(config: SystemConfigEntity)
}

@Dao
interface LedgerEntryDao {
    @Query("SELECT * FROM ledger_entries ORDER BY timestamp DESC")
    fun getAllLedgerEntries(): Flow<List<LedgerEntryEntity>>

    @Query("SELECT * FROM ledger_entries WHERE debitWalletId = :walletId OR creditWalletId = :walletId ORDER BY timestamp DESC")
    fun getLedgerEntriesForWallet(walletId: Int): Flow<List<LedgerEntryEntity>>

    @Query("SELECT COALESCE(SUM(CASE WHEN debitWalletId = :walletId THEN amountMinor ELSE 0 END) - SUM(CASE WHEN creditWalletId = :walletId THEN amountMinor ELSE 0 END), 0) FROM ledger_entries")
    suspend fun calculateWalletBalanceMinor(walletId: Int): Long

    @Query("SELECT COALESCE(SUM(CASE WHEN debitWalletId = :walletId THEN amountMinor ELSE 0 END) - SUM(CASE WHEN creditWalletId = :walletId THEN amountMinor ELSE 0 END), 0) / 100.0 FROM ledger_entries")
    suspend fun calculateWalletBalance(walletId: Int): Double

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLedgerEntry(entry: LedgerEntryEntity): Long
}

@Dao
interface OutboxEventDao {
    @Query("SELECT * FROM outbox_events WHERE status = 'pending' ORDER BY timestamp ASC")
    fun getPendingEvents(): Flow<List<OutboxEventEntity>>

    @Query("SELECT * FROM outbox_events WHERE status = 'pending' ORDER BY timestamp ASC")
    suspend fun getPendingEventsDirect(): List<OutboxEventEntity>

    @Query("SELECT * FROM outbox_events ORDER BY timestamp DESC")
    fun getAllEvents(): Flow<List<OutboxEventEntity>>

    @Query("SELECT COUNT(*) FROM outbox_events WHERE status = 'pending'")
    fun getPendingEventsCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM outbox_events WHERE status = 'failed'")
    fun getFailedEventsCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM outbox_events")
    fun getTotalEventsCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: OutboxEventEntity): Long

    @Update
    suspend fun updateEvent(event: OutboxEventEntity)

    @Query("DELETE FROM outbox_events WHERE status = 'synced'")
    suspend fun clearSyncedEvents()
}
