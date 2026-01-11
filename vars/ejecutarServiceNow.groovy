import ServiceNowClient
def call() {
    System.setProperty("file.encoding", "UTF-8")
    pipeline {
        agent any
        
        parameters {
            string(name: 'STR_CAMBIO', defaultValue: '', description: 'Número del Cambio')
            string(name: 'STR_TAREA', defaultValue: '', description: 'Número de la Tarea')
            text(name: 'STR_MENSAJE', defaultValue: 'Actualización desde Jenkins', description: 'Mensaje de la nota')
            booleanParam(name: 'BOOL_CERRAR', defaultValue: false, description: '¿Cerrar la tarea al finalizar?')
        }

        stages {
            stage('Procesar en ServiceNow') {
                steps {
                    script {
                        // Instanciamos el cliente profesional
                        def sn = new ServiceNowClient(this)

                        // 1. Siempre documentamos la nota de tarea (usando los parámetros del usuario)
                        echo "Añadiendo nota a la tarea ${params.STR_TAREA}..."
                        sn.documentarNotaDeTarea(params.STR_CAMBIO, params.STR_TAREA, params.STR_MENSAJE)

                        // 2. Si el usuario marcó el check, cerramos la tarea
                        if (params.BOOL_CERRAR) {
                            echo "Cerrando la tarea ${params.STR_TAREA}..."
                            sn.cerrarTarea(params.STR_CAMBIO, params.STR_TAREA, "${params.STR_MENSAJE}")
                        }
                        
                        // 3. Siempre documentamos la nota del cambio (usando los parámetros del usuario)
                        echo "Añadiendo nota al cambio ${params.STR_CAMBIO}..."
                        sn.documentarNotaDeCambio(params.STR_CAMBIO, params.STR_MENSAJE)
                        
                    }
                }
            }
        }
    }
}
