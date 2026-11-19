package ar.jornadas.ui

import android.app.Application
import androidx.lifecycle.*
import ar.jornadas.Servicios
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

class ProgramaModel(app: Application, private val estado: SavedStateHandle) : AndroidViewModel(app) {
    private val servicios = Servicios.de(app)
    val mesa = servicios.mesa
    private val repo = servicios.agenda
    val charlas = repo.programa.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val hayEdicion: StateFlow<Boolean?> = repo.hayEdicion.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val dia: StateFlow<Int> = estado.getStateFlow(CLAVE_DIA, 0)
    val tema: StateFlow<String> = estado.getStateFlow(CLAVE_TEMA, "Todos")
    private val _fallo = MutableStateFlow(false)
    val fallo: StateFlow<Boolean> = _fallo
    private val _cargando = MutableStateFlow(false)
    val cargando: StateFlow<Boolean> = _cargando
    private var pedido: Job? = null

    init {
        if (servicios.reclamarCargaInicial()) recargar()
    }

    fun elegirDia(valor: Int) { estado[CLAVE_DIA] = valor }

    fun elegirTema(valor: String) { estado[CLAVE_TEMA] = valor }

    fun recargar() {
        if (pedido?.isActive == true) return
        _cargando.value = true
        pedido = viewModelScope.launch {
            try {
                repo.sincronizar()
                _fallo.value = false
            } catch (e: IOException) {
                _fallo.value = true
            } catch (e: HttpException) {
                _fallo.value = true
            } finally {
                _cargando.value = false
            }
        }
    }

    fun marcar(id: String) { viewModelScope.launch { repo.cambiarFavorita(id) } }

    private companion object {
        const val CLAVE_DIA = "dia"
        const val CLAVE_TEMA = "tema"
    }
}
