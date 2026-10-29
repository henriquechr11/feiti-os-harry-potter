package com.example.harry_potter.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "users", indices = [Index(value = ["email"], unique = true)])
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val email: String,
    val passwordHash: String
)

@Entity(
    tableName = "favorites",
    primaryKeys = ["userId", "spellId"],
    foreignKeys = [ForeignKey(
        entity = UserEntity::class,
        parentColumns = ["id"],
        childColumns = ["userId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class FavoriteEntity(
    val userId: Long,
    val spellId: String,
    val spellName: String,
    val incantation: String?,
    val category: String?,
    val effect: String?,
    val hand: String?
)

@Entity(
    tableName = "battle_results",
    indices = [Index("userId")],
    foreignKeys = [ForeignKey(
        entity = UserEntity::class,
        parentColumns = ["id"],
        childColumns = ["userId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class BattleResultEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val opponentName: String,
    val difficulty: String,
    val won: Boolean,
    val turns: Int,
    val playedAt: Long
)

@Dao
interface UserDao {
    @Insert
    suspend fun insert(user: UserEntity): Long

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun findByEmail(email: String): UserEntity?
}

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorites WHERE userId = :userId ORDER BY spellName")
    fun observeForUser(userId: Long): Flow<List<FavoriteEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE userId = :userId AND spellId = :spellId)")
    fun isFavorite(userId: Long, spellId: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE userId = :userId AND spellId = :spellId")
    suspend fun delete(userId: Long, spellId: String)
}

@Dao
interface BattleDao {
    @Insert
    suspend fun insert(result: BattleResultEntity)

    @Query("SELECT * FROM battle_results WHERE userId = :userId ORDER BY playedAt DESC")
    fun observeForUser(userId: Long): Flow<List<BattleResultEntity>>
}

@Database(
    entities = [UserEntity::class, FavoriteEntity::class, BattleResultEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun battleDao(): BattleDao

    companion object {
        // Adds the duel history without wiping existing users and favorites.
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `battle_results` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`userId` INTEGER NOT NULL, `opponentName` TEXT NOT NULL, `difficulty` TEXT NOT NULL, " +
                        "`won` INTEGER NOT NULL, `turns` INTEGER NOT NULL, `playedAt` INTEGER NOT NULL, " +
                        "FOREIGN KEY(`userId`) REFERENCES `users`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_battle_results_userId` ON `battle_results` (`userId`)")
            }
        }

        fun create(context: Context): AppDatabase = Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "spellbook.db"
        ).addMigrations(MIGRATION_1_2).build()
    }
}
