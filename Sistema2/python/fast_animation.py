import pandas as pd
import matplotlib.pyplot as plt
from mpl_toolkits.mplot3d import Axes3D
from matplotlib.animation import FuncAnimation, FFMpegWriter
import numpy as np
from tqdm import tqdm
from pathlib import Path
import time # <-- Añadido para un temporizador manual

def parse_new_format_data(filename):
    """
    Analiza el archivo de datos de simulación con el nuevo formato 't=...'.
    """
    with open(filename, 'r') as f:
        lines = f.readlines()

    particle_data_by_time = {}
    current_time = None
    current_particles = []

    print("Procesando archivo de datos...")
    for line in tqdm(lines, desc="Leyendo líneas"):
        line = line.strip()
        if not line:
            continue

        # --- LÓGICA CORRECTA: Detectar header de tiempo ---
        if line.startswith('t='):
            if current_time is not None and current_particles:
                particle_data_by_time[current_time] = np.array(current_particles)

            try:
                current_time = float(line.split('=')[1])
                current_particles = []
            except (ValueError, IndexError):
                continue
        # --- LÓGICA CORRECTA: Leer línea de partícula ---
        else:
            parts = line.split(';')
            if len(parts) >= 5:  # id;galaxyId;x;y;z
                try:
                    galaxy_id = int(parts[1])
                    x = float(parts[2])
                    y = float(parts[3])
                    z = float(parts[4])
                    current_particles.append([galaxy_id, x, y, z])
                except (ValueError, IndexError):
                    pass

    # Asegúrate de guardar el último paso de tiempo
    if current_time is not None and current_particles:
        particle_data_by_time[current_time] = np.array(current_particles)

    return particle_data_by_time


def animate(particle_data, output_filename='galaxy_animation.mp4'):
    """
    Crea y guarda una animación 3D de la simulación de partículas.
    Esta versión es mucho más eficiente que la del ejemplo.
    """
    if not particle_data:
        print("No hay datos para animar.")
        return

    timesteps = sorted(particle_data.keys())

    fig = plt.figure(figsize=(12, 9))
    ax = fig.add_subplot(111, projection='3d')

    # --- Estilo de gráfico científico (fondo blanco) ---
    ax.set_facecolor('white')
    fig.patch.set_facecolor('white')
    ax.xaxis.pane.fill = False
    ax.yaxis.pane.fill = False
    ax.zaxis.pane.fill = False
    ax.grid(True, linestyle='--', alpha=0.6)

    # Determina los límites de los ejes para que la vista sea estable
    all_positions = np.vstack([data[:, 1:4] for data in particle_data.values()])
    min_coords = all_positions.min(axis=0)
    max_coords = all_positions.max(axis=0)

    margin = np.max(max_coords - min_coords) * 0.1
    ax.set_xlim([min_coords[0] - margin, max_coords[0] + margin])
    ax.set_ylim([min_coords[1] - margin, max_coords[1] + margin])
    ax.set_zlim([min_coords[2] - margin, max_coords[2] + margin])

    ax.set_xlabel('X')
    ax.set_ylabel('Y')
    ax.set_zlabel('Z')

    # Paleta de colores sólidos
    colors = ['blue', 'red', 'green', 'purple', 'orange', 'brown']

    # Objeto de dispersión inicial (vacío) que se actualizará
    scatter = ax.scatter([], [], [], s=15, alpha=0.8)
    title = ax.set_title('')

    # --- CAMBIO: Crear la barra de progreso antes de la animación ---
    pbar = tqdm(total=len(timesteps), desc="Renderizando video", unit="frame")

    def update(frame):
        """Función de actualización eficiente, sin usar ax.cla()"""
        time = timesteps[frame]
        positions = particle_data[time] # Array de [galaxy_id, x, y, z]

        # Asigna colores basados en el galaxy_id
        particle_colors = [colors[int(gid) % len(colors)] for gid in positions[:, 0]]

        # Actualiza las posiciones y colores del scatter plot directamente
        scatter._offsets3d = (positions[:, 1], positions[:, 2], positions[:, 3])
        scatter.set_color(particle_colors)

        title.set_text(f'Simulación de Galaxias - Tiempo: {time:.4f}')

        # --- CAMBIO: Actualizar la barra de progreso en cada frame ---
        pbar.update(1)

        return scatter, title

    # Crea la animación
    ani = FuncAnimation(fig, update, frames=len(timesteps), blit=False, interval=30)

    # Guarda la animación
    print("\nGuardando animación... Esto puede tardar unos minutos.")
    start_time = time.time()
    try:
        writer = FFMpegWriter(fps=30, metadata=dict(artist='Me'), bitrate=1800)
        ani.save(output_filename, writer=writer, dpi=150)
    except FileNotFoundError:
        print("\n❌ ERROR: No se encontró el programa 'ffmpeg'.")
        print("   Por favor, instálalo y asegúrate de que esté en el PATH de tu sistema.")
        print("   - En macOS: 'brew install ffmpeg'")
        print("   - En Linux: 'sudo apt-get install ffmpeg'")
    finally:
        # --- CAMBIO: Asegurarse de cerrar la barra de progreso siempre ---
        pbar.close()

    end_time = time.time()
    print(f"\nAnimación guardada como '{output_filename}' en {end_time - start_time:.2f} segundos.")


if __name__ == '__main__':
    # --- ¡IMPORTANTE! Cambia esto por el nombre de tu archivo de datos ---
    filename = Path(__file__).parent / 'data' / 'sim_dt_0.001.csv'

    print(f"Cargando y analizando datos desde '{filename}'...")
    data = parse_new_format_data(filename)

    if not data:
        print("No se encontraron datos válidos en el archivo.")
    else:
        print(f"Datos cargados exitosamente. {len(data)} pasos de tiempo encontrados.")
        animate(data, output_filename='galaxy_animation_white.mp4')