package com.cristobalcariqueo.defensadedeudores.di

import androidx.room.Room
import com.cristobalcariqueo.defensadedeudores.data.local.DefensaDatabase
import com.cristobalcariqueo.defensadedeudores.data.remote.SupabaseClientProvider
import com.cristobalcariqueo.defensadedeudores.data.repository.AuthRepository
import com.cristobalcariqueo.defensadedeudores.data.repository.AuthRepositoryImpl
import com.cristobalcariqueo.defensadedeudores.data.repository.PersonRepository
import com.cristobalcariqueo.defensadedeudores.data.repository.PersonRepositoryImpl
import com.cristobalcariqueo.defensadedeudores.data.repository.RegistryRepository
import com.cristobalcariqueo.defensadedeudores.data.repository.RegistryRepositoryImpl
import com.cristobalcariqueo.defensadedeudores.data.repository.SettingsRepository
import com.cristobalcariqueo.defensadedeudores.data.repository.SettingsRepositoryImpl
import com.cristobalcariqueo.defensadedeudores.data.repository.SourceRepository
import com.cristobalcariqueo.defensadedeudores.data.repository.SourceRepositoryImpl
import com.cristobalcariqueo.defensadedeudores.data.repository.TrackRepository
import com.cristobalcariqueo.defensadedeudores.data.repository.TrackRepositoryImpl
import com.cristobalcariqueo.defensadedeudores.data.sync.ConnectivityObserver
import com.cristobalcariqueo.defensadedeudores.data.sync.SyncEngine
import com.cristobalcariqueo.defensadedeudores.ui.screens.config.ConfigViewModel
import com.cristobalcariqueo.defensadedeudores.ui.screens.conflicts.ConflictsViewModel
import com.cristobalcariqueo.defensadedeudores.ui.screens.main.MainViewModel
import com.cristobalcariqueo.defensadedeudores.ui.screens.people.PeopleViewModel
import com.cristobalcariqueo.defensadedeudores.ui.screens.sources.SourcesViewModel
import com.cristobalcariqueo.defensadedeudores.ui.screens.starting.StartingViewModel
import com.cristobalcariqueo.defensadedeudores.ui.screens.track.TrackViewModel
import com.cristobalcariqueo.defensadedeudores.ui.screens.trackconfig.TrackConfigViewModel
import io.github.jan.supabase.SupabaseClient
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

// Track/Registry repositories get added as their implementations land -- tasks #6, #7.
val appModule = module {
    single<SupabaseClient> { SupabaseClientProvider.client }

    single {
        Room.databaseBuilder(androidContext(), DefensaDatabase::class.java, DefensaDatabase.NAME)
            // Pre-release: no shipped installs to migrate yet
            .fallbackToDestructiveMigration()
            .build()
    }
    single { get<DefensaDatabase>().personDao() }
    single { get<DefensaDatabase>().sourceDao() }
    single { get<DefensaDatabase>().settingsDao() }
    single { get<DefensaDatabase>().trackDao() }
    single { get<DefensaDatabase>().registryDao() }
    single { get<DefensaDatabase>().syncDao() }

    single<PersonRepository> { PersonRepositoryImpl(get(), get()) }
    single<SourceRepository> { SourceRepositoryImpl(get(), get()) }
    single<SettingsRepository> { SettingsRepositoryImpl(get()) }
    single<TrackRepository> { TrackRepositoryImpl(get()) }
    single<RegistryRepository> { RegistryRepositoryImpl(get(), get()) }
    single<AuthRepository> { AuthRepositoryImpl(get()) }
    single { ConnectivityObserver(androidContext()) }
    single { SyncEngine(get(), get(), get(), get()) }

    viewModel { PeopleViewModel(get()) }
    viewModel { SourcesViewModel(get()) }
    viewModel { StartingViewModel(get(), get(), get()) }
    viewModel { MainViewModel(get(), get()) }
    viewModel { (trackId: String) -> TrackViewModel(trackId, get(), get(), get(), get(), get()) }
    viewModel { (trackId: String) -> TrackConfigViewModel(trackId, get(), get(), get()) }
    viewModel { ConfigViewModel(get(), get(), get(), get()) }
    viewModel { ConflictsViewModel(get(), get()) }
}
