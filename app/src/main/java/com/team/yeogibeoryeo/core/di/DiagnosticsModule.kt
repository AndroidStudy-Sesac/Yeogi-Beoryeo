package com.team.yeogibeoryeo.core.di

import com.team.yeogibeoryeo.core.diagnostics.CrashlyticsNonFatalErrorReporter
import com.team.yeogibeoryeo.domain.diagnostics.NonFatalErrorReporter
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DiagnosticsModule {
    @Binds
    @Singleton
    abstract fun bindNonFatalErrorReporter(
        reporter: CrashlyticsNonFatalErrorReporter,
    ): NonFatalErrorReporter
}
