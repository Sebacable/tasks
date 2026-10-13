# Habit Tracker (Android)

App de tareas y hábitos con períodos diario / semanal / mensual, historial y estadísticas por área.

## Obtener el APK
1. Hacer push a `main`: GitHub Actions compila solo (también se puede lanzar a mano desde **Actions → Build Android APK → Run workflow**).
2. Entrar a la ejecución terminada y descargar el artefacto **app-debug**.
3. Descomprimir e instalar `app-debug.apk` (Android 8.0 o superior; hay que permitir "instalar apps de origen desconocido").

## Cómo funciona
- Cada tarea se marca **por período**: al cambiar el día/semana/mes aparece destildada de nuevo.
- Cada cumplimiento queda guardado en el historial (tabla `completions`).
- **% cumplido** = períodos cumplidos / períodos transcurridos desde que se creó la tarea (el período en curso solo cuenta si ya está hecho, para no bajar el promedio antes de tiempo).
- **Racha**: períodos seguidos cumplidos.
- Mantener apretada una tarea → eliminarla (con confirmación).
- Menú ☰ → áreas: filtrar por área, con tareas pendientes y totales. Sin filtro, la lista se agrupa por área.
- Al crear una tarea se sugieren las áreas existentes (evita duplicados como "Salud" y "salud").
