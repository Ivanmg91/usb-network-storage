# USBLocal — Gestor de Archivos SMB para Android

Aplicación Android nativa para acceder al almacenamiento USB conectado a un router mediante SMB/Samba.

## Requisitos

- **Android Studio** Ladybug (2024.2) o superior
- **JDK 17**
- **Dispositivo Android** con API 26+ (Android 8.0+)
- **Router** con almacenamiento USB y Samba/SMB activado
- El dispositivo debe estar **conectado a la misma red Wi-Fi** que el router

## Compilar e instalar

### Desde Android Studio

1. Abre la carpeta del proyecto en Android Studio
2. Espera a que Gradle sincronice las dependencias
3. Conecta tu dispositivo Android por USB (o usa un emulador)
4. Haz clic en **Run** ▶️ o `Shift+F10`

### Desde terminal

```bash
# En la raíz del proyecto
./gradlew assembleDebug

# Instalar en dispositivo conectado
./gradlew installDebug
```

El APK se genera en: `app/build/outputs/apk/debug/app-debug.apk`

## Configurar una conexión SMB

1. Abre la app **USBLocal**
2. Pulsa **+** para añadir un servidor
3. Rellena los campos:
   - **Dirección IP**: la IP de tu router (ej: `192.168.1.1`)
   - **Recurso compartido**: el nombre del USB (ej: `USB`, `sda1`, `storage`)
   - **Usuario/Contraseña**: las credenciales SMB de tu router
   - Activa **Acceso anónimo** si tu router no requiere credenciales
4. Pulsa **Probar conexión** para verificar paso a paso
5. Si todo es correcto, pulsa **Guardar**
6. Pulsa sobre la conexión para abrir el explorador de archivos

## ¿Cómo encontrar los datos de conexión?

### IP del router
- Normalmente `192.168.1.1` o `192.168.0.1`
- En Android: Ajustes → Wi-Fi → tu red → Gateway

### Nombre del recurso compartido
- Accede al panel de administración del router
- Busca la sección **USB / Storage / Samba**
- El nombre del recurso compartido suele ser `USB`, `sda1`, o un nombre personalizado

### Credenciales
- Algunos routers usan las mismas credenciales del panel de administración
- Otros tienen usuarios SMB específicos
- Si el acceso es público, activa **Acceso anónimo**

## Tecnología

| Componente | Tecnología |
|---|---|
| Lenguaje | Kotlin 2.0 |
| UI | Jetpack Compose + Material 3 |
| SMB | smbj 0.13.0 (SMB2/SMB3) |
| Arquitectura | MVVM |
| DI | Hilt |
| Seguridad | EncryptedSharedPreferences |

## Compatibilidad SMB

- ✅ **SMB2** — soportado completamente
- ✅ **SMB3** — soportado completamente
- ❌ **SMB1** — no soportado por seguridad

> **Nota**: Los routers muy antiguos que solo soporten SMB1 no serán compatibles.
> La mayoría de routers fabricados después de 2015 soportan SMB2 como mínimo.

## Estructura del proyecto

```
app/src/main/java/com/usblocal/app/
├── data/
│   ├── model/          # Modelos de datos (SmbConnection, SmbFile, SmbError)
│   ├── smb/            # SmbDataSource (interfaz) + SmbDataSourceImpl (smbj)
│   ├── repository/     # ConnectionRepository, FileRepository
│   └── storage/        # SecureStorage (EncryptedSharedPreferences)
├── di/                 # Módulo Hilt
├── ui/
│   ├── theme/          # Material 3 (colores, tipografía, tema)
│   ├── navigation/     # NavGraph
│   ├── connections/    # Pantalla de conexiones + formulario
│   ├── browser/        # Explorador de archivos + componentes
│   └── common/         # Componentes reutilizables
└── util/               # FileUtils, NetworkUtils
```

## Licencia

Proyecto personal — uso privado.
