package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index
import androidx.room.ColumnInfo

@Entity(
    tableName = "users",
    indices = [Index(value = ["tenantId", "phone"], unique = true)]
)
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val tenantId: String = "yemen_central_sadr",
    val phone: String,
    val role: String, // "client", "merchant", "driver", "admin"
    val walletBalance: Double, // cached display only; authoritative ledger uses walletBalanceMinor/ledger amountMinor
    @ColumnInfo(defaultValue = "0") val walletBalanceMinor: Long = Money.toMinor(walletBalance),
    val status: String, // "active", "busy", "offline", "suspended"
    @ColumnInfo(defaultValue = "''") val fullName: String = "",
    @ColumnInfo(defaultValue = "''") val displayName: String = "",
    @ColumnInfo(defaultValue = "'none'") val businessType: String = "none", // none, pharmacy, marketplace, delivery, admin
    @ColumnInfo(defaultValue = "''") val businessName: String = "",
    @ColumnInfo(defaultValue = "''") val responsibleName: String = "",
    @ColumnInfo(defaultValue = "''") val contactPhone: String = "",
    @ColumnInfo(defaultValue = "''") val city: String = "",
    @ColumnInfo(defaultValue = "''") val district: String = "",
    @ColumnInfo(defaultValue = "''") val address: String = "",
    @ColumnInfo(defaultValue = "0.0") val gpsLatitude: Double = 0.0,
    @ColumnInfo(defaultValue = "0.0") val gpsLongitude: Double = 0.0,
    @ColumnInfo(defaultValue = "''") val licenseNumber: String = "",
    @ColumnInfo(defaultValue = "''") val licenseImageUri: String = "",
    @ColumnInfo(defaultValue = "''") val workingHours: String = "",
    @ColumnInfo(defaultValue = "0") val deliversOrders: Boolean = false,
    @ColumnInfo(defaultValue = "0") val serviceRadiusKm: Int = 0,
    @ColumnInfo(defaultValue = "''") val merchantCategory: String = "",
    @ColumnInfo(defaultValue = "''") val deliveryPolicy: String = "",
    @ColumnInfo(defaultValue = "''") val vehicleType: String = "",
    @ColumnInfo(defaultValue = "''") val vehiclePlate: String = "",
    @ColumnInfo(defaultValue = "'pending'") val approvalStatus: String = "pending",
    @ColumnInfo(defaultValue = "0") val isProfileComplete: Boolean = false
)

@Entity(
    tableName = "products",
    indices = [Index(value = ["tenantId", "id"])]
)
data class ProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val tenantId: String = "yemen_central_sadr",
    val merchantId: Int,
    val name: String,
    val price: Double,
    @ColumnInfo(defaultValue = "0") val priceMinor: Long = Money.toMinor(price),
    val category: String, // "medicine", "ride", "clothing", "wholesale"
    val locationName: String,
    @ColumnInfo(defaultValue = "''") val description: String = "",
    @ColumnInfo(defaultValue = "''") val brand: String = "",
    @ColumnInfo(defaultValue = "'قطعة'") val unitText: String = "قطعة",
    val isAvailable: Boolean = true,
    
    // Core Pharmacological properties for Batch, Expiry & FIFO costing
    val batchNumber: String = "B26-09A",
    val expiryTimestamp: Long = System.currentTimeMillis() + (365L * 24 * 60 * 60 * 1000), // 1 Year Default
    val purchaseCost: Double = 1000.0,
    @ColumnInfo(defaultValue = "100000") val purchaseCostMinor: Long = Money.toMinor(purchaseCost),
    val isRecalled: Boolean = false,
    val totalStock: Int = 250
)

@Entity(
    tableName = "orders",
    indices = [Index(value = ["tenantId", "id"])]
)
data class OrderEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val tenantId: String = "yemen_central_sadr",
    val clientId: Int,
    val merchantId: Int?,
    val driverId: Int?,
    val productName: String,
    val category: String,
    val totalPrice: Double,
    @ColumnInfo(defaultValue = "0") val totalPriceMinor: Long = Money.toMinor(totalPrice),
    val deliveryFee: Double,
    @ColumnInfo(defaultValue = "0") val deliveryFeeMinor: Long = Money.toMinor(deliveryFee),
    val commissionAmount: Double,
    @ColumnInfo(defaultValue = "0") val commissionAmountMinor: Long = Money.toMinor(commissionAmount),
    val status: String, // "pending", "funds_frozen", "completed", "cancelled", "CONFLICT"
    val otpReleaseCode: String,
    val isLocalMeshSigned: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    
    // Price conflict details for Offline-First resilience simulation
    val originalPriceAtRequest: Double = 0.0,
    @ColumnInfo(defaultValue = "0") val originalPriceAtRequestMinor: Long = Money.toMinor(originalPriceAtRequest),
    val serverUpdatedPriceConflict: Double = 0.0,
    @ColumnInfo(defaultValue = "0") val serverUpdatedPriceConflictMinor: Long = Money.toMinor(serverUpdatedPriceConflict)
)

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Int,
    val orderId: Int?,
    val type: String, // "credit" (deposit), "debit" (spend), "hold" (frozen), "release" (payout)
    val amount: Double, // display/backward compatibility only
    @ColumnInfo(defaultValue = "0") val amountMinor: Long = Money.toMinor(amount),
    val providerName: String, // "الكريمي", "سويري كاش", "تحويل يدوي"
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "ledger_entries",
    indices = [
        Index(value = ["tenantId", "debitWalletId"]),
        Index(value = ["tenantId", "creditWalletId"])
    ]
)
data class LedgerEntryEntity(
    @PrimaryKey(autoGenerate = true) val entryId: Int = 0,
    val tenantId: String = "yemen_central_sadr",
    val debitWalletId: Int,   // Accrued/funded identity. E.g., Client/Merchant ID.
    val creditWalletId: Int,  // Origin/funded subtraction. e.g. -1 for external bank wire الكريمي
    val amount: Double,       // display/backward compatibility only
    @ColumnInfo(defaultValue = "0") val amountMinor: Long = Money.toMinor(amount), // authoritative amount for accounting
    val orderId: Int? = null,
    val narrative: String,    // Purpose descripto
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "outbox_events",
    indices = [Index(value = ["tenantId", "status"])]
)
data class OutboxEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val tenantId: String = "yemen_central_sadr",
    val eventType: String, // "ORDER_CREATE", "LEDGER_TX", "PRICE_CONFLICT"
    val payload: String,   // Compact structured operation log
    val status: String = "pending", // "pending", "failed", "synced"
    val attempts: Int = 0,
    val lastError: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val orderId: Int,
    val senderRole: String, // "client", "merchant", "driver"
    val senderName: String,
    val messageText: String,
    val isVoiceNote: Boolean = false,
    val voiceDurationSec: Int = 0,
    val voiceFileSizeBytes: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "reviews")
