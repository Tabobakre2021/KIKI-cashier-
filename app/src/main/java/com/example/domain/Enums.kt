package com.example.domain

enum class ProductCategory(val arabicTitle: String, val shortTitle: String) {
    POULTRY("الدواجن ومشتقاتها", "الدواجن"),
    POULTRY_PROCESSED("مصنعات الفراخ", "مصنعات فراخ"),
    BEEF_PROCESSED("مصنعات اللحوم البقرية", "لحوم بقرية"),
    DAIRY_CHEESE_FISH("الأجبان والألبان والفسيخ", "أجبان وفسيخ"),
    OLIVES_PICKLES_SPICES("الزيتون والمخللات والبهارات", "زيتون وبهارات")
}

enum class PackagingUnit(val arabicLabel: String) {
    RATL("رطل"),
    KILO("كيلو"),
    PIECE("قطعة/حبة"),
    PACK("عبوة/باكت"),
    CARTON("كرتونة"),
    DOZEN_TRAY("دستة/طبق"),
    SACK("شوال/جوال"),
    JERRICAN("جركانة")
}

enum class WalletChannel(val arabicLabel: String) {
    CASH("كاش"),
    BANKAK("بنكك"),
    FAWRY("فوري"),
    OCASH("أوكاش"),
    SAHIL("ساهل")
}

enum class PaymentChannel(val arabicLabel: String, val isBankApp: Boolean = false) {
    CASH("كاش", false),
    BANKAK("بنكك", true),
    FAWRY("فوري", true),
    OCASH("أوكاش", true),
    SAHIL("ساهل", true),
    SPLIT("دفع مشترك", false);

    fun toWalletOrNull(): WalletChannel? = when (this) {
        CASH -> WalletChannel.CASH
        BANKAK -> WalletChannel.BANKAK
        FAWRY -> WalletChannel.FAWRY
        OCASH -> WalletChannel.OCASH
        SAHIL -> WalletChannel.SAHIL
        SPLIT -> null
    }
}

enum class ExpenseCategory(val arabicLabel: String) {
    ELECTRICITY("الكهرباء"),
    RENT("الإيجار"),
    LABOR("العمالة"),
    BAGS_SUPPLIES("الأكياس والتغليف"),
    TRANSPORT("الترحيل"),
    MAINTENANCE("الصيانة"),
    WATER("المياه"),
    OTHER("مصروفات أخرى")
}

enum class PricingMode(val arabicLabel: String) {
    MARKUP("نسبة الزيادة على التكلفة (Markup)"),
    MARGIN("هامش الربح من سعر البيع (Margin)")
}

enum class StockMovementType(val arabicLabel: String, val isInbound: Boolean) {
    OPENING("رصيد افتتاحي", true),
    PURCHASE("توريد مشتريات", true),
    SALE("مبيعات", false),
    CUSTOMER_RETURN("مرتجع عميل", true),
    SUPPLIER_RETURN("مرتجع لمورد", false),
    DAMAGED("تالف", false),
    EXPIRED("منتهي الصلاحية", false),
    ADJUSTMENT_IN("تسوية بالزيادة", true),
    ADJUSTMENT_OUT("تسوية بالنقصان", false)
}

enum class DateFilter(val arabicLabel: String) {
    TODAY("اليوم"),
    YESTERDAY("أمس"),
    LAST_7_DAYS("الأسبوع"),
    THIS_MONTH("الشهر"),
    CUSTOM("فترة مخصصة"),
    ALL("الكل")
}

enum class AuditActionType(val arabicLabel: String) {
    SALE_CREATED("تسجيل فاتورة بيع"),
    SALE_RETURNED("مرتجع مبيعات"),
    SALE_VOIDED("إلغاء/عكس فاتورة"),
    PURCHASE_CREATED("فاتورة مشتريات"),
    PRICE_CHANGED("تعديل سعر بيع"),
    COST_CHANGED("تعديل تكلفة شراء"),
    EXPENSE_CREATED("تسجيل مصروف"),
    STOCK_ADJUSTED("تعديل / تسوية مخزون"),
    ZREPORT_CLOSED("تقفيل يومية (Z-Report)"),
    ZREPORT_REOPENED("إعادة فتح يومية"),
    USER_UPDATED("تحديث مستخدم وصلاحيات"),
    DEBT_PAYMENT("سداد / تحصيل مديونية"),
    WALLET_TRANSFER("تحويل بين المحافظ"),
    BACKUP_CREATED("إنشاء نسخة احتياطية"),
    BACKUP_RESTORED("استعادة نسخة احتياطية"),
    USER_LOGIN("تسجيل دخول"),
    USER_LOGOUT("تسجيل خروج")
}

enum class BackupFrequency(val arabicLabel: String) {
    OFF("يدوي فقط"),
    DAILY("يومي تلقائي"),
    WEEKLY("أسبوعي تلقائي")
}

enum class ThemePreference(val arabicLabel: String) {
    SYSTEM("حسب النظام"),
    LIGHT("الوضع الفاتح"),
    DARK("الوضع الداكن")
}
