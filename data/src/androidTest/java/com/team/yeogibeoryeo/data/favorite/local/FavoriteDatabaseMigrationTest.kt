package com.team.yeogibeoryeo.data.favorite.local

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.team.yeogibeoryeo.data.favorite.di.FavoriteDatabaseModule
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
    fun `버전1에서3으로_마이그레이션하면_기존즐겨찾기가_유지된다`() {
        migrationTestHelper.createDatabase(DATABASE_NAME, 1).apply {
            execSQL(
                """
                INSERT INTO favorites (type, targetId, savedAtMillis)
                VALUES ('ITEM_GUIDE', 'paper-pack', 1234)
                """.trimIndent(),
            )
            close()
        }

        migrationTestHelper.runMigrationsAndValidate(
            DATABASE_NAME,
            3,
            true,
            FavoriteDatabaseModule.FAVORITE_DATABASE_MIGRATION_1_2,
            FavoriteDatabaseModule.FAVORITE_DATABASE_MIGRATION_2_3,
        ).use { migratedDatabase ->
            migratedDatabase.assertFavorite(
                expectedType = "ITEM_GUIDE",
                expectedTargetId = "paper-pack",
                expectedSavedAtMillis = 1234L,
            )
        }
    }

    @Test
    fun `버전2에서3으로_마이그레이션하면_기존즐겨찾기와_장소스냅샷이_유지된다`() {
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
            3,
            true,
            FavoriteDatabaseModule.FAVORITE_DATABASE_MIGRATION_2_3,
        ).use { migratedDatabase ->
            migratedDatabase.assertFavorite(
                expectedType = "COLLECTION_SPOT",
                expectedTargetId = "spot-1",
                expectedSavedAtMillis = 5678L,
            )
            migratedDatabase.assertCollectionSpotSnapshot()
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

    private companion object {
        const val DATABASE_NAME = "favorite-migration-test"
    }
}