data class ReviewEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val orderId: Int,
    val reviewerId: Int,
    val revieweeId: Int,
    val rating: Int, // 1 to 5
    val tags: String, // Comma-separated options
    val comment: String,
    val timestamp: Long = System.currentTimeMillis()
)



@Entity(
    tableName = "pharmacy_offers",
    indices = [
        Index(value = ["tenantId", "orderId"]),
        Index(value = ["tenantId", "merchantId"])
    ]
)
data class PharmacyOfferEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val tenantId: String = "yemen_central_sadr",
    val orderId: Int,
    val merchantId: Int,
    val price: Double,
    @ColumnInfo(defaultValue = "0") val priceMinor: Long = Money.toMinor(price),
    val availableQuantity: Int = 1,
    val preparationMinutes: Int = 30,
    val note: String = "",
    val alternativeMedicineName: String = "",
    val expiryDateText: String = "غير محدد",
    val status: String = "offered", // offered, accepted, rejected, unavailable
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "order_timeline",
    indices = [Index(value = ["tenantId", "orderId"])]
)
data class OrderTimelineEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val tenantId: String = "yemen_central_sadr",
    val orderId: Int,
    val actorRole: String,
    val title: String,
    val note: String,
    val statusSnapshot: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "notifications",
    indices = [
        Index(value = ["tenantId", "userId", "isRead"]),
        Index(value = ["tenantId", "orderId"])
    ]
)
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val tenantId: String = "yemen_central_sadr",
    val userId: Int,
    val orderId: Int? = null,
    val title: String,
    val body: String,
    val severity: String = "info", // info, success, warning, danger
    val isRead: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "disputes",
    indices = [
        Index(value = ["tenantId", "orderId"]),
        Index(value = ["tenantId", "status"])
    ]
)
data class DisputeEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val tenantId: String = "yemen_central_sadr",
    val orderId: Int,
    val openedByUserId: Int,
    val reason: String,
    val details: String,
    val status: String = "open", // open, refund_client, release_merchant, closed
    val adminDecision: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val resolvedAt: Long? = null
)

@Entity(
    tableName = "prescriptions",
    indices = [Index(value = ["tenantId", "orderId"], unique = true)]
)
data class PrescriptionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val tenantId: String = "yemen_central_sadr",
    val orderId: Int,
    val clientId: Int,
    val medicineName: String,
    val isRequired: Boolean = true,
    val hasAttachment: Boolean = false,
    val status: String = "required", // not_required, required, uploaded, approved, rejected
    val rejectionReason: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "pharmacy_verifications",
    indices = [Index(value = ["tenantId", "merchantId"], unique = true)]
)
data class PharmacyVerificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val tenantId: String = "yemen_central_sadr",
    val merchantId: Int,
    val pharmacyName: String,
    val licenseNumber: String,
    val city: String,
    val status: String = "pending", // pending, approved, rejected, suspended
    val rejectionReason: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "system_configs")
data class SystemConfigEntity(
    @PrimaryKey val id: String = "central_config",
    val primaryColor: String = "#10B981",
    val secondaryColor: String = "#FBBF24",
    val appTitle: String = "مَجْمَع الضمان والاقتصاد الهجين",
    val rideLabel: String = "موتور وباص سريع",
    val pharmacyLabel: String = "البحث عن دواء",
    val clothingLabel: String = "سوق الملابس والملبوسات",
    val marketplaceOrder: String = "pharmacy,market,influencers,delivery",
    val isPromoBannerVisible: Boolean = true,
    val promoBannerText: String = "🌙 مبارك عليكم ما تبقى من الشهر الكريم - مجمع الضمان يسهر لخدمتكم"
)
