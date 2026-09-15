package com.omnichat.arena.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey val id: String,
    val title: String,
    /** comma-separated ProviderIds, e.g. "GEMINI,CLAUDE" */
    val providerIds: String,
    val mode: String, // SOLO | ARENA
    val createdAt: Long,
    val updatedAt: Long,
    val pinned: Boolean = false,
)

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val role: String, // USER | AI | VERDICT
    val providerId: String?,
    val modelName: String?,
    val text: String,
    val latencyMs: Long?,
    val status: String, // DONE | ERROR
    val createdAt: Long,
)

@Dao
interface ChatDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertConversation(c: ConversationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(m: MessageEntity)

    @Query("SELECT * FROM conversations ORDER BY pinned DESC, updatedAt DESC")
    fun observeConversations(): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM messages WHERE conversationId = :cid ORDER BY createdAt ASC")
    fun observeMessages(cid: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE conversationId = :cid ORDER BY createdAt ASC")
    suspend fun getMessages(cid: String): List<MessageEntity>
}

@Database(entities = [ConversationEntity::class, MessageEntity::class], version = 1, exportSchema = false)
abstract class ChatDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao
}
