package ar.jornadas

import android.content.Context
import androidx.room.Room
import ar.jornadas.data.*
import ar.jornadas.lab.ServicioLocal
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.atomic.AtomicBoolean

class Servicios private constructor(context: Context) {
    val mesa = ServicioLocal(context)
    private val archivo = Room.databaseBuilder(context, Archivo::class.java, "cartelera.db")
        .addMigrations(Archivo.DE_1_A_2).build()
    private val api = Retrofit.Builder().baseUrl("https://programa.invalid/")
        .client(OkHttpClient.Builder().addInterceptor(mesa).build())
        .addConverterFactory(GsonConverterFactory.create()).build().create(ProgramaApi::class.java)
    val agenda = AgendaRepository(api, archivo.cartelera())
    private val cargaInicial = AtomicBoolean(false)

    fun reclamarCargaInicial(): Boolean = cargaInicial.compareAndSet(false, true)

    companion object {
        @Volatile private var unica: Servicios? = null

        fun de(context: Context): Servicios = unica ?: synchronized(this) {
            unica ?: Servicios(context.applicationContext).also { unica = it }
        }
    }
}
