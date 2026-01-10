import ServiceNowClient
def call() {
    pipeline {
        agent any
        
        parameters {
            string(name: 'STR_CAMBIO', defaultValue: 'CHG0000005', description: 'Número del Cambio')
            string(name: 'STR_TAREA', defaultValue: 'CTASK0010003', description: 'Número de la Tarea')
            text(name: 'STR_MENSAJE', defaultValue: 'Actualización desde Jenkins', description: 'Mensaje de la nota')
            booleanParam(name: 'BOOL_CERRAR', defaultValue: false, description: '¿Cerrar la tarea al finalizar?')
        }

        stages {
            stage('Procesar en ServiceNow') {
                steps {
                    script {
                        // Instanciamos el cliente profesional
                        def sn = new ServiceNowClient(this)

                        // 1. Siempre documentamos la nota (usando los parámetros del usuario)
                        echo "Añadiendo nota a la tarea ${params.STR_TAREA}..."
                        sn.documentarNotaDeTarea(params.STR_CAMBIO, params.STR_TAREA, params.STR_MENSAJE)

                        // 2. Si el usuario marcó el check, cerramos la tarea
                        if (params.BOOL_CERRAR) {
                            echo "Cerrando la tarea ${params.STR_TAREA}..."
                            sn.cerrarTarea(params.STR_CAMBIO, params.STR_TAREA, "Cierre automático: ${params.STR_MENSAJE}")
                        }
                    }
                }
            }
        }
    }
}
