package com.example.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        UserEntity::class,
        ProductEntity::class,
        OrderEntity::class,
        TransactionEntity::class,
        ChatMessageEntity::class,
        ReviewEntity::class,
        PharmacyOfferEntity::class,
        OrderTimelineEntity::class,
        NotificationEntity::class,
        DisputeEntity::class,
        PrescriptionEntity::class,
        PharmacyVerificationEntity::class,
        SystemConfigEntity::class,
        LedgerEntryEntity::class,
        OutboxEventEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun productDao(): ProductDao
    abstract fun orderDao(): OrderDao
    abstract fun transactionDao(): TransactionDao
    abstract fun chatDao(): ChatDao
    abstract fun reviewDao(): ReviewDao
    abstract fun pharmacyOfferDao(): PharmacyOfferDao
    abstract fun orderTimelineDao(): OrderTimelineDao
    abstract fun notificationDao(): NotificationDao
    abstract fun disputeDao(): DisputeDao
    abstract fun prescriptionDao(): PrescriptionDao
    abstract fun pharmacyVerificationDao(): PharmacyVerificationDao
    abstract fun systemConfigDao(): SystemConfigDao
    abstract fun ledgerEntryDao(): LedgerEntryDao
    abstract fun outboxEventDao(): OutboxEventDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                createReviewsTable(db)
                createSystemConfigTable(db)
                seedDefaultSystemConfig(db)
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                ensureLegacyCoreColumns(db)
                createLedgerTable(db)
                createOutboxTable(db)
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                createPharmacyOfferTable(db)
                createOrderTimelineTable(db)
                createNotificationTable(db)
                createDisputeTable(db)
                createPrescriptionTable(db)
                createPharmacyVerificationTable(db)
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                addMinorUnitColumns(db)
                backfillMinorUnitColumns(db)
            }
        }

        private fun tableExists(db: SupportSQLiteDatabase, tableName: String): Boolean {
            db.query("SELECT name FROM sqlite_master WHERE type='table' AND name=?", arrayOf(tableName)).use { cursor ->
                return cursor.moveToFirst()
            }
        }

        private fun columnExists(db: SupportSQLiteDatabase, tableName: String, columnName: String): Boolean {
            if (!tableExists(db, tableName)) return false
            db.query("PRAGMA table_info(`$tableName`)").use { cursor ->
                val nameIndex = cursor.getColumnIndex("name")
                while (cursor.moveToNext()) {
                    if (nameIndex >= 0 && cursor.getString(nameIndex) == columnName) return true
                }
            }
            return false
        }

        private fun addColumnIfMissing(
            db: SupportSQLiteDatabase,
            tableName: String,
            columnName: String,
            columnDefinition: String
        ) {
            if (tableExists(db, tableName) && !columnExists(db, tableName, columnName)) {
                db.execSQL("ALTER TABLE `$tableName` ADD COLUMN `$columnName` $columnDefinition")
            }
        }

        private fun createReviewsTable(db: SupportSQLiteDatabase) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `reviews` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `orderId` INTEGER NOT NULL,
                    `reviewerId` INTEGER NOT NULL,
                    `revieweeId` INTEGER NOT NULL,
                    `rating` INTEGER NOT NULL,
                    `tags` TEXT NOT NULL,
                    `comment` TEXT NOT NULL,
                    `timestamp` INTEGER NOT NULL
                )
            """.trimIndent())
        }

        private fun createSystemConfigTable(db: SupportSQLiteDatabase) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `system_configs` (
                    `id` TEXT PRIMARY KEY NOT NULL,
                    `primaryColor` TEXT NOT NULL,
                    `secondaryColor` TEXT NOT NULL,
                    `appTitle` TEXT NOT NULL,
                    `rideLabel` TEXT NOT NULL,
                    `pharmacyLabel` TEXT NOT NULL,
                    `clothingLabel` TEXT NOT NULL,
                    `marketplaceOrder` TEXT NOT NULL,
                    `isPromoBannerVisible` INTEGER NOT NULL DEFAULT 1,
                    `promoBannerText` TEXT NOT NULL DEFAULT ''
                )
            """.trimIndent())
        }

        private fun seedDefaultSystemConfig(db: SupportSQLiteDatabase) {
            db.execSQL("""
                INSERT OR IGNORE INTO `system_configs` (
                    id, primaryColor, secondaryColor, appTitle, rideLabel,
                    pharmacyLabel, clothingLabel, marketplaceOrder,
                    isPromoBannerVisible, promoBannerText
                ) VALUES (
                    'central_config', '#10B981', '#FBBF24',
                    'مَجْمَع الضمان والاقتصاد الهجين', 'موتور وباص سريع',
                    'البحث عن دواء', 'سوق الملابس والملبوسات',
                    'ride,pharmacy,clothing,wholesale', 1,
                    '🌙 مبارك عليكم ما تبقى من الشهر الكريم - مجمع الضمان يسهر لخدمتكم'
                )
            """.trimIndent())
        }

        private fun ensureLegacyCoreColumns(db: SupportSQLiteDatabase) {
            addColumnIfMissing(db, "users", "tenantId", "TEXT NOT NULL DEFAULT 'tenant_صنعاء_وسط'")
            addColumnIfMissing(db, "products", "tenantId", "TEXT NOT NULL DEFAULT 'tenant_صنعاء_وسط'")
            addColumnIfMissing(db, "products", "batchNumber", "TEXT NOT NULL DEFAULT 'B-LEGACY'")
            addColumnIfMissing(db, "products", "expiryTimestamp", "INTEGER NOT NULL DEFAULT 0")
            addColumnIfMissing(db, "products", "purchaseCost", "REAL NOT NULL DEFAULT 0.0")
            addColumnIfMissing(db, "products", "isRecalled", "INTEGER NOT NULL DEFAULT 0")
            addColumnIfMissing(db, "products", "totalStock", "INTEGER NOT NULL DEFAULT 0")
            addColumnIfMissing(db, "orders", "tenantId", "TEXT NOT NULL DEFAULT 'tenant_صنعاء_وسط'")
            addColumnIfMissing(db, "orders", "originalPriceAtRequest", "REAL NOT NULL DEFAULT 0.0")
            addColumnIfMissing(db, "orders", "serverUpdatedPriceConflict", "REAL NOT NULL DEFAULT 0.0")
        }

        private fun createLedgerTable(db: SupportSQLiteDatabase) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `ledger_entries` (
                    `entryId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `tenantId` TEXT NOT NULL,
                    `debitWalletId` INTEGER NOT NULL,
                    `creditWalletId` INTEGER NOT NULL,
                    `amount` REAL NOT NULL,
                    `amountMinor` INTEGER NOT NULL DEFAULT 0,
                    `orderId` INTEGER,
                    `narrative` TEXT NOT NULL,
                    `timestamp` INTEGER NOT NULL
                )
            """.trimIndent())
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_ledger_entries_tenantId_debitWalletId` ON `ledger_entries` (`tenantId`, `debitWalletId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_ledger_entries_tenantId_creditWalletId` ON `ledger_entries` (`tenantId`, `creditWalletId`)")
        }

        private fun createOutboxTable(db: SupportSQLiteDatabase) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `outbox_events` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `tenantId` TEXT NOT NULL,
                    `eventType` TEXT NOT NULL,
                    `payload` TEXT NOT NULL,
                    `status` TEXT NOT NULL,
                    `attempts` INTEGER NOT NULL,
                    `lastError` TEXT,
                    `timestamp` INTEGER NOT NULL
                )
            """.trimIndent())
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_outbox_events_tenantId_status` ON `outbox_events` (`tenantId`, `status`)")
        }

        private fun createPharmacyOfferTable(db: SupportSQLiteDatabase) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `pharmacy_offers` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `tenantId` TEXT NOT NULL,
                    `orderId` INTEGER NOT NULL,
                    `merchantId` INTEGER NOT NULL,
                    `price` REAL NOT NULL,
                    `priceMinor` INTEGER NOT NULL DEFAULT 0,
                    `availableQuantity` INTEGER NOT NULL,
                    `preparationMinutes` INTEGER NOT NULL,
                    `note` TEXT NOT NULL,
                    `alternativeMedicineName` TEXT NOT NULL,
                    `expiryDateText` TEXT NOT NULL,
                    `status` TEXT NOT NULL,
                    `timestamp` INTEGER NOT NULL
                )
            """.trimIndent())
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_pharmacy_offers_tenantId_orderId` ON `pharmacy_offers` (`tenantId`, `orderId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_pharmacy_offers_tenantId_merchantId` ON `pharmacy_offers` (`tenantId`, `merchantId`)")
        }

        private fun createOrderTimelineTable(db: SupportSQLiteDatabase) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `order_timeline` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `tenantId` TEXT NOT NULL,
                    `orderId` INTEGER NOT NULL,
                    `actorRole` TEXT NOT NULL,
                    `title` TEXT NOT NULL,
                    `note` TEXT NOT NULL,
                    `statusSnapshot` TEXT NOT NULL,
                    `timestamp` INTEGER NOT NULL
                )
            """.trimIndent())
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_order_timeline_tenantId_orderId` ON `order_timeline` (`tenantId`, `orderId`)")
        }

        private fun createNotificationTable(db: SupportSQLiteDatabase) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `notifications` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `tenantId` TEXT NOT NULL,
                    `userId` INTEGER NOT NULL,
                    `orderId` INTEGER,
                    `title` TEXT NOT NULL,
                    `body` TEXT NOT NULL,
                    `severity` TEXT NOT NULL,
                    `isRead` INTEGER NOT NULL,
                    `timestamp` INTEGER NOT NULL
                )
            """.trimIndent())
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_notifications_tenantId_userId_isRead` ON `notifications` (`tenantId`, `userId`, `isRead`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_notifications_tenantId_orderId` ON `notifications` (`tenantId`, `orderId`)")
        }

        private fun createDisputeTable(db: SupportSQLiteDatabase) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `disputes` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `tenantId` TEXT NOT NULL,
                    `orderId` INTEGER NOT NULL,
                    `openedByUserId` INTEGER NOT NULL,
                    `reason` TEXT NOT NULL,
                    `details` TEXT NOT NULL,
                    `status` TEXT NOT NULL,
                    `adminDecision` TEXT NOT NULL,
                    `timestamp` INTEGER NOT NULL,
                    `resolvedAt` INTEGER
                )
            """.trimIndent())
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_disputes_tenantId_orderId` ON `disputes` (`tenantId`, `orderId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_disputes_tenantId_status` ON `disputes` (`tenantId`, `status`)")
        }

        private fun createPrescriptionTable(db: SupportSQLiteDatabase) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `prescriptions` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `tenantId` TEXT NOT NULL,
                    `orderId` INTEGER NOT NULL,
                    `clientId` INTEGER NOT NULL,
                    `medicineName` TEXT NOT NULL,
                    `isRequired` INTEGER NOT NULL,
                    `hasAttachment` INTEGER NOT NULL,
                    `status` TEXT NOT NULL,
                    `rejectionReason` TEXT NOT NULL,
                    `timestamp` INTEGER NOT NULL
                )
            """.trimIndent())
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_prescriptions_tenantId_orderId` ON `prescriptions` (`tenantId`, `orderId`)")
        }

        private fun createPharmacyVerificationTable(db: SupportSQLiteDatabase) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `pharmacy_verifications` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `tenantId` TEXT NOT NULL,
                    `merchantId` INTEGER NOT NULL,
                    `pharmacyName` TEXT NOT NULL,
                    `licenseNumber` TEXT NOT NULL,
                    `city` TEXT NOT NULL,
                    `status` TEXT NOT NULL,
                    `rejectionReason` TEXT NOT NULL,
                    `timestamp` INTEGER NOT NULL
                )
            """.trimIndent())
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_pharmacy_verifications_tenantId_merchantId` ON `pharmacy_verifications` (`tenantId`, `merchantId`)")
        }

        private fun addMinorUnitColumns(db: SupportSQLiteDatabase) {
            addColumnIfMissing(db, "users", "walletBalanceMinor", "INTEGER NOT NULL DEFAULT 0")
            addColumnIfMissing(db, "products", "priceMinor", "INTEGER NOT NULL DEFAULT 0")
            addColumnIfMissing(db, "products", "purchaseCostMinor", "INTEGER NOT NULL DEFAULT 0")
            addColumnIfMissing(db, "orders", "totalPriceMinor", "INTEGER NOT NULL DEFAULT 0")
            addColumnIfMissing(db, "orders", "deliveryFeeMinor", "INTEGER NOT NULL DEFAULT 0")
            addColumnIfMissing(db, "orders", "commissionAmountMinor", "INTEGER NOT NULL DEFAULT 0")
            addColumnIfMissing(db, "orders", "originalPriceAtRequestMinor", "INTEGER NOT NULL DEFAULT 0")
            addColumnIfMissing(db, "orders", "serverUpdatedPriceConflictMinor", "INTEGER NOT NULL DEFAULT 0")
            addColumnIfMissing(db, "transactions", "amountMinor", "INTEGER NOT NULL DEFAULT 0")
            addColumnIfMissing(db, "ledger_entries", "amountMinor", "INTEGER NOT NULL DEFAULT 0")
            addColumnIfMissing(db, "pharmacy_offers", "priceMinor", "INTEGER NOT NULL DEFAULT 0")
        }

        private fun backfillMinorUnitColumns(db: SupportSQLiteDatabase) {
            if (tableExists(db, "users")) db.execSQL("UPDATE `users` SET walletBalanceMinor = CAST(ROUND(walletBalance * 100.0) AS INTEGER) WHERE walletBalanceMinor = 0 AND walletBalance != 0")
            if (tableExists(db, "products")) {
                db.execSQL("UPDATE `products` SET priceMinor = CAST(ROUND(price * 100.0) AS INTEGER) WHERE priceMinor = 0 AND price != 0")
                db.execSQL("UPDATE `products` SET purchaseCostMinor = CAST(ROUND(purchaseCost * 100.0) AS INTEGER) WHERE purchaseCostMinor = 0 AND purchaseCost != 0")
            }
            if (tableExists(db, "orders")) {
                db.execSQL("UPDATE `orders` SET totalPriceMinor = CAST(ROUND(totalPrice * 100.0) AS INTEGER) WHERE totalPriceMinor = 0 AND totalPrice != 0")
                db.execSQL("UPDATE `orders` SET deliveryFeeMinor = CAST(ROUND(deliveryFee * 100.0) AS INTEGER) WHERE deliveryFeeMinor = 0 AND deliveryFee != 0")
                db.execSQL("UPDATE `orders` SET commissionAmountMinor = CAST(ROUND(commissionAmount * 100.0) AS INTEGER) WHERE commissionAmountMinor = 0 AND commissionAmount != 0")
                db.execSQL("UPDATE `orders` SET originalPriceAtRequestMinor = CAST(ROUND(originalPriceAtRequest * 100.0) AS INTEGER) WHERE originalPriceAtRequestMinor = 0 AND originalPriceAtRequest != 0")
                db.execSQL("UPDATE `orders` SET serverUpdatedPriceConflictMinor = CAST(ROUND(serverUpdatedPriceConflict * 100.0) AS INTEGER) WHERE serverUpdatedPriceConflictMinor = 0 AND serverUpdatedPriceConflict != 0")
            }
            if (tableExists(db, "transactions")) db.execSQL("UPDATE `transactions` SET amountMinor = CAST(ROUND(amount * 100.0) AS INTEGER) WHERE amountMinor = 0 AND amount != 0")
            if (tableExists(db, "ledger_entries")) db.execSQL("UPDATE `ledger_entries` SET amountMinor = CAST(ROUND(amount * 100.0) AS INTEGER) WHERE amountMinor = 0 AND amount != 0")
            if (tableExists(db, "pharmacy_offers")) db.execSQL("UPDATE `pharmacy_offers` SET priceMinor = CAST(ROUND(price * 100.0) AS INTEGER) WHERE priceMinor = 0 AND price != 0")
        }
    }
}
