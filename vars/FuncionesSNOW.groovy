import java.net.URLEncoder
/**
 * FuncionesSNOW.groovy
 *
 * Accede a API Service Now para documentar, asignar tareas, etc.
 * @author Alejandro de San Claudio Mesa
 * @version 1.0.1
 *
 *
 *
 */


// VARIABLES GLOBALES DEL SCRIPT


def baseUrl
def credsId
def proxyCreds

// MÉTODO DE INICIALIZACIÓN

def init(url, creds, proxy) {

    // Quitamos la barra final si existe
    this.baseUrl = url.endsWith('/') ? url[0..-2] : url
    this.credsId = creds
    this.proxyCreds = proxy
}


// MÉTODOS PRIVADOS / AUXILIARES

/**
 * Obtiene id interno de una tarea
 *
 * @param numeroCambio: numero de cambio padre de la tarea a obtener para evitar duplicados
 * @param numeroTarea
 * @return sys_id | null
 */
def obtenerSysIdDeTarea(String numeroCambio, String numeroTarea) {
    String query = URLEncoder.encode("number=${numeroTarea}^change_request.number=${numeroCambio}", "UTF-8")
    
    def response = httpRequest(
        url: "${baseUrl}/api/now/table/change_task?sysparm_query=${query}&sysparm_fields=sys_id",
        httpProxy: proxyCreds,
        authentication: credsId,
        httpMode: 'GET',
        ignoreSslErrors: true, 
        contentType: 'APPLICATION_JSON'
    )

    def json = readJSON text: response.content
    return (json.result && json.result.size() > 0) ? json.result[0].sys_id : null
}

/**
 * Obtiene id interno de un cambio
 *
 * @param numeroCambio
 * @return sys_id | null
 */
def obtenerSysIdDeCambio(String numeroCambio) {
    String query = URLEncoder.encode("number=${numeroCambio}", "UTF-8")
    
    def response = httpRequest(
        url: "${baseUrl}/api/now/table/change_request?sysparm_query=${query}&sysparm_fields=sys_id",
        httpProxy: proxyCreds,
        authentication: credsId,
        httpMode: 'GET',
        ignoreSslErrors: true,
        contentType: 'APPLICATION_JSON'
    )
    def json = readJSON text: response.content
    return (json.result && json.result.size() > 0) ? json.result[0].sys_id : null
}

/**
 * Obtiene id interno de un usuario
 *
 * @param nombreCompleto
 * @return sys_id | null
 */
def obtenerIdUsuario(String nombreCompleto) {
    String query = URLEncoder.encode("name=${nombreCompleto}", "UTF-8")
    
    def response = httpRequest(
        url: "${baseUrl}/api/now/table/sys_user?sysparm_query=${query}&sysparm_fields=sys_id",
        httpProxy: proxyCreds,
        authentication: credsId,
        httpMode: 'GET',
        ignoreSslErrors: true,
        contentType: 'APPLICATION_JSON'
    )
    
    def json = readJSON text: response.content
    return (json.result && json.result.size() > 0) ? json.result[0].sys_id : null
}


// MÉTODOS PÚBLICOS

/**
 * Documenta en las notas de una tarea
 *
 * @param numeroCambio: numero de cambio padre de la tarea a obtener para evitar duplicados
 * @param numeroTarea
 * @param mensajeNota: mensaje a añadir en las notas de la tarea
 */
void documentarNotaDeTarea(String numeroCambio, String numeroTarea, String mensajeNota) {
    String sid = obtenerSysIdDeTarea(numeroCambio, numeroTarea)
    if (!sid) error("Error: No se encontró la tarea ${numeroTarea} en el cambio ${numeroCambio}")
    
    httpRequest(
        url: "${baseUrl}/api/now/table/change_task/${sid}",
        httpProxy: proxyCreds,
        authentication: credsId,
        httpMode: 'PUT',
        ignoreSslErrors: true,
        contentType: 'APPLICATION_JSON',
        requestBody: writeJSON(json: [work_notes: mensajeNota], returnText: true)
    )
}    

/**
 * Documenta en las notas de un cambio
 *
 * @param numeroCambio
 * @param mensajeNota: mensaje a añadir en las notas del cambio
 */
void documentarNotaDeCambio(String numeroCambio, String mensajeNota) {
    
    String sid = obtenerSysIdDeCambio(numeroCambio)
    if (!sid) error("Error: No se encontró el cambio ${numeroCambio}")

    httpRequest(
        url: "${baseUrl}/api/now/table/change_request/${sid}",
        httpProxy: proxyCreds,
        authentication: credsId,
        httpMode: 'PUT',
        ignoreSslErrors: true,
        contentType: 'APPLICATION_JSON',
        requestBody: writeJSON(json: [work_notes: mensajeNota], returnText: true)
    )
}

/**
 * Cierra una tarea y documenta su motivo de cierre
 *
 * @param numeroCambio: numero de cambio padre de la tarea a obtener para evitar duplicados
 * @param numeroTarea
 * @param notasDeCierre: mensaje a añadir en las nota de cierre
 */
