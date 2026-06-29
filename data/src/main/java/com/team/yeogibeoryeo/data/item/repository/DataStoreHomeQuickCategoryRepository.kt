package com.team.yeogibeoryeo.data.item.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.team.yeogibeoryeo.data.item.di.ItemPreferencesDataStore
import com.team.yeogibeoryeo.domain.item.model.DisposalCategory
import com.team.yeogibeoryeo.domain.item.repository.HomeQuickCategoryRepository
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

class DataStoreHomeQuickCategoryRepository
    @Inject
    constructor(
        @param:ItemPreferencesDataStore private val dataStore: DataStore<Preferences>,
    ) : HomeQuickCategoryRepository {
        override fun observeHomeQuickCategories(): Flow<List<DisposalCategory>> =
            dataStore.data
                .catch { exception ->
                    if (exception is CancellationException) throw exception
                    emit(emptyPreferences())
                }
                .map { preferences ->
                    preferences[HOME_QUICK_CATEGORIES_KEY].toCategories()
                }

        override suspend fun toggleHomeQuickCategory(
            category: DisposalCategory,
            maxSelectedCount: Int,
        ) {
            val boundedMaxSelectedCount = maxSelectedCount.coerceAtLeast(0)

            try {
                dataStore.edit { preferences ->
                    val current = preferences[HOME_QUICK_CATEGORIES_KEY].toCategories()
                    val updated =
                        if (category in current) {
                            current - category
                        } else if (current.size >= boundedMaxSelectedCount) {
                            current
                        } else {
                            current + category
                        }

                    preferences[HOME_QUICK_CATEGORIES_KEY] = updated.joinToString(CategorySeparator) { it.name }
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                // 저장 실패 시 현재 값 유지
            }
        }

        override suspend fun limitHomeQuickCategories(maxSelectedCount: Int) {
            val boundedMaxSelectedCount = maxSelectedCount.coerceAtLeast(0)

            try {
                dataStore.edit { preferences ->
                    val current = preferences[HOME_QUICK_CATEGORIES_KEY].toCategories()
                    preferences[HOME_QUICK_CATEGORIES_KEY] =
                        current
                            .take(boundedMaxSelectedCount)
                            .joinToString(CategorySeparator) { it.name }
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                // 저장 실패 시 현재 값 유지
            }
        }

        private fun String?.toCategories(): List<DisposalCategory> =
            this
                ?.split(CategorySeparator)
                ?.mapNotNull { name -> DisposalCategory.entries.firstOrNull { it.name == name } }
                ?.distinct()
                .orEmpty()

        private companion object {
            const val CategorySeparator = ","
            val HOME_QUICK_CATEGORIES_KEY = stringPreferencesKey("pinned_disposal_categories")
        }
    }
