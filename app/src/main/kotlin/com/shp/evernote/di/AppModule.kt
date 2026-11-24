package com.shp.evernote.di

import android.content.Context
import androidx.room.Room
import com.shp.evernote.auth.AuthProvider
import com.shp.evernote.auth.LocalOnlyAuthProvider
import com.shp.evernote.data.files.FilePhotoStore
import com.shp.evernote.data.files.PhotoStore
import com.shp.evernote.data.local.AppDatabase
import com.shp.evernote.data.local.NoteDao
import com.shp.evernote.data.repository.NoteRepository
import com.shp.evernote.data.repository.OfflineNoteRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun database(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.NAME).build()

    @Provides
    fun noteDao(db: AppDatabase): NoteDao = db.noteDao()

    @Provides
    @IoDispatcher
    fun ioDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides
    @Singleton
    fun authProvider(): AuthProvider = LocalOnlyAuthProvider()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class BindingsModule {
    @Binds
    abstract fun noteRepository(impl: OfflineNoteRepository): NoteRepository

    @Binds
    abstract fun photoStore(impl: FilePhotoStore): PhotoStore
}
