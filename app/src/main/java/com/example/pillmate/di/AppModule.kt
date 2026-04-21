package com.example.pillmate.di

import android.app.Application
import com.example.pillmate.data.remote.firebase.FirestoreLogRepository
import com.example.pillmate.data.remote.firebase.FirestoreMedicationRepository
import com.example.pillmate.data.remote.firebase.FirestoreScheduleRepository
import com.example.pillmate.domain.repository.LogRepository
import com.example.pillmate.domain.repository.MedicationRepository
import com.example.pillmate.domain.repository.ScheduleRepository
import com.example.pillmate.domain.usecase.LogTaskUseCase
import com.example.pillmate.presentation.viewmodel.HomeViewModel
import com.example.pillmate.presentation.viewmodel.ReminderViewModel
import com.example.pillmate.presentation.viewmodel.TaskLogViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import com.example.pillmate.data.local.database.AppDatabase
import com.example.pillmate.data.repository.CabinetRepositoryImpl
import com.example.pillmate.domain.repository.CabinetRepository
import com.example.pillmate.presentation.viewmodel.CabinetViewModel
import org.koin.android.ext.koin.androidContext
import com.example.pillmate.data.remote.api.OpenFdaApi
import com.example.pillmate.data.repository.DrugLibraryRepositoryImpl
import com.example.pillmate.domain.repository.DrugLibraryRepository
import com.example.pillmate.presentation.viewmodel.DrugLibraryViewModel
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.example.pillmate.domain.usecase.*
import com.example.pillmate.notification.TaskNotificationManager
import com.example.pillmate.presentation.viewmodel.AuthViewModel
import com.example.pillmate.presentation.viewmodel.DebugViewModel
import com.example.pillmate.util.AlarmTracker
import com.example.pillmate.util.DataGenerator

val appModule = module {
    single { FirebaseAuth.getInstance() }
    single { FirebaseFirestore.getInstance() }
    
    // Provide profileId dynamically from current user
    factory { get<FirebaseAuth>().currentUser?.uid ?: "" }
    factory { DataGenerator(get()) }

    single<MedicationRepository> { FirestoreMedicationRepository(get(), get()) }
    single<LogRepository> { FirestoreLogRepository(get()) }
    single<ScheduleRepository> { FirestoreScheduleRepository(get()) }
    
    single { AlarmTracker(get()) }
    
    factory { LogTaskUseCase(get(), get(), get()) }
    factory { GetHomeTasksUseCase(get(), get()) }
    factory { CreateScheduleUseCase(get()) }
    factory { UpdateScheduleUseCase(get()) }
    factory { ManageReminderUseCase(get(), get()) }
    factory { SyncAlarmsUseCase(get(), get(), get()) }
    
    viewModel { (profileId: String) -> TaskLogViewModel(get(), get(), profileId) }
    
    single { TaskNotificationManager(get()) }

    single { AppDatabase.getDatabase(androidContext()) }
    single { get<AppDatabase>().medicationDao() }
    single { get<AppDatabase>().supplyLogDao() }
    single<CabinetRepository> { CabinetRepositoryImpl(get(), get()) }
    
    single {
        Retrofit.Builder()
            .baseUrl("https://api.fda.gov/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OpenFdaApi::class.java)
    }
    
    single<DrugLibraryRepository> { DrugLibraryRepositoryImpl(get()) }
}

val viewModelModule = module {
    viewModel { HomeViewModel(get(), get(), get(), get()) }
    viewModel { TaskLogViewModel(get(), get(), get()) }
    viewModel { ReminderViewModel(get(), get(), get()) }
    viewModel { DebugViewModel(get(), get(), get(), get(), get(), get(), get(), get()) }
    viewModel { CabinetViewModel(get(), androidContext() as Application) }
    viewModel { DrugLibraryViewModel(get(), androidContext() as Application) }
    viewModel { AuthViewModel(get(), get(), get(), get()) }
}
