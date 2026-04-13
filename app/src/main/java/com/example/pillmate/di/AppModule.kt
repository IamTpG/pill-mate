package com.example.pillmate.di

import com.example.pillmate.data.remote.firebase.FirestoreLogRepository
import com.example.pillmate.data.remote.firebase.FirestoreMedicationRepository
import com.example.pillmate.data.remote.firebase.FirestoreScheduleRepository
import com.example.pillmate.domain.repository.LogRepository
import com.example.pillmate.domain.repository.MedicationRepository
import com.example.pillmate.domain.repository.ScheduleRepository
import com.example.pillmate.domain.usecase.LogMedicationUseCase
import com.example.pillmate.presentation.viewmodel.HomeViewModel
import com.example.pillmate.presentation.viewmodel.MedicationLogViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import com.example.pillmate.data.local.database.AppDatabase
import com.example.pillmate.data.repository.CabinetRepositoryImpl
import com.example.pillmate.domain.repository.CabinetRepository
import com.example.pillmate.presentation.viewmodel.CabinetViewModel
import org.koin.android.ext.koin.androidContext

val appModule = module {
    single { FirebaseAuth.getInstance() }
    single { FirebaseFirestore.getInstance() }
    
    // Provide profileId dynamically from current user
    factory { get<FirebaseAuth>().currentUser?.uid ?: "" }

    single<MedicationRepository> { FirestoreMedicationRepository(get(), get()) }
    single<LogRepository> { FirestoreLogRepository(get()) }
    single<ScheduleRepository> { FirestoreScheduleRepository(get()) }
    
    single { com.example.pillmate.util.DataGenerator(get()) }
    single { com.example.pillmate.util.FcmTokenManager(get()) }

    factory { LogMedicationUseCase(get(), get()) }
    factory { com.example.pillmate.domain.usecase.GetHomeTasksUseCase(get(), get()) }
    factory { com.example.pillmate.domain.usecase.CreateScheduleUseCase(get()) }
    
    single { com.example.pillmate.notification.MedicationNotificationManager(get()) }

    single { AppDatabase.getDatabase(androidContext()) }
    single { get<AppDatabase>().medicationDao() }
    single { get<AppDatabase>().supplyLogDao() }
    single<CabinetRepository> { CabinetRepositoryImpl(get(), get()) }
}

val viewModelModule = module {
    viewModel { HomeViewModel(get(), get(), get()) }
    viewModel { MedicationLogViewModel(get(), get()) }
    viewModel { com.example.pillmate.presentation.viewmodel.DebugViewModel(get(), get(), get(), get(), get()) }
    viewModel { CabinetViewModel(get(), androidContext() as android.app.Application) }
}
