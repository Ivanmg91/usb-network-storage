<div align="center">
  <h1>USBLocal 📁🚀</h1>
  <p><strong>El gestor de archivos definitivo para el USB de tu router</strong></p>
</div>

¡Bienvenido a **USBLocal**! Esta aplicación Android nativa está diseñada para hacerte la vida más fácil. Si tienes un pendrive o disco duro conectado al router de tu casa y quieres acceder a tus películas, fotos y documentos desde el móvil sin cables, ¡esta es tu app!

## ✨ Características principales

- 📡 **Conexión universal**: Compatible con routers modernos y antiguos (soporta desde el clásico **SMB1** hasta **SMB2/SMB3**).
- 🎬 **Visualizador integrado**: Abre fotos, textos y vídeos directamente desde el router.
- ⚡ **Subidas y Descargas rápidas**: Pasa archivos de tu móvil al pendrive (o viceversa) con un solo toque y viendo el progreso.
- 📊 **Control de espacio**: Te muestra en tiempo real cuánto espacio libre te queda en el USB (ideal si lo formateas en FAT32 o NTFS).
- 🔐 **Seguro y Privado**: Tus contraseñas se guardan encriptadas en tu propio móvil.

---

## 📱 ¿Cómo descargo la aplicación?

¡No necesitas saber programar ni tener herramientas instaladas! 

1. Ve a la sección de [**Releases**](../../releases) (Lanzamientos) en la parte derecha de la página principal de GitHub.
2. Haz clic en la última versión publicada (ej. v1.0).
3. En el apartado **Assets**, descarga el archivo `.apk`.
4. Pásalo a tu móvil Android, ábrelo (te pedirá permiso para instalar apps desconocidas) ¡e instálalo!

---

## 🛠️ ¿Cómo conecto mi USB?

Es súper fácil, solo necesitas estar conectado al Wi-Fi de tu casa:

1. Abre la app **USBLocal** en tu móvil.
2. Pulsa el botón **+** abajo a la derecha.
3. Rellena los datos de tu router:
   - **Dirección IP**: Casi siempre es `192.168.1.1` o `192.168.0.1`
   - **Recurso compartido**: Si no sabes cómo se llama tu pendrive, ¡déjalo en blanco! La app te mostrará luego las carpetas automáticamente.
   - **Usuario/Contraseña**: Si tu router no te pide clave para acceder al USB, marca la casilla **Acceso anónimo**.
4. ¡Dale a **Probar Conexión**! La app revisará paso a paso que todo funcione correctamente.
5. Si salen los ticks verdes ✅, dale a **Guardar**.

> 💡 **Tip de uso:** Para subir un archivo al pendrive, recuerda primero pulsar en la carpeta (por ejemplo `usb1_1_1`) para entrar en él. ¡En la pantalla principal no hay espacio libre porque es solo el índice del router!

---

## 💻 Para Desarrolladores (Geeks)

Si quieres trastear con el código de la app, el código está limpísimo y utiliza lo último de Android:

| Componente | Tecnología |
|---|---|
| Lenguaje | Kotlin 2.0 |
| Interfaz (UI) | Jetpack Compose + Material 3 |
| Protocolo de Red | jcifs-ng (Soporte total SMB1/SMB2/SMB3) |
| Arquitectura | MVVM + Corrutinas de Kotlin |
| Inyección (DI) | Hilt |
| Seguridad | EncryptedSharedPreferences |

### Compilar desde consola
Si prefieres generar el APK en tu propio ordenador:
```bash
./gradlew assembleDebug
```
El APK se guardará en: `app/build/outputs/apk/debug/app-debug.apk`

---
*Hecho con ❤️ para que no tengas que estar pinchando y sacando el pendrive del router nunca más.*
