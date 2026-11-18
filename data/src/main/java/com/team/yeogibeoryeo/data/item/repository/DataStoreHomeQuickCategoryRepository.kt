package com.team.yeogibeoryeo.data.item.repository

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.team.yeogibeoryeo.data.item.di.ItemPreferencesDataStore
import com.team.yeogibeoryeo.domain.diagnostics.NonFatalApi
import com.team.yeogibeoryeo.domain.diagnostics.NonFatalCategory
import com.team.yeogibeoryeo.domain.diagnostics.NonFatalErrorContext
import com.team.yeogibeoryeo.domain.diagnostics.NonFatalErrorReporter
import com.team.yeogibeoryeo.domain.diagnostics.NonFatalStage
import com.team.yeogibeoryeo.domain.item.model.DisposalCategory
import com.team.yeogibeoryeo.domain.item.repository.HomeQuickCategoryRepository
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.retryWhen

class DataStoreHomeQuickCategoryRepository
    @Inject
    constructor(
        @param:ItemPreferencesDataStore private val dataStore: DataStore<Preferences>,
        private val reporter: NonFatalErrorReporter,
    ) : HomeQuickCategoryRepository {
        override fun observeHomeQuickCategories(): Flow<List<DisposalCategory>> =
            flow {
                var retryDelayMillis = INITIAL_READ_RETRY_DELAY_MILLIS
                emitAll(
                    dataStore.data
                        .onEach { retryDelayMillis = INITIAL_READ_RETRY_DELAY_MILLIS }
                        .retryWhen { exception, _ ->
                            if (exception !is IOException || exception is CorruptionException) {
                                return@retryWhen false
                            }
                            currentCoroutineContext().ensureActive()
                            if (retryDelayMillis == INITIAL_READ_RETRY_DELAY_MILLIS) {
                                reporter.report(exception, CACHE_READ_ERROR_CONTEXT)
                            }
                            delay(retryDelayMillis)
                            retryDelayMillis = (retryDelayMillis * 2).coerceAtMost(MAX_READ_RETRY_DELAY_MILLIS)
                            true
                        }
                        .catch { exception ->
                            if (exception is CancellationException || exception is Error) throw exception
                            reporter.report(exception, CACHE_READ_ERROR_CONTEXT)
                            emit(emptyPreferences())
                        },
                )
            }.map { preferences ->
                preferences[HOME_QUICK_CATEGORIES_KEY].toCategories()
            }

        override suspend fun toggleHomeQuickCategory(
            category: DisposalCategory,
            maxSelectedCount: Int,
        ) {
            val boundedMaxSelectedCount = maxSelectedCount.coerceAtLeast(0)

            updateHomeQuickCategories { current ->
                if (category in current) {
                    current - category
                } else if (current.size >= boundedMaxSelectedCount) {
                    current
                } else {
                    current + category
                }
            }
        }

        override suspend fun limitHomeQuickCategories(maxSelectedCount: Int) {
            val boundedMaxSelectedCount = maxSelectedCount.coerceAtLeast(0)

            updateHomeQuickCategories { current -> current.take(boundedMaxSelectedCount) }
        }

        private suspend fun updateHomeQuickCategories(
            transform: (List<DisposalCategory>) -> List<DisposalCategory>,
        ) {
            try {
                dataStore.edit { preferences ->
                    val current = preferences[HOME_QUICK_CATEGORIES_KEY].toCategories()
                    preferences[HOME_QUICK_CATEGORIES_KEY] =
                        transform(current)
                            .joinToString(CATEGORY_SEPARATOR) { it.name }
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                reporter.report(exception, CACHE_WRITE_ERROR_CONTEXT)
                // 저장 실패 시 현재 값 유지
            }
        }

        private fun String?.toCategories(): List<DisposalCategory> =
            this
                ?.split(CATEGORY_SEPARATOR)
                ?.mapNotNull { name -> DisposalCategory.entries.firstOrNull { it.name == name } }
                ?.distinct()
                .orEmpty()

        private companion object {
            const val CATEGORY_SEPARATOR = ","
            const val INITIAL_READ_RETRY_DELAY_MILLIS = 1_000L
            const val MAX_READ_RETRY_DELAY_MILLIS = 30_000L
            val HOME_QUICK_CATEGORIES_KEY = stringPreferencesKey("pinned_disposal_categories")
            val CACHE_READ_ERROR_CONTEXT =
                NonFatalErrorContext(
                    api = NonFatalApi.ITEM_GUIDE,
                    stage = NonFatalStage.CACHE_READ,
                    category = NonFatalCategory.CACHE,
                )
            val CACHE_WRITE_ERROR_CONTEXT =
                NonFatalErrorContext(
                    api = NonFatalApi.ITEM_GUIDE,
                    stage = NonFatalStage.CACHE_WRITE,
                    category = NonFatalCategory.CACHE,
                )
        }
    }
