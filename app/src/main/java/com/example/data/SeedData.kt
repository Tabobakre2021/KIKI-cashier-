package com.example.data

import com.example.data.entity.ProductEntity
import com.example.data.entity.UserEntity
import com.example.domain.PackagingUnit
import com.example.domain.ProductCategory

object SeedData {

    fun defaultUsers(): List<UserEntity> = listOf(
        UserEntity(
            displayName = "المدير العام",
            roleTitle = "مدير النظام (Admin)",
            pinCode = "1234",
            mustChangePin = true, // Enforces new PIN creation on first launch per Section 22.6
            isAdmin = true,
            canRecordSales = true,
            canRecordPurchases = true,
            canRecordExpenses = true,
            canEditPrices = true,
            canAddProducts = true,
            canEditProducts = true,
            canReverseOrDelete = true,
            canViewInventory = true,
            canManageParties = true,
            canViewSalesReports = true,
            canViewZReport = true,
            canExportData = true,
            canManageUsers = true,
            canReopenZReport = true,
            canManageBackup = true,
            canViewProfits = true,
            isActive = true
        ),
        UserEntity(
            displayName = "كاشير الكاونتر",
            roleTitle = "كاشير مبيعات",
            pinCode = "0000",
            mustChangePin = false,
            isAdmin = false,
            canRecordSales = true,
            canRecordPurchases = false,
            canRecordExpenses = true,
            canEditPrices = false,
            canAddProducts = false,
            canEditProducts = false,
            canReverseOrDelete = false,
            canViewInventory = true,
            canManageParties = false,
            canViewSalesReports = false,
            canViewZReport = false,
            canExportData = false,
            canManageUsers = false,
            canReopenZReport = false,
            canManageBackup = false,
            canViewProfits = false, // Exclusive Admin Profit Protection active
            isActive = true
        )
    )

