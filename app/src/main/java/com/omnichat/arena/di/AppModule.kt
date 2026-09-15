package com.omnichat.arena.di

import android.content.Context
import androidx.room.Room
import com.omnichat.arena.core.AiProvider
import com.omnichat.arena.core.FakeProvider
import com.omnichat.arena.data.ChatDatabase
import com.omnichat.arena.data.EncryptedSecretStore
import com.omnichat.arena.data.SecretStore
import com.omnichat.arena.providers.DuckAiProvider
import com.omnichat.arena.providers.GeminiSessionProvider
import com.omnichat.arena.providers.GroqProvider
import com.omnichat.arena.providers.PollinationsProvider
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import dagger.multibindings.StringKey
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides @Singleton
    fun okHttp(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS) // SSE streams stay open
        .writeTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    @Provides @Singleton
    fun database(
        @ApplicationContext ctx: Context,
        secrets: SecretStore,
    ): ChatDatabase {
        // DB passphrase stored in Keystore-backed prefs; generated once.
        var pass = secrets.getRaw("OMNI_DB_PASSPHRASE")
        if (pass.isNullOrBlank()) {
            pass = java.util.UUID.randomUUID().toString() + java.util.UUID.randomUUID().toString()
            secrets.setRaw("OMNI_DB_PASSPHRASE", pass)
        }
        val factory = SupportFactory(SQLiteDatabase.getBytes(pass.toCharArray()))
        return Room.databaseBuilder(ctx, ChatDatabase::class.java, "omni-chat.db")
            .openHelperFactory(factory)
            .build()
    }

    @Provides @Singleton
    fun chatDao(db: ChatDatabase) = db.chatDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class ProviderModule {
    @Binds @Singleton
    abstract fun secrets(impl: EncryptedSecretStore): SecretStore

    @Binds @IntoMap @StringKey("FAKE") @Singleton
    abstract fun fake(p: FakeProvider): AiProvider

    @Binds @IntoMap @StringKey("GEMINI") @Singleton
    abstract fun gemini(p: GeminiSessionProvider): AiProvider

    @Binds @IntoMap @StringKey("DUCK") @Singleton
    abstract fun duck(p: DuckAiProvider): AiProvider

    @Binds @IntoMap @StringKey("POLLINATIONS") @Singleton
    abstract fun pollinations(p: PollinationsProvider): AiProvider

    @Binds @IntoMap @StringKey("GROQ") @Singleton
    abstract fun groq(p: GroqProvider): AiProvider
    // Sessions next: DEEPSEEK_SES, PERPLEXITY, CLAUDE, GROK — one @Binds line each.
}
