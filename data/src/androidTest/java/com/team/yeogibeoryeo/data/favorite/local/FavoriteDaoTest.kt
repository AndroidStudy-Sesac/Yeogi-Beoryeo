package com.team.yeogibeoryeo.data.favorite.local

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FavoriteDaoTest {
    private lateinit var database: FavoriteDatabase
    private lateinit var dao: FavoriteDao

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database =
            Room.inMemoryDatabaseBuilder(
                context,
                FavoriteDatabase::class.java,
            ).build()
        dao = database.favoriteDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun toggleFavorite_addsFavorite_whenTargetIsNotSaved() =
        runBlocking {
            val entity = favoriteEntity(type = "ITEM_GUIDE", targetId = "paper-pack")

            val isFavorite = dao.toggleFavorite(entity)

            assertTrue(isFavorite)
            assertTrue(dao.isFavorite(type = "ITEM_GUIDE", targetId = "paper-pack"))
        }

    @Test
    fun toggleFavorite_removesFavorite_whenTargetIsAlreadySaved() =
        runBlocking {
            val entity = favoriteEntity(type = "ITEM_GUIDE", targetId = "paper-pack")
            dao.upsertFavorite(entity)

            val isFavorite = dao.toggleFavorite(entity)

            assertFalse(isFavorite)
            assertFalse(dao.isFavorite(type = "ITEM_GUIDE", targetId = "paper-pack"))
        }

    @Test
    fun observeFavorites_ordersFavoritesBySavedAtDescending() =
        runBlocking {
            val oldFavorite = favoriteEntity(type = "ITEM_GUIDE", targetId = "old", savedAtMillis = 1L)
            val newFavorite = favoriteEntity(type = "ITEM_GUIDE", targetId = "new", savedAtMillis = 2L)

            dao.upsertFavorite(oldFavorite)
            dao.upsertFavorite(newFavorite)

            assertEquals(listOf(newFavorite, oldFavorite), dao.observeFavorites().first())
        }

    @Test
    fun favoritesWithSameTargetIdCanBeSavedForDifferentTypes() =
        runBlocking {
            val itemGuideFavorite = favoriteEntity(type = "ITEM_GUIDE", targetId = "shared-id")
            val collectionSpotFavorite = favoriteEntity(type = "COLLECTION_SPOT", targetId = "shared-id")

            dao.upsertFavorite(itemGuideFavorite)
            dao.upsertFavorite(collectionSpotFavorite)

            assertEquals(
                setOf(itemGuideFavorite, collectionSpotFavorite),
                dao.observeFavorites().first().toSet(),
            )
            assertTrue(dao.isFavorite(type = "ITEM_GUIDE", targetId = "shared-id"))
            assertTrue(dao.isFavorite(type = "COLLECTION_SPOT", targetId = "shared-id"))
        }

    private fun favoriteEntity(
        type: String,
        targetId: String,
        savedAtMillis: Long = 1L,
    ): FavoriteEntity =
        FavoriteEntity(
            type = type,
            targetId = targetId,
            savedAtMillis = savedAtMillis,
        )
}
