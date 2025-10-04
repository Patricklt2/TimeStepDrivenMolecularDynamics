import pandas as pd
import matplotlib.pyplot as plt
from mpl_toolkits.mplot3d import Axes3D
from matplotlib.animation import FuncAnimation, FFMpegWriter
import numpy as np
from tqdm import tqdm # --- CAMBIO CLAVE: Importar tqdm ---
from pathlib import Path # --- CAMBIO CLAVE: Importar Path para rutas robustas ---

def parse_simulation_data(filename):
    """
    --- CAMBIO CLAVE: Función de parseo adaptada al nuevo formato 't=...' ---
    Analiza el archivo de datos de simulación con el formato moderno.

    Args:
        filename (str): La ruta al archivo CSV.

    Returns:
        tuple: Una tupla conteniendo:
            - all_timesteps (list): Una lista de los valores de tiempo.
            - particle_data (dict): Un diccionario donde las claves son los pasos de tiempo
                                     y los valores son arrays de NumPy con las posiciones
                                     (x, y, z) de las partículas.
    """
    with open(filename, 'r') as f:
        lines = f.readlines()

    particle_data = {}
    all_timesteps = []
    current_time = None
    current_particles = []

    print("Procesando archivo de datos...")
    # Usamos tqdm aquí también para la carga de datos
    for line in tqdm(lines, desc="Leyendo archivo"):
        line = line.strip()
        if not line:
            continue

        # Lógica para detectar el header de tiempo
        if line.startswith('t='):
            if current_time is not None and current_particles:
                particle_data[current_time] = np.array(current_particles)

            try:
                current_time = float(line.split('=')[1])
                if current_time not in all_timesteps:
                    all_timesteps.append(current_time)
                current_particles = []
            except (ValueError, IndexError):
                continue
        else:
            # Es una línea de datos de partícula
            # Formato nuevo: id;galaxyId;x;y;z;...
            parts = line.split(';')
            if len(parts) >= 5:
                try:
                    # Extraemos las posiciones x, y, z (índices 2, 3, 4)
                    x = float(parts[2])
                    y = float(parts[3])
                    z = float(parts[4])
                    current_particles.append([x, y, z])
                except (ValueError, IndexError):
                    pass

    # Asegúrate de guardar el último paso de tiempo
    if current_time is not None and current_particles:
        particle_data[current_time] = np.array(current_particles)

    # Ordena los pasos de tiempo
    all_timesteps.sort()

    # Se mantiene la lógica original para rellenar frames faltantes por si acaso
    num_particles = 0
    if particle_data:
        num_particles = len(list(particle_data.values())[0])

    for t in all_timesteps:
        if t not in particle_data or particle_data[t].shape[0] < num_particles:
            if all_timesteps.index(t) > 0:
                prev_t = all_timesteps[all_timesteps.index(t) - 1]
                particle_data[t] = particle_data[prev_t]
            else:
                particle_data[t] = np.zeros((num_particles, 3))

    return all_timesteps, particle_data


def animate(timesteps, particle_data, output_filename='galaxy_animation.mp4'):
    """
    Crea y guarda una animación 3D de la simulación de partículas.
    """
    fig = plt.figure(figsize=(10, 8))
    ax = fig.add_subplot(111, projection='3d')

    # Determina los límites de los ejes (lógica original mantenida)
    all_positions = np.vstack([data for data in particle_data.values()])
    min_coords = all_positions.min(axis=0)
    max_coords = all_positions.max(axis=0)

    # Dales un poco de margen (lógica original mantenida)
    ax.set_xlim([min_coords[0] - 1, max_coords[0] + 1])
    ax.set_ylim([min_coords[1] - 1, max_coords[1] + 1])
    ax.set_zlim([min_coords[2] - 1, max_coords[2] + 1])

    ax.set_xlabel('X')
    ax.set_ylabel('Y')
    ax.set_zlabel('Z')

    title = ax.set_title('')

    # --- CAMBIO CLAVE: Crear la barra de progreso antes de la animación ---
    pbar = tqdm(total=len(timesteps), desc="Renderizando video", unit="frame")

    def update(frame):
        # Limpia los datos anteriores (método original ineficiente mantenido como se pidió)
        ax.cla()

        # Re-establece los límites y etiquetas para cada frame
        ax.set_xlim([min_coords[0] - 1, max_coords[0] + 1])
        ax.set_ylim([min_coords[1] - 1, max_coords[1] + 1])
        ax.set_zlim([min_coords[2] - 1, max_coords[2] + 1])
        ax.set_xlabel('X')
        ax.set_ylabel('Y')
        ax.set_zlabel('Z')

        # Obtiene las posiciones para el frame actual
        time = timesteps[frame]
        positions = particle_data[time]

        # Actualiza el gráfico de dispersión
        ax.scatter(positions[:, 0], positions[:, 1], positions[:, 2], s=5, c='blue', alpha=0.7)
        title.set_text(f'Simulación de Galaxias - Tiempo: {time:.3f}')

        # --- CAMBIO CLAVE: Actualizar la barra de progreso en cada frame ---
        pbar.update(1)

        return ax,

    # Crea la animación
    ani = FuncAnimation(fig, update, frames=len(timesteps), blit=False)

    # Guarda la animación
    print("\nGuardando animación... Esto puede tardar unos minutos.")
    try:
        writer = FFMpegWriter(fps=30, metadata=dict(artist='Me'), bitrate=1800)
        ani.save(output_filename, writer=writer, dpi=150)
    except FileNotFoundError:
        print("\n❌ ERROR: No se encontró el programa 'ffmpeg'.")
        print("   Por favor, instálalo y asegúrate de que esté en el PATH de tu sistema.")
    finally:
        # --- CAMBIO CLAVE: Asegurarse de cerrar la barra de progreso siempre ---
        pbar.close()

    print(f"\nAnimación guardada como '{output_filename}'")


if __name__ == '__main__':
    # --- CAMBIO CLAVE: Usar Path para una ruta de archivo más segura ---
    filename = Path(__file__).parent / 'data' / 'sim_dt_0.001.csv'

    print(f"Cargando y analizando datos desde '{filename}'...")
    timesteps, data = parse_simulation_data(filename)

    if not data:
        print("No se encontraron datos válidos en el archivo.")
    else:
        print(f"Datos cargados exitosamente. {len(timesteps)} pasos de tiempo encontrados.")
        animate(timesteps, data)