    fun defaultProducts(): List<ProductEntity> = listOf(
        // أولاً: الدواجن ومشتقاتها (1 - 9)
        ProductEntity(name = "فراخ", category = ProductCategory.POULTRY, defaultUnit = PackagingUnit.KILO, sellingPrice = 26000L, sortOrder = 1),
        ProductEntity(name = "بيض", category = ProductCategory.POULTRY, defaultUnit = PackagingUnit.DOZEN_TRAY, sellingPrice = 0L, sortOrder = 2),
        ProductEntity(name = "صدور فيليه", category = ProductCategory.POULTRY, defaultUnit = PackagingUnit.KILO, sellingPrice = 0L, sortOrder = 3),
        ProductEntity(name = "أفخاذ", category = ProductCategory.POULTRY, defaultUnit = PackagingUnit.KILO, sellingPrice = 0L, sortOrder = 4),
        ProductEntity(name = "أجنحة", category = ProductCategory.POULTRY, defaultUnit = PackagingUnit.KILO, sellingPrice = 0L, sortOrder = 5),
        ProductEntity(name = "كبدة", category = ProductCategory.POULTRY, defaultUnit = PackagingUnit.KILO, sellingPrice = 0L, sortOrder = 6),
        ProductEntity(name = "قوانص", category = ProductCategory.POULTRY, defaultUnit = PackagingUnit.KILO, sellingPrice = 0L, sortOrder = 7),
        ProductEntity(name = "قلوب", category = ProductCategory.POULTRY, defaultUnit = PackagingUnit.KILO, sellingPrice = 0L, sortOrder = 8),
        ProductEntity(name = "رقاب", category = ProductCategory.POULTRY, defaultUnit = PackagingUnit.KILO, sellingPrice = 0L, sortOrder = 9),

        // ثانياً: مصنعات الفراخ (10 - 13)
        ProductEntity(name = "سجوك فراخ 350ج", category = ProductCategory.POULTRY_PROCESSED, defaultUnit = PackagingUnit.PACK, sellingPrice = 19000L, sortOrder = 10),
        ProductEntity(name = "كفتة فراخ 350ج", category = ProductCategory.POULTRY_PROCESSED, defaultUnit = PackagingUnit.PACK, sellingPrice = 19000L, sortOrder = 11),
        ProductEntity(name = "بيرقر فراخ 350ج", category = ProductCategory.POULTRY_PROCESSED, defaultUnit = PackagingUnit.PACK, sellingPrice = 0L, sortOrder = 12),
        ProductEntity(name = "هوت دوغ سعودي", category = ProductCategory.POULTRY_PROCESSED, defaultUnit = PackagingUnit.PACK, sellingPrice = 12000L, sortOrder = 13),

        // ثالثاً: مصنعات اللحوم البقرية (14 - 22)
        ProductEntity(name = "سجوك بقري 500ج", category = ProductCategory.BEEF_PROCESSED, defaultUnit = PackagingUnit.PACK, sellingPrice = 0L, sortOrder = 14),
        ProductEntity(name = "سجوك بقري 350ج", category = ProductCategory.BEEF_PROCESSED, defaultUnit = PackagingUnit.PACK, sellingPrice = 20000L, sortOrder = 15),
        ProductEntity(name = "كفتة بقري 500ج", category = ProductCategory.BEEF_PROCESSED, defaultUnit = PackagingUnit.PACK, sellingPrice = 25000L, sortOrder = 16),
        ProductEntity(name = "كفتة بقري 350ج", category = ProductCategory.BEEF_PROCESSED, defaultUnit = PackagingUnit.PACK, sellingPrice = 20000L, sortOrder = 17),
        ProductEntity(name = "بيرقر بقري 1 كيلو", category = ProductCategory.BEEF_PROCESSED, defaultUnit = PackagingUnit.PACK, sellingPrice = 38000L, sortOrder = 18),
        ProductEntity(name = "بيرقر بقري 500ج", category = ProductCategory.BEEF_PROCESSED, defaultUnit = PackagingUnit.PACK, sellingPrice = 25000L, sortOrder = 19),
        ProductEntity(name = "بيرقر بقري 350ج", category = ProductCategory.BEEF_PROCESSED, defaultUnit = PackagingUnit.PACK, sellingPrice = 0L, sortOrder = 20),
        ProductEntity(name = "هوت دوغ بقري 1 كيلو", category = ProductCategory.BEEF_PROCESSED, defaultUnit = PackagingUnit.PACK, sellingPrice = 0L, sortOrder = 21),
        ProductEntity(name = "لحمة مفرومة", category = ProductCategory.BEEF_PROCESSED, defaultUnit = PackagingUnit.KILO, sellingPrice = 0L, sortOrder = 22),

        // رابعاً: الأجبان والألبان والفسيخ (23 - 29)
        ProductEntity(name = "جبنة الدويم", category = ProductCategory.DAIRY_CHEESE_FISH, defaultUnit = PackagingUnit.KILO, sellingPrice = 46000L, sortOrder = 23),
        ProductEntity(name = "جبنة مضفرة", category = ProductCategory.DAIRY_CHEESE_FISH, defaultUnit = PackagingUnit.KILO, sellingPrice = 0L, sortOrder = 24),
        ProductEntity(name = "جبنة موزريلا", category = ProductCategory.DAIRY_CHEESE_FISH, defaultUnit = PackagingUnit.KILO, sellingPrice = 60000L, sortOrder = 25),
        ProductEntity(name = "مش بلدي", category = ProductCategory.DAIRY_CHEESE_FISH, defaultUnit = PackagingUnit.KILO, sellingPrice = 16000L, sortOrder = 26),
        ProductEntity(name = "زبادي بلدي رطل", category = ProductCategory.DAIRY_CHEESE_FISH, defaultUnit = PackagingUnit.RATL, sellingPrice = 4000L, sortOrder = 27),
        ProductEntity(name = "حليب طازج رطل", category = ProductCategory.DAIRY_CHEESE_FISH, defaultUnit = PackagingUnit.RATL, sellingPrice = 2500L, sortOrder = 28),
        ProductEntity(name = "فسيخ", category = ProductCategory.DAIRY_CHEESE_FISH, defaultUnit = PackagingUnit.KILO, sellingPrice = 20000L, sortOrder = 29),

        // خامساً: الزيتون والمخللات والبهارات (30 - 35)
        ProductEntity(name = "زيتون شرائح", category = ProductCategory.OLIVES_PICKLES_SPICES, defaultUnit = PackagingUnit.KILO, sellingPrice = 24000L, sortOrder = 30),
        ProductEntity(name = "زيتون حب", category = ProductCategory.OLIVES_PICKLES_SPICES, defaultUnit = PackagingUnit.KILO, sellingPrice = 30000L, sortOrder = 31),
        ProductEntity(name = "مخلل", category = ProductCategory.OLIVES_PICKLES_SPICES, defaultUnit = PackagingUnit.KILO, sellingPrice = 0L, sortOrder = 32),
        ProductEntity(name = "دكوة", category = ProductCategory.OLIVES_PICKLES_SPICES, defaultUnit = PackagingUnit.KILO, sellingPrice = 34000L, sortOrder = 33),
        ProductEntity(name = "فراولة مجمدة", category = ProductCategory.OLIVES_PICKLES_SPICES, defaultUnit = PackagingUnit.KILO, sellingPrice = 0L, sortOrder = 34),
        ProductEntity(name = "بهارات سولو", category = ProductCategory.OLIVES_PICKLES_SPICES, defaultUnit = PackagingUnit.PACK, sellingPrice = 0L, sortOrder = 35)
    )
}
