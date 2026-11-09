package org.mobile.growfy.core.di

import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.bind
import org.koin.dsl.module
import org.mobile.growfy.core.config.AppConfig
import org.mobile.growfy.core.network.DataChanges
import org.mobile.growfy.core.network.SessionEvents
import org.mobile.growfy.core.network.createHttpClient
import org.mobile.growfy.core.platform.DeepLinks
import org.mobile.growfy.core.storage.AppPreferences
import org.mobile.growfy.core.storage.DataStoreTokenStorage
import org.mobile.growfy.core.storage.TokenStorage
import org.mobile.growfy.core.ui.LocationFilter
import org.mobile.growfy.core.ui.Messenger
import org.mobile.growfy.data.remote.AuthApi
import org.mobile.growfy.data.remote.BusinessApi
import org.mobile.growfy.data.remote.ProposedBusinessApi
import org.mobile.growfy.data.repository.AuthRepositoryImpl
import org.mobile.growfy.data.repository.GoogleAccountRepositoryImpl
import org.mobile.growfy.data.repository.InsightsRepositoryImpl
import org.mobile.growfy.data.repository.LocationRepositoryImpl
import org.mobile.growfy.data.repository.NotificationRepositoryImpl
import org.mobile.growfy.data.repository.PostRepositoryImpl
import org.mobile.growfy.data.repository.ReviewRepositoryImpl
import org.mobile.growfy.data.repository.TemplateRepositoryImpl
import org.mobile.growfy.domain.repository.AuthRepository
import org.mobile.growfy.domain.repository.GoogleAccountRepository
import org.mobile.growfy.domain.repository.InsightsRepository
import org.mobile.growfy.domain.repository.LocationRepository
import org.mobile.growfy.domain.repository.NotificationRepository
import org.mobile.growfy.domain.repository.PostRepository
import org.mobile.growfy.domain.repository.ReviewRepository
import org.mobile.growfy.domain.repository.TemplateRepository
import org.mobile.growfy.feature.auth.LoginViewModel
import org.mobile.growfy.feature.home.HomeViewModel
import org.mobile.growfy.feature.main.MainViewModel
import org.mobile.growfy.feature.more.GoogleAccountsViewModel
import org.mobile.growfy.feature.more.MoreViewModel
import org.mobile.growfy.feature.more.NotificationsViewModel
import org.mobile.growfy.feature.more.TemplatesViewModel
import org.mobile.growfy.feature.onboarding.OnboardingViewModel
import org.mobile.growfy.feature.posts.ComposePostViewModel
import org.mobile.growfy.feature.posts.PostsViewModel
import org.mobile.growfy.feature.reviews.ReplyViewModel
import org.mobile.growfy.feature.reviews.ReviewsViewModel
import org.mobile.growfy.navigation.SessionRouter
import org.mobile.growfy.navigation.TabRequests

/** Provides the HttpClientEngine, DataStore and UrlLauncher, which differ per platform. */
expect val platformModule: Module

val coreModule = module {
    single<TokenStorage> { DataStoreTokenStorage(get()) }
    singleOf(::AppPreferences)
    singleOf(::SessionEvents)
    singleOf(::DataChanges)
    singleOf(::Messenger)
    singleOf(::LocationFilter)
    singleOf(::DeepLinks)
    single { createHttpClient(engine = get(), config = get(), tokenStorage = get(), sessionEvents = get()) }
}

val dataModule = module {
    singleOf(::AuthApi)
    singleOf(::BusinessApi)
    singleOf(::ProposedBusinessApi)
    singleOf(::AuthRepositoryImpl) bind AuthRepository::class
    singleOf(::GoogleAccountRepositoryImpl) bind GoogleAccountRepository::class
    singleOf(::LocationRepositoryImpl) bind LocationRepository::class
    singleOf(::ReviewRepositoryImpl) bind ReviewRepository::class
    singleOf(::PostRepositoryImpl) bind PostRepository::class
    singleOf(::InsightsRepositoryImpl) bind InsightsRepository::class
    singleOf(::TemplateRepositoryImpl) bind TemplateRepository::class
    singleOf(::NotificationRepositoryImpl) bind NotificationRepository::class
    singleOf(::SessionRouter)
    singleOf(::TabRequests)
}

val featureModule = module {
    viewModelOf(::LoginViewModel)
    viewModelOf(::OnboardingViewModel)
    viewModelOf(::MainViewModel)
    viewModelOf(::HomeViewModel)
    viewModelOf(::ReviewsViewModel)
    viewModel { params -> ReplyViewModel(params.get(), get(), get(), get(), get(), get()) }
    viewModelOf(::PostsViewModel)
    viewModel { params -> ComposePostViewModel(params.get(), get(), get(), get(), get()) }
    viewModelOf(::MoreViewModel)
    viewModelOf(::TemplatesViewModel)
    viewModelOf(::GoogleAccountsViewModel)
    viewModelOf(::NotificationsViewModel)
}

fun initKoin(config: AppConfig, appDeclaration: KoinAppDeclaration = {}): KoinApplication = startKoin {
    appDeclaration()
    modules(
        module { single { config } },
        platformModule,
        coreModule,
        dataModule,
        featureModule,
    )
}
