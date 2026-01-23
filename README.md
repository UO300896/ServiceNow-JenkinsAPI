# ServiceNow-JenkinsAPI
API y Pipeline Ejemplo para documentar en ServiceNow directamente desde Jenkins

## Descripción
El archivo vars/FuncionesSNOW.groovy está implementado como Global Variable de Jenkins, con programación funcional y permitiendo el uso de proxys en entorno administrativos
tradicionales. Tiene las mismas funcionalidades que src/ServiceNowClient.groovy

El archivo src/ServiceNowClient.groovy sirve como módulo orientado a objetos para acceder a las propiedades de la API de SNOW, 
permitiendo documentación de tareas y cambios tanto en notas de trabajo como notas de cierre.

El archivo vars/ejecutarServiceNow sirve como ejemplo de Pipeline implementando las funcionalidades con un ejemplo,
solicitando al usuario parámetros para el número de cambio, número de tarea, mensaje que utilizar y booleano de cierre de tarea.

## REQUISITOS
-Instancia de SNOW (se puede conseguir desde el portal oficial de Service Now Developers).

-Instancia de Jenkins.

-Credenciales de usuario de SNOW (usuario y contraseña) introducidas en Jenkins:

      -Manage Jenkins -> Credentials.
      
-Plugin HTTP Request habilitado en Jenkins (se puede trabajar sin él. Aunque se sustituirían los métodos httpRequest por curls manuales).

-Plugin Pipeline Utility Steps habilitado en Jenkins (posibilita lectura de JSON).
