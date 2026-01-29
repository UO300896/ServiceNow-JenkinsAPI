class ServiceNowClient implements Serializable {
    private def steps
    private String baseUrl
    private String credsId

    ServiceNowClient(steps, String baseUrl, String credsId) {
        this.steps = steps
        this.baseUrl = baseUrl
        this.credsId = credsId
    }

    // Métodos privados/auxiliares
    /**
     * Método privado para obtener el sys_id de la tarea dinámicamente según los parámetros.
     */
    private String obtenerSysId(String numeroCambio, String numeroTarea) {
        // Codificamos la query para evitar el error de caracteres ilegales.
        String query = URLEncoder.encode("number=${numeroTarea}^change_request.number=${numeroCambio}", "UTF-8")
        
        def response = steps.httpRequest(
            url: "${baseUrl}/api/now/table/change_task?sysparm_query=${query}&sysparm_fields=sys_id",
            authentication: credsId,
            httpMode: 'GET',
            contentType: 'APPLICATION_JSON'
        )
        def json = steps.readJSON text: response.content
        return (json.result && json.result.size() > 0) ? json.result[0].sys_id : null
    }

    /**
     * Método privado para obtener el sys_id del cambio dinámicamente según los parámetros.
     */
    private String obtenerSysId(String numeroCambio) {
        // Codificamos la query para evitar el error de caracteres ilegales.
        String query = URLEncoder.encode("number=${numeroCambio}", "UTF-8")
        
        def response = steps.httpRequest(
            url: "${baseUrl}/api/now/table/change_request?sysparm_query=${query}&sysparm_fields=sys_id",
            authentication: credsId,
            httpMode: 'GET',
            contentType: 'APPLICATION_JSON'
        )
        def json = steps.readJSON text: response.content
        return (json.result && json.result.size() > 0) ? json.result[0].sys_id : null
    }

    /**
     * Método privado para obtener el sys_id de un usuario por su nombre completo.
     */
    private String obtenerIdUsuario(String nombreCompleto) {
        // Codificamos el nombre porque puede tener espacios o tildes
        String query = URLEncoder.encode("name=${nombreCompleto}", "UTF-8")
        
        // Buscamos en la tabla de usuarios (sys_user)
        def response = steps.httpRequest(
            url: "${baseUrl}/api/now/table/sys_user?sysparm_query=${query}&sysparm_fields=sys_id",
            authentication: credsId,
            httpMode: 'GET',
            contentType: 'APPLICATION_JSON'
        )
        
        def json = steps.readJSON text: response.content
        // Devolvemos el ID si lo encontramos, o null si no existe
        return (json.result && json.result.size() > 0) ? json.result[0].sys_id : null
    }

    // Métodos públicos
    
    /**
     * Añade una Work Note en la tarea.
     */
    void documentarNotaDeTarea(String numeroCambio, String numeroTarea, String mensajeNota) {
        // 1. Buscamos el ID de la tarea
        String sid = obtenerSysId(numeroCambio, numeroTarea)
        if (!sid) steps.error("Error: No se encontró la tarea ${numeroTarea} en el cambio ${numeroCambio}")
        // 2. Llamada a API con los cambios a realizar
        steps.httpRequest(
            url: "${baseUrl}/api/now/table/change_task/${sid}",
            authentication: credsId,
            httpMode: 'PUT',
            contentType: 'APPLICATION_JSON',
            requestBody: steps.writeJSON(json: [work_notes: mensajeNota], returnText: true)
        )
    }    
    
    /**
     * Añade una Work Note en el cambio.
     */
    void documentarNotaDeCambio(String numeroCambio, String mensajeNota) {
        // 1. Buscamos el ID de la tarea
        String sid = obtenerSysId(numeroCambio)
        if (!sid) steps.error("Error: No se encontró el cambio ${numeroCambio}")

        // 2. Llamada a API con los cambios a realizar
        steps.httpRequest(
            url: "${baseUrl}/api/now/table/change_request/${sid}",
            authentication: credsId,
            httpMode: 'PUT',
            contentType: 'APPLICATION_JSON',
            requestBody: steps.writeJSON(json: [work_notes: mensajeNota], returnText: true)
        )
    }
    
    /**
     * Cierra la tarea con la información de cierre requerida por ServiceNow.
     */
    void cerrarTarea(String numeroCambio, String numeroTarea, String notasDeCierre) {
        // 1. Buscamos el ID de la tarea
        String sid = obtenerSysId(numeroCambio, numeroTarea)
        if (!sid) steps.error("Error: No se encontró la tarea para cerrar")

        // 2. Declaramos cambios a realizar
        def payload = [
            state: "3",                 // Estado 'Closed'
            close_code: "successful",   // Código 'Cerrado completado'
            close_notes: notasDeCierre
        ]

        // 3. Llamada a API con los cambios a realizar anteriormente declarados
        steps.httpRequest(
            url: "${baseUrl}/api/now/table/change_task/${sid}",
            authentication: credsId,
            httpMode: 'PUT',
            contentType: 'APPLICATION_JSON',
            requestBody: steps.writeJSON(json: payload, returnText: true)
        )
    }

    /**
 * Pone una tarea en espera usando el campo booleano y el motivo específico.
 */
void ponerTareaEnEspera(String numeroCambio, String numeroTarea, String motivo) {
    // 1. Buscamos el ID de la tarea
    String sid = obtenerSysId(numeroCambio, numeroTarea)
    if (!sid) steps.error("No se encontró la tarea ${numeroTarea}")

    // 2. Declaramos cambios a realizar
    def payload = [
        on_hold: true,
        on_hold_reason: motivo
    ]

    // 3. Llamada a API con los cambios a realizar anteriormente declarados
    steps.httpRequest(
            url: "${baseUrl}/api/now/table/change_task/${sid}",
            authentication: credsId,
            httpMode: 'PUT',
            contentType: 'APPLICATION_JSON',
            requestBody: steps.writeJSON(json: payload, returnText: true)
        )
    }

        /**
 * Quita una tarea en espera usando el campo booleano y el motivo específico.
 */
void quitarTareaEnEspera(String numeroCambio, String numeroTarea) {
    // 1. Buscamos el ID de la tarea
    String sid = obtenerSysId(numeroCambio, numeroTarea)
    if (!sid) steps.error("No se encontró la tarea ${numeroTarea}")

    // 2. Declaramos cambios a realizar
    def payload = [
        on_hold: false,
        on_hold_reason: ""
    ]

    // 3. Llamada a API con los cambios a realizar anteriormente declarados
    steps.httpRequest(
            url: "${baseUrl}/api/now/table/change_task/${sid}",
            authentication: credsId,
            httpMode: 'PUT',
            contentType: 'APPLICATION_JSON',
            requestBody: steps.writeJSON(json: payload, returnText: true)
        )
    }

    /**
     * Pone la tarea en ejecución.
     */
    void ponerTareaEnEjecucion(String numeroCambio, String numeroTarea) {
        // 1. Buscamos el ID de la tarea
        String sid = obtenerSysId(numeroCambio, numeroTarea)
        if (!sid) steps.error("Error: No se encontró la tarea para cerrar")

        // 2. Llamada a API con los cambios a realizar
        steps.httpRequest(
            url: "${baseUrl}/api/now/table/change_task/${sid}",
            authentication: credsId,
            httpMode: 'PUT',
            contentType: 'APPLICATION_JSON',
            requestBody: steps.writeJSON(json: [state: 2], returnText: true) //Estado 2 = En Ejecución
        )
    }


    /**
     * Asigna la tarea a una persona específica.
     */
    void asignarTarea(String numeroCambio, String numeroTarea, String nombrePersona) {
        // 1. Buscamos el ID de la tarea
        String sidTarea = obtenerSysId(numeroCambio, numeroTarea)
        if (!sidTarea) steps.error("No se encontró la tarea ${numeroTarea}")

        // 2. Buscamos el ID de la persona
        String sidUsuario = obtenerIdUsuario(nombrePersona)
        if (!sidUsuario) steps.error("Error: No se encontró al usuario '${nombrePersona}' en ServiceNow. Revisa el nombre exacto.")

        // 3. Llamada a API con los cambios a realizar
        steps.httpRequest(
            url: "${baseUrl}/api/now/table/change_task/${sidTarea}",
            authentication: credsId,
            httpMode: 'PUT',
            contentType: 'APPLICATION_JSON',
            requestBody: steps.writeJSON(json: [assigned_to: sidUsuario], returnText: true)
        )
    }

    /**
     * Busca una tarea en base a otra. 
     * Útil para buscar tareas hermanas.
     */
    String buscarTarea(String numeroTarea, int cantidadASumar) {
        // Regex para separar letras de números y revisar formato
        def matcher = (numeroTarea =~ /([A-Za-z]+)(\d+)/)
        if (!matcher.find()) steps.error("La tarea ${numeroTarea} no cumple el formato solicitado")
            
            String prefijo = matcher[0][1] // normalmente "CTASK"
            String digitos = matcher[0][2] 
            int longitud = digitos.length() // Útil para no perder ceros de la izquierda si los hubiera
            
            // Se suma el número solicitado
            def nuevoNumero = digitos.toInteger() + cantidadASumar
            
            // Reconstruimos con ceros a la izquierda (si los hubiera)
            return prefijo + nuevoNumero.toString().padLeft(longitud, '0')
    }
}
