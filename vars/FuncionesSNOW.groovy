import java.net.URLEncoder
/**
 * FuncionesSNOW.groovy
 *
 * Accede a API Service Now para documentar, asignar tareas, etc.
 * @author Alejandro de San Claudio Mesa
 * @version 1.0.0
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


def obtenerSysId(String numeroCambio, String numeroTarea) {
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


void documentarNotaDeTarea(String numeroCambio, String numeroTarea, String mensajeNota) {
    String sid = obtenerSysId(numeroCambio, numeroTarea)
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

void cerrarTarea(String numeroCambio, String numeroTarea, String notasDeCierre) {
    String sid = obtenerSysId(numeroCambio, numeroTarea)
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

void ponerTareaEnEspera(String numeroCambio, String numeroTarea, String motivo) {
    String sid = obtenerSysId(numeroCambio, numeroTarea)
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

void ponerTareaEnEjecucion(String numeroCambio, String numeroTarea) {
    String sid = obtenerSysId(numeroCambio, numeroTarea)
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

void asignarTarea(String numeroCambio, String numeroTarea, String nombrePersona) {
    String sidTarea = obtenerSysId(numeroCambio, numeroTarea)
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