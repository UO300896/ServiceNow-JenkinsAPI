class ServiceNowClient implements Serializable {
    private def steps
    private String baseUrl = "https://dev342177.service-now.com" //Sustituir por tu instancia particular
    private String credsId = "snow-credentials" //Sustituir por el nombre de que tengan tus credenciales en Jenkins

    ServiceNowClient(steps) {
        this.steps = steps
    }

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
     * Añade una Work Note en la tarea.
     */
    void documentarNotaDeTarea(String numeroCambio, String numeroTarea, String mensajeNota) {
        String sid = obtenerSysId(numeroCambio, numeroTarea)
        if (!sid) steps.error("Error: No se encontró la tarea ${numeroTarea} en el cambio ${numeroCambio}")

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
        String sid = obtenerSysId(numeroCambio)
        if (!sid) steps.error("Error: No se encontró el cambio ${numeroCambio}")

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
        String sid = obtenerSysId(numeroCambio, numeroTarea)
        if (!sid) steps.error("Error: No se encontró la tarea para cerrar")

        def payload = [
            state: "3",                 // Estado 'Closed'
            close_code: "successful",   // Código 'Cerrado completado'
            close_notes: notasDeCierre
        ]

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
    String sid = obtenerSysId(numeroCambio, numeroTarea)
    if (!sid) steps.error("No se encontró la tarea ${numeroTarea}")

    
    def payload = [
        on_hold: true,
        on_hold_reason: motivo
    ]

    steps.httpRequest(
            url: "${baseUrl}/api/now/table/change_task/${sid}",
            authentication: credsId,
            httpMode: 'PUT',
            contentType: 'APPLICATION_JSON',
            requestBody: steps.writeJSON(json: payload, returnText: true)
        )
    }
}
