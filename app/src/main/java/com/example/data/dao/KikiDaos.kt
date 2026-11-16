package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.entity.AuditLogEntity
import com.example.data.entity.CustomerEntity
import com.example.data.entity.DebtPaymentEntity
import com.example.data.entity.ExpenseEntity
import com.example.data.entity.PaymentAllocationEntity
import com.example.data.entity.ProductEntity
import com.example.data.entity.PurchaseEntity
import com.example.data.entity.ReturnEntity
import com.example.data.entity.SaleEntity
import com.example.data.entity.SaleItemEntity
import com.example.data.entity.SaleWithItems
import com.example.data.entity.StockMovementEntity
import com.example.data.entity.SupplierEntity
import com.example.data.entity.UserEntity
import com.example.data.entity.WalletTransferEntity
import com.example.data.entity.ZReportEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Query("SELECT * FROM products ORDER BY sortOrder ASC, id ASC")
    fun observeAll(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products ORDER BY sortOrder ASC, id ASC")
    suspend fun getAllOnce(): List<ProductEntity>

    @Query("SELECT COUNT(*) FROM products")
    suspend fun count(): Int

    @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): ProductEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(products: List<ProductEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(product: ProductEntity): Long

    @Update
    suspend fun update(product: ProductEntity)

    @Query("DELETE FROM products WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE products SET stockQuantity = stockQuantity + :delta, updatedAt = :updatedAt WHERE id = :productId")
    suspend fun adjustStock(productId: Long, delta: Double, updatedAt: Long = System.currentTimeMillis())
}

@Dao
interface SaleDao {
    @Transaction
    @Query("SELECT * FROM sales ORDER BY timestamp DESC")
    fun observeAllWithItems(): Flow<List<SaleWithItems>>

    @Transaction
    @Query("SELECT * FROM sales ORDER BY timestamp DESC")
    suspend fun getAllWithItemsOnce(): List<SaleWithItems>

    @Query("SELECT COUNT(*) FROM sales")
    suspend fun count(): Int

    @Query("SELECT * FROM sales WHERE id = :saleId LIMIT 1")
    suspend fun getById(saleId: Long): SaleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSale(sale: SaleEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSaleItems(items: List<SaleItemEntity>)

    @Transaction
    suspend fun insertSaleWithItems(sale: SaleEntity, items: List<SaleItemEntity>): Long {
        val saleId = insertSale(sale)
        insertSaleItems(items.map { it.copy(saleId = saleId) })
        return saleId
    }

    @Query("SELECT * FROM sale_items WHERE saleId = :saleId")
    suspend fun getItemsForSale(saleId: Long): List<SaleItemEntity>

    @Query("UPDATE sales SET isVoided = 1 WHERE id = :saleId")
    suspend fun markVoided(saleId: Long)
}

@Dao
interface PurchaseDao {
    @Query("SELECT * FROM purchases ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<PurchaseEntity>>

    @Query("SELECT * FROM purchases ORDER BY timestamp DESC")
    suspend fun getAllOnce(): List<PurchaseEntity>

    @Query("SELECT COUNT(*) FROM purchases")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(purchase: PurchaseEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<PurchaseEntity>)
}

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses ORDER BY timestamp DESC")
    suspend fun getAllOnce(): List<ExpenseEntity>

    @Query("SELECT COUNT(*) FROM expenses")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(expense: ExpenseEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ExpenseEntity>)
}

@Dao
interface PaymentAllocationDao {
    @Query("SELECT * FROM payment_allocations ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<PaymentAllocationEntity>>

    @Query("SELECT * FROM payment_allocations ORDER BY timestamp DESC")
    suspend fun getAllOnce(): List<PaymentAllocationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<PaymentAllocationEntity>)
}

@Dao
interface StockMovementDao {
    @Query("SELECT * FROM stock_movements ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<StockMovementEntity>>

    @Query("SELECT * FROM stock_movements ORDER BY timestamp DESC")
    suspend fun getAllOnce(): List<StockMovementEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: StockMovementEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<StockMovementEntity>)
}

@Dao
interface ReturnDao {
    @Query("SELECT * FROM returns ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<ReturnEntity>>

    @Query("SELECT * FROM returns ORDER BY timestamp DESC")
    suspend fun getAllOnce(): List<ReturnEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ReturnEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ReturnEntity>)
}

@Dao
interface PartyDao {
    @Query("SELECT * FROM suppliers ORDER BY name ASC")
    fun observeSuppliers(): Flow<List<SupplierEntity>>

    @Query("SELECT * FROM suppliers ORDER BY name ASC")
    suspend fun getSuppliersOnce(): List<SupplierEntity>

    @Query("SELECT * FROM suppliers WHERE id = :id LIMIT 1")
    suspend fun getSupplierById(id: Long): SupplierEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSupplier(supplier: SupplierEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllSuppliers(items: List<SupplierEntity>)

    @Update
    suspend fun updateSupplier(supplier: SupplierEntity)

    @Query("SELECT * FROM customers ORDER BY name ASC")
    fun observeCustomers(): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers ORDER BY name ASC")
    suspend fun getCustomersOnce(): List<CustomerEntity>

    @Query("SELECT * FROM customers WHERE id = :id LIMIT 1")
    suspend fun getCustomerById(id: Long): CustomerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: CustomerEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllCustomers(items: List<CustomerEntity>)

    @Update
    suspend fun updateCustomer(customer: CustomerEntity)

    @Query("SELECT * FROM debt_payments ORDER BY timestamp DESC")
    fun observeDebtPayments(): Flow<List<DebtPaymentEntity>>

    @Query("SELECT * FROM debt_payments ORDER BY timestamp DESC")
    suspend fun getDebtPaymentsOnce(): List<DebtPaymentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDebtPayment(payment: DebtPaymentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllDebtPayments(items: List<DebtPaymentEntity>)
}

@Dao
interface WalletTransferDao {
    @Query("SELECT * FROM wallet_transfers ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<WalletTransferEntity>>

    @Query("SELECT * FROM wallet_transfers ORDER BY timestamp DESC")
    suspend fun getAllOnce(): List<WalletTransferEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transfer: WalletTransferEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<WalletTransferEntity>)
}

@Dao
interface UserDao {
    @Query("SELECT * FROM users ORDER BY isAdmin DESC, id ASC")
    fun observeAll(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users ORDER BY isAdmin DESC, id ASC")
    suspend fun getAllOnce(): List<UserEntity>

    @Query("SELECT COUNT(*) FROM users")
    suspend fun count(): Int

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(users: List<UserEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(user: UserEntity): Long

    @Update
    suspend fun update(user: UserEntity)

    @Query("DELETE FROM users WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface ZReportDao {
    @Query("SELECT * FROM z_reports ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<ZReportEntity>>

    @Query("SELECT * FROM z_reports ORDER BY timestamp DESC")
    suspend fun getAllOnce(): List<ZReportEntity>

    @Query("SELECT * FROM z_reports WHERE isClosed = 1 AND isReopened = 0 ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestActiveClosed(): ZReportEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(report: ZReportEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ZReportEntity>)

    @Update
    suspend fun update(report: ZReportEntity)
}

@Dao
interface AuditLogDao {
    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<AuditLogEntity>>

    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC")
    suspend fun getAllOnce(): List<AuditLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: AuditLogEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<AuditLogEntity>)
}
