package com.example.pillmate.di

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

    factory { LogTaskUseCase(get(), get()) }
    factory { com.example.pillmate.domain.usecase.GetHomeTasksUseCase(get(), get()) }
    factory { com.example.pillmate.domain.usecase.CreateScheduleUseCase(get()) }
    factory { com.example.pillmate.domain.usecase.UpdateScheduleUseCase(get()) }
    factory { com.example.pillmate.domain.usecase.ManageReminderUseCase(get(), get()) }
    factory { com.example.pillmate.domain.usecase.SyncAlarmsUseCase(get(), get()) }
    factory { com.example.pillmate.domain.usecase.SyncFcmTokenUseCase(get()) }
    
    single { com.example.pillmate.notification.TaskNotificationManager(get()) }
}

val viewModelModule = module {
    viewModel { HomeViewModel(get(), get(), get()) }
    viewModel { TaskLogViewModel(get(), get()) }
    viewModel { ReminderViewModel(get(), get(), get()) }
    viewModel { com.example.pillmate.presentation.viewmodel.DebugViewModel(get(), get(), get(), get(), get(), get()) }
}
