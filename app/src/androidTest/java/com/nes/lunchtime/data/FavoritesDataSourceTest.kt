package com.nes.lunchtime.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nes.lunchtime.data.favorites.FavoritesDataSource
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration tests for FavoritesDataSource using real Android DataStore.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class FavoritesDataSourceTest {

    private lateinit var testContext: Context

    @Before
    fun setup() {
        testContext = ApplicationProvider.getApplicationContext()
    }

    /**
     * Helper to create a DataStore in the backgroundScope of the test.
     * This ensures the DataStore's internal coroutines are cancelled when the test finishes,
     * preventing UncompletedCoroutinesError.
     */
    private fun TestScope.createDataStore(name: String): DataStore<Preferences> {
        return PreferenceDataStoreFactory.create(
            scope = backgroundScope,
            produceFile = {
                testContext.preferencesDataStoreFile("${name}_${System.nanoTime()}")
            }
        )
    }

    @Test
    fun initialState_isEmpty() = runTest {
        val dataSource = FavoritesDataSource(createDataStore("test_initial"))
        val favorites = dataSource.getFavorites().first()
        assertTrue(favorites.isEmpty())
    }

    @Test
    fun addFavorite_persistsToDataStore() = runTest {
        val dataSource = FavoritesDataSource(createDataStore("test_add"))
        dataSource.addFavorite("restaurant_1")
        val favorites = dataSource.getFavorites().first()
        assertEquals(1, favorites.size)
        assertTrue(favorites.contains("restaurant_1"))
    }

    @Test
    fun addMultipleFavorites_allArePersisted() = runTest {
        val dataSource = FavoritesDataSource(createDataStore("test_multiple_add"))
        dataSource.addFavorite("restaurant_1")
        dataSource.addFavorite("restaurant_2")
        dataSource.addFavorite("restaurant_3")

        val favorites = dataSource.getFavorites().first()
        assertEquals(3, favorites.size)
        assertTrue(favorites.contains("restaurant_1"))
        assertTrue(favorites.contains("restaurant_2"))
        assertTrue(favorites.contains("restaurant_3"))
    }

    @Test
    fun removeFavorite_removesFromDataStore() = runTest {
        val dataSource = FavoritesDataSource(createDataStore("test_remove"))
        dataSource.addFavorite("restaurant_1")
        dataSource.removeFavorite("restaurant_1")

        val favorites = dataSource.getFavorites().first()
        assertFalse(favorites.contains("restaurant_1"))
        assertTrue(favorites.isEmpty())
    }

    @Test
    fun removeFavorite_keepsOtherFavorites() = runTest {
        val dataSource = FavoritesDataSource(createDataStore("test_remove_keep"))
        dataSource.addFavorite("restaurant_1")
        dataSource.addFavorite("restaurant_2")
        dataSource.addFavorite("restaurant_3")

        dataSource.removeFavorite("restaurant_2")

        val favorites = dataSource.getFavorites().first()
        assertEquals(2, favorites.size)
        assertTrue(favorites.contains("restaurant_1"))
        assertFalse(favorites.contains("restaurant_2"))
        assertTrue(favorites.contains("restaurant_3"))
    }

    @Test
    fun addSameFavorite_doesNotCreateDuplicates() = runTest {
        val dataSource = FavoritesDataSource(createDataStore("test_duplicate"))
        dataSource.addFavorite("restaurant_1")
        dataSource.addFavorite("restaurant_1")

        val favorites = dataSource.getFavorites().first()
        assertEquals(1, favorites.size)
        assertTrue(favorites.contains("restaurant_1"))
    }

    @Test
    fun removeFavorite_thatDoesNotExist_doesNothing() = runTest {
        val dataSource = FavoritesDataSource(createDataStore("test_remove_nonexistent"))
        dataSource.addFavorite("restaurant_1")
        dataSource.addFavorite("restaurant_2")

        dataSource.removeFavorite("restaurant_999")

        val favorites = dataSource.getFavorites().first()
        assertEquals(2, favorites.size)
        assertTrue(favorites.contains("restaurant_1"))
        assertTrue(favorites.contains("restaurant_2"))
    }

    @Test
    fun getFavorites_emitsUpdatesOnChanges() = runTest {
        val dataSource = FavoritesDataSource(createDataStore("test_emits"))
        val collectedFavorites = mutableListOf<Set<String>>()
        
        // DataStore emits initial value, then 3 more updates
        val job = backgroundScope.launch {
            dataSource.getFavorites().collect { collectedFavorites.add(it) }
        }

        // 1. Initial empty state
        runCurrent()
        
        // 2. Add first
        dataSource.addFavorite("restaurant_1")
        runCurrent()
        
        // 3. Add second
        dataSource.addFavorite("restaurant_2")
        runCurrent()
        
        // 4. Remove first
        dataSource.removeFavorite("restaurant_1")
        runCurrent()

        assertTrue("Should have collected at least 4 emissions", collectedFavorites.size >= 4)
        assertTrue("Last state should contain restaurant_2", collectedFavorites.last().contains("restaurant_2"))
        assertFalse("Last state should not contain restaurant_1", collectedFavorites.last().contains("restaurant_1"))
    }

    @Test
    fun dataStore_persists_betweenDataSourceInstances() = runTest {
        val dataStore = createDataStore("test_instances")
        val dataSource = FavoritesDataSource(dataStore)
        
        dataSource.addFavorite("restaurant_1")
        dataSource.addFavorite("restaurant_2")

        // When: Creating new DataSource instance with same DataStore
        val newDataSource = FavoritesDataSource(dataStore)

        // Then: Favorites persist
        val favorites = newDataSource.getFavorites().first()
        assertEquals(2, favorites.size)
        assertTrue(favorites.contains("restaurant_1"))
        assertTrue(favorites.contains("restaurant_2"))
    }

    @Test
    fun addFavorite_handlesSpecialCharacters() = runTest {
        val dataSource = FavoritesDataSource(createDataStore("test_special_chars"))
        val specialId = "restaurant_!@#$%^&*()_+-=[]{}|;:',.<>?"
        dataSource.addFavorite(specialId)

        val favorites = dataSource.getFavorites().first()
        assertTrue(favorites.contains(specialId))
    }

    @Test
    fun addFavorite_handlesVeryLongIds() = runTest {
        val dataSource = FavoritesDataSource(createDataStore("test_long_id"))
        val longId = "restaurant_" + "a".repeat(100) // Reduced repeat for instrumented test speed
        dataSource.addFavorite(longId)

        val favorites = dataSource.getFavorites().first()
        assertTrue(favorites.contains(longId))
    }

    @Test
    fun addFavorite_handlesEmptyString() = runTest {
        val dataSource = FavoritesDataSource(createDataStore("test_empty_string"))
        dataSource.addFavorite("")

        val favorites = dataSource.getFavorites().first()
        assertTrue(favorites.contains(""))
    }

    @Test
    fun multipleConcurrentAdds_allPersist() = runTest {
        val dataSource = FavoritesDataSource(createDataStore("test_concurrent_add"))
        
        launch { dataSource.addFavorite("restaurant_1") }
        launch { dataSource.addFavorite("restaurant_2") }
        launch { dataSource.addFavorite("restaurant_3") }
        launch { dataSource.addFavorite("restaurant_4") }
        launch { dataSource.addFavorite("restaurant_5") }

        advanceUntilIdle()

        val favorites = dataSource.getFavorites().first()
        assertEquals(5, favorites.size)
    }

    @Test
    fun addAndRemoveConcurrently_handlesCorrectly() = runTest {
        val dataSource = FavoritesDataSource(createDataStore("test_concurrent_mixed"))
        dataSource.addFavorite("restaurant_1")
        dataSource.addFavorite("restaurant_2")
        dataSource.addFavorite("restaurant_3")

        launch { dataSource.addFavorite("restaurant_4") }
        launch { dataSource.removeFavorite("restaurant_2") }
        launch { dataSource.addFavorite("restaurant_5") }

        advanceUntilIdle()

        val favorites = dataSource.getFavorites().first()
        assertTrue(favorites.contains("restaurant_1"))
        assertFalse(favorites.contains("restaurant_2"))
        assertTrue(favorites.contains("restaurant_3"))
        assertTrue(favorites.contains("restaurant_4"))
        assertTrue(favorites.contains("restaurant_5"))
    }
}
