package fr.avantdevoter.app.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/**
 * Copie locale des données : tout s'affiche sans réseau.
 * Chaque mise à jour remplace l'ensemble (les données viennent toutes de la source, rien n'est saisi ici).
 */

/** Fiche d'un candidat, sans ses mesures (rangées à part pour la recherche du lot 2). */
@Entity(tableName = "candidat")
data class CandidatRow(
    @PrimaryKey val id: String,
    val tri: String,
    val statut: String,
    val json: String,
)

@Entity(tableName = "mesure", primaryKeys = ["candidatId", "id"], indices = [Index("theme")])
data class MesureRow(
    val candidatId: String,
    val id: String,
    val ordre: Int,
    val theme: String,
    val texte: String,
    val nature: String,
    val sourceTitre: String,
    val sourceUrl: String,
    val sourceDate: String,
)

@Entity(tableName = "sondage")
data class SondageRow(@PrimaryKey val id: String, val terrainFin: String, val json: String)

@Entity(tableName = "changement")
data class ChangementRow(@PrimaryKey(autoGenerate = true) val n: Long = 0, val date: String, val json: String)

/** Le reste (source, domaines, moyenne, calendrier…), en JSON sous une clé. */
@Entity(tableName = "meta")
data class MetaRow(@PrimaryKey val cle: String, val valeur: String)

@Dao
interface DataDao {
    @Query("SELECT * FROM candidat ORDER BY tri")
    fun candidats(): Flow<List<CandidatRow>>

    @Query("SELECT * FROM mesure ORDER BY candidatId, ordre")
    fun mesures(): Flow<List<MesureRow>>

    @Query("SELECT * FROM sondage ORDER BY terrainFin DESC")
    fun sondages(): Flow<List<SondageRow>>

    @Query("SELECT * FROM changement ORDER BY n")
    fun changements(): Flow<List<ChangementRow>>

    @Query("SELECT * FROM changement ORDER BY n")
    suspend fun changementsNow(): List<ChangementRow>

    @Query("SELECT * FROM meta")
    fun metas(): Flow<List<MetaRow>>

    @Query("SELECT COUNT(*) FROM candidat")
    suspend fun nbCandidats(): Int

    @Query("DELETE FROM candidat") suspend fun viderCandidats()
    @Query("DELETE FROM mesure") suspend fun viderMesures()
    @Query("DELETE FROM sondage") suspend fun viderSondages()
    @Query("DELETE FROM changement") suspend fun viderChangements()
    @Query("DELETE FROM meta") suspend fun viderMetas()

    @Insert suspend fun insererCandidats(rows: List<CandidatRow>)
    @Insert suspend fun insererMesures(rows: List<MesureRow>)
    @Insert suspend fun insererSondages(rows: List<SondageRow>)
    @Insert suspend fun insererChangements(rows: List<ChangementRow>)
    @Insert suspend fun insererMetas(rows: List<MetaRow>)

    /** Remplace toutes les données d'un coup : l'appli ne voit jamais un état à moitié mis à jour. */
    @Transaction
    suspend fun remplacer(
        candidats: List<CandidatRow>,
        mesures: List<MesureRow>,
        sondages: List<SondageRow>,
        changements: List<ChangementRow>,
        metas: List<MetaRow>,
    ) {
        viderCandidats(); viderMesures(); viderSondages(); viderChangements(); viderMetas()
        insererCandidats(candidats)
        insererMesures(mesures)
        insererSondages(sondages)
        insererChangements(changements)
        insererMetas(metas)
    }
}

@Database(
    entities = [CandidatRow::class, MesureRow::class, SondageRow::class, ChangementRow::class, MetaRow::class],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): DataDao

    companion object {
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "avant-de-voter.db").build()
    }
}
