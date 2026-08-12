package com.team.yeogibeoryeo.data.favorite.local

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.team.yeogibeoryeo.data.favorite.di.FavoriteDatabaseModule
import com.team.yeogibeoryeo.data.item.local.ItemCategoryLocalSource
import com.team.yeogibeoryeo.data.item.local.ItemGuideDetail
import com.team.yeogibeoryeo.data.item.local.WasteDictionaryItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FavoriteDatabaseMigrationTest {
    @get:Rule
    val migrationTestHelper =
        MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            FavoriteDatabase::class.java,
        )

    @Test
    fun `버전1에서4로_마이그레이션하면_품목_표시명_즐겨찾기를_안정_ID로_변환한다`() {
        migrationTestHelper.createDatabase(DATABASE_NAME, 1).apply {
            execSQL(
                """
                INSERT INTO favorites (type, targetId, savedAtMillis)
                VALUES ('ITEM_GUIDE', '종이팩', 1234)
                """.trimIndent(),
            )
            close()
        }

        migrationTestHelper.runMigrationsAndValidate(
            DATABASE_NAME,
            4,
            true,
            *migrations,
        ).use { migratedDatabase ->
            migratedDatabase.assertFavorite(
                expectedType = "ITEM_GUIDE",
                expectedTargetId = "item-guide-0002",
                expectedSavedAtMillis = 1234L,
            )
        }
    }

    @Test
    fun `버전2에서4로_마이그레이션하면_장소_즐겨찾기와_스냅샷을_유지한다`() {
        migrationTestHelper.createDatabase(DATABASE_NAME, 2).apply {
            execSQL(
                """
                INSERT INTO favorites (type, targetId, savedAtMillis)
                VALUES ('COLLECTION_SPOT', 'spot-1', 5678)
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO collection_spot_favorite_snapshots (
                    targetId,
                    name,
                    spotType,
                    address,
                    detailLocation,
                    latitude,
                    longitude
                ) VALUES (
                    'spot-1',
                    '중구 재활용센터',
                    'RECYCLING_CENTER',
                    '서울특별시 중구',
                    '1층 입구',
                    37.5,
                    127.0
                )
                """.trimIndent(),
            )
            close()
        }

        migrationTestHelper.runMigrationsAndValidate(
            DATABASE_NAME,
            4,
            true,
            *migrations,
        ).use { migratedDatabase ->
            migratedDatabase.assertFavorite(
                expectedType = "COLLECTION_SPOT",
                expectedTargetId = "spot-1",
                expectedSavedAtMillis = 5678L,
            )
            migratedDatabase.assertCollectionSpotSnapshot()
        }
    }

    @Test
    fun `버전3에서4로_마이그레이션하면_이전_이름을_합치고_다른_유형은_유지한다`() {
        migrationTestHelper.createDatabase(DATABASE_NAME, 3).apply {
            execSQL(
                """
                INSERT INTO favorites (type, targetId, savedAtMillis) VALUES
                    ('ITEM_GUIDE', '종이팩', 100),
                    ('ITEM_GUIDE', '우유팩(이전)', 200),
                    ('ITEM_GUIDE', '우유팩(사전 이전)', 150),
                    ('ITEM_GUIDE', '삭제된 품목', 300),
                    ('COLLECTION_SPOT', 'spot-1', 400),
                    ('REGIONAL_GUIDE', 'region-1', 500)
                """.trimIndent(),
            )
            close()
        }

        migrationTestHelper.runMigrationsAndValidate(
            DATABASE_NAME,
            4,
            true,
            *migrations,
        ).use { migratedDatabase ->
            assertEquals(
                listOf(
                    FavoriteRow("COLLECTION_SPOT", "spot-1", 400L),
                    FavoriteRow("ITEM_GUIDE", "item-guide-0002", 200L),
                    FavoriteRow("REGIONAL_GUIDE", "region-1", 500L),
                ),
                migratedDatabase.readFavorites(),
            )
        }
    }

    private fun SupportSQLiteDatabase.assertFavorite(
        expectedType: String,
        expectedTargetId: String,
        expectedSavedAtMillis: Long,
    ) {
        query("SELECT type, targetId, savedAtMillis FROM favorites").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(expectedType, cursor.getString(0))
            assertEquals(expectedTargetId, cursor.getString(1))
            assertEquals(expectedSavedAtMillis, cursor.getLong(2))
            assertFalse(cursor.moveToNext())
        }
    }

    private fun SupportSQLiteDatabase.readFavorites(): List<FavoriteRow> =
        query(
            """
            SELECT type, targetId, savedAtMillis
            FROM favorites
            ORDER BY type, targetId
            """.trimIndent(),
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(
                        FavoriteRow(
                            type = cursor.getString(0),
                            targetId = cursor.getString(1),
                            savedAtMillis = cursor.getLong(2),
                        ),
                    )
                }
            }
        }

    private fun SupportSQLiteDatabase.assertCollectionSpotSnapshot() {
        query(
            """
            SELECT targetId, name, spotType, address, detailLocation, latitude, longitude
            FROM collection_spot_favorite_snapshots
            """.trimIndent(),
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("spot-1", cursor.getString(0))
            assertEquals("중구 재활용센터", cursor.getString(1))
            assertEquals("RECYCLING_CENTER", cursor.getString(2))
            assertEquals("서울특별시 중구", cursor.getString(3))
            assertEquals("1층 입구", cursor.getString(4))
            assertEquals(37.5, cursor.getDouble(5), 0.0)
            assertEquals(127.0, cursor.getDouble(6), 0.0)
            assertFalse(cursor.moveToNext())
        }
    }

    private data class FavoriteRow(
        val type: String,
        val targetId: String,
        val savedAtMillis: Long,
    )

    private val migrations
        get() =
            FavoriteDatabaseModule.favoriteDatabaseMigrations(
                FavoriteDatabaseModule.itemGuideIdMappings(TestItemCategoryLocalSource),
            )

    private object TestItemCategoryLocalSource : ItemCategoryLocalSource {
        override fun getSynonyms(): Map<String, String> = emptyMap()

        override fun getGuideDetails(): Map<String, ItemGuideDetail> =
            mapOf(
                "종이팩" to
                    ItemGuideDetail(
                        id = "item-guide-0002",
                        legacyNames = listOf("우유팩(이전)"),
                        steps = emptyList(),
                        cautions = emptyList(),
                        tip = null,
                        relatedSpotTypes = emptyList(),
                    ),
            )

        override fun getWasteDictionaryItems(): List<WasteDictionaryItem> =
            listOf(
                WasteDictionaryItem(
                    id = "item-guide-0002",
                    name = "종이팩",
                    legacyNames = listOf("우유팩(사전 이전)"),
                    categoryPaths = emptyList(),
                    similarItems = emptyList(),
                    dischargeMethods = emptyList(),
                    features = emptyList(),
                    notes = emptyList(),
                ),
            )
    }

    private companion object {
        const val DATABASE_NAME = "favorite-migration-test"
    }
}
