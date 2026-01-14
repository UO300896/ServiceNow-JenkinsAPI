import ServiceNowClient
def call() {
    System.setProperty("file.encoding", "UTF-8")
    pipeline {
        agent any
        
        parameters {
            string(name: 'STR_CAMBIO', defaultValue: '', description: 'Número del Cambio')
            string(name: 'STR_TAREA', defaultValue: '', description: 'Número de la Tarea')
            text(name: 'STR_MENSAJE', defaultValue: 'Actualización desde Jenkins', description: 'Mensaje de la nota')
            text(name: 'STR_MENSAJE_EN_ESPERA', defaultValue: 'En espera a fecha de ejecución', description: 'Motivo de poner en espera')
            string(name: 'STR_USUARIO', defaultValue: 'David Loo', description: 'Nombre del usuario')
            booleanParam(name: 'BOOL_CERRAR', defaultValue: false, description: '¿Cerrar la tarea al finalizar?')
        }

        stages {
            stage('Procesar en ServiceNow') {
                steps {
                    script {
                        // Instanciamos el cliente profesional
                        def sn = new ServiceNowClient(this)

                        // 1. Siempre ponemos la tarea en ejecución, la asignamos al usuario y documentamos la nota de tarea (usando los parámetros del usuario)
                        echo "Asignando tarea ${params.STR_TAREA} a ${params.STR_USUARIO}..."
                        sn.asignarTarea(params.STR_CAMBIO, params.STR_TAREA, params.STR_USUARIO)
                        
                        echo "Poniendo tarea ${params.STR_TAREA} en ejecución..."
                        sn.ponerTareaEnEjecucion(params.STR_CAMBIO, params.STR_TAREA)
                        
                        echo "Añadiendo nota a la tarea ${params.STR_TAREA}..."
                        sn.documentarNotaDeTarea(params.STR_CAMBIO, params.STR_TAREA, params.STR_MENSAJE)

                        // 2. Si el usuario marcó el check, cerramos la tarea y ponemos la tarea siguiente en espera
                        if (params.BOOL_CERRAR) {
                            echo "Cerrando la tarea ${params.STR_TAREA}..."
                            sn.cerrarTarea(params.STR_CAMBIO, params.STR_TAREA, "${params.STR_MENSAJE}")

                            echo "Buscando tarea siguiente..."
                            String tareaEnEspera = sn.buscarTarea(params.STR_TAREA, 1) // Busca la tarea inmediatamente siguiente
                            echo "Poniendo tarea en espera ${tareaEnEspera}..."
                            sn.ponerTareaEnEspera(params.STR_CAMBIO, tareaEnEspera, "${params.STR_MENSAJE_EN_ESPERA}")
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
