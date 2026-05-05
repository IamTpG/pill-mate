package com.example.pillmate.di

import android.app.Application
import com.example.pillmate.data.repository.FirestoreLogRepositoryImpl
import com.example.pillmate.data.repository.FirestoreMedicationRepositoryImpl
import com.example.pillmate.data.repository.FirestoreScheduleRepositoryImpl
import com.example.pillmate.domain.repository.LogRepository
import com.example.pillmate.domain.repository.MedicationRepository
import com.example.pillmate.domain.repository.ScheduleRepository
import com.example.pillmate.domain.usecase.LogTaskUseCase
import com.example.pillmate.presentation.viewmodel.HomeViewModel
import com.example.pillmate.presentation.viewmodel.TaskLogViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import com.example.pillmate.data.local.database.AppDatabase
import com.example.pillmate.data.repository.HybridMedicationRepositoryImpl
import com.example.pillmate.data.repository.RoomMedicationRepositoryImpl
import com.example.pillmate.presentation.viewmodel.ScheduleBuilderViewModel
import com.example.pillmate.presentation.viewmodel.CabinetViewModel
import org.koin.android.ext.koin.androidContext
import com.example.pillmate.data.remote.api.OpenFdaApi
import com.example.pillmate.data.repository.DrugLibraryRepositoryImpl
import com.example.pillmate.domain.repository.DrugLibraryRepository
import com.example.pillmate.presentation.viewmodel.DrugLibraryViewModel
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.example.pillmate.data.repository.FirestoreAppointmentRepositoryImpl
import com.example.pillmate.domain.repository.AppointmentRepository
import com.example.pillmate.domain.usecase.AddAppointmentUseCase
import com.example.pillmate.domain.usecase.DeleteAppointmentUseCase
import com.example.pillmate.domain.usecase.GetAppointmentsUseCase
import com.example.pillmate.domain.usecase.UpdateAppointmentUseCase
import com.example.pillmate.presentation.viewmodel.AppointmentViewModel
import com.example.pillmate.domain.usecase.*
import com.example.pillmate.notification.TaskNotificationManager
import com.example.pillmate.presentation.viewmodel.AuthViewModel
import com.example.pillmate.presentation.viewmodel.DebugViewModel
import com.example.pillmate.presentation.viewmodel.ProfileViewModel
import com.example.pillmate.util.AlarmTracker
import com.example.pillmate.util.DataGenerator
import com.example.pillmate.util.FcmTokenManager
import com.example.pillmate.data.repository.AIChatRepository
import com.example.pillmate.data.repository.FirestoreHealthMetricRepositoryImpl
import com.example.pillmate.domain.repository.HealthMetricRepository
import com.example.pillmate.notification.HealthReminderManager
import com.example.pillmate.presentation.viewmodel.AIChatViewModel
import com.example.pillmate.presentation.viewmodel.VitalsViewModel
import com.google.firebase.functions.FirebaseFunctions

val appModule = module {
    single { FirebaseAuth.getInstance() }
    single { FirebaseFirestore.getInstance() }
    single { FirebaseFunctions.getInstance() }
    
    // Provide profileId dynamically from current user
    factory { get<FirebaseAuth>().currentUser?.uid ?: "" }
    single<DataGenerator> { DataGenerator(get()) }

    single<MedicationRepository> {
        val roomRepo = RoomMedicationRepositoryImpl(get(), get())
        val firestoreRepo = FirestoreMedicationRepositoryImpl(get(), get())
        HybridMedicationRepositoryImpl(
            localRepo = roomRepo,
            remoteRepo = firestoreRepo,
            supplyLogDao = get(),
            firestore = get(),
            networkChecker = com.example.pillmate.util.NetworkChecker(androidContext())
        )
    }
    single<LogRepository> { FirestoreLogRepositoryImpl(get()) }
    single<ScheduleRepository> { FirestoreScheduleRepositoryImpl(get()) }
    single<HealthMetricRepository> { FirestoreHealthMetricRepositoryImpl(get()) }
    
    single { AlarmTracker(get()) }
    single { FcmTokenManager(get()) }
    single { com.example.pillmate.util.SyncManager(get()) }

    factory { LogTaskUseCase(get(), get(), get()) }
    factory { DeleteMedicationUseCase(get(), get(), get()) }
    factory { GetHomeTasksUseCase(get(), get()) }
    factory { CreateScheduleUseCase(get()) }
    factory { UpdateScheduleUseCase(get()) }
    factory { ManageReminderUseCase(get(), get(), get()) }
    factory { SyncAlarmsUseCase(get(), get(), get(), get()) }
    factory { SyncFcmTokenUseCase(get()) }
    factory { GetNextTaskUseCase(get(), get()) }
    factory { LogHealthMetricUseCase(get()) }
    factory { GetHealthMetricsUseCase(get()) }
    factory { UpdateHydrationGoalUseCase(get(), get()) }
    factory { GetWidgetDataUseCase(get(), get(), get()) }

    viewModel { (profileId: String) -> TaskLogViewModel(get(), get(), profileId) }
    viewModel { (profileId: String) -> VitalsViewModel(get(), get(), get(), get(), profileId) }

    single { TaskNotificationManager(get()) }
    single { HealthReminderManager(get()) }

    single { AppDatabase.getDatabase(androidContext()) }
    single { get<AppDatabase>().medicationDao() }
    single { get<AppDatabase>().supplyLogDao() }
    single { get<AppDatabase>().profileDao() }
    single {
        Retrofit.Builder()
            .baseUrl("https://api.fda.gov/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OpenFdaApi::class.java)
    }
    
    single<DrugLibraryRepository> { DrugLibraryRepositoryImpl(get()) }
    single { get<AppDatabase>().chatDao() }
    single { AIChatRepository(get(), get(), get()) }
    
    single<AppointmentRepository> { FirestoreAppointmentRepositoryImpl(get()) }
    factory { GetAppointmentsUseCase(get()) }
    factory { AddAppointmentUseCase(get()) }
    factory { UpdateAppointmentUseCase(get()) }
    factory { DeleteAppointmentUseCase(get()) }
}

val viewModelModule = module {
    viewModel { HomeViewModel(get(), get(), get(), get()) }
    viewModel { TaskLogViewModel(get(), get(), get()) }
    viewModel { AppointmentViewModel(get(), get(), get(), get()) }
    viewModel { DebugViewModel(get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    viewModel { CabinetViewModel(get(), get(), get(), get(), androidContext() as Application) }
    viewModel { DrugLibraryViewModel(get(), androidContext() as Application) }
    viewModel { ScheduleBuilderViewModel(get(), get(), get(), get()) }
    viewModel { AuthViewModel(get(), get(), get(), get(), get()) }
    viewModel { ProfileViewModel(get(), get(), get()) }
    viewModel { AIChatViewModel(get(), get(), get()) }
}