void cerrarTarea(String numeroCambio, String numeroTarea, String notasDeCierre) {
    String sid = obtenerSysIdDeTarea(numeroCambio, numeroTarea)
    if (!sid) error("Error: No se encontró la tarea para cerrar")

    def payload = [
        state: "3",
        close_code: "successful",
        close_notes: notasDeCierre
    ]

    httpRequest(
        url: "${baseUrl}/api/now/table/change_task/${sid}",
        httpProxy: proxyCreds,
        authentication: credsId,
        httpMode: 'PUT',
        ignoreSslErrors: true,
        contentType: 'APPLICATION_JSON',
        requestBody: writeJSON(json: payload, returnText: true)
    )
}

/**
 * Pone tarea en espera con un motivo determinado
 *
 * @param numeroCambio: numero de cambio padre de la tarea a obtener para evitar duplicados
 * @param numeroTarea
 * @param motivo: motivo por el cual se pone en espera la tarea
 */
void ponerTareaEnEspera(String numeroCambio, String numeroTarea, String motivo) {
    String sid = obtenerSysIdDeTarea(numeroCambio, numeroTarea)
    if (!sid) error("No se encontró la tarea ${numeroTarea}")

    def payload = [
        on_hold: true,
        on_hold_reason: motivo
    ]

    httpRequest(
        url: "${baseUrl}/api/now/table/change_task/${sid}",
        httpProxy: proxyCreds,
        authentication: credsId,
        httpMode: 'PUT',
        ignoreSslErrors: true,
        contentType: 'APPLICATION_JSON',
        requestBody: writeJSON(json: payload, returnText: true)
    )
}

/**
 * Quita el estado de en espera de una tarea
 *
 * @param numeroCambio: numero de cambio padre de la tarea a obtener para evitar duplicados
 * @param numeroTarea
 */
void quitarTareaEnEspera(String numeroCambio, String numeroTarea) {
    String sid = obtenerSysIdDeTarea(numeroCambio, numeroTarea)
    if (!sid) error("No se encontró la tarea ${numeroTarea}")

    def payload = [
        on_hold: false,
        on_hold_reason: ""
    ]

    httpRequest(
        url: "${baseUrl}/api/now/table/change_task/${sid}",
        httpProxy: proxyCreds,
        authentication: credsId,
        httpMode: 'PUT',
        ignoreSslErrors: true,
        contentType: 'APPLICATION_JSON',
        requestBody: writeJSON(json: payload, returnText: true)
    )
}

/**
 * Pone tarea en curso/ejecución
 *
 * @param numeroCambio: numero de cambio padre de la tarea a obtener para evitar duplicados
 * @param numeroTarea
 */
void ponerTareaEnEjecucion(String numeroCambio, String numeroTarea) {
    String sid = obtenerSysIdDeTarea(numeroCambio, numeroTarea)
    if (!sid) error("Error: No se encontró la tarea para poner en ejecución")

    httpRequest(
        url: "${baseUrl}/api/now/table/change_task/${sid}",
        httpProxy: proxyCreds,
        authentication: credsId,
        httpMode: 'PUT',
        ignoreSslErrors: true,
        contentType: 'APPLICATION_JSON',
        requestBody: writeJSON(json: [state: 2], returnText: true)
    )
}

/**
 * Asigna una tarea a un usuario dado su nombre completo
 *
 * @param numeroCambio: numero de cambio padre de la tarea a obtener para evitar duplicados
 * @param numeroTarea
 * @param nombrePersona: nombre completo de la persona a la cual asignar la tarea. debe coincidir exactamente con el nombre registrado en SNOW
 */
void asignarTarea(String numeroCambio, String numeroTarea, String nombrePersona) {
    String sidTarea = obtenerSysIdDeTarea(numeroCambio, numeroTarea)
    if (!sidTarea) error("No se encontró la tarea ${numeroTarea}")

    String sidUsuario = obtenerIdUsuario(nombrePersona)
    if (!sidUsuario) error("Error: No se encontró al usuario '${nombrePersona}'")

    httpRequest(
        url: "${baseUrl}/api/now/table/change_task/${sidTarea}",
        httpProxy: proxyCreds,
        authentication: credsId,
        httpMode: 'PUT',
        ignoreSslErrors: true,
        contentType: 'APPLICATION_JSON',
        requestBody: writeJSON(json: [assigned_to: sidUsuario], returnText: true)
    )
}

/**
 * Devuelve el número de una tarea que incluya una relación numérica directa con otra. Útil para encontrar tareas hermanas
 *
 * @param numeroTarea
 * @param cantidadASumar: cantidad a sumar (o restar si se introduce un número negativo) a la tarea original para hallar otra
 * @return número de tarea resultante. Debe verificarse posteriormente que dicha tarea exista realmente
 */
String buscarTarea(String numeroTarea, int cantidadASumar) {
    def matcher = (numeroTarea =~ /([A-Za-z]+)(\d+)/)
    if (!matcher.find()) {
        error("La tarea ${numeroTarea} no cumple el formato solicitado")
        return null
    }
        
    String prefijo = matcher[0][1]
    String digitos = matcher[0][2] 
    int longitud = digitos.length()
    
    def nuevoNumero = digitos.toInteger() + cantidadASumar
    
    return prefijo + nuevoNumero.toString().padLeft(longitud, '0')
}


return this