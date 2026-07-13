package com.cristobalcariqueo.defensadedeudores.data.repository

import com.cristobalcariqueo.defensadedeudores.domain.model.EditResult
import com.cristobalcariqueo.defensadedeudores.domain.model.GraphSlice
import com.cristobalcariqueo.defensadedeudores.domain.model.MatchSuggestion
import com.cristobalcariqueo.defensadedeudores.domain.model.Person
import com.cristobalcariqueo.defensadedeudores.domain.model.Registry
import com.cristobalcariqueo.defensadedeudores.domain.model.RegistryFilter
import com.cristobalcariqueo.defensadedeudores.domain.model.RegistryWithNames
import com.cristobalcariqueo.defensadedeudores.domain.model.Settings
import com.cristobalcariqueo.defensadedeudores.domain.model.Source
import com.cristobalcariqueo.defensadedeudores.domain.model.Track
import kotlinx.coroutines.flow.Flow

/**
 * Contracts only -- implementations land with their respective feature tasks
 * (People/Sources CRUD, Track screen, retReg matching). Kept here so the
 * data/domain/ui layering is visible from the scaffold onward.
 */
interface TrackRepository {
    fun observeTracks(): Flow<List<Track>>
    fun observeTrack(trackId: String): Flow<Track?>
    /** Quick-pick debtor shortcuts for the track's quick-create area. */
    fun observeShortcutPeople(trackId: String): Flow<List<Person>>
    suspend fun create(name: String, shortcutPersonIds: List<String>): Track
    suspend fun rename(trackId: String, name: String)
    suspend fun softDelete(trackId: String)
}

interface PersonRepository {
    fun observePeople(): Flow<List<Person>>
    suspend fun create(name: String): Person
    suspend fun update(personId: String, name: String)
    /** Hard-deletes if unreferenced by any registry, otherwise soft-deletes. */
    suspend fun delete(personId: String)
}

interface SourceRepository {
    fun observeSources(): Flow<List<Source>>
    suspend fun create(name: String): Source
    suspend fun update(sourceId: String, name: String)
    /** Hard-deletes if unreferenced by any registry, otherwise soft-deletes. */
    suspend fun delete(sourceId: String)
}

interface RegistryRepository {
    /**
     * Filtered, paginated, newest-first table rows. Serves both tables: the
     * recent table is page 0 with the configured recent size; the historical
     * table pages through the same filtered set.
     */
    fun observeRows(
        trackId: String,
        filter: RegistryFilter,
        limit: Int,
        offset: Int,
    ): Flow<List<RegistryWithNames>>

    /** Row count of the filtered set -- drives the historical table's pagination. */
    fun observeCount(trackId: String, filter: RegistryFilter): Flow<Int>

    /** Circular-graph slices, unchecked normal regs only (outstanding debt). */
    fun observeDebtorSlices(trackId: String): Flow<List<GraphSlice>>
    fun observeSourceSlices(trackId: String): Flow<List<GraphSlice>>

    /** Net outstanding for the track: unchecked normal minus unchecked returns. */
    fun observeOutstandingTotal(trackId: String): Flow<Long>

    suspend fun createNormal(
        trackId: String,
        personId: String,
        sourceId: String,
        amount: Int,
        note: String?,
    ): Registry

    /** Returns have no source (schema: source_id null for type = return). */
    suspend fun createReturn(trackId: String, personId: String, amount: Int, note: String?): Registry

    /**
     * Edit flow (SCOPE.md): created today -> in-place update; older -> the old
     * row is superseded and a corrected row links back via supersedes_id. A
     * matched reg first credits its retReg back, then re-applies only if the
     * corrected amount still fully fits.
     */
    suspend fun editAmount(registryId: String, newAmount: Int): EditResult

    /** Debtor's unchecked normal regs in the track (return-search list + retro matching). */
    suspend fun uncheckedNormals(trackId: String, personId: String): List<Registry>

    /** Debtor's live retRegs in the track (forward matching). */
    suspend fun uncheckedRetRegs(trackId: String, personId: String): List<Registry>

    /** Return-search Path A: check the exact-sum selection directly, no retReg. */
    suspend fun settleExact(registryIds: List<String>)

    /** Applies a confirmed [MatchSuggestion] atomically. Never called without user confirmation. */
    suspend fun applyMatch(suggestion: MatchSuggestion)
}

interface SettingsRepository {
    /** Emits defaults while the singleton row doesn't exist yet (first launch). */
    fun observeSettings(): Flow<Settings>
    suspend fun update(settings: Settings)
    /** Flips the onboarded flag that gates the Starting screen. */
    suspend fun markOnboarded()
}
