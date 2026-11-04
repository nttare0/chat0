package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ConduitDao {

    @Query("SELECT * FROM bridges ORDER BY networkId ASC")
    fun observeBridges(): Flow<List<BridgeAccountEntity>>

    @Query("SELECT * FROM bridges")
    suspend fun getAllBridgesOnce(): List<BridgeAccountEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBridges(bridges: List<BridgeAccountEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBridge(bridge: BridgeAccountEntity)

    @Query(
        """
        SELECT * FROM conversations
        WHERE isArchived = 0
        ORDER BY isPinned DESC, lastMessageTimestamp DESC
        """
    )
    fun observeActiveConversations(): Flow<List<ConversationEntity>>

    @Query(
        """
        SELECT * FROM conversations
        ORDER BY isPinned DESC, lastMessageTimestamp DESC
        """
    )
    fun observeAllConversations(): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE id = :conversationId LIMIT 1")
    fun observeConversationById(conversationId: Long): Flow<ConversationEntity?>

    @Query("SELECT * FROM conversations WHERE id = :conversationId LIMIT 1")
    suspend fun getConversationByIdOnce(conversationId: Long): ConversationEntity?

    @Query("SELECT COUNT(*) FROM conversations")
    suspend fun countConversations(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conversation: ConversationEntity): Long

    @Update
    suspend fun updateConversation(conversation: ConversationEntity)

    @Query("UPDATE conversations SET unreadCount = 0 WHERE id = :conversationId")
    suspend fun markConversationRead(conversationId: Long)

    @Query("UPDATE conversations SET isPinned = :pinned WHERE id = :conversationId")
    suspend fun setConversationPinned(conversationId: Long, pinned: Boolean)

    @Query("UPDATE conversations SET isArchived = :archived WHERE id = :conversationId")
    suspend fun setConversationArchived(conversationId: Long, archived: Boolean)

    @Query("UPDATE conversations SET isMuted = :muted WHERE id = :conversationId")
    suspend fun setConversationMuted(conversationId: Long, muted: Boolean)

    @Query("UPDATE conversations SET isKeyVerified = :verified WHERE id = :conversationId")
    suspend fun setConversationVerified(conversationId: Long, verified: Boolean)

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    fun observeMessagesForConversation(conversationId: Long): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages ORDER BY timestamp DESC")
    fun observeAllMessages(): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages")
    suspend fun getAllMessagesOnce(): List<MessageEntity>

    @Query("SELECT COUNT(*) FROM messages")
    fun observeTotalEncryptedMessageCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity): Long
}

@Database(
    entities = [
        BridgeAccountEntity::class,
        ConversationEntity::class,
        MessageEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class ConduitDatabase : RoomDatabase() {
    abstract fun conduitDao(): ConduitDao

    companion object {
        @Volatile
        private var INSTANCE: ConduitDatabase? = null

        fun getInstance(context: Context): ConduitDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    ConduitDatabase::class.java,
                    "conduit_encrypted_vault.db"
                )
                    .fallbackToDestructiveMigration(true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
